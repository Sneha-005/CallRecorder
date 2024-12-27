package com.example.myapplication

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var outputFile: File? = null
    private lateinit var recordingsRecyclerView: RecyclerView
    private lateinit var recordingsAdapter: RecordingsAdapter
    private val recordingsList = mutableListOf<File>()
    private var mediaPlayer: MediaPlayer? = null

    private fun playRecording(file: File) {
        if (!file.exists()) {
            Toast.makeText(this, "File does not exist", Toast.LENGTH_SHORT).show()
            return
        }
        stopCurrentRecording()
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    Toast.makeText(this@MainActivity, "Playback completed", Toast.LENGTH_SHORT).show()
                }
            }
            Toast.makeText(this, "Playing: ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error playing recording: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun stopCurrentRecording() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
                release()
            }
        }
        mediaPlayer = null
    }
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                if (isGranted) {
                    Toast.makeText(this, "$permissionName granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "$permissionName denied", Toast.LENGTH_SHORT).show()
                }
            }
        }

    private val callStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                when (state) {
                    TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                        startRecording()
                    }
                    TelephonyManager.EXTRA_STATE_IDLE -> {
                        stopRecording()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recordingsRecyclerView = findViewById(R.id.rvRecordings)
        recordingsRecyclerView.layoutManager = LinearLayoutManager(this)
        recordingsAdapter = RecordingsAdapter(recordingsList)
        recordingsRecyclerView.adapter = recordingsAdapter
        val numberOfRecordings = getNumberOfRecordings()
        Toast.makeText(this, "Number of recordings: $numberOfRecordings", Toast.LENGTH_SHORT).show()
        requestPermissions()

        val filter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        registerReceiver(callStateReceiver, filter)

        loadRecordings()
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.WAKE_LOCK,
            Manifest.permission.FOREGROUND_SERVICE
        )

        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest)
        }
    }

    private fun startRecording() {
        if (isRecording) return

        try {
            outputFile = createOutputFile()
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.
                VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile!!.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Recording failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        if (!isRecording) return

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
            loadRecordings()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error stopping recording", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createOutputFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        return File.createTempFile("call_recording_${timeStamp}_", ".3gp", storageDir)
    }

    private fun loadRecordings() {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val files = storageDir?.listFiles { file ->
            file.isFile && file.name.startsWith("call_recording_") && file.name.endsWith(".3gp")
        }
        recordingsList.clear()
        if (files != null) {
            recordingsList.addAll(files)
        }
        recordingsAdapter.notifyDataSetChanged()
    }
    private fun getNumberOfRecordings(): Int {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val files = storageDir?.listFiles { file ->
            file.isFile && file.name.startsWith("call_recording_") && file.name.endsWith(".3gp")
        }
        return files?.size ?: 0
    }
    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
        unregisterReceiver(callStateReceiver)
        stopRecording()
    }

    private inner class RecordingsAdapter(private val recordings: List<File>) :
        RecyclerView.Adapter<RecordingsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val fileNameTextView: TextView = view.findViewById(R.id.tvCallerName)
            val playButton: Button = view.findViewById(R.id.btnPlayRecording)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_recording, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val recording = recordings[position]
            holder.fileNameTextView.text = recording.name
            holder.playButton.setOnClickListener {
                playRecording(recording)
            }
        }

        override fun getItemCount() = recordings.size
    }
}