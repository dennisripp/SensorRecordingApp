package dev.ostfalia.iotcam.ui.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.hardware.camera2.*
import android.hardware.camera2.params.DynamicRangeProfiles
import android.hardware.camera2.params.OutputConfiguration
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaScannerConnection
import android.os.*
import android.util.Log
import android.util.Range
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.slider.LabelFormatter.LABEL_GONE
import com.google.android.material.slider.Slider
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import dev.ostfalia.iotcam.BuildConfig
import dev.ostfalia.iotcam.MainActivity
import dev.ostfalia.iotcam.camera2.*
import dev.ostfalia.iotcam.camera2.CameraInfo.*
import dev.ostfalia.iotcam.databinding.FragmentCameraPreviewBinding
import dev.ostfalia.iotcam.network.*
import dev.ostfalia.iotcam.network.oauth.Authenticator
import dev.ostfalia.iotcam.ui.ToastFactory
import dev.ostfalia.iotcam.ui.fragments.sensors.SensorData
import dev.ostfalia.iotcam.ui.fragments.sensors.SensorFragmentRecordingCoupled
import dev.ostfalia.iotcam.ui.fragments.sensors.SensorRecording
import dev.ostfalia.iotcam.ui.fragments.sensors.SensorTypeName
import dev.ostfalia.iotcam.utils.EnvironmentMeta
import dev.ostfalia.iotcam.utils.getById
import dev.ostfalia.iotcam.utils.getPreviewOutputSize
import kotlinx.coroutines.*
import java.io.File
import java.lang.Runnable
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@RequiresApi(Build.VERSION_CODES.O)
@AndroidEntryPoint //do not delete Petra needs this for her sensors!!!!!!
class CameraPreviewFragment : Fragment() {

    private val argsPassed: CameraPreviewFragmentArgs by navArgs()
    /** Android ViewBinding */
    private var _fragmentBinding: FragmentCameraPreviewBinding? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val fragmentBinding get() = _fragmentBinding!!
    //meta data collect
    private val sharedViewModel: CameraSensorSharedViewModel by activityViewModels()
    var systemTimeRecordingStart:Long = 0
    var systemTimeRecordingStop: Long = 0

    private val pipeline: Pipeline by lazy {
        if (args.useHardware) {
            HardwarePipeline(
                argsPassed.width,
                argsPassed.height,
                argsPassed.fps,
                args.filterOn,
                characteristics,
                encoder,
                fragmentBinding.viewFinder
            )
        } else {
            SoftwarePipeline(
                argsPassed.width,
                argsPassed.height,
                argsPassed.fps,
                args.filterOn,
                characteristics,
                encoder,
                fragmentBinding.viewFinder
            )
        }
    }



    data class Args(
        var width: Int,
        var height: Int,
        var fps: Int,
        val filterOn: Boolean,
        val cameraId: String,
        val dynamicRange: Long,
        val useHardware: Boolean,
        val previewStabilization: Boolean
    )

    data class PreviewSize(
        var width: Int,
        var height: Int,
    )

    val args = Args(
        1920,
        1080,
        30,
        false,
        "0",
        DynamicRangeProfiles.STANDARD,
        useHardware = true,
        previewStabilization = false
    )

    var itemPicked: Int = 0

/*
    private val pipeline: Pipeline by lazy {
        if (true) {
            HardwarePipeline(args.width, args.height, args.fps, args.filterOn,
                characteristics, encoder, fragmentBinding.viewFinder)
        } else {
            SoftwarePipeline(args.width, args.height, args.fps, args.filterOn,
                characteristics, encoder, fragmentBinding.viewFinder)
        }
    }
*/
    /** AndroidX navigation arguments */
//    private val args: CameraPreviewFragmentArgs by navArgs()

    /** Host's navigation controller */
    private val navController: NavController by lazy {
        Navigation.findNavController(
            requireActivity(), androidx.navigation.R.id.nav_controller_view_tag
        )
    }

