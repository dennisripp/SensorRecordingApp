package dev.ostfalia.iotcam.ui.fragments.sensors

import kotlinx.serialization.Serializable//chack SensorAppTrial for build and dependecies

@Serializable //later for json
data class AllDataFromSensors(var data: MutableList<SensorRecording>)
