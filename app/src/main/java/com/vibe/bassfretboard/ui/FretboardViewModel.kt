package com.vibe.bassfretboard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vibe.bassfretboard.audio.AudioEngine
import com.vibe.bassfretboard.audio.AudioTrackEngine
import com.vibe.bassfretboard.model.FretPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PressedNote(
    val position: FretPosition,
    val startNanos: Long,
)

class FretboardViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)
    private val engine: AudioEngine = AudioTrackEngine()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _pressedNotes = MutableStateFlow<Map<Long, PressedNote>>(emptyMap())
    val pressedNotes: StateFlow<Map<Long, PressedNote>> = _pressedNotes

    private val engineHandles = mutableMapOf<Long, Long>()

    fun onPointerDown(pointerId: Long, position: FretPosition) {
        engine.start()
        val handle = engine.noteOn(position.midi)
        engineHandles[pointerId] = handle
        _pressedNotes.value = _pressedNotes.value + (pointerId to PressedNote(position, System.nanoTime()))
    }

    fun onPointerMove(pointerId: Long, position: FretPosition) {
        val current = _pressedNotes.value[pointerId]?.position ?: return
        if (current.stringIndex != position.stringIndex || current.fret != position.fret) {
            onPointerUp(pointerId)
            onPointerDown(pointerId, position)
        }
    }

    fun onPointerUp(pointerId: Long) {
        engineHandles.remove(pointerId)?.let { engine.noteOff(it) }
        _pressedNotes.value = _pressedNotes.value - pointerId
    }

    fun setTuningId(id: String) = viewModelScope.launch { settingsRepository.setTuningId(id) }

    fun setFretCount(count: Int) = viewModelScope.launch { settingsRepository.setFretCount(count) }

    fun setVertical(vertical: Boolean) = viewModelScope.launch { settingsRepository.setVertical(vertical) }

    fun setLeftHanded(leftHanded: Boolean) = viewModelScope.launch { settingsRepository.setLeftHanded(leftHanded) }

    fun setShowNoteNames(show: Boolean) = viewModelScope.launch { settingsRepository.setShowNoteNames(show) }

    override fun onCleared() {
        engine.allNotesOff()
        engine.stop()
        super.onCleared()
    }
}