    /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */
    private val cameraManager: CameraManager by lazy {
        val context = requireContext().applicationContext
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    /** [CameraCharacteristics] corresponding to the provided Camera ID */
    private val characteristics: CameraCharacteristics by lazy {
        cameraManager.getCameraCharacteristics(args.cameraId)
    }

    /** File where the recording will be saved */
    private val outputFile: File by lazy { createFile(requireContext(), "mp4") }

    /**
     * Setup a [Surface] for the encoder
     */
    private val encoderSurface: Surface by lazy {
        encoder.getInputSurface()
    }

    /** [EncoderWrapper] utility class */
    private val encoder: EncoderWrapper by lazy { createEncoder() }

    /** [HandlerThread] where all camera operations run */
    private val cameraThread = HandlerThread("CameraThread").apply { start() }

    /** [Handler] corresponding to [cameraThread] */
    private val cameraHandler = Handler(cameraThread.looper)

    /** Performs recording animation of flashing screen */
    private val animationTask: Runnable by lazy {
        Runnable {
            // Flash white animation
          //  fragmentBinding.overlay.foreground = Color.argb(30, 255, 255, 255).toDrawable()


            // Wait for ANIMATION_FAST_MILLIS
            fragmentBinding.overlay.postDelayed({
                if (isCurrentlyRecording()) {
                    // Remove white flash animation

                    if(aniIdx > 100) backwards = true
                    if(aniIdx < 0) backwards = false

                    val fac = ParametricBlend(aniIdx.toFloat() / 100)

                    var value: Int = (fac * 100).toInt()
                    fragmentBinding.progressBar.progress = value

                    if(backwards)
                        aniIdx--
                    else
                        aniIdx++


                 //   fragmentBinding.overlay.foreground = null
                    // Restart animation recursively
                    if (isCurrentlyRecording()) {
                        fragmentBinding.overlay.postDelayed(
                            animationTask, MainActivity.ANIMATION_SUPERFAST_MILLIS
                        )
                    }
                }
            }, MainActivity.ANIMATION_SUPERFAST_MILLIS)
        }
    }

    var aniIdx: Int = 0;
    var backwards: Boolean = false

    fun ParametricBlend(t: Float): Float {
        val sqt = t * t
        return sqt / (2.0f * (sqt - t) + 1.0f)
    }



    /** Captures frames from a [CameraDevice] for our video recording */
    private lateinit var session: CameraCaptureSession

    /** The [CameraDevice] that will be opened in this fragment */
    private lateinit var camera: CameraDevice

    /** Requests used for preview only in the [CameraCaptureSession] */
    private val previewRequest: CaptureRequest? by lazy {
        pipeline.createPreviewRequest(session, args.previewStabilization)
    }

    /** Requests used for preview and recording in the [CameraCaptureSession] */
    private val recordRequest: CaptureRequest by lazy {
        pipeline.createRecordRequest(session, args.previewStabilization)
    }

    private var recordingStartMillis: Long = 0L

    /** Orientation of the camera as 0, 90, 180, or 270 degrees */
    private val orientation: Int by lazy {
        characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
    }

    @Volatile
    private var recordingStarted = false

    @Volatile
    private var recordingComplete = false

    /** Condition variable for blocking until the recording completes */
    private val cvRecordingStarted = ConditionVariable(false)
    private val cvRecordingComplete = ConditionVariable(false)

    //again petra sensors
    private var chosenSensorList: MutableList<SensorTypeName>? = null
    private var allSensorsDeleteFlag: Boolean = false


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _fragmentBinding = FragmentCameraPreviewBinding.inflate(inflater, container, false)
        fragmentBinding.captureButton.isEnabled = false

        itemPicked = argsPassed.itemNo
        args.width = argsPassed.width
        args.height = argsPassed.height
        args.fps = argsPassed.fps
        initResolutionSlider()

        (activity as MainActivity?)!!.requestLocation()
        return fragmentBinding.root
    }

    private fun setTitle(text: String) {
        (requireActivity() as AppCompatActivity).supportActionBar?.title = text
    }

