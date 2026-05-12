package com.vtu.nammarailubuddy

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private var isTracking = mutableStateOf(false)
    private var distanceToTarget = mutableStateOf(0.0)
    private val stationPingMap = mutableStateMapOf<String, Int>()
    private var lastPingInfo = mutableStateOf("Ready to track community pings...")

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        requestLocationPermission()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                    NammaRailuMainScreen()
                }
            }
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            1
        )
    }

    private fun calculateRouteDistance(from: Station, to: Station) {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(from.lat, from.lon, to.lat, to.lon, results)
        distanceToTarget.value = (results[0] / 1000).toDouble()
    }

    private fun startLiveTracking(targetStation: Station) {
        // Corrected Priority import to use Google GMS Location Priority
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return

                val results = FloatArray(1)
                android.location.Location.distanceBetween(
                    location.latitude, location.longitude,
                    targetStation.lat, targetStation.lon,
                    results
                )

                val currentDist = (results[0] / 1000).toDouble()
                distanceToTarget.value = currentDist

                if (currentDist <= 5.0 && isTracking.value) {
                    checkAlarmCondition(targetStation.name)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, Looper.getMainLooper())
        }
    }

    private fun sendPlatformPing(stationName: String) {
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val currentCount = (stationPingMap[stationName] ?: 0) + 1
        stationPingMap[stationName] = currentCount

        lastPingInfo.value = "📢 PASSENGER ALERT: Train at $stationName (Confirmed by $currentCount users) at $time"

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        Toast.makeText(this, "Ping sent for $stationName!", Toast.LENGTH_SHORT).show()
    }

    private fun simulateMovement(targetName: String) {
        if (!isTracking.value) return
        distanceToTarget.value = (distanceToTarget.value - 5.0).coerceAtLeast(0.0)
        checkAlarmCondition(targetName)
    }

    private fun checkAlarmCondition(stationName: String) {
        if (distanceToTarget.value <= 5.0 && isTracking.value) {
            triggerAlarm("5KM to $stationName!")
            isTracking.value = false
            locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        }
    }

    private fun triggerAlarm(message: String) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 500, 1000), 0))

        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@MainActivity, alarmUri)
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                prepare()
                start()
            }

            Handler(Looper.getMainLooper()).postDelayed({
                mediaPlayer?.let {
                    if (it.isPlaying) it.stop()
                    it.release()
                }
                mediaPlayer = null
                vibrator.cancel()
            }, 10000)

        } catch (e: Exception) {
            e.printStackTrace()
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun NammaRailuMainScreen() {
        var fromStation by remember { mutableStateOf(TrainDataProvider.karnatakaStations[2]) }
        var toStation by remember { mutableStateOf(TrainDataProvider.karnatakaStations[0]) }
        var expandedFrom by remember { mutableStateOf(false) }
        var expandedTo by remember { mutableStateOf(false) }

        LaunchedEffect(fromStation, toStation) {
            calculateRouteDistance(fromStation, toStation)
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("NAMMA RAILU BUDDY", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2196F3))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Notifications, contentDescription = null, tint = Color(0xFF1976D2))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(lastPingInfo.value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0D47A1))
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = { expandedFrom = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(fromStation.name, fontSize = 11.sp, maxLines = 1)
                    }
                    DropdownMenu(expanded = expandedFrom, onDismissRequest = { expandedFrom = false }) {
                        TrainDataProvider.karnatakaStations.forEach { s ->
                            DropdownMenuItem(text = { Text(s.name) }, onClick = { fromStation = s; expandedFrom = false })
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(onClick = { expandedTo = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(toStation.name, fontSize = 11.sp, maxLines = 1)
                    }
                    DropdownMenu(expanded = expandedTo, onDismissRequest = { expandedTo = false }) {
                        TrainDataProvider.karnatakaStations.forEach { s ->
                            DropdownMenuItem(text = { Text(s.name) }, onClick = { toStation = s; expandedTo = false })
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DISTANCE TO ${toStation.name.uppercase()}", fontSize = 12.sp, color = Color.Gray)
                    Text("${String.format("%.2f", distanceToTarget.value)} KM", fontSize = 54.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(15.dp))

            Text("Platform Guide (Coach Position):", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            val coaches = listOf("ENG", "GEN", "S1", "S2", "S3", "S4", "LADIES", "S5", "S6", "GEN")
            LazyRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(coaches) { coach ->
                    Box(modifier = Modifier.size(70.dp, 45.dp).background(when (coach) { "ENG" -> Color.Black; "LADIES" -> Color(0xFFE91E63); else -> Color(0xFF1565C0) }, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                        Text(coach, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { sendPlatformPing(fromStation.name) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("PING ${fromStation.name.uppercase()}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { sendPlatformPing(toStation.name) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("PING ${toStation.name.uppercase()}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(onClick = { simulateMovement(toStation.name) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF673AB7))) {
                Text("DEMO: REDUCE 5KM", color = Color.White)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (isTracking.value) {
                        isTracking.value = false
                        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
                    } else {
                        isTracking.value = true
                        startLiveTracking(toStation)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isTracking.value) Color.Red else Color.Black)
            ) {
                Text(if (isTracking.value) "CANCEL ALARM" else "START JOURNEY (SET 5KM ALARM)", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}