package com.iab.youtload.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.iab.youtload.MainActivity
import com.iab.youtload.R
import com.yausername.youtubedl_android.DownloadProgressCallback
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
//import com.yausername.youtubedl_android.DownloadProgressCallback
//import com.yausername.youtubedl_android.YoutubeDL
//import com.yausername.youtubedl_android.YoutubeDLException
//import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File


class DownloadService : Service() {

    private var downloading = false
    private var request: YoutubeDLRequest? = null
    private var processId = "MyProcessDownloadId"
    // 1. Create a NotificationManagerCompat instance
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var progress = 0f
    private val notificationId: Int = 1010

    override fun onBind(i: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        var url = intent?.getStringExtra("url")
        var formatCode = intent?.getStringExtra("formatCode")
        if(url != null && url != "null" && url != "" && formatCode != null && formatCode != "") {
            startDownload(url, formatCode)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startDownload(url: kotlin.String, formatCode: String) {
        try {
            YoutubeDL.getInstance().init(this@DownloadService)
            showNotification(this@DownloadService)
            val youtubeDLDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "youtload"
            )
            CoroutineScope(Dispatchers.IO).launch {
                val request = YoutubeDLRequest(url)
                request.addOption("-f", formatCode)
                request.addOption("-o", youtubeDLDir.absolutePath + "/%(title)s.%(ext)s")
                YoutubeDL.getInstance().execute(request,
                    DownloadProgressCallback { fl: Float, l: Long, s: String ->
                        updateNotificationProgress(fl.toInt())
                    })
            }
        } catch (e: YoutubeDLException) {

            Toast.makeText(this@DownloadService, "error: $e", Toast.LENGTH_LONG).show()
            Log.e("youtube_downloader", "failed to initialize youtubedl-android", e)
        }
    }

    private fun stopDownloading() {

    }

    private fun showNotification(context: Context) {
        notificationManager = NotificationManagerCompat.from(context)
        // 2. Create a notification channel (for Android 8.0 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "download_channel_id",
                "Download Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 3. Create a PendingIntent that will launch your app when the notification is clicked
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, FLAG_MUTABLE)

        // 4. Create a progress bar notification using the NotificationCompat.Builder
        notificationBuilder = NotificationCompat.Builder(this, "download_channel_id")

        var notification = notificationBuilder
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("Download in progress")
            .setContentText("Downloading your file")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setProgress(100, 0, false)
            .build()

        // 5. Show the notification
        startForeground(notificationId, notification)
    }

    private fun updateNotificationProgress(progress: Int) {
        if(this::notificationBuilder.isInitialized) {
            // Update the notification with a new progress value
            notificationBuilder.setProgress(100, progress, false)
            notificationManager.notify(notificationId, notificationBuilder.build())
        }
    }
}