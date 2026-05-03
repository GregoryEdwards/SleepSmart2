package com.sleepsmart.app.tracking

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.sleepsmart.app.MainActivity
import com.sleepsmart.app.R
import com.sleepsmart.app.SleepSmartApp
import com.sleepsmart.app.audio.RingBuffer
import com.sleepsmart.app.classifier.HmmSmoother
import com.sleepsmart.app.classifier.SleepStageClassifier
import com.sleepsmart.app.core.time.Clock
import com.sleepsmart.app.audio.FeatureExtractor
import com.sleepsmart.app.data.repository.PreferencesRepository
import com.sleepsmart.app.data.repository.SessionRepository
import com.sleepsmart.app.domain.ComputeSleepScoreUseCase
import com.sleepsmart.app.wake.AlarmScheduler
import com.sleepsmart.app.wake.SmartWakeEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import javax.inject.Inject

@AndroidEntryPoint
class SleepTrackingService : LifecycleService() {

    @Inject lateinit var audioProvider: AudioCaptureProvider
    @Inject lateinit var featureExtractor: FeatureExtractor
    @Inject lateinit var classifier: SleepStageClassifier
    @Inject lateinit var smoother: HmmSmoother
    @Inject lateinit var wakeEngine: SmartWakeEngine
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var sessionRepo: SessionRepository
    @Inject lateinit var prefs: PreferencesRepository
    @Inject lateinit var clock: Clock
    @Inject lateinit var trackingState: TrackingStateHolder
    @Inject lateinit var scoreUseCase: ComputeSleepScoreUseCase

    private var sessionJob: Job? = null
    private var sessionId: String? = null
    private var startedAt: Instant? = null
    private var firedAt: Instant? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> startTracking()
            ACTION_STOP -> stopTracking()
        }
        return START_REDELIVER_INTENT
    }

    private fun startTracking() {
        if (sessionJob?.isActive == true) return
        startForegroundCompat()
        sessionJob = lifecycleScope.launch { runSession() }
    }

    private fun stopTracking() {
        sessionJob?.cancel()
        sessionJob = null
        lifecycleScope.launch { finalizeSession() }
    }

    private suspend fun runSession() {
        val pref = prefs.preferences.first()
        val now = clock.now()
        val target = nextTargetWakeAt(pref.targetWakeAtMinutesOfDay, now)
        val session = sessionRepo.startNew(now, target, pref.wakeWindowMinutes)
        sessionId = session.id
        startedAt = now
        classifier.reset()
        smoother.reset()
        wakeEngine.configure(target, pref.wakeWindowMinutes)
        alarmScheduler.schedule(target) // backup

        val capture = audioProvider.current()
        val sampleRate = com.sleepsmart.app.audio.AndroidAudioCapture.SAMPLE_RATE
        val ring = RingBuffer(sampleRate * 30)
        val agg = EpochAggregator(session.id, sessionRepo)

        var epochIndex = 0
        // Demo mode emits 0.5 s = "30 s of real audio". Use a smaller sampling
        // window so synthetic flows still produce visible epochs in 8 minutes.
        val pickPeriodMs: Long = if (pref.demoModeEnabled) 500L else 30_000L

        trackingState.setTracking(session.id, now.toEpochMilliseconds(), 0)

        capture.samples()
            .onEach { ring.write(it) }
            .sample(pickPeriodMs)
            .map { ring.snapshot() }
            .collect { window ->
                if (window.size < sampleRate * 2) return@collect // too few samples
                val features = featureExtractor.extract(window)
                val sessionMinutes = ((clock.now() - (startedAt ?: clock.now())).inWholeSeconds / 60).toInt()
                val raw = classifier.classify(features, epochIndex, sessionMinutes)
                val smoothed = smoother.process(raw)
                agg.add(epochIndex, clock.nowMillis(), smoothed, features)
                if (agg.shouldFlush()) agg.flush()

                val nowInst = clock.now()
                wakeEngine.observe(smoothed, nowInst)?.let { fireAt ->
                    alarmScheduler.schedule(fireAt)
                    firedAt = fireAt
                }

                epochIndex++
                trackingState.setTracking(session.id, now.toEpochMilliseconds(), epochIndex)
            }

        // If the flow ends naturally (cancel), aggregator flushed in finalize.
    }

    private suspend fun finalizeSession() {
        val sid = sessionId ?: run {
            stopForegroundCompat()
            stopSelf()
            return
        }
        val endedAt = clock.now()
        val rows = sessionRepo.epochsFor(sid)
        val pref = prefs.preferences.first()
        val s = scoreUseCase.compute(rows, pref.sleepGoalHours)
        sessionRepo.finishSession(
            sessionId = sid,
            endedAt = endedAt,
            firedAt = firedAt,
            score = s.score,
            timeAsleepMinutes = s.asleepMinutes,
            disturbances = sessionRepo.disturbanceCountFor(sid)
        )
        trackingState.setFinished(sid)
        sessionId = null
        firedAt = null
        stopForegroundCompat()
        stopSelf()
    }

    private fun startForegroundCompat() {
        val notif = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIF_ID,
                notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION") stopForeground(true)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, SleepSmartApp.CHANNEL_TRACKING)
            .setContentTitle(getString(R.string.tracking_notification_title))
            .setContentText(getString(R.string.tracking_notification_body))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pi)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (sessionJob?.isActive == true) {
            // Best effort: cancel and finalize off the main thread is not possible
            // here, so we rely on stopTracking() callers to do it before destroy.
            sessionJob?.cancel()
        }
    }

    private fun nextTargetWakeAt(targetMinutes: Int, now: Instant): Instant {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = now.toEpochMilliseconds()
            set(java.util.Calendar.HOUR_OF_DAY, targetMinutes / 60)
            set(java.util.Calendar.MINUTE, targetMinutes % 60)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= now.toEpochMilliseconds()) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return Instant.fromEpochMilliseconds(cal.timeInMillis)
    }

    companion object {
        const val ACTION_START = "com.sleepsmart.app.action.START_TRACKING"
        const val ACTION_STOP = "com.sleepsmart.app.action.STOP_TRACKING"
        private const val NOTIF_ID = 9001
    }
}
