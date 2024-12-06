package com.example.hearingaids

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import com.konovalov.vad.silero.config.SampleRate

private val SAMPLE_RATE = SampleRate.SAMPLE_RATE_16K
private const val SAMPLE_RATE_INT = (6400 * 2).toInt()
private val FRAME_SIZE = FrameSize.FRAME_SIZE_512
private val MODE = Mode.NORMAL

private const val RECORD_AUDIO_PERMISSION_REQUEST_CODE = 1

class MainActivity : AppCompatActivity() {

    private lateinit var equalizer: Equalizer
    private var audioRecord: AudioRecord? = null
    private var isAudioMonitoringEnabled = false
    private lateinit var toggleAudioButton: Button
    private lateinit var latencyTextView: TextView
    private lateinit var averageLatencyTextView: TextView
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private lateinit var noiseCancellationSwitch: Switch

    private val seekBars = mutableListOf<SeekBar>()

    // For latency measurement
    private var startTimestamp = 0L
    private var endTimestamp = 0L
    private val latencyValues = mutableListOf<Double>()
    private val MAX_LATENCY_VALUES = 100

    // VadSilero for speech detection
    private lateinit var vadSilero: VadSilero

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toggleAudioButton = findViewById(R.id.toggleButton)
        latencyTextView = findViewById(R.id.latencyTextView)
        averageLatencyTextView = findViewById(R.id.averageLatencyTextView)
        noiseCancellationSwitch = findViewById(R.id.noiseCancellationSwitch)

        // Check if RECORD_AUDIO permission is granted, if not, request it
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_REQUEST_CODE
            )
        } else {
            startAudioMonitoring()
        }

        setupToggleButton()
        setupEqualizer()

        // Initialize VadSilero
        vadSilero = VadSilero(applicationContext, SAMPLE_RATE, FRAME_SIZE, MODE)
    }

    private fun setupToggleButton() {
        updateToggleButtonText()

        toggleAudioButton.setOnClickListener {
            isAudioMonitoringEnabled = !isAudioMonitoringEnabled
            if (isAudioMonitoringEnabled) {
                startAudioMonitoring()
            } else {
                stopAudioMonitoring()
            }
            updateToggleButtonText()
        }
    }

    private fun updateToggleButtonText() {
        if (this::toggleAudioButton.isInitialized) {
            toggleAudioButton.text = if (isAudioMonitoringEnabled) "Stop Monitoring" else "Start Monitoring"
        }
    }

    private fun setupEqualizer() {
        val presetSpinner = findViewById<Spinner>(R.id.presetSpinner)
        val presetNames = arrayOf("Flat", "Low Freq", "High Freq")
        val presetAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, presetNames)
        presetAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        presetSpinner.adapter = presetAdapter

        presetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val preset = presetNames[position]
                applyPreset(preset)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Initialize seek bars and set listeners
        val numBands = equalizer.numberOfBands
        val levelRange = equalizer.bandLevelRange
        val minLevel = levelRange[0]

        seekBars.add(findViewById(R.id.band1SeekBar))
        seekBars.add(findViewById(R.id.band2SeekBar))
        seekBars.add(findViewById(R.id.band3SeekBar))
        seekBars.add(findViewById(R.id.band4SeekBar))
        seekBars.add(findViewById(R.id.band5SeekBar))

        for (i in 0 until numBands) {
            val seekBar = seekBars[i]
            val maxSliderValue = 100 // Set the maximum value
            seekBar.max = maxSliderValue
            seekBar.progress = 50 // Set the middle position to full volume
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val multiplier = 2.0 // Increase volume by 100%
                    equalizer.setBandLevel(i.toShort(), (minLevel * multiplier).toInt().toShort())
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
    }

    private fun applyPreset(preset: String) {
        when (preset) {
            "Flat" -> {
                seekBars.forEach { it.progress = 50 }
            }
            "Low Freq" -> {
                seekBars[0].progress = 80
                seekBars[1].progress = 80
                seekBars[2].progress = 50
                seekBars[3].progress = 35
                seekBars[4].progress = 35
            }
            "High Freq" -> {
                seekBars[0].progress = 35
                seekBars[1].progress = 35
                seekBars[2].progress = 50
                seekBars[3].progress = 80
                seekBars[4].progress = 80
            }
        }
    }

    private var audioTrack: AudioTrack? = null

    private fun startAudioMonitoring() {
        // Check for RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Permission is granted, proceed with audio recording setup
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE_INT,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        // Use a smaller buffer size to reduce latency
        val bufferSizeInBytes = minBufferSize / 2

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE_INT,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSizeInBytes
        )

        audioRecord?.startRecording()

        // Initialize AudioTrack for playback
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(SAMPLE_RATE_INT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val audioTrackBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE_INT,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        // Use a smaller buffer size to reduce latency
        audioTrack = AudioTrack(
            audioAttributes,
            audioFormat,
            audioTrackBufferSize / 2,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        audioTrack?.play()

        // Set up Equalizer for AudioTrack
        equalizer = Equalizer(0, audioTrack!!.audioSessionId)
        equalizer.enabled = true

        val audioProcessingThread = Thread {
            val buffer = ShortArray(bufferSizeInBytes)

            while (isAudioMonitoringEnabled) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                if (bytesRead > 0) {
                    // Apply noise cancellation using VadSilero if enabled
                    if (noiseCancellationSwitch.isChecked) {
                        val isSpeech = vadSilero.isSpeech(buffer)
                        if (isSpeech) {
                            audioTrack?.write(buffer, 0, bytesRead) // Write speech directly
                        } else {
                            // Do not write non-speech (noise) to audioTrack
                        }
                    } else {
                        // If noise cancellation is disabled, write raw audio to audioTrack
                        audioTrack?.write(buffer, 0, bytesRead)
                    }

                    // Calculate and update latency in milliseconds
                    endTimestamp = System.nanoTime()
                    val latency = getLatency()
                    updateLatency(latency)
                    startTimestamp = System.nanoTime()

                }
            }

            // Ensure we stop and release only if the audioRecord is in a valid state
            audioRecord?.let {
                if (it.state == AudioRecord.STATE_INITIALIZED) {
                    it.stop()
                    it.release()
                }
            }

            audioRecord = null

            audioTrack?.let {
                if (it.state == AudioTrack.STATE_INITIALIZED) {
                    it.stop()
                    it.release()
                }
            }

            audioTrack = null
        }

        audioProcessingThread.start()
    }

    private fun getLatency(): Double {
        return (endTimestamp - startTimestamp) / 1_000_000.0 // Convert nanoseconds to milliseconds
    }

    private fun updateLatency(latency: Double) {
        // Update real-time latency TextView
        runOnUiThread {
            latencyTextView.text = "Real-time Latency: %.2f ms".format(latency)
        }

        // Add latency to the list for average calculation
        latencyValues.add(latency)

        // Calculate average latency every MEASUREMENT_INTERVAL seconds
        if (latencyValues.size >= MAX_LATENCY_VALUES) {
            val averageLatency = latencyValues.average()
            runOnUiThread {
                averageLatencyTextView.text = "Average Latency: %.2f ms".format(averageLatency)
            }

            // Clear latencyValues after calculating average latency
            latencyValues.clear()
        }
    }

    private fun stopAudioMonitoring() {
        isAudioMonitoringEnabled = false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startAudioMonitoring()
            } else {
                Log.e("PermissionDenied", "Record audio permission denied")
            }
        }
    }
}