package com.ame38.autocalendaralarms

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

// a plain notification's default sound is easy to sleep through and doesn't
// feel like "an alarm going off" - this plays a looping tone on the alarm
// stream (so it isn't silenced the way notification sound can be) plus a
// repeating vibration, until the user dismisses it or a timeout hits
class AlarmSoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoStopRunnable = Runnable { stopSelf() }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val eventId = intent?.getLongExtra(AlarmReceiver.EXTRA_EVENT_ID, -1L) ?: -1L
        val title = intent?.getStringExtra(AlarmReceiver.EXTRA_EVENT_TITLE) ?: getString(R.string.app_name)

        NotificationHelper.ensureChannel(this)
        startForeground(ALARM_NOTIFICATION_ID, buildNotification(eventId, title))
        startAlarmSound()
        startVibration()

        handler.removeCallbacks(autoStopRunnable)
        handler.postDelayed(autoStopRunnable, AUTO_STOP_MILLIS)

        return START_NOT_STICKY
    }

    private fun buildNotification(eventId: Long, title: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            eventId.toInt(),
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = PendingIntent.getBroadcast(
            this,
            eventId.toInt(),
            Intent(this, AlarmDismissReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.alarm_notification_title))
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
            .addAction(0, getString(R.string.dismiss_alarm), dismissIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun startAlarmSound() {
        val defaultUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val customUri = CalendarPrefs.getRingtoneUri(this)?.let { Uri.parse(it) }

        mediaPlayer = if (customUri != null) {
            play(customUri) ?: play(defaultUri)
        } else {
            play(defaultUri)
        }
    }

    // falls back to the device's default alarm tone if a previously picked
    // custom ringtone can no longer be played (e.g. it was deleted)
    private fun play(uri: Uri?): MediaPlayer? {
        if (uri == null) return null
        return try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmSoundService, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    // tagged as USAGE_ALARM (where the API supports it) so this follows the
    // phone's own "Alarm" vibration toggle/intensity under Settings > Sound
    // & vibration, the same way the alarm tone follows the alarm volume
    private fun startVibration() {
        val pattern = longArrayOf(0, 800, 500)
        val vibrator = getVibrator()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val attributes = VibrationAttributes.Builder()
                .setUsage(VibrationAttributes.USAGE_ALARM)
                .build()
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0), attributes)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, 0)
        }
    }

    private fun stopVibration() {
        getVibrator().cancel()
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoStopRunnable)
        mediaPlayer?.let {
            try {
                it.stop()
            } catch (e: IllegalStateException) {
                // already stopped/never started - nothing to clean up
            }
            it.release()
        }
        mediaPlayer = null
        stopVibration()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ALARM_NOTIFICATION_ID = 999001
        private const val AUTO_STOP_MILLIS = 2 * 60 * 1000L
    }
}
