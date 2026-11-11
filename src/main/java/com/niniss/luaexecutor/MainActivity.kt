package com.niniss.luaexecutor

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

object CrashLog {
    private const val PREFS = "crashlog"
    private var installed = false

    fun install(ctx: Context) {
        if (installed) return
        installed = true
        val app = ctx.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString("last", Log.getStackTraceString(e)).commit()
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(t, e)
        }
    }

    fun read(ctx: Context): String? =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("last", null)

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().commit()
    }
}

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashLog.install(this)
        val d = resources.displayMetrics.density
        val pad = (16 * d).toInt()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(pad, (40 * d).toInt(), pad, pad)

        fun addButton(label: String, onClick: () -> Unit) {
            val b = Button(this)
            b.text = label
            b.setOnClickListener { onClick() }
            root.addView(b)
        }

        val title = TextView(this)
        title.text = "Lua Executor – diagnostic"
        title.setTextColor(Color.parseColor("#00c853"))
        title.textSize = 20f
        root.addView(title)

        addButton("1. Autoriser l'affichage par-dessus les autres applis") {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
        addButton("2. Autoriser les notifications") {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        addButton("3. Lancer la fenêtre flottante") { launch() }
        addButton("Effacer le rapport d'erreur") {
            CrashLog.clear(this)
            refresh()
        }

        status = TextView(this)
        status.setTextColor(Color.WHITE)
        status.textSize = 13f
        status.setTextIsSelectable(true)
        root.addView(status)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Color.parseColor("#0f0f0f"))
        scroll.addView(root)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        refresh()
    }

    private fun refresh() {
        val overlay = Settings.canDrawOverlays(this)
        val notif = Build.VERSION.SDK_INT < 33 ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val crash = CrashLog.read(this)
        status.text = "Android ${Build.VERSION.SDK_INT} - ${Build.MODEL}\n" +
            "Affichage par-dessus : ${if (overlay) "OUI" else "NON"}\n" +
            "Notifications : ${if (notif) "OUI" else "NON"}\n\n" +
            (if (crash != null) "DERNIERE ERREUR :\n$crash" else "Aucune erreur enregistrée.")
    }

    private fun launch() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Fais d'abord l'étape 1", Toast.LENGTH_LONG).show()
            return
        }
        try {
            startForegroundService(Intent(this, OverlayService::class.java))
            moveTaskToBack(true)
        } catch (e: Throwable) {
            status.text = "ERREUR au lancement :\n" + Log.getStackTraceString(e)
        }
    }
}
