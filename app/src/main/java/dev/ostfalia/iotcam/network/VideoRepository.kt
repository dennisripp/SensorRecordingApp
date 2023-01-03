package dev.ostfalia.iotcam.network

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import com.beust.klaxon.Klaxon
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ostfalia.iotcam.network.oauth.Authenticator
import dev.ostfalia.iotcam.network.upload.datamodels.RecordingMetadataDataModel
import dev.ostfalia.iotcam.ui.fragments.VideoListViewFragment
import java.io.File
import dev.ostfalia.iotcam.network.*
import dev.ostfalia.iotcam.ui.ToastFactory
import java.lang.Long
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.ArrayList

@Singleton
open class VideoListViewFiles {
    var paths: ArrayList<String> = arrayListOf()
    var names: ArrayList<String> = arrayListOf()
    var description: ArrayList<String> = arrayListOf()
    var project: ArrayList<String> = arrayListOf()
    var tag: ArrayList<String> = arrayListOf()
    var thumbnails: ArrayList<Bitmap> = arrayListOf()
    var length: ArrayList<Double> = arrayListOf()


    fun size() : Int {
        return paths.size
    }

    fun clear()  {
        length.clear()
        description.clear()
        paths.clear()
        names.clear()
        project.clear()
        tag.clear()
        thumbnails.clear()
    }

    fun get() :  ArrayList<String> {
        return paths
    }

    fun get(index: Int) :  ArrayList<String> {
        return arrayListOf(paths[index])
    }
}

@Singleton
class VideoRepository @Inject constructor(@ApplicationContext var appContext: Context) {

    private var builder: MaterialAlertDialogBuilder? = null

    private var _videoListViewFiles: VideoListViewFiles = VideoListViewFiles()
    val videoListViewFiles: VideoListViewFiles get() = _videoListViewFiles
    private val tf: ToastFactory = ToastFactory()

    @Inject
    lateinit var storageManager: StorageManager

    data class UploadFiles(
        val videoFile: File,
        val sensorFile: File,
        val recordingMetaFile: File
    )

    data class UploadConfig(
        val context: Context,
        val path: ArrayList<String>,
        val view: VideoListViewFragment,
        val authenticator: Authenticator,
        val authorizationLauncher: ActivityResultLauncher<Intent>,
        val progressBar: ProgressBar
    )

    data class UploadStatus(
        val success: Boolean,
        val message: String
    )