    private fun initResolutionSlider() {
        val slider = fragmentBinding.resolutionSlider
        val camInfo = CameraInfo.returnfilteredSortedCameraConfigs(requireContext(), "0")

        try {
            slider.value = argsPassed.itemNo.toFloat()
        } catch (e: Exception) {
            slider.value = 0F
        }

        slider.valueFrom = 0F
        slider.valueTo = camInfo.size.toFloat() - 1F
        slider.stepSize = 1F
        slider.labelBehavior = LABEL_GONE

        slider.addOnChangeListener { slider, value, fromUser ->
            val config = camInfo.get(value.toInt())
            args.height = config.size.height
            args.width = config.size.width
            args.fps = config.fps
            setTitle("${config.size.width}x${config.size.height} ${config.fps}fps")
            Log.d("Slider: ", args.height.toString() + " x "  + args.width.toString())
        }

        slider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            @SuppressLint("RestrictedApi")
            override fun onStartTrackingTouch(slider: Slider) {

            }

            @SuppressLint("RestrictedApi")
            override fun onStopTrackingTouch(slider: Slider) {
                itemPicked = slider.value.toInt()
                reopenCameraFragment()
            }
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // If we're displaying HDR, set the screen brightness to maximum. Otherwise, the preview
        // image will appear darker than video playback. It is up to the app to decide whether
        // this is appropriate - high brightness with HDR capture may dissipate a lot of heat.
        // In dark ambient environments, setting the brightness too high may make it uncomfortable
        // for users to view the screen, so apps will need to calibrate this depending on their
        // use case.

        if (args.dynamicRange != DynamicRangeProfiles.STANDARD) {
            val window = requireActivity().getWindow()
            var params = window.getAttributes()
            params.screenBrightness = 1.0f
            window.setAttributes(params)
        }

        super.onCreate(savedInstanceState)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }


    @SuppressLint("MissingPermission")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fragmentBinding.viewFinder.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                pipeline.destroyWindowSurface()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder, format: Int, width: Int, height: Int
            ) = Unit



            override fun surfaceCreated(holder: SurfaceHolder) {

                // Selects appropriate preview size and configures view finder
                val previewSize = getPreviewOutputSize(
                    fragmentBinding.viewFinder.display, characteristics, SurfaceHolder::class.java
                )
                Log.d(
                    TAG,
                    "View finder size: ${fragmentBinding.viewFinder.width} x ${fragmentBinding.viewFinder.height}"
                )
                Log.d(TAG, "Selected preview size: $previewSize")
                fragmentBinding.viewFinder.setAspectRatio(previewSize.width, previewSize.height)

                pipeline.setPreviewSize(previewSize)

                // To ensure that size is set, initialize camera in the view's thread
                fragmentBinding.viewFinder.post {
                    pipeline.createResources(holder.surface)
                    surface = holder
                    initializeCamera(pipeline)

                }

            }
        })
        //Petras Sensors
        chosenSensorList = sharedViewModel.getSensorList()
        allSensorsDeleteFlag = sharedViewModel.getSensorFlag()
        Log.d("Reveived SensorList", chosenSensorList.toString())
    }

    var surface: SurfaceHolder? = null

    private fun isCurrentlyRecording(): Boolean {
        return recordingStarted && !recordingComplete
    }

    private fun createEncoder(): EncoderWrapper {
        val videoEncoder = when {
            args.dynamicRange == DynamicRangeProfiles.STANDARD -> MediaFormat.MIMETYPE_VIDEO_AVC
            args.dynamicRange < DynamicRangeProfiles.PUBLIC_MAX -> MediaFormat.MIMETYPE_VIDEO_HEVC
            else -> throw IllegalArgumentException("Unknown dynamic range format")
        }

        val codecProfile = when {
            args.dynamicRange == DynamicRangeProfiles.HLG10 -> MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10
            args.dynamicRange == DynamicRangeProfiles.HDR10 -> MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10
            args.dynamicRange == DynamicRangeProfiles.HDR10_PLUS -> MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus
            else -> -1
        }

        var width = argsPassed.width
        var height = argsPassed.height
        var orientationHint = orientation

        if (args.useHardware) {
            if (orientation == 90 || orientation == 270) {
                width = argsPassed.height
                height = argsPassed.width
            }
            orientationHint = 0
        }

        return EncoderWrapper(
            width,
            height,
            RECORDER_VIDEO_BITRATE,
            args.fps,
            orientationHint,
            videoEncoder,
            codecProfile,
            outputFile
        )
    }

    /**Sensor Inject
     *sensor recording functionalities
     * */
    @Inject
    lateinit var sensorDataCollect: SensorFragmentRecordingCoupled



    /**
     * Begin all camera operations in a coroutine in the main thread. This function:
     * - Opens the camera
     * - Configures the camera session
     * - Starts the preview by dispatching a repeating request
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun initializeCamera(pipeline: Pipeline) = lifecycleScope.launch(Dispatchers.Main) {
        sharedViewModel.saveCameraInfo(getOrientation(), argsPassed.width, argsPassed.height)
        // Open the selected camera
        camera = openCamera(cameraManager, args.cameraId, cameraHandler)

        // Creates list of Surfaces where the camera will output frames
        val targets = pipeline.getTargets()

        // Start a capture session using our open camera and list of Surfaces where frames will go
        session = createCaptureSession(camera, targets!!, cameraHandler)
        // Sends the capture request as frequently as possible until the session is torn down or
        //  session.stopRepeating() is called
        try {
            if (previewRequest == null) {
                session.setRepeatingRequest(recordRequest!!, null, cameraHandler)
            } else {
                session.setRepeatingRequest(previewRequest!!, null, cameraHandler)
            }
        } catch (e: IllegalArgumentException) {
            println("EXCEPTION CAUGHT ${this.toString()}:  ${e.toString()}")
            reopenCameraFragment()
        }

        val buildeReq = session.device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(targets.get(0))

            if (true) {
                set(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE,
                    CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_PREVIEW_STABILIZATION)
            }
        }

        fragmentBinding.viewFinder.setOnTouchListener(
            FocusOntouchHandler(
                characteristics,
                buildeReq,
                session,
                cameraHandler
            )
        )

        // React to user touching the capture button
        fragmentBinding.captureButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked)  lifecycleScope.launch(Dispatchers.IO) {

                /* If the recording was already started in the past, do nothing. */
                if (!recordingStarted) {
                    // Prevents screen rotation during the video recording
                    requireActivity().requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_LOCKED

                    fragmentBinding.resolutionSlider.isEnabled = false;

                    pipeline.actionDown(encoderSurface)
                    // Finalizes encoder setup and starts recording
                    recordingStarted = true
                    encoder.start()
                    cvRecordingStarted.open()
                    pipeline.startRecording()
                    //start recording with sensors
                    sensorDataCollect.startRecording(chosenSensorList!!, allSensorsDeleteFlag)
                    // Start recording repeating requests, which will stop the ongoing preview
                    //  repeating requests without having to explicitly call
                    //  `session.stopRepeating`
                    if (previewRequest != null) {

                        session.setRepeatingRequest(
                            recordRequest, object : CameraCaptureSession.CaptureCallback() {
                                override fun onCaptureCompleted(
                                    session: CameraCaptureSession,
                                    request: CaptureRequest,
                                    result: TotalCaptureResult
                                ) {
                                    if (isCurrentlyRecording()) {
                                        encoder.frameAvailable()
                                    }
                                }
                            }, cameraHandler
                        )
                        Log.d(TAG, "Recording check356")
                    }
                    systemTimeRecordingStart = System.currentTimeMillis() * 1_000_000
                    //Log.d("CHeckTime", systemTimeRecordingStart.toString())

                    recordingStartMillis = System.currentTimeMillis()
                    Log.d(TAG, "Recording started")

                    // Starts recording animation
                    aniIdx = 0
                    fragmentBinding.overlay.post(animationTask)
                }

            }
            true
            if (!isChecked) lifecycleScope.launch(Dispatchers.Main){
                cvRecordingStarted.block()

                /* Wait for at least one frame to process so we don't have an empty video */
                encoder.waitForFirstFrame()

                session.stopRepeating()

                pipeline.clearFrameListener()


                /* Wait until the session signals onReady */
                cvRecordingComplete.block()

                // Unlocks screen rotation after recording finished
                requireActivity().requestedOrientation =
                    ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

                // Requires recording of at least MIN_REQUIRED_RECORDING_TIME_MILLIS
                val elapsedTimeMillis = System.currentTimeMillis() - recordingStartMillis
                if (elapsedTimeMillis < MIN_REQUIRED_RECORDING_TIME_MILLIS) {
                    delay(MIN_REQUIRED_RECORDING_TIME_MILLIS - elapsedTimeMillis)
                }

                delay(MainActivity.ANIMATION_SLOW_MILLIS)
                //blockbutton so shutting down process is not disrupted
                //fragmentBinding.captureButton.isEnabled = false
                Log.d(TAG, "Recording stopped. Output file: $outputFile")
                encoder.shutdown()
                //stop sensor recording
                sensorDataCollect.stopRecording()
                systemTimeRecordingStop = System.currentTimeMillis() * 1_000_000

                pipeline.cleanup()

                //used Sensors for meta data
                saveUsedSensorList()

                // Broadcasts the media file to the rest of the system
                MediaScannerConnection.scanFile(
                    requireView().context, arrayOf(outputFile.absolutePath), null, null
                )
                sharedViewModel.storeRecordingStartStop(systemTimeRecordingStart, systemTimeRecordingStop)

                //var jsonString = gson.toJson(nw.generateSampleData())


                val camTimeStampsList = encoder.GetFrameTimeStampList()
                val camTimeStamps = SensorData(getMyLongValue(camTimeStampsList.first()), camTimeStampsList)
                var sensorRecording_camTimeStamps = SensorRecording(SensorTypeName.CAM_TIMESTAMPS, mutableListOf<SensorData>(camTimeStamps))
                //was added late to concept, not optimally done
                sensorDataCollect.saveCameraTimeStampsToSensorValues(sensorRecording_camTimeStamps)

                var jsonStringSensorData = sensorDataCollect.createJsonData()

                fragmentBinding.resolutionSlider.isEnabled = true;

                // storing data
                val sensorPath: String = outputFile.getSensorPath()
                val metaPath: String = outputFile.getMetaPath()


                val sensorFile = File(sensorPath)
                val recordMetaFile = File(metaPath)
                sensorFile.printWriter().use { out ->
                    out.println(jsonStringSensorData)
                }


                //ask user to keep or dismiss recorded data
                //and navigate back to camera preview fragment
                showKeepDismissDiaglog(outputFile, sensorFile, recordMetaFile)
            }
            true
        }
    }

    fun getMyLongValue(vararg any: Any) : Long {
        return when(val tmp = any.first()) {
            is Number -> tmp.toLong()
            else -> throw Exception("not a number") // or do something else reasonable for your case
        }
    }

    private var builder: MaterialAlertDialogBuilder? = null

    private fun showKeepDismissDiaglog(videoFile: File, sensorFile: File, recordMetaFile: File) {
        val applicationContext = requireActivity()

        builder = MaterialAlertDialogBuilder(applicationContext)
        builder!!.setMessage("keep recording and meta?").setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->

                val metaSampleData = sharedViewModel.generateSampleData()
                var enumDialogBuilder = MaterialAlertDialogBuilder(requireContext())
                val asc = Array<CharSequence>(EnvironmentMeta.values().size) { i -> getById(i).toString() }

                enumDialogBuilder.setCancelable(false)
                enumDialogBuilder!!.setItems(asc,
                    DialogInterface.OnClickListener { dialog, which ->
                        //TODO: using free spot, replace !!
                        metaSampleData.freeText = getById(which)?.name
                        metaSampleData.appVersion = BuildConfig.VERSION_NAME
                        metaSampleData.projectID = argsPassed.projectID
                        metaSampleData.recordingStarted = systemTimeRecordingStart
                        metaSampleData.recordingStopped = systemTimeRecordingStop

                        var jsonString = Gson().toJson(metaSampleData)

                        recordMetaFile.printWriter().use { out ->
                            out.println(jsonString)
                        }
                        ToastFactory().showToast(requireContext(), "picked: ${getById(which)?.name}\nfile saved")
                        reopenCameraFragment()
                        dialog.dismiss()
                    })

                val enumDialog = enumDialogBuilder!!.create()
                enumDialog.setTitle("pick your environment")
                enumDialog.show()

            }.setNegativeButton("No") { dialog, id -> //  Action for 'NO' Button
                dialog.cancel()
                videoFile.delete()
                sensorFile.delete()
                recordMetaFile.delete()
                Toast.makeText(
                    applicationContext, "files deleted", Toast.LENGTH_SHORT
                ).show()
                reopenCameraFragment()
            }

        val alert = builder!!.create()
        alert.setTitle("recording finished")
        alert.show()
    }

    private fun reopenCameraFragment() {
        val navControllers = findNavController()
        navControllers.navigate(
            CameraPreviewFragmentDirections.actionCameraToCamera(
                args.height,
                args.width,
                args.fps,
                itemPicked,
                argsPassed.projectID
            )
        )

    }

    //Orientation for MetaData
    private fun getOrientation(): String {
        try {

            var sensorOrientation =  characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
            var deviceOrientation = OrientationEventListener.ORIENTATION_UNKNOWN
            deviceOrientation = (deviceOrientation + 45) / 90 * 90
            var calcOrientation = (sensorOrientation + deviceOrientation + 360) % 360

            Log.d("getOrientation", calcOrientation.toString())

            if (calcOrientation == 90){
                return "portrait"
            }
            if (calcOrientation == 180){
                return "landscape"
            }
            return "undetermined"
        } catch (e: Exception){
            return "na"
        }


    }

    @Inject
    lateinit var authenticator: Authenticator;

    private val authorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            authenticator.handleAuthorizationResponse(result.data!!)
        }
    }

    /** Opens the camera and returns the opened device (as the result of the suspend coroutine) */
    @SuppressLint("MissingPermission")
    private suspend fun openCamera(
        manager: CameraManager, cameraId: String, handler: Handler? = null
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) = cont.resume(device)

            override fun onDisconnected(device: CameraDevice) {
                Log.w(TAG, "Camera $cameraId has been disconnected")
                requireActivity().finish()
            }

            override fun onError(device: CameraDevice, error: Int) {
                val msg = when (error) {
                    ERROR_CAMERA_DEVICE -> "Fatal (device)"
                    ERROR_CAMERA_DISABLED -> "Device policy"
                    ERROR_CAMERA_IN_USE -> "Camera in use"
                    ERROR_CAMERA_SERVICE -> "Fatal (service)"
                    ERROR_MAX_CAMERAS_IN_USE -> "Maximum cameras in use"
                    else -> "Unknown"
                }
                val exc = RuntimeException("Camera $cameraId error: ($error) $msg")
                Log.e(TAG, exc.message, exc)
                if (cont.isActive) cont.resumeWithException(exc)
            }
        }, handler)
    }

    /**
     * Creates a [CameraCaptureSession] with the dynamic range profile set.
     */
    private fun setupSessionWithDynamicRangeProfile(
        device: CameraDevice,
        targets: List<Surface>,
        handler: Handler? = null,
        stateCallback: CameraCaptureSession.StateCallback
    ): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val outputConfigs = mutableListOf<OutputConfiguration>()
            for (target in targets) {
                val outputConfig = OutputConfiguration(target)
                outputConfig.setDynamicRangeProfile(args.dynamicRange)
                outputConfigs.add(outputConfig)
            }

            device.createCaptureSessionByOutputConfigurations(
                outputConfigs, stateCallback, handler
            )
            return true
        } else {
            device.createCaptureSession(targets, stateCallback, handler)
            return false
        }
    }

    /**
     * Creates a [CameraCaptureSession] and returns the configured session (as the result of the
     * suspend coroutine)
     */
    private suspend fun createCaptureSession(
        device: CameraDevice, targets: List<Surface>, handler: Handler? = null
    ): CameraCaptureSession = suspendCoroutine { cont ->
        val stateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) = cont.resume(session)

            override fun onConfigureFailed(session: CameraCaptureSession) {
                val exc = RuntimeException("Camera ${device.id} session configuration failed")
                Log.e(TAG, exc.message, exc)
                cont.resumeWithException(exc)
            }

            /** Called after all captures have completed - shut down the encoder */
            override fun onReady(session: CameraCaptureSession) {
                if (!isCurrentlyRecording()) {
                    return
                }

                recordingComplete = true
                pipeline.stopRecording()
                cvRecordingComplete.open()
            }
        }

        setupSessionWithDynamicRangeProfile(device, targets, handler, stateCallback)
    }

    override fun onResume() {
        super.onResume()
        getActivity()?.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        fragmentBinding.captureButton.isEnabled = true
    }

    override fun onStop() {
        super.onStop()
        try {
            getActivity()?.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
            camera.close()

        } catch (exc: Throwable) {
            Log.e(TAG, "Error closing camera", exc)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraThread.quitSafely()
        encoderSurface.release()
        //(activity as MainActivity?)!!.stopGPSCallBack()

    }

    companion object {
        private val TAG = CameraPreviewFragment::class.java.simpleName

        private const val RECORDER_VIDEO_BITRATE: Int = 10_000_000
        private const val MIN_REQUIRED_RECORDING_TIME_MILLIS: Long = 1000L

        /** Creates a [File] named with the current date and time */
        @RequiresApi(Build.VERSION_CODES.O)
        private fun createFile(context: Context, extension: String): File {
            val sm = StorageManager(context)
            val videofilename = sm.getVideoFileName(extension)
            val videofilepath = sm.getVideoFileDir()

            return File(videofilepath, videofilename)
        }
    }




    private fun saveUsedSensorList() {

        val usedSensorList =  sensorDataCollect.getSensorList()

        sharedViewModel.saveUsedSensorList(usedSensorList)
    }
}



