package dev.ostfalia.iotcam.ui.fragments.sensors
import kotlinx.serialization.Serializable

@Serializable //later for json
data class SensorData(var timestamp: Long, var sensorValues: ArrayList<Any>)
