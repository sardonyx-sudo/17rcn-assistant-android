package org.rotary.goodsassistant.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("rotary_assistant_prefs", Context.MODE_PRIVATE)

    var gasUrl: String
        get() {
            val saved = prefs.getString(KEY_GAS_URL, null)
            return if (!saved.isNullOrBlank()) saved else DEFAULT_GAS_URL
        }
        set(value) = prefs.edit().putString(KEY_GAS_URL, value.trim()).apply()

    var defaultAddress: String
        get() {
            val saved = prefs.getString(KEY_DEFAULT_ADDRESS, null)
            return if (!saved.isNullOrBlank()) saved else DEFAULT_ADDRESS
        }
        set(value) = prefs.edit().putString(KEY_DEFAULT_ADDRESS, value.trim()).apply()

    companion object {
        private const val KEY_GAS_URL = "rcn_gas_url"
        private const val KEY_DEFAULT_ADDRESS = "rcn_default_address"

        // 自用測試預設值
        const val DEFAULT_GAS_URL = "https://script.google.com/macros/s/AKfycbxmQqWQK44VN-OCoGKM5_0ekS_k-n1iDbXuU_e0L8KYVbppMWCR7iVA_KePH_pltrly/exec"
        const val DEFAULT_ADDRESS = "台南市安平區永華三街333號6樓-3"
    }
}
