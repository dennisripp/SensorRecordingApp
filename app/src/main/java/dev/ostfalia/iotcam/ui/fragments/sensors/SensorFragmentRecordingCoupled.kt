package dev.ostfalia.iotcam.ui.fragments.sensors

import android.content.Context
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ostfalia.iotcam.Sensors.*
import dev.ostfalia.iotcam.network.upload.datamodels.UsedSensors

import dev.ostfalia.iotcam.ui.fragments.CameraSensorSharedViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorFragmentRecordingCoupled @Inject constructor(@ApplicationContext
                                                         var appContext: Context): Fragment() {

    //Sensor Manager
    private lateinit var sensorManager: SensorManager
    // SensorRecording class
    private var sensorlinAccRecording:SensorRecording? = null
    private var sensorAccRecording:SensorRecording? = null
    private var sensorLightRecording:SensorRecording? = null
    private var sensorMagRecording:SensorRecording? = null
    private var sensorGravRecording:SensorRecording? = null
    private var sensorTempRecording:SensorRecording? = null
    private var sensorProxRecording:SensorRecording? = null
    private var sensorPresRecording:SensorRecording? = null
    private var sensorHumiRecording:SensorRecording? = null
    private var sensorGyroRecording:SensorRecording? = null
    private var sensorRotationVectorRecording:SensorRecording? = null

    //sensorType enum for sensorData
    //private var sensorName: SensorTypeName? = null
    //arraylist of single sensordata data class entries (each containig a timestamp and the senosr data)
    private var linAccdataList:MutableList<SensorData> = ArrayList()
    private var accdataList:MutableList<SensorData> = ArrayList()
    private var lightdataList:MutableList<SensorData> = ArrayList()
    private var magdataList:MutableList<SensorData> = ArrayList()
    private var gravitydataList:MutableList<SensorData> = ArrayList()
    private var tempdataList:MutableList<SensorData> = ArrayList()
    private var proxdataList:MutableList<SensorData> = ArrayList()
    private var pressuredataList:MutableList<SensorData> = ArrayList()
    private var humidataList:MutableList<SensorData> = ArrayList()
    private var gyrodataList:MutableList<SensorData> = ArrayList()
    private var rotationVectordataList:MutableList<SensorData> = ArrayList()
    //all sensor data from a measurement period
    private var storeAllData: MutableList<SensorRecording>? = null
    //sensors via SingleSensor Object
    private var linaccSensor: LinAccSensor? = null
    private var accSensor: AccSensor? = null
    private var lightSensor: LightSensor? = null
    private var magSensor: MagnetometerSensor? = null
    private var gravitySensor: GravitySensor? = null
    private var tempSensor: TempSensor? = null
    private var proximitySensor: ProximitySensor? = null
    private var pressureSensor: PressureSensor? = null
    private var humiditySensor: HumiditySensor? = null
    private var gyroscopeSensor: GyroscopeSensor? = null
    private var rotationVectorSensor: RotationVectorSensor? = null
    //default sensors not necessarily all on phone device
    var defaultSensorList = arrayListOf<SensorTypeName>(SensorTypeName.LIGHT, SensorTypeName.GYROSCOPE,
        SensorTypeName.ROTATION_VECTOR, SensorTypeName.PRESSURE, SensorTypeName.RELATIVE_HUMIDITY,
        SensorTypeName.AMBIENT_TEMPERATURE, SensorTypeName.PROXIMITY, SensorTypeName.GRAVITY, SensorTypeName.MAGNETOMETER,
        SensorTypeName.ACCELEROMETER, SensorTypeName.LINEAR_ACCELERATION)

    // Arraylist of used sensors
    private var usedSensorsList: MutableList<UsedSensors> = ArrayList()
    //shared view model for data
    private val sharedViewModel: CameraSensorSharedViewModel by activityViewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }


    fun startRecording(sensorList: MutableList<SensorTypeName>, deleteFlag: Boolean) {

        //default sensors or chosen sensors
        if(!deleteFlag && sensorList.isEmpty()){

            initSensors(defaultSensorList)

        } else {
            initSensors(sensorList)
        }

    }


    fun stopRecording(){

        /*stop listening to sensors*/
        linaccSensor?.stopListening()
        accSensor?.stopListening()
        lightSensor?.stopListening()
        magSensor?.stopListening()
        gravitySensor?.stopListening()
        tempSensor?.stopListening()
        proximitySensor?.stopListening()
        pressureSensor?.stopListening()
        humiditySensor?.stopListening()
        gyroscopeSensor?.stopListening()
        rotationVectorSensor?.stopListening()
        //store everything before deleting
        storeData()
        //generated used sensor list from available sensor recordings
        defaultSensorList()
        //empty global lists for next measurement
        sensorlinAccRecording = null
        sensorAccRecording = null
        sensorLightRecording = null
        sensorMagRecording = null
        sensorGravRecording = null
        sensorTempRecording = null
        sensorProxRecording = null
        sensorPresRecording = null
        sensorHumiRecording = null
        sensorGyroRecording = null
        sensorRotationVectorRecording = null

        //arraylist of single sensordata data class entries (each containing a timestamp and the sensor data)
        linAccdataList = ArrayList()
        accdataList= ArrayList()
        lightdataList= ArrayList()
        magdataList= ArrayList()
        gravitydataList = ArrayList()
        tempdataList = ArrayList()
        proxdataList = ArrayList()
        pressuredataList= ArrayList()
        humidataList = ArrayList()
        gyrodataList= ArrayList()
        rotationVectordataList = ArrayList()
        //und weil es so viel Spaß macht deinit sensors
        linaccSensor = null
        accSensor= null
        lightSensor= null
        magSensor= null
        gravitySensor = null
        tempSensor = null
        proximitySensor = null
        pressureSensor = null
        humiditySensor= null
        gyroscopeSensor = null
        rotationVectorSensor = null

    }

    private fun initSensors(sensorList: MutableList<SensorTypeName>) {
        //sensorList chosen sensors by user from SensorFragmentView via CameraPreviewFragment
       // Log.d("SensorStart", sensorList.toString())
        for (us in sensorList) {
            if (us == SensorTypeName.MAGNETOMETER) {
                magSensor = MagnetometerSensor(appContext)
                magSensor?.startListening()
                if(magSensor?.doesSensorExist() == true)
                    getData(SensorTypeName.MAGNETOMETER, magSensor!!, magdataList)
            }

            if (us == SensorTypeName.LINEAR_ACCELERATION){
                linaccSensor = LinAccSensor(appContext)
                linaccSensor?.startListening()
                if(linaccSensor?.doesSensorExist() == true)

                    getData(SensorTypeName.LINEAR_ACCELERATION, linaccSensor!!, linAccdataList)
            }
            if (us == SensorTypeName.ACCELEROMETER) {
                accSensor = AccSensor(appContext)
                accSensor?.startListening()
                if(accSensor?.doesSensorExist() == true)
                  getData(SensorTypeName.ACCELEROMETER, accSensor!!, accdataList)
            }
            if (us == SensorTypeName.LIGHT) {
                lightSensor = LightSensor(appContext)
                lightSensor?.startListening()
                if(lightSensor?.doesSensorExist() == true)
                  getData(SensorTypeName.LIGHT, lightSensor!!, lightdataList)
                Log.d("LightCheck", sensorLightRecording.toString())
            }

            if (us == SensorTypeName.GRAVITY) {
                gravitySensor = GravitySensor(appContext)
                gravitySensor?.startListening()
                if(gravitySensor?.doesSensorExist() == true)

                        getData(SensorTypeName.GRAVITY, gravitySensor!!, gravitydataList)
            }
            if (us == SensorTypeName.AMBIENT_TEMPERATURE) {
                tempSensor = TempSensor(appContext)
                tempSensor?.startListening()
                if(tempSensor?.doesSensorExist() == true)

                        getData(SensorTypeName.AMBIENT_TEMPERATURE, tempSensor!!, tempdataList)

            }
            if (us == SensorTypeName.PROXIMITY) {
                proximitySensor = ProximitySensor(appContext)
                proximitySensor?.startListening()
                if(proximitySensor?.doesSensorExist() == true)

                        getData(SensorTypeName.PROXIMITY, proximitySensor!!, proxdataList)
            }
            if (us == SensorTypeName.PRESSURE) {
                pressureSensor = PressureSensor(appContext)
                pressureSensor?.startListening()
                if(pressureSensor?.doesSensorExist() == true)

                        getData(SensorTypeName.PRESSURE, pressureSensor!!, pressuredataList)

            }
            if (us == SensorTypeName.RELATIVE_HUMIDITY) {
                humiditySensor = HumiditySensor(appContext)
                humiditySensor?.startListening()
                if(humiditySensor?.doesSensorExist() == true)

                        getData(SensorTypeName.RELATIVE_HUMIDITY, humiditySensor!!, humidataList)
            }
            if (us == SensorTypeName.GYROSCOPE) {
                gyroscopeSensor = GyroscopeSensor(appContext)
                gyroscopeSensor?.startListening()
                if(gyroscopeSensor?.doesSensorExist() == true)

                        getData(SensorTypeName.GYROSCOPE, gyroscopeSensor!!, gyrodataList)
            }
            if (us == SensorTypeName.ROTATION_VECTOR) {
                rotationVectorSensor = RotationVectorSensor(appContext)
                rotationVectorSensor?.startListening()
                if(rotationVectorSensor?.doesSensorExist() == true)
                  getData(
                        SensorTypeName.ROTATION_VECTOR,
                        rotationVectorSensor!!,
                        rotationVectordataList
                    )
            }
        }

    }


    private fun startListening(){

        linaccSensor?.startListening()
        accSensor?.startListening()
        lightSensor?.startListening()
        magSensor?.startListening()
        gravitySensor?.startListening()
        tempSensor?.startListening()
        proximitySensor?.startListening()
        pressureSensor?.startListening()
        humiditySensor?.startListening()
        gyroscopeSensor?.startListening()
        rotationVectorSensor?.startListening()

    }
    private fun getData(sensorName: SensorTypeName, sensorObject: SingleSensor, dataList: MutableList<SensorData>) {
        var sensorName = sensorName
        var tmpSensorRecording:SensorRecording? = null
        sensorObject?.setOnSensorValuesChangedListener { values ->
            var readingPoint: ArrayList<Any> = ArrayList()
            //readingPoint.set(0, values.values[0])
            for(i in 0..values.values.size-1) {
                readingPoint+=values.values[i]
            }
            val tmpData = SensorData(values.timestamp, readingPoint)

            //for dev purpose
            //Log.d("Sensorname", sensorName.toString())
            //Log.d("tmpData", tmpData.toString())

            //add for each sensor measurement
            dataList += tmpData!!
            //for dev purpose
            //Log.d("dataList", dataList.toString())
            for (sensor in dataList) {
               // Log.d("All_SensorEntries", sensor.toString())
            }

            tmpSensorRecording = SensorRecording(sensorName, dataList)
            if(sensorName == SensorTypeName.ACCELEROMETER)
                sensorAccRecording = tmpSensorRecording
            if(sensorName == SensorTypeName.LIGHT)
                sensorLightRecording = tmpSensorRecording
            if(sensorName == SensorTypeName.MAGNETOMETER)
                sensorMagRecording = tmpSensorRecording
            if(sensorName == SensorTypeName.GRAVITY)
                sensorGravRecording = tmpSensorRecording
            if(sensorName == SensorTypeName.AMBIENT_TEMPERATURE)
                sensorTempRecording = tmpSensorRecording
            if(sensorName == SensorTypeName.PROXIMITY)
                sensorProxRecording = tmpSensorRecording
            if(sensorName == SensorTypeName.PRESSURE)
                sensorPresRecording = tmpSensorRecording
            if(sensorName == SensorTypeName.RELATIVE_HUMIDITY)
                sensorHumiRecording = tmpSensorRecording
            if(sensorName == SensorTypeName.LINEAR_ACCELERATION)
                sensorlinAccRecording = tmpSensorRecording
            if(sensorName == SensorTypeName.GYROSCOPE)
                sensorGyroRecording = tmpSensorRecording
            if(sensorName == SensorTypeName.ROTATION_VECTOR)
                sensorRotationVectorRecording = tmpSensorRecording
           // Log.d("TempRecordiing", sensorLightRecording.toString())
        }

    }


    fun storeData() {

        val data = mutableListOf<SensorRecording>()
        if(sensorAccRecording != null){
            data+=sensorAccRecording!!
        }
        if(sensorPresRecording != null) {
            data += sensorPresRecording!!
        }
        if(sensorGravRecording != null) {
            data += sensorGravRecording!!
        }
        if(sensorLightRecording != null) {
            data += sensorLightRecording!!
        }
        if(sensorGyroRecording != null) {
            data += sensorGyroRecording!!
        }
        if(sensorHumiRecording != null) {
            data += sensorHumiRecording!!
        }
        if(sensorMagRecording != null) {
            data += sensorMagRecording!!
        }
        if(sensorProxRecording != null) {
            data += sensorProxRecording!!
        }
        if(sensorTempRecording != null) {
            data += sensorTempRecording!!
        }
        if(sensorlinAccRecording != null) {
            data += sensorlinAccRecording!!
        }
        if(sensorRotationVectorRecording != null) {
            data += sensorRotationVectorRecording!!
        }

        //all sensor data from one video recording in global variable
        storeAllData = data

    }

    fun saveCameraTimeStampsToSensorValues(cameraFrameTimeStamps: SensorRecording){

        storeAllData!!.add(cameraFrameTimeStamps)

    }

    fun addSensortoList(sensorRecording: SensorRecording) {
        this.storeAllData?.add(sensorRecording)
        print("DENNIS SENSOR ADDED")
    }


    // sensor recording data to json used in CameraPreview
    fun createJsonData(): String{
        var gson = Gson()
        var jsonList = gson.toJson(storeAllData)
        Log.d("json: ", jsonList.toString())
        //Log.d("testdat", data.toString())
        //Log.d("List of all sensorRecordings", data.toString())
        storeAllData = null
        return jsonList
    }
    //not sure if I need extra, used in defualt sensors
    private fun <T> createInternalJsonData(data: List<T>): String{
        var gson = Gson()
        var jsonList = gson.toJson(data)
        //Log.d("json: ", jsonList.toString())
        //Log.d("testdat", data.toString())
        //Log.d("List of all sensorRecordings", data.toString())
        return jsonList
    }
    //List of used Sensors
    private fun defaultSensorList() {

        var usedSensor: UsedSensors
        //sensor_delay hard coded at the moment: 200 000 µs --> singleSensor startListening() if changes are applied
        //light sensor sampling frequency will not be affected, remains at 0
        if (sensorlinAccRecording != null){
            usedSensor = UsedSensors((sensorlinAccRecording!!.sensorTypeName?.name), "[m/s^2]", 200000, linaccSensor?.getmanufacturer())
            usedSensorsList += usedSensor//add to arrayList
        }
        if (sensorAccRecording != null){
            usedSensor = UsedSensors((sensorAccRecording!!.sensorTypeName?.name), "[m/s^2]", 200000, accSensor?.getmanufacturer())
            usedSensorsList += usedSensor//add to arrayList
        }
        if (sensorLightRecording != null){
            usedSensor = UsedSensors((sensorLightRecording!!.sensorTypeName?.name), "[lx]", 0, lightSensor?.getmanufacturer())
            usedSensorsList += usedSensor//add to arrayList
        }
        if (sensorMagRecording != null){
            usedSensor = UsedSensors((sensorMagRecording!!.sensorTypeName?.name), "[µT]", 200000, magSensor?.getmanufacturer())
            usedSensorsList += usedSensor//add to arrayList
        }
        if (sensorGravRecording != null){
            usedSensor = UsedSensors((sensorGravRecording!!.sensorTypeName?.name), "[m/s^2]", 200000, gravitySensor?.getmanufacturer())
            usedSensorsList += usedSensor//add to arrayList
        }
        if (sensorTempRecording != null){
            usedSensor = UsedSensors((sensorTempRecording!!.sensorTypeName?.name), "[°C]", 200000, tempSensor?.getmanufacturer())
            usedSensorsList += usedSensor//add to arrayList
        }
        if (sensorProxRecording != null){
            usedSensor = UsedSensors((sensorProxRecording!!.sensorTypeName?.name), "[cm]", 200000, proximitySensor?.getmanufacturer())
            usedSensorsList += usedSensor//add to arrayList
        }
        if (sensorPresRecording != null){
            usedSensor = UsedSensors((sensorPresRecording!!.sensorTypeName?.name), "[hPa]", 200000, pressureSensor?.getmanufacturer())
            usedSensorsList += usedSensor//add to arrayList
        }
        if (sensorHumiRecording != null){
            usedSensor = UsedSensors((sensorHumiRecording!!.sensorTypeName?.name), "[%]", 200000, humiditySensor?.getmanufacturer())
            usedSensorsList += usedSensor//add to arrayList
        }
        if (sensorGyroRecording != null){
            usedSensor = UsedSensors((sensorGyroRecording!!.sensorTypeName?.name), "[rad/s]", 200000, gyroscopeSensor?.getmanufacturer())
            usedSensorsList += usedSensor//add to arrayList
        }
        if (sensorRotationVectorRecording != null){
            usedSensor = UsedSensors((sensorRotationVectorRecording!!.sensorTypeName?.name), "[n/a]", 200000, rotationVectorSensor?.getmanufacturer())
            usedSensorsList += usedSensor//add to arrayList
        }

        Log.d("usedsensorlist", usedSensorsList.toString())

    }

    fun getSensorList(): MutableList<UsedSensors> {
        //Log.d("usedsensorlist", usedSensorsList.toString())
        var tmpusedSensorsList = usedSensorsList
        usedSensorsList = ArrayList()
        return tmpusedSensorsList
    }



}