package com.raynox.strap

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import org.json.JSONObject
import java.io.File

object FlagManager {

    const val ROBLOX_PKG = "com.roblox.client"
    const val FILE_NAME = "ClientAppSettings.json"

    // Path dari reverse engineering Masterstrap APK:
    // nativeSetFilesDirectory  -> getFilesDir()  -> exe/ClientSettings/
    // nativeSetExternalDirectory -> getExternalFilesDir() -> exe/ClientSettings/
    // fallback: /data/local/tmp/ (root only)
    private val DIRECT_PATHS = listOf(
        "/data/data/$ROBLOX_PKG/files/exe/ClientSettings",
        "/data/data/$ROBLOX_PKG/files/ClientSettings",
        "/data/user/0/$ROBLOX_PKG/files/exe/ClientSettings",
        "/data/user/0/$ROBLOX_PKG/files/ClientSettings",
        "/data/local/tmp"
    )

    fun getRobloxExternalDir(): File =
        File(Environment.getExternalStorageDirectory(),
            "Android/data/$ROBLOX_PKG/files/exe/ClientSettings")

    // Auto-build SAF URI langsung ke folder Roblox
    // content://com.android.externalstorage.documents/tree/primary:Android%2Fdata%2F...
    fun buildAutoUri(): Uri {
        val path = "Android/data/$ROBLOX_PKG/files/exe/ClientSettings"
        val encoded = path.replace("/", "%2F")
        return Uri.parse("content://com.android.externalstorage.documents/tree/primary%3A$encoded")
    }

    fun buildFlagJson(flags: Map<String, String>): String {
        val obj = JSONObject()
        flags.forEach { (k, v) -> obj.put(k, parseValue(v)) }
        return obj.toString(2)
    }

    private fun parseValue(v: String): Any = when {
        v.equals("true", true) -> true
        v.equals("false", true) -> false
        v.toIntOrNull() != null -> v.toInt()
        v.toLongOrNull() != null -> v.toLong()
        else -> v
    }

    sealed class InjectResult {
        data class Success(val method: String, val path: String) : InjectResult()
        data class Failed(val reason: String) : InjectResult()
    }

    fun inject(ctx: Context, flags: Map<String, String>, safUri: Uri?): InjectResult {
        val json = buildFlagJson(flags)

        // Metode 1: SAF via URI yang sudah dipilih user
        if (safUri != null) {
            val r = injectSAF(ctx, safUri, json)
            if (r is InjectResult.Success) return r
        }

        // Metode 2: SAF auto - langsung pakai URI hardcode ke folder Roblox
        // Works di Android 10-12 kalau permission sudah pernah di-grant
        val r2 = injectSAF(ctx, buildAutoUri(), json)
        if (r2 is InjectResult.Success) return r2

        // Metode 3: Direct file (Android 9- atau root)
        val r3 = injectDirect(json)
        if (r3 is InjectResult.Success) return r3

        // Metode 4: External storage tanpa SAF
        val r4 = injectExternal(json)
        if (r4 is InjectResult.Success) return r4

        return InjectResult.Failed("Semua metode gagal. Pilih folder manual atau Salin JSON.")
    }

    private fun injectSAF(ctx: Context, treeUri: Uri, json: String): InjectResult {
        return try {
            val root = DocumentFile.fromTreeUri(ctx, treeUri)
                ?: return InjectResult.Failed("TreeUri invalid")
            if (!root.exists()) return InjectResult.Failed("Folder tidak exist")

            // Roblox baca dari exe/ClientSettings/ sesuai nativeSetFilesDirectory
            val targetDir = when (root.name) {
                "ClientSettings", "exe" -> root
                else -> {
                    val exe = root.findFile("exe") ?: root.createDirectory("exe")
                        ?: return InjectResult.Failed("Gagal buat folder exe")
                    exe.findFile("ClientSettings") ?: exe.createDirectory("ClientSettings")
                        ?: return InjectResult.Failed("Gagal buat folder ClientSettings")
                }
            }

            val file = targetDir.findFile(FILE_NAME)
                ?: targetDir.createFile("application/json", FILE_NAME)
                ?: return InjectResult.Failed("Gagal buat file")

            ctx.contentResolver.openOutputStream(file.uri, "wt")?.use {
                it.write(json.toByteArray(Charsets.UTF_8))
            } ?: return InjectResult.Failed("Gagal buka outputstream")

            InjectResult.Success("SAF", file.uri.path ?: treeUri.toString())
        } catch (e: Exception) {
            InjectResult.Failed("SAF: ${e.message}")
        }
    }

    private fun injectDirect(json: String): InjectResult {
        for (path in DIRECT_PATHS) {
            val dir = File(path)
            try {
                if (!dir.exists()) dir.mkdirs()
                if (dir.canWrite()) {
                    File(dir, FILE_NAME).writeText(json, Charsets.UTF_8)
                    return InjectResult.Success("Direct", "$path/$FILE_NAME")
                }
            } catch (_: Exception) {}
        }
        return InjectResult.Failed("Path direct tidak bisa ditulis (butuh root)")
    }

    private fun injectExternal(json: String): InjectResult {
        return try {
            val dir = getRobloxExternalDir()
            if (!dir.exists()) dir.mkdirs()
            File(dir, FILE_NAME).writeText(json, Charsets.UTF_8)
            InjectResult.Success("External", "${dir.absolutePath}/$FILE_NAME")
        } catch (e: Exception) {
            InjectResult.Failed("External: ${e.message}")
        }
    }

    // Intent SAF yang otomatis navigate ke folder Roblox via EXTRA_INITIAL_URI
    fun buildPickerIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            putExtra("android.provider.extra.INITIAL_URI", buildAutoUri())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
    }
}
