package com.fitareq.downloadmanager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class DownloadService: Service() {
    private val NOTIFICATION_ID = 1002
    private val binder = LocalBinder()
    private var progressListener: ProgressListener? = null
    inner class LocalBinder: Binder() {
        fun getService(): DownloadService = this@DownloadService
    }
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        startDownload()
        return START_NOT_STICKY
    }

    private fun createNotification(): Notification {
        // Create a notification channel if Android version is Oreo or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "download_channel"
            val channel = NotificationChannel(
                channelId,
                "Download Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Create a notification
        val notificationBuilder = NotificationCompat.Builder(this, "download_channel")
            .setContentTitle("Downloading File")
            .setContentText("Download in progress")
            .setSmallIcon(android.R.drawable.ic_menu_upload)

        return notificationBuilder.build()
    }

    private fun startDownload() {

    }
}

interface  ProgressListener{
    fun onProgressUpdate(progress: Int)
}