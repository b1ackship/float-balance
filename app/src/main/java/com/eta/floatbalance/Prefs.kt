package com.eta.floatbalance

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("balance", Context.MODE_PRIVATE)
    var apiKey: String
        get() = sp.getString("api_key", "") ?: ""
        set(v) { sp.edit().putString("api_key", v).apply() }
}
