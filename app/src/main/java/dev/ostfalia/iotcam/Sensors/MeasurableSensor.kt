package dev.ostfalia.iotcam.Sensors

abstract class MeasurableSensor(
    protected val sensorType: Int
) {

    protected var onSensorValuesChanged: ((SensorTupel) -> Unit)? = null

    abstract fun doesSensorExist(): Boolean

    abstract fun startListening()
    abstract fun stopListening()

    fun setOnSensorValuesChangedListener(listener: (SensorTupel) -> Unit) {
        onSensorValuesChanged = listener
    }
}