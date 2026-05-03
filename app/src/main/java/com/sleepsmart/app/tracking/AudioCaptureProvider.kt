package com.sleepsmart.app.tracking

import com.sleepsmart.app.audio.AndroidAudioCapture
import com.sleepsmart.app.audio.AudioCapture
import com.sleepsmart.app.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Picks live or synthetic AudioCapture based on user prefs.
 */
@Singleton
class AudioCaptureProvider @Inject constructor(
    private val live: AndroidAudioCapture,
    private val synthetic: SyntheticAudioCapture,
    private val prefs: PreferencesRepository
) {
    suspend fun current(): AudioCapture {
        val p = prefs.preferences.first()
        return if (p.demoModeEnabled) synthetic else live
    }
}
