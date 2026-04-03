package com.solomonprime.tricorder.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.sqrt

class AcousticViewModel(application: Application) : AndroidViewModel(application) {

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null

    private val _decibels = MutableStateFlow(0f)
    val decibels = _decibels.asStateFlow()

    private val _waveform = MutableStateFlow<List<Float>>(emptyList())
    val waveform = _waveform.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (recordingJob?.isActive == true) return

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
                audioRecord?.startRecording()
                recordingJob = viewModelScope.launch(Dispatchers.IO) {
                    val buffer = ShortArray(bufferSize)
                    val history = mutableListOf<Float>()
                    
                    while (isActive) {
                        val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (readSize > 0) {
                            var sum = 0.0
                            for (i in 0 until readSize) {
                                sum += buffer[i] * buffer[i]
                            }
                            val amplitude = sqrt(sum / readSize)
                            
                            val db = if (amplitude > 0) 20 * log10(amplitude) else 0.0
                            _decibels.value = db.toFloat()

                            val normalizedDb = (db.toFloat() / 100f).coerceIn(0f, 1f)
                            history.add(normalizedDb)
                            if (history.size > 50) history.removeAt(0)
                            _waveform.value = history.toList()
                        }
                        delay(50)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioRecord = null
    }

    override fun onCleared() {
        super.onCleared()
        stopRecording()
    }
}
