package com.iab.youtload

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.webkit.URLUtil
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.iab.youtload.databinding.ActivityMainBinding
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.io.File


class  MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        isStoragePermissionGranted()

        MobileAds.initialize(
            this
        ) { }

        val adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest)

        YoutubeDL.getInstance().init(this)

        binding.activityMainDownloadButton.setOnClickListener {
            if(binding.activityMainUrlEt.text.toString() != "") {
                val urlString = binding.activityMainUrlEt.text.toString()
                if(URLUtil.isValidUrl(urlString)) {
                    // proceed
                    // first getting url data and if we got some data then proceed to next  screen.

                    getUrlInfo(urlString)
                } else {
                    // Toast that say enter a valid url.
                    Toast.makeText(this, "Please enter valid url !", Toast.LENGTH_LONG).show()
                }
            } else {
                // Toast that say enter video url.
                Toast.makeText(this, "Please enter video url !", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                true
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(WRITE_EXTERNAL_STORAGE),
                    1
                )
                false
            }
        } else {
            true
        }
    }

    private fun getDownloadLocation(): File {
        val downloadsDir: File =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val youtubeDLDir = File(downloadsDir, "youtload")
        if (!youtubeDLDir.exists()) youtubeDLDir.mkdir()
        return youtubeDLDir
    }

    private fun getUrlInfo(url: String) {
        binding.loadingAnimation.loadingAnimation.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val request = YoutubeDLRequest(url)
            val streamInfo = YoutubeDL.getInstance().getInfo(request)
            MainScope().launch {
                binding.loadingAnimation.loadingAnimation.visibility = View.GONE
                if(streamInfo.formats != null && streamInfo.formats.size >0) {
                    // move to showFormates screen
                    var intent = Intent(this@MainActivity, ShowFormatScreen::class.java)
                    intent.putExtra("url", url)
                    this@MainActivity.startActivity(intent)
                } else {
                    Toast.makeText(this@MainActivity, "No formate found !!", Toast.LENGTH_LONG).show()
                }

            }
        }
    }
}