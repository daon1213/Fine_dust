package com.daon.finedust_part4_06.appwidget

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.daon.finedust_part4_06.R
import com.daon.finedust_part4_06.data.Repository
import com.daon.finedust_part4_06.data.models.airquality.Grade
import com.google.android.gms.location.LocationServices

import kotlinx.coroutines.launch

class SimpleAirQualityWidgetProvider: AppWidgetProvider() {

    override fun onUpdate(
        context: Context?,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray?
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        ContextCompat.startForegroundService(
            context!!,
            Intent(context, UpdateWidgetService::class.java)
        )
    }

    // foreground service
    class UpdateWidgetService: LifecycleService() {

        override fun onCreate() {
            super.onCreate()
            createChannelIfNeeded()
            startForeground( // 서비스 시작
                NOTIFICATION_ID,
                createNotification())
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                val updateViews = RemoteViews(packageName, R.layout.widget_simple).apply{
                    setTextViewText(
                        R.id.resultTextView,
                        "권한 없음"
                    )
                    setViewVisibility(R.id.labelTextView, View.INVISIBLE)
                    setViewVisibility(R.id.gradeLabelTextView, View.INVISIBLE)
                }
                updateWidget(updateViews)
                stopSelf()
            } else {
                LocationServices.getFusedLocationProviderClient(this).lastLocation
                    .addOnSuccessListener { location ->
                        lifecycleScope.launch {
                            try {
                                val nearbyMonitoringStation = Repository.getNearbyMonitoringStation(
                                    location.latitude,
                                    location.longitude
                                )
                                val measuredValue =
                                    Repository.getLatestAirQualityData(nearbyMonitoringStation!!.stationName!!)
                                val updateViews =
                                    RemoteViews(packageName, R.layout.widget_simple).apply {
                                        setViewVisibility(R.id.labelTextView, View.VISIBLE)
                                        setViewVisibility(R.id.gradeLabelTextView, View.VISIBLE)

                                        val currentGrade =
                                            (measuredValue?.khaiGrade ?: Grade.UNKNOWN)
                                        setTextViewText(R.id.resultTextView, currentGrade.emoji)
                                        setTextViewText(R.id.gradeLabelTextView, currentGrade.label)
                                    }
                                updateWidget(updateViews)
                            } catch (exception : Exception) {
                                exception.printStackTrace()
                            } finally {
                                stopSelf()
                            }
                        }
                    }
            }
            // default : START_STICKY
            return super.onStartCommand(intent, flags, startId)
        }

        override fun onDestroy() {
            super.onDestroy()
            stopForeground(true)
        }

        private fun createChannelIfNeeded() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // 26 이상
                (getSystemService(NOTIFICATION_SERVICE) as? NotificationManager)
                    ?.createNotificationChannel(
                        NotificationChannel(
                            WIDGET_REFRESH_CHANNEL_ID,
                            "위젯 갱신 채널",
                            NotificationManager.IMPORTANCE_LOW
                        )
                    )
            }
        }

        private fun createNotification() =
            NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setChannelId(WIDGET_REFRESH_CHANNEL_ID)
                .build()

        private fun updateWidget (updateView: RemoteViews) {
            val widgetProvider = ComponentName(this, SimpleAirQualityWidgetProvider::class.java)
            AppWidgetManager.getInstance(this).updateAppWidget(widgetProvider, updateView)
        }
    }

    companion object{
        private const val WIDGET_REFRESH_CHANNEL_ID = "WIDGET_REFRESH_CHANNEL_ID"
        private const val NOTIFICATION_ID = 101
    }
}