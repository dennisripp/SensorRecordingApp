package dev.ostfalia.iotcam.ui.fragments

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.ostfalia.iotcam.MainActivity
import dev.ostfalia.iotcam.R
import dev.ostfalia.iotcam.camera2.CameraInfo
import dev.ostfalia.iotcam.databinding.FragmentWelcomeBinding
import dev.ostfalia.iotcam.network.APIRequestHandler
import dev.ostfalia.iotcam.network.VideoRepository
import dev.ostfalia.iotcam.network.oauth.Authenticator
import dev.ostfalia.iotcam.ui.ToastFactory
import dev.ostfalia.iotcam.utils.hideKeyboard
import javax.inject.Inject


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
@AndroidEntryPoint
class WelcomeFragment : Fragment() {

    private var _binding: FragmentWelcomeBinding? = null
    val tf: ToastFactory = ToastFactory()

    @Inject
    lateinit var authenticator: Authenticator;

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    //collect user for metadata
    private val sharedViewModel: CameraSensorSharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentWelcomeBinding.inflate(inflater, container, false)
        (activity as MainActivity?)!!.requestLocation()
        return binding.root

    }

    private val authorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            authenticator.handleAuthorizationResponse(result.data!!, welcometextbox = binding.textviewWelcome)
            updateFragment()

        }
    }

    private fun updateFragment() {
        val ft: FragmentTransaction = requireFragmentManager().beginTransaction()
        if (Build.VERSION.SDK_INT >= 26) {
            ft.setReorderingAllowed(false)
        }
        ft.detach(this).attach(this).commit()
    }

    private fun updateData() {
        val name = authenticator.getUserName()
        if (name != null) {
            binding.textviewWelcome.text = "$name";
            sharedViewModel.saveUser("$name")
        }
        else {
            binding.textviewWelcome.text = "user is not logged in"
            sharedViewModel.saveUser("user is not logged in")
        }

        updateFragment()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateData()

        binding.textinputProject.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                // If the event is a key-down event on the "enter" button
                if (event.getAction() === KeyEvent.ACTION_DOWN &&
                    keyCode == KeyEvent.KEYCODE_ENTER
                ) {
                    hideKeyboard()
                    return true
                }
                return false
            }
        })

        binding.buttonFirst.setOnClickListener {

            val (succeed, message) = APIRequestHandler().areUploadConditionsMet(
                requireContext(),
                authenticator,
                authorizationLauncher
            )

            if(succeed) {
                tf.showToast(requireContext(),"already logged in")
                updateData()
            } else {
                tf.showToast(requireContext(), message)
            }
        }

        binding.buttonRecord.setOnClickListener {
            val navControllers = findNavController()
            val cameraList = CameraInfo.returnfilteredSortedCameraConfigs(requireContext(), "0")
            var info : CameraInfo.Companion.CameraInfo? = null
            var idx : Int = 0

            if(!cameraList.isEmpty()) {
                info = cameraList.first()
                val fullhd = Size(1920, 1080)
                for(infoT in cameraList) {
                    if(infoT.size == fullhd) {
                        info = infoT
                        idx = cameraList.indexOf(infoT)
                        break
                    }
                }
            }

            navControllers.navigate(
                WelcomeFragmentDirections.actionFirstFragmentToSecondFragment(
                    info!!.size.height,
                    info.size.width,
                    info.fps,
                    idx,
                    binding?.textinputProject?.text.toString()
                )
            )
            //main activity gps local
        }

        binding.buttonLibrary.setOnClickListener {
            findNavController().navigate(R.id.action_WelcomeFragment_to_LibraryFragment)
        }

        binding.buttonSensor.setOnClickListener {
            findNavController().navigate(R.id.action_WelcomeFragment_to_sensorFragment)

        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        //(activity as MainActivity?)!!.stopGPSCallBack()
    }
}