    @RequiresApi(Build.VERSION_CODES.O)
    fun refreshVideoFiles() {
        _videoListViewFiles.clear()

        val videoFileDir = storageManager.getVideoFileDir()
        File(videoFileDir).walkBottomUp().forEach {
            if (it.extension.equals("mp4")) {
                val thumbnail = ThumbnailUtils.createVideoThumbnail(
                    it.absolutePath,
                    MediaStore.Video.Thumbnails.MINI_KIND
                )
                if (thumbnail == null) {
                    it.delete()
                } else {
                    val jsonString =  File(it.getMetaPath()).bufferedReader().use { it.readText() }
                    val metaDataModel = Klaxon().parse<RecordingMetadataDataModel>(jsonString)

                    val retriever = MediaMetadataRetriever();
                    retriever.setDataSource(it.absolutePath)
                    val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    val frameCnt = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT);
                    val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                    val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                    val timeInSec = java.lang.Double.parseDouble(time) / 1000.0;
                    val descriptionString = "${height}x${width}px, ${frameCnt} frames total"
                    retriever.release()

                    _videoListViewFiles.thumbnails.add(thumbnail)
                    _videoListViewFiles.paths.add(it.absolutePath)
                    _videoListViewFiles.names.add(it.name)
                    _videoListViewFiles.project.add(metaDataModel?.projectID!!)
                    _videoListViewFiles.tag.add(metaDataModel?.freeText!!)
                    _videoListViewFiles.length.add(timeInSec)
                    _videoListViewFiles.description.add(descriptionString)
                }
            }
        }
    }


    private fun areUploadConditionsMet(uploadConfig: UploadConfig)
    : UploadStatus {
        val apiHandler = APIRequestHandler()

        if (!apiHandler.isOnline(uploadConfig.context)) {
            return UploadStatus(false, "currently no internet connection")
        }

        if (!uploadConfig.authenticator.isAuthorized()) {
            uploadConfig.authenticator.openAuthIntent(uploadConfig.authorizationLauncher)
            return UploadStatus(false, "not authorized, redirecting..")
        }

        if (uploadConfig.authenticator.isTokenExpired()) {
            uploadConfig.authenticator.authorizationRefreshRequest()
            return UploadStatus(false, "token expired, fetching new..")
        }

        return UploadStatus(true, "succeeded..")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun uploadChecked(uploadConfig: UploadConfig) {
        val (succeed, message) = areUploadConditionsMet(uploadConfig)
        if(!succeed) {
            setActionShowSnack(tf.createSnackbar(message, uploadConfig.view), uploadConfig)
            return
        }

        for (path in uploadConfig.path) {
            uploadData(uploadConfig, path)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun prepareUpload(uploadFiles: UploadFiles): UploadFiles {
        val rand = getRandomized()
        return UploadFiles(
            uploadFiles.videoFile.rename(rand),
            uploadFiles.sensorFile.rename(rand),
            uploadFiles.recordingMetaFile.rename(rand)
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setActionShowSnack(snackbar: Snackbar, uploadConfig: UploadConfig) {
        snackbar.setAction("Retry") { _ ->
            uploadChecked(uploadConfig)
        }
        snackbar.show()
    }

    private fun getFilesFromPath(path: String) : UploadFiles {
        var videoFile = File(path)
        var sensorFile = File(videoFile.getSensorPath())
        var recordingMetaFile = File(videoFile.getMetaPath())

        return UploadFiles(videoFile, sensorFile, recordingMetaFile)
    }

    private fun deleteFiles(paths: ArrayList<String>) {
        for(str in paths) {
            val uploadFiles = getFilesFromPath(str)

            uploadFiles.videoFile.delete()
            uploadFiles.sensorFile.delete()
            uploadFiles.recordingMetaFile.delete()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun uploadData(uploadConfig: UploadConfig, path: String) {
        val oldFiles = getFilesFromPath(path)
        val newFiles = prepareUpload(oldFiles)

        val apiHandler = APIRequestHandler()
        val jsonString = newFiles.recordingMetaFile.bufferedReader().use { it.readText() }
        val metaString = Klaxon().parse<RecordingMetadataDataModel>(jsonString)
        metaString?.name = newFiles.videoFile.name.substringBeforeLast("_")

        uploadConfig.view.updateView()

        apiHandler.uploadData(
            uploadConfig,
            newFiles.videoFile,
            newFiles.sensorFile,
            metaString!!
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun showDialogDeleteAllUploadAll(uploadConfig: UploadConfig)  {

        builder = MaterialAlertDialogBuilder(uploadConfig.context)
        builder!!.setMessage("upload or delete all").setCancelable(false)
            .setPositiveButton("upload") { dialog, id ->
                uploadChecked(uploadConfig)
            }
            .setNegativeButton("abort") { dialog, id -> //  Action for 'NO' Button
                dialog.cancel()
            }
            .setNeutralButton("delete all") { dialog, id ->
                deleteAllFiles(uploadConfig.view)
                dialog.cancel()
                tf.showToast(
                    uploadConfig.context,
                    "files deleted")
            }

        val alert = builder!!.create()
        alert.setTitle("attention")
        alert.show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun deleteAllFiles(view: VideoListViewFragment) {
        for (path in _videoListViewFiles.paths) {
            File(path).delete()
        }
        view.updateView()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun uploadSingleFileDialog(uploadConfig: UploadConfig) {

        builder = MaterialAlertDialogBuilder(uploadConfig.context)
        builder!!.setMessage("upload or delete").setCancelable(false)
            .setPositiveButton("upload") { dialog, id ->
                uploadChecked(uploadConfig)
            }

            .setNegativeButton("abort") { dialog, id ->
                dialog.cancel()
            }

            .setNeutralButton("delete") { dialog, id ->
                tf.showToast(
                    uploadConfig.context,
                    "files deleted")
                dialog.cancel()
                deleteFiles(uploadConfig.path)
                uploadConfig.view.updateView()
            }

        val alert = builder!!.create()
        alert.setTitle("${uploadConfig.path.first().substringAfterLast("/")}")
        alert.show()
    }
}