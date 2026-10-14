package com.satsrewards.app

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.satsrewards.app.api.ApiClient
import com.satsrewards.app.api.ApiException
import com.satsrewards.app.api.AppConfig
import com.satsrewards.app.api.LedgerEntry
import com.satsrewards.app.api.MeInfo
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val api = ApiClient(Config.BACKEND_URL)

    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var me by mutableStateOf<MeInfo?>(null)
        private set
    var config by mutableStateOf<AppConfig?>(null)
        private set
    var history by mutableStateOf<List<LedgerEntry>>(emptyList())
        private set
    var lastRewardMsg by mutableStateOf<String?>(null)
        private set

    /** Simulated hashrate shown in the UI (display only, like other cloud-mining apps). */
    val simulatedHashrateMhs: Long
        get() = 2000L + (me?.adsToday ?: 0) * 100L

    fun init(context: Context) {
        viewModelScope.launch {
            try {
                config = api.getConfig()
                api.register(deviceId(context))
                refresh()
            } catch (e: Exception) {
                error = friendlyError(e)
            } finally {
                loading = false
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                me = api.getMe()
                history = api.getLedger()
                error = null
            } catch (e: Exception) {
                error = friendlyError(e)
            }
        }
    }

    /** Called when the user fully watched a rewarded ad. */
    fun onAdRewarded() {
        viewModelScope.launch {
            try {
                api.rewardAd()
                lastRewardMsg = "+${config?.rewardSatsPerAd ?: 0} sats credited!"
                refresh()
            } catch (e: Exception) {
                lastRewardMsg = friendlyError(e)
            }
        }
    }

    fun clearRewardMsg() {
        lastRewardMsg = null
    }

    fun withdraw(amountSats: Long, invoice: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                api.withdraw(amountSats, invoice)
                refresh()
                onDone(true, "Withdrawal sent!")
            } catch (e: Exception) {
                onDone(false, friendlyError(e))
            }
        }
    }

    private fun friendlyError(e: Exception): String = when (e) {
        is ApiException -> when (e.message) {
            "daily_cap_reached" -> "Daily ad limit reached. Come back tomorrow!"
            "too_fast" -> "Please wait a moment before the next ad."
            "insufficient_balance" -> "Not enough sats yet."
            "amount_too_small" -> "Amount below the minimum withdrawal."
            "invalid_invoice" -> "This Lightning invoice looks invalid."
            else -> "Error: ${e.message}"
        }
        else -> "Network error. Check your connection."
    }

    @SuppressLint("HardwareIds")
    private fun deviceId(context: Context): String =
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown-device"
}

fun Long.toBtcString(): String = "%.8f".format(this / 100_000_000.0)
