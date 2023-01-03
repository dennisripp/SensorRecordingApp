package dev.ostfalia.iotcam

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.location.Location
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import java.lang.ref.WeakReference

object GPSLocationManager {

    private lateinit var activity: WeakReference<Activity>
    private lateinit var locationRequest: LocationRequest
    private lateinit var onUpdateLocation: WeakReference<(location: Location) -> Unit>

    private var interval: Long = 10000
    private var fastestIntervall: Long = 1000
    private var priority: Int = LocationRequest.PRIORITY_HIGH_ACCURACY

    private val locationCallback = object  : LocationCallback(){
        override fun onLocationAvailability(p0: LocationAvailability) {
            if(p0.isLocationAvailable){
                activity.get()?.let {
                    //open GPS settings
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    it.startActivity(intent)
                }
            }
        }

        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            if(locationResult?.lastLocation == null){
                Log.d("onLocationResult", "lastLocation is not available")
            }
            onUpdateLocation.get()?.invoke(locationResult.lastLocation!!)

        }
    }

    object Builder{
        fun build(): Builder{
            return this
        }
       fun create(activity: Activity): GPSLocationManager {

           GPSLocationManager.activity = WeakReference(activity)
           locationRequest = LocationRequest.create()
           locationRequest.setInterval(0)
           locationRequest.setFastestInterval(fastestIntervall)
           locationRequest.setSmallestDisplacement(0f)
           locationRequest.priority = priority
         // locationRequest.setNumUpdates(0)

           return GPSLocationManager
       }
    }

    fun request(onUpdateLocation: (location: Location) -> Unit){
        this.onUpdateLocation = WeakReference(onUpdateLocation)
        requestWithoutService()
    }

    @SuppressLint("MissingPermission")
    private fun requestWithoutService() {
        activity.get()?.let{
            LocationServices.getFusedLocationProviderClient(it).requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper())
        }
    }

    fun stopGPSCallBack(activity: Activity) {
        stop(activity)
    }

    private fun stop(activity: Activity){
        LocationServices.getFusedLocationProviderClient(activity).removeLocationUpdates(
            locationCallback)
    }
}
