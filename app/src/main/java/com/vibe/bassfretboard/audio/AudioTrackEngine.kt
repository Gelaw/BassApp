package com.vibe.bassfretboard.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Process
import com.vibe.bassfretboard.model.Notes
import java.util.Random
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.concurrent.thread
import kotlin.math.tanh

class AudioTrackEngine(
    private val maxVoices: Int = 8,
    override val sampleRate: Int = 44100,
) : AudioEngine {

    private sealed interface EngineEvent
    private data class NoteOnEvent(val handle: Long, val midi: Int, val velocity: Float) : EngineEvent
    private data class NoteOffEvent(val handle: Long) : EngineEvent
    private object AllOffEvent : EngineEvent

    private class Voice(
        val handle: Long,
        val oscillator: KarplusStrongVoice,
        val velocity: Float,
    ) {
        var gain = 1f
        var releasing = false
        var active = true
    }

    private val events = ConcurrentLinkedQueue<EngineEvent>()
    private val voices = mutableListOf<Voice>()
    private var nextHandle = 1L
    private var audioTrack: AudioTrack? = null
    private var renderThread: Thread? = null
    @Volatile private var running = false

    override fun start() {
        if (running) return
        running = true
        val track = createTrack()
        audioTrack = track
        track.play()
        renderThread = thread(name = "bass-audio-render", priority = Thread.MAX_PRIORITY) {
            renderLoop(track)
        }
    }

    override fun stop() {
        running = false
        renderThread?.join(1000)
        renderThread = null
        audioTrack?.let {
            it.stop()
            it.release()
        }
        audioTrack = null
        synchronized(voices) { voices.clear() }
    }

    override fun noteOn(midi: Int, velocity: Float): Long {
        val handle = synchronized(this) { nextHandle++ }
        events.add(NoteOnEvent(handle, midi, velocity))
        return handle
    }

    override fun noteOff(handle: Long) {
        events.add(NoteOffEvent(handle))
    }

    override fun allNotesOff() {
        events.add(AllOffEvent)
    }

    private fun createTrack(): AudioTrack {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(minBuffer * 2)
                .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                attributes,
                format,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuffer * 2,
                AudioTrack.MODE_STREAM,
            )
        }
    }

    private fun renderLoop(track: AudioTrack) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        val blockFrames = 256
        val mix = DoubleArray(blockFrames)
        val pcm = ShortArray(blockFrames)
        val releaseStep = 1.0 / (sampleRate * RELEASE_SECONDS)
        while (running) {
            drainEvents()
            val activeVoices = synchronized(voices) { voices.filter { it.active } }
            for (i in 0 until blockFrames) {
                var sum = 0.0
                for (v in activeVoices) {
                    if (v.releasing) {
                        v.gain -= releaseStep
                        if (v.gain <= 0f) {
                            v.gain = 0f
                            v.active = false
                        }
                    }
                    sum += v.oscillator.next() * v.gain * v.velocity
                }
                mix[i] = sum
            }
            for (i in 0 until blockFrames) {
                val clipped = tanh(mix[i])
                pcm[i] = (clipped * Short.MAX_VALUE).toInt().toShort()
            }
            var written = 0
            while (written < blockFrames) {
                val n = track.write(pcm, written, blockFrames - written)
                if (n <= 0) break
                written += n
            }
        }
    }

    private fun drainEvents() {
        while (true) {
            when (val e = events.poll() ?: return) {
                is NoteOnEvent -> startVoice(e.handle, e.midi, e.velocity)
                is NoteOffEvent ->
                    synchronized(voices) { voices.firstOrNull { it.handle == e.handle }?.releasing = true }
                is AllOffEvent ->
                    synchronized(voices) { voices.forEach { it.releasing = true } }
            }
        }
    }

    private fun startVoice(handle: Long, midi: Int, velocity: Float) {
        synchronized(voices) {
            if (voices.count { it.active } >= maxVoices) {
                val victim = voices.filter { it.active }.minByOrNull { it.gain } ?: return
                victim.active = false
                voices.remove(victim)
            }
            val osc = KarplusStrongVoice(sampleRate.toDouble(), Random())
            osc.frequency = Notes.frequencyOf(midi)
            osc.blend = 0.0
            osc.stretch = stretchFor(midi)
            osc.excitement = 1.0
            osc.pluck()
            voices.add(Voice(handle, osc, velocity))
        }
    }

    private fun stretchFor(midi: Int): Double {
        val t = ((midi - 24) / 96.0).coerceIn(0.0, 1.0)
        return 0.96 - 0.35 * t
    }

    companion object {
        private const val RELEASE_SECONDS = 0.08
    }
}
