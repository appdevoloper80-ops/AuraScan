package com.example.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Google AdMob Rewarded Ads using the official Google test ad unit.
 * Ad Unit ID: ca-app-pub-3940256099942544/5224354917
 */
object RewardedAdManager {

    private const val TAG = "RewardedAdManager"

    // Official Google Mobile Ads Rewarded Test Ad Unit ID
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    private var rewardedAd: RewardedAd? = null
    private var isInitialized = false

    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    /**
     * Initialize Google Mobile Ads SDK once per application lifecycle.
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "MobileAds initialized: $status")
                isInitialized = true
                loadAd(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MobileAds", e)
        }
    }

    /**
     * Preloads or reloads a rewarded ad unit.
     */
    fun loadAd(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
        if (_isLoading.value) {
            onComplete?.invoke(false)
            return
        }

        _isLoading.value = true
        _statusMessage.value = "Loading rewarded test ad..."

        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            context,
            TEST_REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    Log.d(TAG, "Rewarded ad successfully loaded")
                    rewardedAd = ad
                    _isAdLoaded.value = true
                    _isLoading.value = false
                    _statusMessage.value = "Rewarded ad ready to show!"
                    onComplete?.invoke(true)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Rewarded ad failed to load: ${loadAdError.message} (code: ${loadAdError.code})")
                    rewardedAd = null
                    _isAdLoaded.value = false
                    _isLoading.value = false
                    _statusMessage.value = "Ad failed to load: ${loadAdError.message}"
                    onComplete?.invoke(false)
                }
            }
        )
    }

    /**
     * Shows the rewarded ad if loaded.
     * Invokes [onUserEarnedReward] when the user completes watching the ad.
     */
    fun showAd(
        activity: Activity,
        onUserEarnedReward: (amount: Int, type: String) -> Unit,
        onAdDismissed: () -> Unit = {},
        onAdFailedToShow: (String) -> Unit = {}
    ) {
        val currentAd = rewardedAd
        if (currentAd == null) {
            val error = "Rewarded ad is not ready yet. Please try again in a moment."
            Log.w(TAG, error)
            _statusMessage.value = error
            onAdFailedToShow(error)
            // Attempt reload for next time
            loadAd(activity)
            return
        }

        currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Rewarded ad displayed full screen")
                _statusMessage.value = "Ad is playing..."
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "Failed to show rewarded ad: ${adError.message}")
                rewardedAd = null
                _isAdLoaded.value = false
                _statusMessage.value = "Failed to show ad: ${adError.message}"
                onAdFailedToShow(adError.message)
                loadAd(activity)
            }

            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Rewarded ad dismissed by user")
                rewardedAd = null
                _isAdLoaded.value = false
                _statusMessage.value = "Ad closed."
                onAdDismissed()
                // Automatically preload next ad for seamless subsequent usage
                loadAd(activity)
            }
        }

        currentAd.show(activity) { rewardItem: RewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
            _statusMessage.value = "Reward earned: ${rewardItem.amount} ${rewardItem.type}"
            onUserEarnedReward(rewardItem.amount, rewardItem.type)
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
