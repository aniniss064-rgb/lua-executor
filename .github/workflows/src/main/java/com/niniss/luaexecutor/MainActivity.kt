package com.niniss.luaexecutor

import android.Manifest
import android.app.Activity
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

class MainActivity : Activity() {
    private var status: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installCrashHandler()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(32, 120, 32, 32)

        val title = TextView(this)
        title.text = "Lua Executor - diagnostic"
        title.textSize = 20f
        title.setTextColor(Color.parseColor("#00c853"))
        root.addView(title)

        val b1 = Button(this)
        b1.text = "1. Autoriser l'affichage par-dessus"
        b1.setOnClickListener { askOverlay() }
        root.addView(b1)

        val b2 = Button(this)
        b2.text = "2. Autoriser les notifications"
        b2.setOnClickListener { askNotif() }
        root.addView(b2)

        val b3 = Button(this)
        b3.text = "3. Lancer la fenetre flottante"
        b3.setOnClickListener { launchOverlay() }
        root.addView(b3)

        val b4 = Button(this)
        b4.text = "Effacer le rapport d'erreur"
        b4.setOnClickListener {
            getSharedPreferences("crashlog", MODE_PRIVATE).edit().clear().commit()
            refresh()
        }
        root.addView(b4)

        val s = TextView(this)
        s.setTextColor(Color.WHITE)
        s.textSize = 13f
        s.setTextIsSelectable(true)
        root.addView(s)
        status = s

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

    private fun installCrashHandler() {
        val prefs = getSharedPreferences("crashlog", MODE_PRIVATE)
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(object : Thread.UncaughtExceptionHandler {
            override fun uncaughtException(t: Thread, e: Throwable) {
                try {
                    prefs.edit().putString("last", Log.getStackTraceString(e)).commit()
                } catch (ex: Throwable) {
                }
                if (previous != null) {
                    previous.uncaughtException(t, e)
                }
            }
        })
    }

    private fun askOverlay() {
        val uri = Uri.parse("package:" + packageName)
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri))
    }

    private fun askNotif() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun launchOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            status?.text = "Fais d'abord l'etape 1"
            return
        }
        try {
            startForegroundService(Intent(this, OverlayService::class.java))
            moveTaskToBack(true)
        } catch (e: Throwable) {
            status?.text = "ERREUR au lancement :\n" + Log.getStackTraceString(e)
        }
    }

    private fun refresh() {
        val overlay = Settings.canDrawOverlays(this)
        var notif = true
        if (Build.VERSION.SDK_INT >= 33) {
            notif = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        val crash = getSharedPreferences("crashlog", MODE_PRIVATE).getString("last", null)

        val sb = StringBuilder()
        sb.append("Android ").append(Build.VERSION.SDK_INT).append(" - ").append(Build.MODEL).append("\n")
        sb.append("Affichage par-dessus : ").append(if (overlay) "OUI" else "NON").append("\n")
        sb.append("Notifications : ").append(if (notif) "OUI" else "NON").append("\n\n")
        if (crash != null) {
            sb.append("DERNIERE ERREUR :\n").append(crash)
        } else {
            sb.append("Aucune erreur enregistree.")
        }
        status?.text = sb.toString()
    }
}
