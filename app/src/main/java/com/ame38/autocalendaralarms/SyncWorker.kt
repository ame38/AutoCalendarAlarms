package com.ame38.autocalendaralarms

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

// runs on a periodic background schedule so alarms stay up to date even if
// the app hasn't been opened in a while, same query + filtering as the
// events screen just without anything on screen to update
class SyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        EventSync.resync(applicationContext)
        return Result.success()
    }
}
