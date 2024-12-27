package com.example.myapplication

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.example.myapplication.AppDatabase
import com.example.myapplication.Recording
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CallRecordingService : Service() {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFilePath: String? = null
    private lateinit var database: AppDatabase

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "app_database"
        ).build()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val phoneNumber = intent?.getStringExtra("PHONE_NUMBER") ?: "Unknown"
        if (checkPermissions()) {
            startRecording(phoneNumber)
        } else {
            Log.e("CallRecordingService", "Permissions not granted")
        }
        return START_STICKY
    }

    private fun checkPermissions(): Boolean {
        val recordAudioPermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        val storagePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        return recordAudioPermission == PackageManager.PERMISSION_GRANTED && storagePermission == PackageManager.PERMISSION_GRANTED
    }

    private fun startRecording(phoneNumber: String) {
        try {
            val directory = File(getExternalFilesDir(null), "CallRecordings")
            if (!directory.exists()) directory.mkdirs()

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            outputFilePath = File(directory, "CALL_${timeStamp}.3gp").absolutePath

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(outputFilePath)
                prepare()
                start()
            }
            Log.d("CallRecordingService", "Recording started")
        } catch (e: Exception) {
            Log.e("CallRecordingService", "Error starting recording: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRecording()
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            saveRecording()
            Log.d("CallRecordingService", "Recording stopped")
        } catch (e: Exception) {
            Log.e("CallRecordingService", "Error stopping recording: ${e.message}")
        }
    }

    private fun saveRecording() {

        val recording = Recording(
            callerName = "Unknown Caller",
            filePath = outputFilePath ?: "",
            dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )

        CoroutineScope(Dispatchers.IO).launch {
            database.recordingDao().insertRecording(recording)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}