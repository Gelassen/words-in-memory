package io.github.gelassen.wordinmemory.backgroundjobs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

open abstract class BaseWorker
constructor(
    context: Context,
    params: WorkerParameters,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CoroutineWorker(context, params) {

    object Consts {
        const val KEY_ERROR_MSG = "KEY_ERROR_MSG"
    }

    protected var foregroundIndo: ForegroundInfo


    private val notificationId = 1001
    private val notificationChannelId = 10001
    private val notificationChannelName = "WordsInMemory background task is working"

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager
    init {
        val progress = "Start execute tx on the chain"
        foregroundIndo = createForegroundInfo(progress)
    }

    override suspend fun doWork(): Result {
        TODO("Not yet implemented")
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return foregroundIndo
    }

    private fun createForegroundInfo(progress: String): ForegroundInfo {
        //val id = "10001"//applicationContext.getString(R.string.notification_channel_id)
        val title = "Title"//applicationContext.getString(R.string.notification_title)
        val cancel = "Cancel"//applicationContext.getString(R.string.cancel_download)
        // This PendingIntent can be used to cancel the worker
        val intent = WorkManager.getInstance(applicationContext)
            .createCancelPendingIntent(getId())

        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel()
        }

        val notification = NotificationCompat.Builder(applicationContext, notificationChannelId.toString())
            .setContentTitle(title)
            .setTicker(title)
            .setContentText(progress)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSmallIcon(dagger.android.support.R.drawable.abc_ic_arrow_drop_right_black_24dp)
            .setOngoing(true)
            // Add the cancel action to the notification which can
            // be used to cancel the worker
            .addAction(android.R.drawable.ic_delete, cancel, intent)
            .build()

        return ForegroundInfo(notificationId, notification)  // TODO shall we add FOREGROUND_TYPE  field here?
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createChannel(): Int {
        // Create a Notification channel
        val chan = NotificationChannel(
            notificationChannelId.toString(),
            notificationChannelName,
            NotificationManager.IMPORTANCE_DEFAULT
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        notificationManager.createNotificationChannel(chan)
        return notificationChannelId
    }
}