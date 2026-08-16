package com.ame38.autocalendaralarms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmDismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        context.stopService(Intent(context, AlarmSoundService::class.java))
    }
}
