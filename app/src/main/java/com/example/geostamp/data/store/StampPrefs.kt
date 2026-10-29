package com.example.geostamp.data.store

import android.content.Context
import com.example.geostamp.domain.stamp.StampConfig
import com.example.geostamp.domain.stamp.StampJson
import org.json.JSONObject

/** Remembers the last stamp style the person saved with. */
class StampPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("geostamp_prefs", Context.MODE_PRIVATE)

    fun load(): StampConfig {
        val text = prefs.getString(KEY, null) ?: return StampConfig()
        return runCatching { StampJson.configFromJson(JSONObject(text)) }.getOrDefault(StampConfig())
    }

    fun save(config: StampConfig) {
        prefs.edit().putString(KEY, StampJson.configToJson(config).toString()).apply()
    }

    private companion object {
        const val KEY = "default_stamp_config"
    }
}
