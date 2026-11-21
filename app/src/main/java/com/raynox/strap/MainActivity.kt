package com.raynox.strap

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.raynox.strap.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val adapter = PagerAdapter(this)
        binding.viewPager.adapter = adapter
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = when (pos) {
                0 -> "Launcher"
                1 -> "FastFlags"
                else -> "Presets"
            }
        }.attach()
    }
}

class PagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount() = 3
    override fun createFragment(pos: Int): Fragment = when (pos) {
        0 -> LauncherFragment()
        1 -> FlagsFragment()
        else -> PresetsFragment()
    }
}

// ==================== LAUNCHER FRAGMENT ====================
class LauncherFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val ctx = requireContext()
        val scroll = ScrollView(ctx)
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        scroll.addView(root)

        // Status Roblox
        val version = try {
            ctx.packageManager.getPackageInfo(FlagManager.ROBLOX_PKG, 0).versionName
        } catch (_: PackageManager.NameNotFoundException) { null }

        root.addView(TextView(ctx).apply {
            text = "Raynostrap"
            textSize = 26f
            setPadding(0, 0, 0, 24)
        })

        root.addView(TextView(ctx).apply {
            text = if (version != null) "✓ Roblox terpasang: v$version"
                   else "✗ Roblox belum terpasang"
            textSize = 14f
            setPadding(0, 0, 0, 16)
        })

        // Tombol launcher
        val btnOpen = Button(ctx).apply {
            text = "Buka Roblox"
            isEnabled = version != null
            setOnClickListener {
                ctx.packageManager.getLaunchIntentForPackage(FlagManager.ROBLOX_PKG)?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(it)
                }
            }
        }
        root.addView(btnOpen)

        val btnUpdate = Button(ctx).apply {
            text = if (version == null) "Install Roblox" else "Cek Update"
            setOnClickListener { openPlayStore(ctx) }
        }
        root.addView(btnUpdate)

        // Quick join
        root.addView(TextView(ctx).apply {
            text = "Quick Join"
            textSize = 18f
            setPadding(0, 32, 0, 8)
        })

        val etQuick = EditText(ctx).apply { hint = "Place ID / link game / link private server" }
        root.addView(etQuick)

        root.addView(Button(ctx).apply {
            text = "Join"
            setOnClickListener {
                launchInput(ctx, etQuick.text.toString())
            }
        })

        return scroll
    }

    private fun openPlayStore(ctx: Context) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=${FlagManager.ROBLOX_PKG}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=${FlagManager.ROBLOX_PKG}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun launchInput(ctx: Context, raw: String) {
        val s = raw.trim()
        when {
            s.isEmpty() -> Toast.makeText(ctx, "Isi place ID atau link dulu", Toast.LENGTH_SHORT).show()
            s.all { it.isDigit() } ->
                openUri(ctx, Uri.parse("https://www.roblox.com/games/start?placeId=$s"))
            s.startsWith("http") || s.startsWith("roblox://") ->
                openUri(ctx, Uri.parse(s))
            else -> Toast.makeText(ctx, "Format tidak dikenal", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openUri(ctx: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(Intent(intent).setPackage(FlagManager.ROBLOX_PKG))
        } catch (_: ActivityNotFoundException) {
            try { startActivity(intent) }
            catch (_: ActivityNotFoundException) {
                Toast.makeText(ctx, "Tidak ada app yang bisa buka link ini", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

// ==================== FLAGS FRAGMENT ====================
class FlagsFragment : Fragment() {
    private lateinit var store: FlagStore
    private lateinit var flagsContainer: LinearLayout
    private lateinit var tvStatus: TextView
    private lateinit var tvJson: TextView
    private var flags = mutableMapOf<String, String>()

    private val pickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.data ?: return@registerForActivityResult
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {}
            store.saveSafUri(uri)
            doInject(uri)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        store = FlagStore(requireContext())
        flags = store.loadFlags()

        val scroll = ScrollView(requireContext())
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        scroll.addView(root)

        // Status
        tvStatus = TextView(requireContext()).apply {
            text = "Siap inject"
            textSize = 13f
            setPadding(0, 0, 0, 16)
        }
        root.addView(tvStatus)

        // FPS cap
        root.addView(TextView(requireContext()).apply {
            text = "FPS Cap"
            textSize = 16f
            setPadding(0, 0, 0, 8)
        })

        val fpsList = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        listOf(null, 30, 60, 90, 120, 144, 240).forEach { v ->
            fpsList.addView(Button(requireContext()).apply {
                text = v?.toString() ?: "Default"
                textSize = 11f
                setOnClickListener {
                    if (v == null) flags.remove("DFIntTaskSchedulerTargetFps")
                    else flags["DFIntTaskSchedulerTargetFps"] = v.toString()
                    save(); refreshFlagList()
                }
            })
        }
        root.addView(fpsList)

        // Custom flag
        root.addView(TextView(requireContext()).apply {
            text = "Flag Custom"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        })

        val etKey = EditText(requireContext()).apply { hint = "Nama flag" }
        val etVal = EditText(requireContext()).apply { hint = "Value" }
        root.addView(etKey)
        root.addView(etVal)
        root.addView(Button(requireContext()).apply {
            text = "Tambah Flag"
            setOnClickListener {
                val k = etKey.text.toString().trim()
                val v = etVal.text.toString().trim()
                if (k.isNotBlank() && v.isNotBlank()) {
                    flags[k] = v; save(); refreshFlagList()
                    etKey.text.clear(); etVal.text.clear()
                }
            }
        })

        // Daftar flag aktif
        root.addView(TextView(requireContext()).apply {
            text = "Flag Aktif"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        })
        flagsContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(flagsContainer)
        refreshFlagList()

        // Tombol inject
        root.addView(TextView(requireContext()).apply {
            text = "Terapkan ke Roblox"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        })

        root.addView(Button(requireContext()).apply {
            text = "⚡ Auto Inject"
            setOnClickListener { autoInject() }
        })

        root.addView(Button(requireContext()).apply {
            text = "Pilih Folder Manual"
            setOnClickListener {
                pickerLauncher.launch(FlagManager.buildPickerIntent())
            }
        })

        root.addView(Button(requireContext()).apply {
            text = "Salin JSON"
            setOnClickListener {
                val json = FlagManager.buildFlagJson(flags)
                val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("flags", json))
                Toast.makeText(requireContext(), "JSON disalin!", Toast.LENGTH_SHORT).show()
            }
        })

        // JSON Preview
        root.addView(TextView(requireContext()).apply {
            text = "JSON Preview"
            textSize = 16f
            setPadding(0, 24, 0, 8)
        })
        tvJson = TextView(requireContext()).apply {
            textSize = 11f
            typeface = android.graphics.Typeface.MONOSPACE
        }
        root.addView(tvJson)
        updateJsonPreview()

        return scroll
    }

    private fun autoInject() {
        val ctx = requireContext()
        val savedUri = store.loadSafUri()

        // Coba inject otomatis dulu
        val result = FlagManager.inject(ctx, flags, savedUri)
        when (result) {
            is FlagManager.InjectResult.Success -> {
                tvStatus.text = "✓ Inject berhasil via ${result.method}\n${result.path}\nRestart Roblox!"
                Snackbar.make(requireView(), "✓ Berhasil via ${result.method}!", Snackbar.LENGTH_LONG).show()
            }
            is FlagManager.InjectResult.Failed -> {
                // Auto gagal, minta user pilih folder
                tvStatus.text = "⚠ Auto inject gagal: ${result.reason}\nSilakan pilih folder manual..."
                Snackbar.make(requireView(),
                    "Auto gagal, folder picker akan dibuka...",
                    Snackbar.LENGTH_SHORT).show()
                // Langsung launch picker dengan EXTRA_INITIAL_URI ke folder Roblox
                pickerLauncher.launch(FlagManager.buildPickerIntent())
            }
        }
    }

    private fun doInject(uri: Uri) {
        val result = FlagManager.inject(requireContext(), flags, uri)
        when (result) {
            is FlagManager.InjectResult.Success -> {
                tvStatus.text = "✓ Inject berhasil via ${result.method}\n${result.path}\nRestart Roblox!"
                Snackbar.make(requireView(), "✓ Berhasil!", Snackbar.LENGTH_LONG).show()
            }
            is FlagManager.InjectResult.Failed -> {
                tvStatus.text = "✗ Gagal: ${result.reason}"
                Snackbar.make(requireView(), "Gagal: ${result.reason}", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun save() {
        store.saveFlags(flags)
        updateJsonPreview()
    }

    private fun updateJsonPreview() {
        if (::tvJson.isInitialized) {
            tvJson.text = FlagManager.buildFlagJson(flags)
        }
    }

    private fun refreshFlagList() {
        if (!::flagsContainer.isInitialized) return
        flagsContainer.removeAllViews()
        if (flags.isEmpty()) {
            flagsContainer.addView(TextView(requireContext()).apply {
                text = "Belum ada flag aktif."
                textSize = 13f
            })
            return
        }
        flags.entries.forEach { (k, v) ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
            }
            row.addView(TextView(requireContext()).apply {
                text = "$k = $v"
                textSize = 12f
                typeface = android.graphics.Typeface.MONOSPACE
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            row.addView(Button(requireContext()).apply {
                text = "✕"
                textSize = 11f
                setOnClickListener { flags.remove(k); save(); refreshFlagList() }
            })
            flagsContainer.addView(row)
        }
    }
}

// ==================== PRESETS FRAGMENT ====================
class PresetsFragment : Fragment() {
    private lateinit var store: FlagStore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        store = FlagStore(requireContext())
        val flags = store.loadFlags()

        val scroll = ScrollView(requireContext())
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }
        scroll.addView(root)

        root.addView(TextView(requireContext()).apply {
            text = "Preset Performa"
            textSize = 22f
            setPadding(0, 0, 0, 24)
        })

        root.addView(TextView(requireContext()).apply {
            text = "Tap preset buat apply ke flags aktif. Setelah itu ke tab FastFlags lalu inject."
            textSize = 13f
            setPadding(0, 0, 0, 24)
        })

        Presets.ALL.forEach { preset ->
            val card = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(32, 24, 32, 24)
                setBackgroundColor(0xFF1E1E1E.toInt())
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 0, 0, 16) }
                layoutParams = lp
            }

            card.addView(TextView(requireContext()).apply {
                text = preset.name
                textSize = 15f
                setTextColor(0xFFBB86FC.toInt())
            })

            val flagsText = preset.flags.entries.joinToString("\n") { "  ${it.key} = ${it.value}" }
            card.addView(TextView(requireContext()).apply {
                text = flagsText
                textSize = 11f
                typeface = android.graphics.Typeface.MONOSPACE
                setTextColor(0xFFAAAAAA.toInt())
                setPadding(0, 8, 0, 8)
            })

            card.addView(Button(requireContext()).apply {
                text = "Apply Preset"
                setOnClickListener {
                    val current = store.loadFlags().toMutableMap()
                    current.putAll(preset.flags)
                    store.saveFlags(current)
                    Toast.makeText(requireContext(),
                        "Preset '${preset.name}' applied! Sekarang inject di tab FastFlags.",
                        Toast.LENGTH_SHORT).show()
                }
            })

            root.addView(card)
        }

        return scroll
    }
}
