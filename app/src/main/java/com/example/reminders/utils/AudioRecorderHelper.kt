package com.example.reminders.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.IOException
import java.util.Timer
import java.util.TimerTask

class AudioRecorderHelper(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFilePath: String? = null
    private var currentlyPlayingPath: String? = null
    private var progressUpdateTimer: Timer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

    private val _currentPlayingPath = MutableStateFlow<String?>(null)
    val currentPlayingPath: StateFlow<String?> = _currentPlayingPath

    fun startRecording() {
        val audioFile = File.createTempFile("audio", ".3gp", context.cacheDir)
        audioFilePath = audioFile.absolutePath

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFilePath)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                // Handle exception
            }
        }
    }

    fun stopRecording(): String? {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        return audioFilePath
    }

    fun playAudio(filePath: String) {
        if (mediaPlayer != null && filePath == currentlyPlayingPath) {
            if (mediaPlayer!!.isPlaying) {
                mediaPlayer?.pause()
                _isPlaying.value = false
                stopProgressTimer()
            } else {
                mediaPlayer?.start()
                _isPlaying.value = true
                startProgressTimer()
            }
        } else {
            stopPlaying()
            currentlyPlayingPath = filePath
            _currentPlayingPath.value = filePath
            mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(filePath)
                    prepare()
                    _duration.value = duration
                    start()
                    _isPlaying.value = true
                    setOnCompletionListener {
                        stopPlaying()
                    }
                    startProgressTimer()
                } catch (e: IOException) {
                    stopPlaying()
                }
            }
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer?.seekTo(position)
        _progress.value = position
    }

    private fun startProgressTimer() {
        progressUpdateTimer = Timer()
        progressUpdateTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        _progress.value = it.currentPosition
                    }
                }
            }
        }, 0, 50)
    }

    private fun stopProgressTimer() {
        progressUpdateTimer?.cancel()
        progressUpdateTimer = null
    }

    fun stopPlaying() {
        stopProgressTimer()
        mediaPlayer?.release()
        mediaPlayer = null
        currentlyPlayingPath = null
        _isPlaying.value = false
        _progress.value = 0
        _duration.value = 0
        _currentPlayingPath.value = null
    }
}
