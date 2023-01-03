package dev.ostfalia.iotcam.Sensors


import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor

//add sensors here
class AccSensor(
    context: Context
):  SingleSensor(
    context = context,
    sensorType = Sensor.TYPE_ACCELEROMETER //[m/s^2], acceleration minus Gx, Gy, Gz for each axis
)
class LinAccSensor(
    context: Context
):  SingleSensor(
     context = context,
    sensorType = Sensor.TYPE_LINEAR_ACCELERATION
)

class LightSensor(
    context: Context
):  SingleSensor(
    context = context,
    sensorType = Sensor.TYPE_LIGHT // one value in lux
)

class GyroscopeSensor(
    context: Context
):  SingleSensor(
    context = context,
    sensorType = Sensor.TYPE_GYROSCOPE // radians/s, rotation is positive and counter-clockwise
)

class MagnetometerSensor(
    context: Context
):  SingleSensor(
    context = context,
    sensorType = Sensor.TYPE_MAGNETIC_FIELD // all values in µT x,y,z axis
)

class TempSensor(
    context: Context
):  SingleSensor(
    context = context,
    sensorType = Sensor.TYPE_AMBIENT_TEMPERATURE
)

class ProximitySensor(
    context: Context
):  SingleSensor(
    context = context,
    sensorType = Sensor.TYPE_PROXIMITY
)

class PressureSensor(
    context: Context
):  SingleSensor(
    context = context,
    sensorType = Sensor.TYPE_PRESSURE // atmospheric pressure in mbar
)

class HumiditySensor(
    context: Context
):  SingleSensor(
    context = context,
    sensorType = Sensor.TYPE_RELATIVE_HUMIDITY //relative ambient air humidity in percent
)

class GravitySensor(
    context: Context
):  SingleSensor(
    context = context,
       sensorType = Sensor.TYPE_GRAVITY
)



//    ROTATION_VECTOR virtual sensor
class RotationVectorSensor(
    context: Context
):  SingleSensor(
    context = context,
    sensorType = Sensor.TYPE_ROTATION_VECTOR
)