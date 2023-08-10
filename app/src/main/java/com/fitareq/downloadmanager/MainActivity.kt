package com.fitareq.downloadmanager

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fitareq.downloadmanager.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val downloadUrl =
        "https://www.dropbox.com/scl/fi/h33z5i2w2o1zwgckll90u/sample-30s.mp4?rlkey=dr53zsy0j1ebasceifbu3ifth&dl=1"

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.download.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                callDownload()
            else checkPermissionAndDownload()
        }

    }


    private val STORAGE_PERMISSION = 1002
    private fun checkPermissionAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            callDownload()
        else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                callDownload()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            callDownload()
        }
    }

    private fun callDownload() {
        binding.download.isEnabled = false
        downloadFile { progress ->
            runOnUiThread {

                if (progress == 100) {
                    binding.progress.text = "Download completed"
                    binding.download.isEnabled = true
                }
                else binding.progress.text = "Downloading... \n$progress% completed."
            }

        }
    }

    var downloadID: Long = 0L

    @SuppressLint("Range")
    private fun downloadFile(callBack: (Int) -> Unit) {
        val fileName = "sample_file.mp4"

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle(fileName)
            .setDescription("Downloading")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)

        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        downloadID = downloadManager.enqueue(request)


        GlobalScope.launch(Dispatchers.IO) {
            var progress = 0
            var isDownloadFinished = false
            while (!isDownloadFinished) {
                val cursor: Cursor =
                    downloadManager.query(DownloadManager.Query().setFilterById(downloadID))
                if (cursor.moveToFirst()) {
                    val downloadStatus =
                        cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))
                    when (downloadStatus) {
                        DownloadManager.STATUS_RUNNING -> {
                            val totalBytes =
                                cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            if (totalBytes > 0) {
                                val downloadedBytes =
                                    cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                                progress = (downloadedBytes * 100 / totalBytes).toInt()
                            }
                        }

                        DownloadManager.STATUS_SUCCESSFUL -> {
                            progress = 100
                            isDownloadFinished = true
                        }

                        DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_PENDING -> {
                            // Do nothing for paused or pending states
                        }

                        DownloadManager.STATUS_FAILED -> {
                            isDownloadFinished = true
                        }
                    }
                    //mainHandler.sendMessage(message)
                    callBack(progress)
                }
                cursor.close()
            }


        }
    }
}
