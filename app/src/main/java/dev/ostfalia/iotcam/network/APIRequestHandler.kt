package dev.ostfalia.iotcam.network

import dev.ostfalia.iotcam.network.upload.S3_Resource
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresApi
import dev.ostfalia.iotcam.network.oauth.Authenticator
import dev.ostfalia.iotcam.ui.fragments.VideoListViewFragment
import dev.ostfalia.iotcam.network.upload.UploadDataAPIInterface
import dev.ostfalia.iotcam.network.upload.datamodels.RecordingMetadataDataModel
import dev.ostfalia.iotcam.ui.ToastFactory
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class APIRequestHandler {

    lateinit var s3Resource: S3_Resource
    lateinit var view: VideoListViewFragment
    val tf: ToastFactory = ToastFactory()

    fun isOnline(context: Context?): Boolean {
        val connectivityManager =
            context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }

    fun areUploadConditionsMet(context: Context, authenticator: Authenticator, authorizationLauncher: ActivityResultLauncher<Intent>)
            : VideoRepository.UploadStatus {
        val apiHandler = APIRequestHandler()

        if (!apiHandler.isOnline(context)) {
            return VideoRepository.UploadStatus(false, "currently no internet connection")
        }

        if (!authenticator.isAuthorized()) {
            authenticator.openAuthIntent(authorizationLauncher)
            return VideoRepository.UploadStatus(false, "not authorized, redirecting..")
        }

        if (authenticator.isTokenExpired()) {
            authenticator.authorizationRefreshRequest()
            return VideoRepository.UploadStatus(false, "token expired, fetching new..")
        }

        return VideoRepository.UploadStatus(true, "succeeded..")
    }

    fun uploadData(
        uploadConfig: VideoRepository.UploadConfig,
        videoFile: File,
        sensorDataFile: File,
        recordingMetaDataFile: RecordingMetadataDataModel
    ) {
        view = uploadConfig.view
        val progressBar = view.getProgressBar()
        progressBar.visibility = View.VISIBLE
        val context = uploadConfig.context

        val apiInterface = UploadDataAPIInterface.create()
            .getS3UploadResource(
                token = uploadConfig.authenticator.getToken(),
                body =  recordingMetaDataFile
            )

        apiInterface.enqueue(object : Callback<S3_Resource> {
            override fun onResponse(call: Call<S3_Resource>?, response: Response<S3_Resource>?) {

                if (response?.body() != null) {
                    println("uploadData RESPONSE 1: ${response.body()!!.toString()}")
                }
                else {
                    println("uploadData FAILED 1: ${response?.code()!!.toString()}")
                    tf.showToast(context,"uploadData FAILED 1: ${response.code().toString()} ${response.message().toString()} ")
                    return
                }

                s3Resource = response.body()!!

                if (s3Resource == null) {
                    tf.showToast(
                        context,
                        "s3 resource null. abort."
                    )

                    return
                }

                uploadSensorData(context, sensorDataFile)
                uploadVideoData(context, videoFile, progressBar)
            }

            override fun onFailure(call: Call<S3_Resource>?, t: Throwable?) {
                progressBar.visibility = View.GONE

                tf.showToast(
                    context,
                    "upload request failed",
                )

                println("uploadData FAILURE 1: ${t?.cause.toString()}")
            }
        })
    }


    private fun uploadSensorData(context: Context, sensorData: File) {
        val url: String = s3Resource.uploadResourceSensorData!!.url.toString()
        val key: RequestBody =
            s3Resource.uploadResourceSensorData!!.fields!!.key!!.toRequestBody("text/plain".toMediaTypeOrNull());
        val xamzsecuritytoken: RequestBody =
            s3Resource.uploadResourceSensorData!!.fields!!.xamzsecuritytoken!!.toRequestBody("text/plain".toMediaTypeOrNull());
        val xamzcredential: RequestBody =
            s3Resource.uploadResourceSensorData!!.fields!!.xamzcredential!!.toRequestBody("text/plain".toMediaTypeOrNull());
        val xamzalgorithm: RequestBody =
            s3Resource.uploadResourceSensorData!!.fields!!.xamzalgorithm!!.toRequestBody("text/plain".toMediaTypeOrNull());
        val xamzdate: RequestBody =
            s3Resource.uploadResourceSensorData!!.fields!!.xamzdate!!.toRequestBody("text/plain".toMediaTypeOrNull());
        val signature: RequestBody =
            s3Resource.uploadResourceSensorData!!.fields!!.signature!!.toRequestBody("text/plain".toMediaTypeOrNull());
        val policy: RequestBody =
            s3Resource.uploadResourceSensorData!!.fields!!.policy!!.toRequestBody("text/plain".toMediaTypeOrNull());
        val file: RequestBody = sensorData!!.asRequestBody("application/json".toMediaTypeOrNull());

        val apiInterface = UploadDataAPIInterface.create().uploadToBucket(
            url,
            key,
            xamzsecuritytoken,
            xamzcredential,
            xamzalgorithm,
            xamzdate,
            signature,
            policy,
            file
        )

        apiInterface.enqueue(object : Callback<Void> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<Void>?, response: Response<Void>?) {

                tf.showToast(
                    context,
                    "sensordata upload successful, file will be deleted",
                )

                sensorData.delete()
                view.updateView()

                println("RESPONSE Sensor Data : ${response?.code()} ${response?.message()}")
            }

            override fun onFailure(call: Call<Void>?, t: Throwable?) {
                tf.showToast(
                    context,
                    "sensordata upload failed",
                )

                println("Failure Sensor Data: ${t?.cause.toString()}")
            }
        })
    }


    private fun uploadVideoData(context: Context, videoFile: File? = null, progressBar: ProgressBar) {


        val url: String = s3Resource.uploadResourceVideo!!.url.toString()
        val key: RequestBody =
            s3Resource.uploadResourceVideo!!.fields!!.key!!.toRequestBody("text/plain".toMediaTypeOrNull());
        val xamzsecuritytoken: RequestBody =
            s3Resource.uploadResourceVideo!!.fields!!.xamzsecuritytoken!!.toRequestBody("text/plain".toMediaTypeOrNull());
        val xamzcredential: RequestBody =
            s3Resource.uploadResourceVideo!!.fields!!.xamzcredential!!.toRequestBody("text/plain".toMediaTypeOrNull());
        val xamzalgorithm: RequestBody =
            s3Resource.uploadResourceVideo!!.fields!!.xamzalgorithm!!.toRequestBody("text/plain".toMediaTypeOrNull());
        val xamzdate: RequestBody =
            s3Resource.uploadResourceVideo!!.fields!!.xamzdate!!.toRequestBody("text/plain".toMediaTypeOrNull());
        val signature: RequestBody =
            s3Resource.uploadResourceVideo!!.fields!!.signature!!.toRequestBody("text/plain".toMediaTypeOrNull());
        val policy: RequestBody =
            s3Resource.uploadResourceVideo!!.fields!!.policy!!.toRequestBody("text/plain".toMediaTypeOrNull());
        val file: RequestBody =
            RequestBody.create("video/mp4".toMediaTypeOrNull(), videoFile!!.readBytes());

        val apiInterface2 = UploadDataAPIInterface.create().uploadToBucket(
            url,
            key,
            xamzsecuritytoken,
            xamzcredential,
            xamzalgorithm,
            xamzdate,
            signature,
            policy,
            file
        )

        apiInterface2!!.enqueue(object : Callback<Void> {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onResponse(call: Call<Void>?, response: Response<Void>?) {
                progressBar.visibility = View.GONE

                tf.showToast(
                    context,
                    "video upload successful, file will be deleted"
                )

                videoFile.delete()

                val path_sensor = videoFile.getSensorPath()
                val path_meta = videoFile.getMetaPath()

                println("PATHS: ${path_sensor} ${File(path_sensor).delete()}")
                println("PATHS: ${path_meta} ${File(path_meta).delete()}")


                view.updateView()
                println("RESPONSE Video Upload : ${response?.code()} ${response?.message()}")
            }

            override fun onFailure(call: Call<Void>?, t: Throwable?) {
                progressBar.visibility = View.GONE

                tf.showToast(
                    context,
                    "video upload failed",
                )

                println("Failure Video Upload  2: ${t?.cause.toString()}")
            }
        })
    }
}