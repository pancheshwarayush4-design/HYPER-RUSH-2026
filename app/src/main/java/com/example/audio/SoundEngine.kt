package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.*
import kotlin.math.*

class SoundEngine {
    private val sampleRate = 22050
    private var isMuted = false
    private var masterVolume = 0.85f
    private var isMusicMuted = false

    private val audioScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var musicJob: Job? = null

    // Engine loop audio track
    private var engineTrack: AudioTrack? = null
    private var isEngineRunning = false
    private var targetRpmRatio = 0.15f
    private var currentRpmRatio = 0.15f
    private var lastSpeedKmh = 0f
    private var currentGear = 1

    init {
        try {
            initEngineSynth()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    fun setMusicMuted(muted: Boolean) {
        isMusicMuted = muted
        if (muted) {
            stopMusic()
        } else {
            startMusic()
        }
    }

    fun setVolume(vol: Float) {
        masterVolume = vol.coerceIn(0f, 1f)
    }

    private fun initEngineSynth() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, 2048)

        engineTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        engineTrack?.play()
        isEngineRunning = true

        audioScope.launch {
            val chunk = ShortArray(512)
            var phase1 = 0.0
            var phase2 = 0.0
            var subPhase = 0.0

            while (isEngineRunning) {
                if (isMuted || masterVolume <= 0.01f) {
                    chunk.fill(0)
                    engineTrack?.write(chunk, 0, chunk.size)
                    delay(25)
                    continue
                }

                currentRpmRatio += (targetRpmRatio - currentRpmRatio) * 0.18f
                // Multi-gear pitch synthesis: simulate aggressive sports exhaust
                val baseFreq = 48.0 + (currentRpmRatio * 260.0) // 48Hz idle rumble to 308Hz redline
                val vol = masterVolume * 0.32f

                for (i in chunk.indices) {
                    val step1 = 2.0 * Math.PI * baseFreq / sampleRate
                    val step2 = 2.0 * Math.PI * (baseFreq * 1.5) / sampleRate
                    val subStep = 2.0 * Math.PI * (baseFreq * 0.5) / sampleRate

                    phase1 = (phase1 + step1) % (2.0 * Math.PI)
                    phase2 = (phase2 + step2) % (2.0 * Math.PI)
                    subPhase = (subPhase + subStep) % (2.0 * Math.PI)

                    // Distorted sawtooth-sine mix + sub-bass pulse for throaty roar
                    val s1 = sin(phase1)
                    val s2 = (phase1 / Math.PI) - 1.0 // sawtooth
                    val s3 = sin(phase2) * 0.35
                    val sub = sin(subPhase) * 0.3
                    val sample = ((s1 * 0.35 + s2 * 0.35 + s3 + sub) * vol).coerceIn(-1.0, 1.0)
                    chunk[i] = (sample * 32767.0).toInt().toShort()
                }

                engineTrack?.write(chunk, 0, chunk.size)
            }
        }
    }

    fun updateEngineAudio(speedKmh: Float, throttleInput: Float) {
        // Multi-gear transmission simulation
        val gearMaxSpeeds = listOf(22f, 45f, 70f, 100f, 140f)
        var gear = 1
        for (i in gearMaxSpeeds.indices) {
            if (speedKmh <= gearMaxSpeeds[i]) {
                gear = i + 1
                break
            }
            if (i == gearMaxSpeeds.size - 1) gear = 5
        }

        val prevGearMax = if (gear == 1) 0f else gearMaxSpeeds[gear - 2]
        val curGearMax = gearMaxSpeeds[gear - 1]
        val gearProgress = ((speedKmh - prevGearMax) / (curGearMax - prevGearMax)).coerceIn(0f, 1f)

        val targetRatio = 0.12f + (gearProgress * 0.72f) + (throttleInput * 0.16f)
        targetRpmRatio = targetRatio.coerceIn(0.08f, 1.0f)

        // Turbo blow-off valve sound when releasing throttle after high speed
        if (lastSpeedKmh > 65f && throttleInput < 0.05f && speedKmh < lastSpeedKmh) {
            playTurboBlowoff()
        }
        lastSpeedKmh = speedKmh
        currentGear = gear
    }

    fun playTurboBlowoff() {
        if (isMuted) return
        audioScope.launch {
            playNoiseBurst(durationMs = 180, volume = 0.35f * masterVolume, isHeavy = false)
        }
    }

    fun playSpringBounce() {
        if (isMuted) return
        playProceduralTone(listOf(180.0, 320.0, 560.0), durationMs = 120, type = ToneType.BRIGHT)
    }

    fun playBoostPad() {
        if (isMuted) return
        audioScope.launch {
            playProceduralTone(listOf(440.0, 660.0, 880.0, 1100.0), durationMs = 150, type = ToneType.CRYSTAL)
            playNoiseBurst(durationMs = 220, volume = 0.4f * masterVolume)
        }
    }

    fun playTireScreech() {
        if (isMuted) return
        playProceduralTone(listOf(1800.0, 1950.0, 1720.0), durationMs = 90, type = ToneType.BRIGHT)
    }

    fun playChestUnlock() {
        if (isMuted) return
        audioScope.launch {
            val fanfares = listOf(523.25, 659.25, 783.99, 1046.50, 1318.51, 1567.98)
            for (f in fanfares) {
                playSingleTone(f, 90, 0.45f * masterVolume, ToneType.CRYSTAL)
                delay(60)
            }
        }
    }

