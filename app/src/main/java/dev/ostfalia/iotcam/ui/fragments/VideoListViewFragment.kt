package dev.ostfalia.iotcam.ui.fragments


import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.ProgressBar
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.ListFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dev.ostfalia.iotcam.databinding.FragmentVideolistBinding
import dev.ostfalia.iotcam.network.*
import dev.ostfalia.iotcam.network.oauth.Authenticator
import dev.ostfalia.iotcam.ui.fragments.videolistview.VideoListAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList


@AndroidEntryPoint
class VideoListViewFragment : ListFragment() {
    private val argsPassed: VideoListViewFragmentArgs by navArgs()

    private var _binding: FragmentVideolistBinding? = null
    private val binding get() = _binding!!
    private var videoListView: ListView? = null

    @Inject
    lateinit var authenticator: Authenticator

    @Inject
    lateinit var videoRepository: VideoRepository

    private val authorizationLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            authenticator.handleAuthorizationResponse(result.data!!)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateAdapter(refresh: Boolean = true) {

        if(refresh)
            videoRepository.refreshVideoFiles()


        val videoListAdapter =
            VideoListAdapter(
                requireActivity().baseContext,
                videoRepository.videoListViewFiles
            )
        listAdapter = videoListAdapter
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateView() = lifecycleScope.launch(Dispatchers.Main) {
        updateAdapter()
        _binding!!.countTextView.text =
            if (videoRepository.videoListViewFiles.size() == 1) "${videoRepository.videoListViewFiles.size()} item" else "${videoRepository.videoListViewFiles.size()} items"
        (videoListView?.adapter as BaseAdapter).notifyDataSetChanged()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        updateAdapter(refresh = false)

        _binding = FragmentVideolistBinding.inflate(inflater, container, false)
        videoListView = _binding?.list

        binding.selectAllButton.setOnClickListener {
            videoRepository.showDialogDeleteAllUploadAll(
                VideoRepository.UploadConfig(
                    requireContext(),
                    videoRepository.videoListViewFiles.get(),
                    this,
                    authenticator,
                    authorizationLauncher,
                    getProgressBar()
            ))
        }

        updateView()

        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        val path = videoRepository.videoListViewFiles.get(position)
        videoRepository.uploadSingleFileDialog(
            VideoRepository.UploadConfig(
                requireContext(),
                path,
                this,
                authenticator,
                authorizationLauncher,
                getProgressBar()
            ))
    }

    fun getProgressBar() : ProgressBar {
        return binding.progressBar
    }
}
