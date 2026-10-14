package com.satsrewards.app.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class MeInfo(
    val balanceSats: Long,
    val totalEarnedSats: Long,
    val adsWatchedTotal: Int,
    val adsToday: Int,
)

data class AppConfig(
    val rewardSatsPerAd: Int,
    val maxAdsPerDay: Int,
    val minAdIntervalSec: Int,
    val minWithdrawSats: Int,
)

data class LedgerEntry(
    val type: String,
    val amountSats: Long,
    val createdAt: String,
)

class ApiException(message: String) : Exception(message)

class ApiClient(baseUrl: String) {
    private val base = baseUrl.trimEnd('/')
    private val http = OkHttpClient()
    private var token: String? = null
    private val jsonType = "application/json".toMediaType()

    private suspend fun call(
        method: String,
        path: String,
        body: JSONObject? = null,
        auth: Boolean = true,
    ): JSONObject = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(base + path)
        if (auth) builder.header("Authorization", "Bearer ${token.orEmpty()}")
        if (method == "POST") {
            builder.post((body?.toString() ?: "{}").toRequestBody(jsonType))
        } else {
            builder.get()
        }
        http.newCall(builder.build()).execute().use { resp ->
            val json = JSONObject(resp.body?.string().orEmpty().ifEmpty { "{}" })
            if (!resp.isSuccessful) throw ApiException(json.optString("error", "http_${resp.code}"))
            json
        }
    }

    suspend fun register(deviceId: String): String {
        val json = call("POST", "/auth/register", JSONObject().put("device_id", deviceId), auth = false)
        token = json.getString("token")
        return token!!
    }

    suspend fun getConfig(): AppConfig {
        val json = call("GET", "/config", auth = false)
        return AppConfig(
            rewardSatsPerAd = json.getInt("reward_sats_per_ad"),
            maxAdsPerDay = json.getInt("max_ads_per_day"),
            minAdIntervalSec = json.getInt("min_ad_interval_sec"),
            minWithdrawSats = json.getInt("min_withdraw_sats"),
        )
    }

    suspend fun getMe(): MeInfo {
        val json = call("GET", "/me")
        return MeInfo(
            balanceSats = json.getLong("balance_sats"),
            totalEarnedSats = json.getLong("total_earned_sats"),
            adsWatchedTotal = json.getInt("ads_watched_total"),
            adsToday = json.getInt("ads_today"),
        )
    }

    suspend fun rewardAd(): Long {
        val json = call("POST", "/rewards/ad", JSONObject())
        return json.getLong("balance_sats")
    }

    suspend fun withdraw(amountSats: Long, invoice: String): Long {
        val json = call(
            "POST", "/withdraw",
            JSONObject().put("amount_sats", amountSats).put("invoice", invoice),
        )
        return json.getLong("balance_sats")
    }

    suspend fun getLedger(limit: Int = 50): List<LedgerEntry> {
        val json = call("GET", "/ledger?limit=$limit")
        val arr = json.getJSONArray("entries")
        return List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            LedgerEntry(
                type = o.getString("type"),
                amountSats = o.getLong("amount_sats"),
                createdAt = o.getString("created_at"),
            )
        }
    }
}
