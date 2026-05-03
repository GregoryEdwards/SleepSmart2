package com.sleepsmart.app.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

interface AudioCapture {
    /** Cold flow. One subscriber. Cancellation stops the AudioRecord. */
    fun samples(): Flow<ShortArray>
}

class AudioCaptureException(message: String, cause: Throwable? = null) :
    RuntimeException(message, cause)

@Singleton
class AndroidAudioCapture @Inject constructor(
    @ApplicationContext private val context: Context
) : AudioCapture {

    @SuppressLint("MissingPermission")
    override fun samples(): Flow<ShortArray> = callbackFlow {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            close(AudioCaptureException("RECORD_AUDIO permission not granted"))
            return@callbackFlow
        }

        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        if (minBuf <= 0) {
            close(AudioCaptureException("AudioRecord.getMinBufferSize returned $minBuf"))
            return@callbackFlow
        }
        val bufBytes = maxOf(minBuf, BUFFER_FRAMES * 2)
        val source = if (Build.VERSION.SDK_INT >= 24) {
            MediaRecorder.AudioSource.UNPROCESSED
        } else {
            MediaRecorder.AudioSource.MIC
        }

        val recorder = try {
            AudioRecord(source, SAMPLE_RATE, CHANNEL, ENCODING, bufBytes)
        } catch (t: Throwable) {
            close(AudioCaptureException("Failed to create AudioRecord", t))
            return@callbackFlow
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            close(AudioCaptureException("AudioRecord not initialized"))
            return@callbackFlow
        }

        try {
            recorder.startRecording()
        } catch (t: Throwable) {
            recorder.release()
            close(AudioCaptureException("startRecording failed", t))
            return@callbackFlow
        }

        // Reusable read buffer.
        val readBuf = ShortArray(BUFFER_FRAMES)
        val thread = Thread({
            try {
                while (!Thread.currentThread().isInterrupted) {
                    val n = recorder.read(readBuf, 0, readBuf.size)
                    if (n <= 0) {
                        if (n == AudioRecord.ERROR_INVALID_OPERATION ||
                            n == AudioRecord.ERROR_BAD_VALUE
                        ) {
                            close(AudioCaptureException("AudioRecord.read error code $n"))
                            return@Thread
                        }
                        continue
                    }
                    // Copy out: send a slice, never the reusable buffer itself.
                    val out = ShortArray(n)
                    System.arraycopy(readBuf, 0, out, 0, n)
                    val sent = trySend(out)
                    if (sent.isFailure && !sent.isClosed) {
                        // backpressure; skip
                    }
                }
            } catch (t: InterruptedException) {
                // expected on cancellation
            } catch (t: Throwable) {
                close(AudioCaptureException("audio thread crashed", t))
            }
        }, "AudioCaptureThread")
        thread.isDaemon = true
        thread.start()

        awaitClose {
            thread.interrupt()
            runCatching { recorder.stop() }
            runCatching { recorder.release() }
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        const val SAMPLE_RATE = 8000
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val BUFFER_FRAMES = 4096   // ~512 ms
    }
}
