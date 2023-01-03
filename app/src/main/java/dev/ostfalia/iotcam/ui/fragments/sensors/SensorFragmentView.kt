package dev.ostfalia.iotcam.ui.fragments.sensors

import android.content.Context.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView.CHOICE_MODE_MULTIPLE
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.ListFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.ostfalia.iotcam.R
import dev.ostfalia.iotcam.Sensors.*
import dev.ostfalia.iotcam.databinding.FragmentSensorBinding
import dev.ostfalia.iotcam.ui.fragments.CameraSensorSharedViewModel
import okhttp3.internal.notify
import okhttp3.internal.notifyAll

@AndroidEntryPoint
class SensorFragmentView : ListFragment(), AdapterView.OnItemClickListener{

    /** Android ViewBinding */
    private var _binding: FragmentSensorBinding? = null
    //    // This property is only valid between onCreateView and
    //    // onDestroyView.
    private val binding get() = _binding!!

    //Sensor Manager
    private lateinit var sensorManager: SensorManager
    // SensorData class
    private var accData:SensorData? = null
    // SensorRecording class
    private val sensorAccRecording:SensorRecording? = null
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

    private var sensorListTypeName: MutableList<SensorTypeName> = ArrayList()
    private var sensorsonDevice: MutableList<SingleSensor> = ArrayList()
    private var sensorListTypeNameAdapter: MutableList<SensorTypeName> = ArrayList()
    private var sensorListTypeNameAfterSelection: MutableList<SensorTypeName> = ArrayList()

    //listview test
    private var  lisView: ListView? = null
    private var  arrayAdapter: ArrayAdapter<SensorTypeName>? = null

    private val sharedViewModel: CameraSensorSharedViewModel by activityViewModels()

    //
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentSensorBinding.inflate(inflater, container, false)
       // sharedViewModel = ViewModelProvider(requireActivity()).get(CameraSensorSharedViewModel::class.java)
        return binding.root

    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //for developing purposes
        if (sensorListTypeName.isEmpty()) {
            initSensors()
            sensorListTypeNameAdapter = sensorListTypeName
        }

        lisView = binding.list
        //Adapter for Listview
        arrayAdapter = ArrayAdapter(requireContext(),
            android.R.layout.simple_list_item_multiple_choice,
            sensorListTypeNameAdapter!!)

        lisView?.adapter = arrayAdapter
        //Allow multiple choice for the list
        lisView?.choiceMode = CHOICE_MODE_MULTIPLE
        //check all boxes =  sensors currently used as default sensors
        var i = 0
        while (i < sensorListTypeNameAdapter.size) {
            lisView?.setItemChecked(i, true)
            i++
        }
        var allSensorsWereDeleted = false
        lisView?.onItemClickListener = this
        sharedViewModel.restoreDeleteSensorList(false)
        sharedViewModel.saveSensors(sensorListTypeName)
        binding.button1.setOnClickListener{
            //save sensors

            sharedViewModel.saveSensors(sensorListTypeNameAfterSelection, )
            var test = sharedViewModel.getSensorList()
            Log.d("List_back", test.toString())

        }
        binding.button3.setOnClickListener{
            //delete list in shared ViewModel
            allSensorsWereDeleted = true
            sharedViewModel.deleteSensorList(allSensorsWereDeleted)
            var i = 0
            while (i < sensorListTypeNameAdapter.size) {
                lisView?.setItemChecked(i, false)
                i++
            }
            sensorListTypeNameAfterSelection = ArrayList()

        }


        binding.list.onItemClickListener = AdapterView.OnItemClickListener {
                parent, view, position, id ->

           val selectedItemText = parent.getItemAtPosition(position)
            Log.d("OnitemSelect", selectedItemText.toString())

            if (selectedItemText !in sensorListTypeNameAfterSelection) {
                sensorListTypeNameAfterSelection?.add(selectedItemText as SensorTypeName)
            } else {
                sensorListTypeNameAfterSelection?.remove(selectedItemText as SensorTypeName)
            }
            Log.d("AfterSelect", sensorListTypeNameAfterSelection.toString())
        }
        //selecting sensor dev

    }
    override fun onItemClick(parent: AdapterView<*>?, v: View, position: Int, id: Long) {
        var options: String = parent?.getItemAtPosition(position) as String
    }
    fun getSensorSelection(): List<SensorTypeName>{
        return sensorListTypeNameAfterSelection
    }

    private fun initSensors() {
        Log.d("testing", "testtest")
        linaccSensor = LinAccSensor(requireContext())
        if(linaccSensor?.doesSensorExist() == true){
            sensorsonDevice += linaccSensor!!
            sensorListTypeName += SensorTypeName.LINEAR_ACCELERATION
        }
        accSensor = AccSensor(requireContext())
        if(accSensor?.doesSensorExist() == true){
            sensorsonDevice += accSensor!!
            sensorListTypeName += SensorTypeName.ACCELEROMETER
        }
        lightSensor = LightSensor(requireContext())
        if(lightSensor?.doesSensorExist() == true){
            sensorsonDevice += lightSensor!!
            sensorListTypeName += SensorTypeName.LIGHT
        }
        magSensor = MagnetometerSensor(requireContext())
        if(magSensor?.doesSensorExist() == true){
            sensorsonDevice += magSensor!!
            sensorListTypeName += SensorTypeName.MAGNETOMETER
        }
        gravitySensor = GravitySensor(requireContext())
        if(linaccSensor?.doesSensorExist() == true){
            sensorsonDevice += gravitySensor!!
            sensorListTypeName += SensorTypeName.GRAVITY
        }
        tempSensor = TempSensor(requireContext())
        if(tempSensor?.doesSensorExist() == true){
            sensorsonDevice += tempSensor!!
            sensorListTypeName += SensorTypeName.AMBIENT_TEMPERATURE
        }
        proximitySensor = ProximitySensor(requireContext())
        if(proximitySensor?.doesSensorExist() == true){
            sensorsonDevice += proximitySensor!!
            sensorListTypeName += SensorTypeName.PROXIMITY
        }
        pressureSensor = PressureSensor(requireContext())
        if(pressureSensor?.doesSensorExist() == true){
            sensorsonDevice += pressureSensor!!
            sensorListTypeName += SensorTypeName.PRESSURE
        }
        humiditySensor = HumiditySensor(requireContext())
        if(humiditySensor?.doesSensorExist() == true){
            sensorsonDevice += humiditySensor!!
            sensorListTypeName += SensorTypeName.RELATIVE_HUMIDITY
        }
        gyroscopeSensor = GyroscopeSensor(requireContext())
        if(gyroscopeSensor?.doesSensorExist() == true){
            sensorsonDevice += gyroscopeSensor!!
            sensorListTypeName += SensorTypeName.GYROSCOPE
        }
        rotationVectorSensor = RotationVectorSensor(requireContext())
        if(rotationVectorSensor?.doesSensorExist() == true){
            sensorsonDevice += rotationVectorSensor!!
            sensorListTypeName += SensorTypeName.ROTATION_VECTOR
        }
        Log.d("sensorListTypeName", sensorListTypeName.toString())
        sensorListTypeNameAfterSelection = sensorListTypeName.toList() as MutableList<SensorTypeName>
        //checkSensors()
    }

    private fun checkSensors(){

        //start SensorListener
        sensorManager = activity?.getSystemService(SENSOR_SERVICE) as SensorManager
        val deviceSensors: List<Sensor> = sensorManager.getSensorList(Sensor.TYPE_ALL)

        for (sensor in deviceSensors) {
            Log.d("All_SensorsonPhone", sensor.toString())
        }

    }





    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

