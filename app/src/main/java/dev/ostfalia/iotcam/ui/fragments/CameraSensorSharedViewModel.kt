package dev.ostfalia.iotcam.ui.fragments

import android.location.Location
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import dev.ostfalia.iotcam.BuildConfig
import dev.ostfalia.iotcam.network.upload.datamodels.RecordingMetadataDataModel
import dev.ostfalia.iotcam.network.upload.datamodels.UsedSensors
import dev.ostfalia.iotcam.ui.fragments.sensors.SensorData
import dev.ostfalia.iotcam.ui.fragments.sensors.SensorRecording
import dev.ostfalia.iotcam.ui.fragments.sensors.SensorTypeName
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class CameraSensorSharedViewModel: ViewModel() {
    //GPS
    var gpsList: ArrayList<Location> = ArrayList()
    //chosen Sensors
    var sensorListTypeNameAfterSelection: MutableList<SensorTypeName> = ArrayList()
    var allSensorsWereDeleted = false
    //GPS
    var gpsLongitude: Double? = 200.0 // fail safe values
    var gpsLatitude: Double? = 200.0 // fail safe values
    lateinit var sensorsUsedDuringRecording: ArrayList<UsedSensors>
    //Camera
    var cameraOrientation: String? = "na"
    var resolutionx: Int? = 0   // fail safe values
    var resolutiony: Int? = 0   // fail safe values
    var userLoggedIn: String = "Tim Tupe"
    //Recording start stop
    var timeStampStarted: Long = 42; // fail safe values
    var timeStampStopped: Long = 42; // fail safe values

    var appVersion = BuildConfig.VERSION_CODE.toString()
    //TimeFrames Camera, init with dummy list
    var tmpArrayList: ArrayList<Any> = arrayListOf<Any>(0)
    var tmpSensorDataInit: SensorData = SensorData(0, tmpArrayList)
    var timeStampsCamera: SensorRecording = SensorRecording(SensorTypeName.CAM_TIMESTAMPS, mutableListOf<SensorData>(tmpSensorDataInit))

    // Sensors
    fun saveSensors(newSensorList: MutableList<SensorTypeName>){
        sensorListTypeNameAfterSelection = newSensorList

       // Log.d("SharedViewModel", _sensorListTypeNameAfterSelection.toString())
    }
    fun getSensorList(): MutableList<SensorTypeName> {
        return sensorListTypeNameAfterSelection
    }
    fun deleteSensorList(noSensorsFlag: Boolean){
        allSensorsWereDeleted = noSensorsFlag
        sensorListTypeNameAfterSelection = ArrayList()

    }
    fun restoreDeleteSensorList(noSensorsFlag: Boolean){
        allSensorsWereDeleted = noSensorsFlag

    }
    fun getSensorFlag(): Boolean{
        return allSensorsWereDeleted
    }


    // used sensors during recording
    fun saveUsedSensorList(usedSensorList: MutableList<UsedSensors>){

        sensorsUsedDuringRecording = usedSensorList as ArrayList<UsedSensors>
        Log.d("sensorsUsedDuringRecording", sensorsUsedDuringRecording.toString())
    }


    fun generateSampleData(): RecordingMetadataDataModel {

        val dm = RecordingMetadataDataModel()

        dm.name = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ" ).format(Calendar.getInstance().time).toString() + "-" + this.getRandomString(8)
        dm.recordingStarted = timeStampStarted
        dm.recordingStopped = timeStampStopped
        dm.usedSensors = ArrayList<UsedSensors>()
        dm.usedSensors = sensorsUsedDuringRecording
        dm.recordingDevice?.deviceName = Build.BRAND.uppercase() + " " + Build.MODEL
        dm.recordingDevice?.manufacturer = Build.MANUFACTURER.uppercase()
        dm.recordingDevice?.operatingSystem = "Android " + Build.VERSION.RELEASE + " - API Level " + Build.VERSION.SDK_INT.toString()
        dm.projectID = "API_TEST_NOREALDATA"
        dm.appVersion = appVersion
        dm.camera?.orientation = cameraOrientation
        dm.camera?.resolution?.x = resolutionx
        dm.camera?.resolution?.y = resolutiony
        dm.user = userLoggedIn
        dm.freeText = "Helene Fischer - Durch die Nacht"
        dm.recordingLocation?.latitude = gpsLatitude
        dm.recordingLocation?.longitude = gpsLongitude
        Log.d("Meta_os", dm.appVersion.toString())
        Log.d("dataclassgps", gpsList.toString())
        return dm
    }

    fun saveCameraInfo(orientation: String, resolution_x: Int, resolution_y: Int){
        cameraOrientation = orientation
        resolutiony = resolution_y
        resolutionx = resolution_x
    }
    fun storeRecordingStartStop(start: Long, stop : Long){
        timeStampStarted = start
        timeStampStopped = stop
    }
    fun saveUser(user: String){
        userLoggedIn =  user
    }

    //for random string generation
    companion object {
        private val characterPool = "qwertzuiopasdfghjklyxcvbnm0123456789"
    }

    private fun getRandomString(sizeOfRandomString: Int): String {

        val sb = StringBuilder(sizeOfRandomString)
        val random = Random()
        for (i in 0 until sizeOfRandomString)
            sb.append(characterPool[random.nextInt(characterPool.length)])
        return sb.toString()
    }

    fun storeGPSData(location: Location) {

        if (gpsList!= null) {
            gpsList += location!!

            gpsLatitude = gpsList?.last()?.latitude
            gpsLongitude = gpsList?.last()?.longitude
            Log.d("sharedViewGps", gpsLatitude.toString())
        }
    }



}