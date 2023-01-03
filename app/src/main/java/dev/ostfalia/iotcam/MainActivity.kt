package dev.ostfalia.iotcam

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager

import android.os.Bundle
import android.provider.Settings
import android.util.Log

import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.location.*
import dagger.hilt.android.AndroidEntryPoint
import dev.ostfalia.iotcam.databinding.ActivityMainBinding
import dev.ostfalia.iotcam.ui.ToastFactory
import dev.ostfalia.iotcam.ui.fragments.CameraSensorSharedViewModel


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    val permissionRequest: MutableList<String> = ArrayList()
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var isCameraPermissionGaranted: Boolean = false
    private var isLocationPermissionGaranted: Boolean = false
    private var isReadPermissionGaranted: Boolean = false
    private var isRecordRDPermissionGaranted: Boolean = false
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val sharedViewModel: CameraSensorSharedViewModel by viewModels()

    companion object {
        /** Combination of all flags required to put activity into immersive mode */
        const val FLAGS_FULLSCREEN =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        /** Milliseconds used for UI animations */
        const val ANIMATION_SUPERFAST_MILLIS = 5L
        const val ANIMATION_FAST_MILLIS = 50L
        const val ANIMATION_SLOW_MILLIS = 100L
        private const val IMMERSIVE_FLAG_TIMEOUT = 500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                // these ids should match the item ids from my_fragment_menu.xml file
                R.id.action_settings -> {
                    ToastFactory().showToast(this, "App Version: ${BuildConfig.VERSION_NAME}")

                    true
                }
                else -> false
            }
        }

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener {
            findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.action_WelcomeFragment_to_LibraryFragment)
        }

        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                isRecordRDPermissionGaranted =
                    permissions[Manifest.permission.CAMERA] ?: isRecordRDPermissionGaranted
                isLocationPermissionGaranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION]
                    ?: isLocationPermissionGaranted
                isReadPermissionGaranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE]
                    ?: isReadPermissionGaranted
                isRecordRDPermissionGaranted =
                    permissions[Manifest.permission.RECORD_AUDIO] ?: isRecordRDPermissionGaranted

            }

    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }



    override fun onResume() {
        super.onResume()
        requestPermission()
        //in the phone settings GPS location "Standort Berechtigungen" need to be enabled per hand by the user
        checkGpsStatus()
        requestLocation()

    }

    //the permissions necessary for an action are implemented in this method.
    //or case where a permission has not already been accepted by the user
    // it must be added to the list of permissions to submit to the user
    fun requestPermission() {
        val locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
        isCameraPermissionGaranted = (ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED)

        isLocationPermissionGaranted = (ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)

        isReadPermissionGaranted = (ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)


        isRecordRDPermissionGaranted = (ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED)

        if (!isCameraPermissionGaranted) {
            permissionRequest.add(Manifest.permission.CAMERA)
        }

        if (!isLocationPermissionGaranted) {
            permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (!isReadPermissionGaranted) {
            permissionRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (!isRecordRDPermissionGaranted) {
            permissionRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissionRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionRequest.toTypedArray())
        }
    }


    private fun isCameraPresentInPhone(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    }
    private fun checkGpsStatus() {
        val locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // GPS is enabled
        } else {
            showGPSDialog(this)
        }
    }

    fun showGPSDialog(context: Context){
        val builder = AlertDialog.Builder(context)
        builder.setMessage("GPS is disabled. Please enable for the duration of this App. When GPS is enabled push back button to return to the App")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)

            }
            .setNegativeButton("No"){ dialog, _ ->
                dialog.cancel()
            }
        val alert = builder.create()
        alert.show()
    }

    @SuppressLint("MissingPermission")
    fun requestLocation() {

        GPSLocationManager.Builder.create(this).request()
        { result ->

            var location: Location = result
            if (location != null) {
                Log.d("storGPS", location.toString())
                sharedViewModel.storeGPSData(location)
            }
        }
    }



    fun  stopGPSCallBack(){
        GPSLocationManager.stopGPSCallBack(this)
    }


}
