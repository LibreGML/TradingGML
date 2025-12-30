package tz.yx.gml

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import tz.yx.gml.homefrag.MainActivity
import java.util.*

class MyNightWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_SHOW_NOTIFICATION = "tz.yx.gml.ACTION_SHOW_NOTIFICATION"
        private const val CHANNEL_ID = "widget_channel_01"
        private const val NOTIFICATION_ID = 1001

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "温馨提醒",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "来自桌面小部件的提醒"
                    enableLights(true)
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            }
        }

        private fun createNotificationClickIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        }

        // 发送通知
        private fun showNotification(context: Context) {
            createNotificationChannel(context)

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("莹轩之音")
                .setContentText("一定要天天开心啊！")
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentIntent(createNotificationClickIntent(context))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent)
        if (intent?.action == ACTION_SHOW_NOTIFICATION) {
            if (isNotificationEnabled(context)) {
                showNotification(context)
            } else {
                openNotificationSettings(context)
            }
        }
        if (intent?.action == ACTION_SHOW_NOTIFICATION) {
            showNotification(context)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        createNotificationChannel(context)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_layout)

        val days = calculateDays()
        views.setTextViewText(R.id.tv_date_info, "2021年2月1日莹轩诞生啦！陪伴了我 $days 天！")

        val clickIntent = Intent(context, MyNightWidgetProvider::class.java).apply {
            action = ACTION_SHOW_NOTIFICATION
        }

        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getBroadcast(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getBroadcast(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun calculateDays(): Int {
        val startDate = Calendar.getInstance().apply {
            set(2021, Calendar.FEBRUARY, 1, 0, 0, 0)
            clear(Calendar.MILLISECOND)
        }
        val endDate = Calendar.getInstance().apply {
            clear(Calendar.MILLISECOND)
        }
        val diffMillis = endDate.timeInMillis - startDate.timeInMillis
        return (diffMillis / (24 * 60 * 60 * 1000)).toInt() + 1
    }


    // 检查通知是否被允许
    private fun isNotificationEnabled(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.areNotificationsEnabled()
        } else {
            true
        }
    }

    // 跳转到通知设置页面
    private fun openNotificationSettings(context: Context) {
        try {
            val intent = Intent().apply {
                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                putExtra("android.provider.extra.APP_PACKAGE", context.packageName)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent().apply {
                action = "android.settings.APPLICATION_DETAILS_SETTINGS"
                data = android.net.Uri.parse("package:${context.packageName}")
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

}