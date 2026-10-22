package com.evans.launcher

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())

    // Tiles are matched by app label, so no package names are needed. Edit order or names here.
    private val wanted: List<Pair<String, (String) -> Boolean>> = listOf(
        "SPORTZFY" to { s: String -> "sportz" in s },
        "SPOTIFY" to { s: String -> "spotify" in s },
        "YT MUSIC" to { s: String -> "youtube" in s && "music" in s },
        "YOUTUBE" to { s: String -> s == "youtube" }
    )

    private fun launchables(): List<ResolveInfo> {
        val a = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0)
        val b = packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
        return (a + b).distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName != packageName }
    }

    private fun open(ri: ResolveInfo) {
        startActivity(Intent().setClassName(ri.activityInfo.packageName, ri.activityInfo.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun allApps(apps: List<ResolveInfo>) {
        val pm = packageManager
        val sorted = apps.sortedBy { it.loadLabel(pm).toString().lowercase() }
        AlertDialog.Builder(this)
            .setTitle("> all apps")
            .setItems(sorted.map { it.loadLabel(pm).toString() }.toTypedArray()) { _, i -> open(sorted[i]) }
            .show()
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val dm = resources.displayMetrics
        val sw = dm.widthPixels
        val sh = dm.heightPixels
        val pm = packageManager
        val apps = launchables()

        val tiles = mutableListOf<Tile>()
        for ((name, rule) in wanted) {
            val ri = apps.firstOrNull { rule(it.loadLabel(pm).toString().lowercase()) } ?: continue
            tiles += Tile(this, name, "[${tiles.size + 1}]", ri.loadIcon(pm), null) { open(ri) }
        }
        val linked = tiles.size
        tiles += Tile(this, "ALL APPS", "[${tiles.size + 1}]", null, ">_") { allApps(apps) }
        tiles += Tile(this, "SETTINGS", "[${tiles.size + 1}]", null, "{ }") {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }

        val root = FrameLayout(this)
        root.clipChildren = false
        root.addView(TechView(this), FrameLayout.LayoutParams(-1, -1))

        val term = TextView(this).apply {
            typeface = Typeface.MONOSPACE
            setTextColor(0xFF00E5FF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sw * 0.0125f)
            setLineSpacing(0f, 1.3f)
        }
        root.addView(term, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START).apply {
            leftMargin = (sw * 0.05f).toInt(); topMargin = (sh * 0.07f).toInt()
        })

        val clock = TextClock(this).apply {
            format24Hour = "HH:mm"; format12Hour = "HH:mm"
            typeface = Typeface.MONOSPACE; gravity = Gravity.END
            setTextColor(0xFFE6F7FF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sw * 0.045f)
        }
        val date = TextClock(this).apply {
            format24Hour = "EEE dd MMM"; format12Hour = "EEE dd MMM"
            typeface = Typeface.MONOSPACE; gravity = Gravity.END
            setTextColor(0xFF00E5FF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sw * 0.014f)
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(clock); addView(date)
        }
        root.addView(box, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply {
            rightMargin = (sw * 0.05f).toInt(); topMargin = (sh * 0.05f).toInt()
        })

        val gap = (sw * 0.012f).toInt()
        val tw = ((sw * 0.9f) / tiles.size - gap).toInt().coerceAtMost((sw * 0.16f).toInt())
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER; clipChildren = false; clipToPadding = false
        }
        for (t in tiles) {
            row.addView(t, LinearLayout.LayoutParams(tw, (tw * 1.1f).toInt()).apply {
                setMargins(gap / 2, 0, gap / 2, 0)
            })
        }
        root.addView(row, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply {
            bottomMargin = (sh * 0.12f).toInt()
        })

        val foot = TextView(this).apply {
            text = "EVANS_TERMINAL  //  D-PAD move  //  OK launch"
            typeface = Typeface.MONOSPACE
            setTextColor(0x6600E5FF)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sw * 0.0095f)
        }
        root.addView(foot, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL).apply {
            bottomMargin = (sh * 0.045f).toInt()
        })

        setContentView(root)
        tiles[0].requestFocus()

        val script = "evans@tv:~# ./start --home\n[ ok ] system online\n[ ok ] $linked targets linked\n> select target"
        var n = 0
        var on = false
        h.post(object : Runnable {
            override fun run() {
                if (n < script.length) {
                    n++
                    term.text = script.substring(0, n) + "_"
                    h.postDelayed(this, if (script[n - 1] == '\n') 280L else 32L)
                } else {
                    on = !on
                    term.text = script + if (on) "_" else ""
                    h.postDelayed(this, 500L)
                }
            }
        })
    }

    override fun onDestroy() {
        h.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    @Deprecated("Launcher stays on screen")
    override fun onBackPressed() {}
}
