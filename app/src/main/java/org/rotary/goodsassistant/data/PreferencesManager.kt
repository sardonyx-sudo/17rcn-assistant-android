package org.rotary.goodsassistant.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rotary_assistant_prefs", Context.MODE_PRIVATE)

    var gasUrl: String
        get() = prefs.getString(KEY_GAS_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_GAS_URL, value.trim()).apply()

    var defaultAddress: String
        get() = prefs.getString(KEY_DEFAULT_ADDRESS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DEFAULT_ADDRESS, value.trim()).apply()

    companion object {
        private const val KEY_GAS_URL = "rcn_gas_url"
        private const val KEY_DEFAULT_ADDRESS = "rcn_default_address"
    }
}
