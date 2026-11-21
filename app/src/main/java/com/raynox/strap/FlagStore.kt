package com.raynox.strap

import android.content.Context
import android.net.Uri
import org.json.JSONObject

class FlagStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("raynostrap_flags", Context.MODE_PRIVATE)

    fun loadFlags(): MutableMap<String, String> {
        val raw = prefs.getString("flags", "{}") ?: "{}"
        return try {
            val obj = JSONObject(raw)
            obj.keys().asSequence().associateWith { obj.get(it).toString() }.toMutableMap()
        } catch (_: Exception) { mutableMapOf() }
    }

    fun saveFlags(flags: Map<String, String>) {
        val obj = JSONObject()
        flags.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit().putString("flags", obj.toString()).apply()
    }

    fun loadSafUri(): Uri? = prefs.getString("saf_uri", null)?.let { Uri.parse(it) }

    fun saveSafUri(uri: Uri) {
        prefs.edit().putString("saf_uri", uri.toString()).apply()
    }

    fun clearSafUri() {
        prefs.edit().remove("saf_uri").apply()
    }
}

// Preset flags performa dari analisa flag Roblox
object Presets {
    data class Preset(val name: String, val flags: Map<String, String>)

    val ALL = listOf(
        Preset("FPS Unlock (240)", mapOf("DFIntTaskSchedulerTargetFps" to "240")),
        Preset("FPS Unlock (144)", mapOf("DFIntTaskSchedulerTargetFps" to "144")),
        Preset("Matikan bayangan", mapOf("FIntRenderShadowIntensity" to "0")),
        Preset("Gray sky", mapOf("FFlagDebugSkyGray" to "true")),
        Preset("MSAA off", mapOf("FIntDebugForceMSAASamples" to "1")),
        Preset("Tekstur kualitas rendah", mapOf(
            "DFFlagTextureQualityOverrideEnabled" to "true",
            "DFIntTextureQualityOverride" to "0"
        )),
        Preset("Matikan post-processing", mapOf("FFlagDisablePostFx" to "true")),
        Preset("Network optimizer", mapOf(
            "DFIntConnectionMTUSize" to "900",
            "DFIntMaxMissedWorldStepsRemembered" to "1"
        )),
        Preset("Render jarak dekat", mapOf(
            "DFIntDebugRestrictGCDistance" to "128",
            "DFIntCullFactorPixelThresholdMainViewHighQuality" to "256"
        )),
        Preset("Mode performa penuh", mapOf(
            "DFIntTaskSchedulerTargetFps" to "240",
            "FIntRenderShadowIntensity" to "0",
            "FFlagDebugSkyGray" to "true",
            "FIntDebugForceMSAASamples" to "1",
            "DFFlagTextureQualityOverrideEnabled" to "true",
            "DFIntTextureQualityOverride" to "0",
            "FFlagDisablePostFx" to "true"
        ))
    )
}
