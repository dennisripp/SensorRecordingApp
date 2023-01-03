package dev.ostfalia.iotcam.ui.fragments.videolistview

import dev.ostfalia.iotcam.network.upload.datamodels.SensorRecordingDataModel
import java.io.File

class DataModel(var name: String, var type: String, var version_number: String, var feature: String)


class VideoListDataModel(var name: String, var video: File, var sensors: SensorRecordingDataModel)