package com.vtu.nammarailubuddy

data class Station(val name: String, val lat: Double, val lon: Double)

object TrainDataProvider {
    val karnatakaStations = listOf(
        Station("Mandya", 12.5218, 76.8951),
        Station("Mysuru", 12.3085, 76.6447),
        Station("KSR Bengaluru", 12.9781, 77.5697),
        Station("Maddur", 12.5851, 77.0456),
        Station("Channapatna", 12.6518, 77.2007),
        Station("Ramanagara", 12.7214, 77.2762),
        Station("Kengeri", 12.9176, 77.4837)
    )
}