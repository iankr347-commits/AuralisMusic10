// Private Test Build  Not for Redistribution

package com.auralis.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.auralis.music.R
import com.auralis.music.playback.MusicService

class HelloWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_PLAY_PAUSE = "com.auralis.music.ACTION_PLAY"
        const val ACTION_NEXT = "com.auralis.music.ACTION_NEXT"
        const val ACTION_PREV = "com.auralis.music.ACTION_PREV"
        const val ACTION_LIKE = "com.auralis.music.ACTION_LIKE"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        // If the action is one of our buttons, forward it to the MusicService
        if (action in listOf(ACTION_PLAY_PAUSE, ACTION_NEXT, ACTION_PREV, ACTION_LIKE)) {
            val serviceIntent = Intent(context, MusicService::class.java).apply {
                this.action = action
            }
            // We use startService (or startForegroundService) to send the command directly
            try {
                context.startService(serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.widget_hello)

        // Wire up the buttons
        views.setOnClickPendingIntent(R.id.btn_prev, getPendingIntent(context, ACTION_PREV))
        views.setOnClickPendingIntent(R.id.btn_play, getPendingIntent(context, ACTION_PLAY_PAUSE))
        views.setOnClickPendingIntent(R.id.btn_next, getPendingIntent(context, ACTION_NEXT))
        views.setOnClickPendingIntent(R.id.btn_like, getPendingIntent(context, ACTION_LIKE))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, HelloWidget::class.java).apply { this.action = action }
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
