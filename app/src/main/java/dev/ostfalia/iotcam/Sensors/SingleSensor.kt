package dev.ostfalia.iotcam.Sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

//trial for abstract not finished yet
abstract class SingleSensor(
    private val context: Context,
    //private val sensorFeature: String,
    sensorType: Int,
): MeasurableSensor(sensorType), SensorEventListener {


    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    override fun doesSensorExist(): Boolean {
        if(!::sensorManager.isInitialized && sensor == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensor = sensorManager.getDefaultSensor(sensorType)
        }
        if (sensorManager.getDefaultSensor(sensorType) != null) {
           return true
        }
        return  false
    }


    override fun startListening() {
        if(!doesSensorExist()) {
            return
        }
        if(!::sensorManager.isInitialized && sensor == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensor = sensorManager.getDefaultSensor(sensorType)
        }
        //here you can change sensor sampling frequency
        sensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

    }

    override fun stopListening() {
        if(!doesSensorExist() || !::sensorManager.isInitialized) {
            return
        }
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(!doesSensorExist()) {
            return
        }
       if(event?.sensor?.type == sensorType) {
           var eventData = SensorTupel( event.values.toList(), event.timestamp)
           onSensorValuesChanged?.invoke(eventData)

       }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) = Unit

    fun getmanufacturer(): String{
        if(!doesSensorExist()) {
            return "sensor does not exist"
        }
        if(!::sensorManager.isInitialized && sensor == null) {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensor = sensorManager.getDefaultSensor(sensorType)
        }
        return sensor?.vendor!!

    }
}