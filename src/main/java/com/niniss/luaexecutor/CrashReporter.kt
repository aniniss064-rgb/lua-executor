package com.niniss.luaexecutor

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import kotlin.system.exitProcess

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 28) {
            val name = Application.getProcessName()
            if (name != null && name.endsWith(":crash")) return
        }
        Thread.setDefaultUncaughtExceptionHandler(object : Thread.UncaughtExceptionHandler {
            override fun uncaughtException(t: Thread, e: Throwable) {
                try {
                    val i = Intent(this@App, CrashActivity::class.java)
                    i.putExtra("trace", Log.getStackTraceString(e))
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    this@App.startActivity(i)
                    Thread.sleep(1200)
                } catch (ex: Throwable) {
                }
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        })
    }
}

class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.setTextColor(Color.WHITE)
        tv.textSize = 12f
        tv.setPadding(32, 120, 32, 32)
        tv.setTextIsSelectable(true)
        val trace = intent.getStringExtra("trace") ?: "(aucun detail)"
        tv.text = "LUA EXECUTOR A PLANTE\nFais une capture de cet ecran.\n\n" + trace
        val sv = ScrollView(this)
        sv.setBackgroundColor(Color.parseColor("#7a0000"))
        sv.addView(tv)
        setContentView(sv)
    }
}
