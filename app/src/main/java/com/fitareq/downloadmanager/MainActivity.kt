package com.fitareq.downloadmanager

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fitareq.downloadmanager.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                // Download completed
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                checkDownloadStatus(downloadId)
            }
        }
    }
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerReceiver(downloadReceiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        binding.download.setOnClickListener {
            checkPermissionsAndDownload()
        }

    }

    override fun onDestroy() {
        super.onDestroy()

        // Unregister the download receiver
        unregisterReceiver(downloadReceiver)
    }

    private fun checkPermissionsAndDownload() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startDownload()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_WRITE_EXTERNAL_STORAGE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDownload()
            }
        }
    }

    private fun startDownload() {
        val downloadUrl =
            "https://www.dropbox.com/scl/fi/h33z5i2w2o1zwgckll90u/sample-30s.mp4?dl=0&rlkey=dr53zsy0j1ebasceifbu3ifth"
        val fileName = "sample_file.mp4"

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle(fileName)
            .setDescription("Downloading")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = downloadManager.enqueue(request)
        checkDownloadStatus(downloadId)

    }

    @SuppressLint("Range")
    private fun checkDownloadStatus(downloadId: Long) {
        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query)

        if (cursor.moveToFirst()) {
            when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    // Download successful
                }

                DownloadManager.STATUS_FAILED -> {
                    // Download failed
                }

                DownloadManager.STATUS_PAUSED -> {
                    // Download paused
                }

                DownloadManager.STATUS_PENDING -> {
                    // Download pending
                }

                DownloadManager.STATUS_RUNNING -> {
                    // Download in progress
                    val downloadedBytes =
                        cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val totalBytes =
                        cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val progress = (downloadedBytes * 100 / totalBytes).toInt()
                    binding.progress.text = "$progress downloading of $totalBytes"
                    // Update UI with progress
                }
            }
        }
    }
    companion object {
        const val REQUEST_WRITE_EXTERNAL_STORAGE = 1
    }
}
