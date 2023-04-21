package com.iab.youtload

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.iab.youtload.adapter.GenericRecyclerViewAdapter
import com.iab.youtload.adapter.GenericRecyclerViewAdapterInterface
import com.iab.youtload.adapter.viewHolder.GenericViewHolder
import com.iab.youtload.databinding.ActivityShowFormateScreenBinding
import com.iab.youtload.databinding.FormatRvItemBinding
import com.iab.youtload.service.DownloadService
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShowFormatScreen : AppCompatActivity() {
    var url: String? = null
    var formatId = ""
    private val binding: ActivityShowFormateScreenBinding by lazy {
        ActivityShowFormateScreenBinding.inflate(layoutInflater)
    }
    private var videoInfo: VideoInfo? = null
    private var mInterstitialAd: InterstitialAd? = null
    private val TAG = "ShowFormatScreen"
    private var adRequest: AdRequest? = null
    private val formatAdapter =
        GenericRecyclerViewAdapter(object : GenericRecyclerViewAdapterInterface {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenericViewHolder {
                val viewHolderBinding = FormatRvItemBinding.inflate(layoutInflater)
                val layoutParam = MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                layoutParam.topMargin = 5.px.toInt()
                layoutParam.bottomMargin = 5.px.toInt()
                viewHolderBinding.root.layoutParams = layoutParam
                viewHolderBinding.root.elevation = 2.px

                return GenericViewHolder(viewHolderBinding)
            }

            override fun onBindViewHolder(holder: GenericViewHolder, position: Int) {
                val holderBinding = holder.binding as FormatRvItemBinding
                videoInfo?.formats?.let { formats ->
                    // load thumbnail
                    Glide.with(this@ShowFormatScreen)
                        .load(videoInfo?.thumbnail)
                        .into(holderBinding.formatRvItemThumbnailIv)
                    // set extention
                    holderBinding.exeTv.text = formats[position].ext
                    holderBinding.resolutionTextTv.text =
                        formats[position].format.removePrefix("${formats[position].formatId} - ")
                    holderBinding.sizeTv.text = "${(formats[position].fileSize / (1024 * 1024))} MB"

                    holderBinding.formatRvItemDownloadBtn.setOnClickListener {
                        if(mInterstitialAd != null) {
                            mInterstitialAd?.show(this@ShowFormatScreen)
                            formatId = formats[position].formatId.toString()
                        } else {
                            formatId = formats[position].formatId.toString()
                        }

                        val intent = Intent(this@ShowFormatScreen, DownloadService::class.java)
                        intent.putExtra("url", url)
                        intent.putExtra("formatCode", formatId)
                        startService(intent)

                    }
                }
            }

            override fun getItemCount(): Int {
                return videoInfo?.formats?.size ?: 0
            }
        })

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        adsInit()

        binding.formatRv.layoutManager = LinearLayoutManager(this)
        binding.formatRv.adapter = formatAdapter
        binding.shimmerFrameLayout.startShimmerAnimation()

        url = intent.getStringExtra("url")
        if (url != null) {
            getFormatAndDisplayOnList(url!!)
            binding.showFormatScreenUrlText.text = url
        } else {
            Toast.makeText(this, "URL not found!!", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * this will init all the ads instance and
     * handle call back of interstitial ads.
     * */
    private fun adsInit() {
        //banner add
        adRequest = AdRequest.Builder().build()
        binding.adView.loadAd(adRequest!!)

        // interstitial add
        loadInterstitialAd()
    }

    private fun loadInterstitialAd() {
        adRequest?.let {
            InterstitialAd.load(this, getString(R.string.Interstitial_id), it, object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    adError.toString().let { Log.d(TAG, it) }
                    mInterstitialAd = null
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    mInterstitialAd = interstitialAd
                    // interstitial ad callback.
                    mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                        override fun onAdClicked() {
                            // Called when a click is recorded for an ad.
                            Log.d(TAG, "Ad was clicked.")
                        }

                        override fun onAdDismissedFullScreenContent() {
                            // Called when ad is dismissed.
                            Log.d(TAG, "Ad dismissed fullscreen content.")
                            mInterstitialAd = null
                            loadInterstitialAd()
                        }

                        override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                            // Called when ad fails to show.
                            Log.e(TAG, "Ad failed to show fullscreen content.")
                            mInterstitialAd = null
                        }

                        override fun onAdImpression() {
                            // Called when an impression is recorded for an ad.
                            Log.d(TAG, "Ad recorded an impression.")
                            loadInterstitialAd()
                        }

                        override fun onAdShowedFullScreenContent() {
                            // Called when ad is shown.
                            Log.d(TAG, "Ad showed fullscreen content.")
                            loadInterstitialAd()
                        }
                    }
                }
            })
        }
    }

    private fun getFormatAndDisplayOnList(url: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val request = YoutubeDLRequest(url)
            videoInfo = YoutubeDL.getInstance().getInfo(request)
            // show list on main ui thread.
            CoroutineScope(Dispatchers.Main).launch {
                if (videoInfo != null && videoInfo!!.formats != null && videoInfo!!.formats.size > 0) {
                    loadData()
                }
            }
        }
    }

    private fun loadData() {
        binding.titleTv.text = videoInfo?.title
        binding.descriptionTv.text = videoInfo?.description
        filterAccordingToFormat()
    }

    private fun filterAccordingToFormat() {
        videoInfo?.let { vinfo ->
            vinfo.formats?.let {
                val videoFormats: ArrayList<VideoFormat> = arrayListOf()
                it.forEach { vF ->
                    if (vF.ext == "m4a" || vF.ext == "mp4" || vF.ext == "flv" || vF.ext == "3gp" || vF.ext == "webm") {
                        if (vF.ext == "mp4") {
                            videoFormats.add(0, vF)
                        } else {
                            videoFormats.add(vF)
                        }
                    }
                }
                vinfo.formats.clear()
                vinfo.formats.addAll(videoFormats)
                formatAdapter.notifyDataSetChanged()
                binding.shimmerFrameLayout.stopShimmerAnimation()
                binding.showFormatScreenTotalFormatText.text =
                    "Total format found : ${videoInfo?.formats?.size ?: 0}"
                binding.formatRv.visibility = View.VISIBLE
                binding.shimmerFrameLayout.visibility = View.GONE
            }
        }
    }
}

//https://youtu.be/BAZ4GXMwH4M