package dev.ostfalia.iotcam.ui.fragments.sensors
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable//chack SensorAppTrial for build and dependecies

data class SensorRecording(
    @SerializedName("sensorType") var sensorTypeName: SensorTypeName?,
    @SerializedName("data")        var data: MutableList<SensorData> )