    fun playCoin() {
        if (isMuted) return
        playProceduralTone(listOf(1046.50, 1318.51), durationMs = 85, type = ToneType.SINE)
    }

    fun playDiamond() {
        if (isMuted) return
        playProceduralTone(listOf(1318.51, 1567.98, 2093.00), durationMs = 120, type = ToneType.CRYSTAL)
    }

    fun playFuel() {
        if (isMuted) return
        playProceduralTone(listOf(523.25, 659.25, 783.99, 1046.50), durationMs = 140, type = ToneType.SINE)
    }

    fun playNitro() {
        if (isMuted) return
        audioScope.launch {
            playNoiseBurst(durationMs = 260, volume = 0.48f * masterVolume)
        }
    }

    fun playCrash() {
        if (isMuted) return
        audioScope.launch {
            playNoiseBurst(durationMs = 480, volume = 0.85f * masterVolume, isHeavy = true)
        }
    }

    fun playLanding() {
        if (isMuted) return
        playProceduralTone(listOf(95.0, 68.0), durationMs = 75, type = ToneType.BASS_THUD)
    }

    fun playStuntFanfare() {
        if (isMuted) return
        playProceduralTone(listOf(659.25, 783.99, 1046.50, 1318.51), durationMs = 200, type = ToneType.BRIGHT)
    }

    fun playVictory() {
        if (isMuted) return
        audioScope.launch {
            val notes = listOf(523.25, 659.25, 783.99, 1046.50, 1318.51)
            for (n in notes) {
                playSingleTone(n, 120, 0.4f * masterVolume)
                delay(70)
            }
        }
    }

    fun playButtonClick() {
        if (isMuted) return
        playProceduralTone(listOf(880.0), durationMs = 35, type = ToneType.SINE)
    }

    fun startMusic() {
        if (isMusicMuted || musicJob?.isActive == true) return
        musicJob = audioScope.launch {
            val chords = listOf(
                listOf(220.0, 261.63, 329.63), // Am
                listOf(174.61, 220.0, 261.63), // F
                listOf(196.0, 246.94, 293.66), // G
                listOf(164.81, 196.0, 246.94)  // Em
            )
            while (isActive && !isMusicMuted) {
                for (chord in chords) {
                    for (step in 0..7) {
                        if (!isActive || isMusicMuted) break
                        val note = chord[step % chord.size] * (if (step % 2 == 0) 1.0 else 2.0)
                        if (!isMuted && masterVolume > 0.05f) {
                            playSingleTone(note, 130, 0.12f * masterVolume)
                        }
                        delay(140)
                    }
                }
            }
        }
    }

    fun stopMusic() {
        musicJob?.cancel()
        musicJob = null
    }

    private fun playProceduralTone(freqs: List<Double>, durationMs: Int, type: ToneType) {
        audioScope.launch {
            val stepDuration = durationMs / freqs.size
            for (f in freqs) {
                playSingleTone(f, stepDuration, 0.35f * masterVolume, type)
            }
        }
    }

    private suspend fun playSingleTone(
        freq: Double,
        durationMs: Int,
        volume: Float,
        type: ToneType = ToneType.SINE
    ) = withContext(Dispatchers.Default) {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(64)
        val buffer = ShortArray(numSamples)
        var phase = 0.0
        val step = 2.0 * Math.PI * freq / sampleRate

        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            val envelope = (1.0 - t).pow(1.5)
            phase = (phase + step) % (2.0 * Math.PI)
            val raw = when (type) {
                ToneType.SINE -> sin(phase)
                ToneType.CRYSTAL -> sin(phase) + 0.5 * sin(phase * 2.0) + 0.25 * sin(phase * 3.0)
                ToneType.BRIGHT -> sin(phase) + 0.3 * sin(phase * 4.0)
                ToneType.BASS_THUD -> sin(phase) * (1.0 - t)
            }
            val sample = (raw * envelope * volume).coerceIn(-1.0, 1.0)
            buffer[i] = (sample * 32767.0).toInt().toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        track.play()
        delay(durationMs.toLong() + 30)
        track.release()
    }

    private suspend fun playNoiseBurst(durationMs: Int, volume: Float, isHeavy: Boolean = false) = withContext(Dispatchers.Default) {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(128)
        val buffer = ShortArray(numSamples)
        var last = 0.0

        for (i in 0 until numSamples) {
            val t = i.toDouble() / numSamples
            val envelope = (1.0 - t).pow(if (isHeavy) 0.8 else 2.0)
            val white = (Math.random() * 2.0) - 1.0
            last = if (isHeavy) (last * 0.85 + white * 0.15) else white
            val sample = (last * envelope * volume).coerceIn(-1.0, 1.0)
            buffer[i] = (sample * 32767.0).toInt().toShort()
        }

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        track.play()
        delay(durationMs.toLong() + 40)
        track.release()
    }

    fun release() {
        isEngineRunning = false
        stopMusic()
        audioScope.cancel()
        try {
            engineTrack?.stop()
            engineTrack?.release()
        } catch (_: Exception) {}
        engineTrack = null
    }

    private enum class ToneType {
        SINE,
        CRYSTAL,
        BRIGHT,
        BASS_THUD
    }
}
