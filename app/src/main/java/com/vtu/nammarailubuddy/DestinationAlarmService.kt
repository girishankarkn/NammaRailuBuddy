package com.vtu.nammarailubuddy

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class DestinationAlarmService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var mediaPlayer: MediaPlayer? = null

    private var destinationLat: Double = 0.0
    private var destinationLng: Double = 0.0
    private var alarmFired: Boolean = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Immediately start in foreground so Android doesn't kill it
        val notification = NotificationCompat.Builder(this, "alarm_service_channel")
            .setContentTitle("Tracking Train Location")
            .setContentText("Monitoring distance to destination...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1001, notification)

        destinationLat = intent?.getDoubleExtra("DEST_LAT", 0.0) ?: 0.0
        destinationLng = intent?.getDoubleExtra("DEST_LNG", 0.0) ?: 0.0

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val results = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, destinationLat, destinationLng, results)
                val distanceKm = results[0] / 1000f

                if (distanceKm <= 5.0f && !alarmFired) {
                    triggerAlarm()
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        }

        return START_STICKY
    }

    private fun triggerAlarm() {
        alarmFired = true
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@DestinationAlarmService, alarmUri)
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onDestroy() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}