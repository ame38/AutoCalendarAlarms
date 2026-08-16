package com.ame38.autocalendaralarms

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

// launched directly by AlarmSoundService the instant an alarm starts ringing,
// so there is always a guaranteed way to stop it - unlike the notification's
// Dismiss button, this doesn't depend on POST_NOTIFICATIONS ever having been
// granted. Back is disabled so it can't be dismissed without actually
// stopping the alarm.
class AlarmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activeInstance = this
        showOverLockScreen()
        setContentView(R.layout.activity_alarm)
        bindEventTitle()

        findViewById<Button>(R.id.stopAlarmButton).setOnClickListener {
            stopService(Intent(this, AlarmSoundService::class.java))
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        bindEventTitle()
    }

    private fun bindEventTitle() {
        val title = intent.getStringExtra(AlarmReceiver.EXTRA_EVENT_TITLE)
        findViewById<TextView>(R.id.alarmEventTitleText).text = title ?: getString(R.string.alarm_notification_title)
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // intentionally not calling super - the only way off this screen is
        // the Stop Alarm button
    }

    override fun onDestroy() {
        if (activeInstance == this) activeInstance = null
        super.onDestroy()
    }

    companion object {
        private var activeInstance: AlarmActivity? = null

        // called by AlarmSoundService when it stops for any reason (dismissed
        // from the notification, muted, auto-stop timeout) so this screen
        // doesn't linger once there's nothing left to stop
        fun finishIfShowing() {
            activeInstance?.finish()
        }
    }
}
