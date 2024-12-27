package com.example.myapplication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        val phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: "Unknown"

        if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
            val serviceIntent = Intent(context, CallRecordingService::class.java).apply {
                putExtra("PHONE_NUMBER", phoneNumber)
            }
            context.startService(serviceIntent)
        } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
            context.stopService(Intent(context, CallRecordingService::class.java))
        }
    }
}