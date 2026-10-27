package com.eta.floatbalance

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object BalanceFetcher {
    fun fetch(apiKey: String): String {
        val conn = (URL("https://api.deepseek.com/user/balance").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code in 200..299) {
                val info = JSONObject(body).optJSONArray("balance_infos")?.optJSONObject(0)
                if (info != null) {
                    val currency = info.optString("currency", "CNY")
                    val total = info.optString("total_balance", "0")
                    "$total $currency"
                } else {
                    "格式异常"
                }
            } else {
                when (code) {
                    401 -> "Key 无效(401)"
                    402 -> "余额不足(402)"
                    else -> "HTTP $code"
                }
            }
        } catch (e: Exception) {
            "请求失败"
        } finally {
            conn.disconnect()
        }
    }
}
