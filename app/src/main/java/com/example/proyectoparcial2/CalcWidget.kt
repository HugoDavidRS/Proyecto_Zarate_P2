package com.example.proyectoparcial2

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb

/** Home-screen widget showing the latest calculation; tapping it opens the calculator. */
class CalcWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        manager.updateAppWidget(ids, views(context))
    }

    companion object {
        private fun views(context: Context): RemoteViews {
            val prefs = context.getSharedPreferences("calc_prefs", Context.MODE_PRIVATE)
            val last = prefs.getString("history", "").orEmpty().lineSequence().firstOrNull()?.split('\t')
            val accent = THEMES[prefs.getInt("theme", 0).coerceIn(0, THEMES.lastIndex)].accent.toArgb()
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            return RemoteViews(context.packageName, R.layout.widget_calc).apply {
                if (last != null && last.size == 2) {
                    setTextViewText(R.id.widget_expr, last[0])
                    setTextViewText(R.id.widget_result, "= ${last[1]}")
                } else {
                    setTextViewText(R.id.widget_expr, "Sin cálculos aún")
                    setTextViewText(R.id.widget_result, "0")
                }
                setTextColor(R.id.widget_title, accent)
                setOnClickPendingIntent(R.id.widget_root, open)
            }
        }

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CalcWidget::class.java))
            if (ids.isNotEmpty()) manager.updateAppWidget(ids, views(context))
        }
    }
}
