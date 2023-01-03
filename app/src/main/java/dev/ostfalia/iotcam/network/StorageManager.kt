package dev.ostfalia.iotcam.network

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.io.path.Path

class StorageManager @Inject constructor(@ApplicationContext var appContext: Context) {
    
    private val contextPath: String = appContext.filesDir.absolutePath
    private val videoFilePath: String = "${contextPath}/videos"

    @RequiresApi(Build.VERSION_CODES.O)
    fun getVideoFileDir() : String {
        val path = Path(videoFilePath)
        if(!Files.exists(path))
            Files.createDirectory(path)

        return videoFilePath
    }
    //YYYY-MM-DDThh:mm:ssTZD-xxxxxxxx)
    fun getVideoFileName(extension: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ssz", Locale.US)
        val fomatted = sdf.format(Date()).substringBeforeLast("+")
        return "${fomatted}-${getRandomized()}_Cam.$extension"
    }
}

fun getRandomized() : Int {
    return (10000000..99999999).random()
}

//extension method
fun File.getSensorPath() : String {
    val path = this.absolutePath.substringBeforeLast("_")
    return "${path}_Sensor.json"
}

fun File.getMetaPath() : String {
    val path = this.absolutePath.substringBeforeLast("_")
    return "${path}_Meta.json"
}

fun File.rename(rand: Int) : File {
    val extension = this.absolutePath.substringAfterLast("_")
    val path = this.absolutePath.substringBeforeLast("_").substringBeforeLast("-")
    val newPath ="${path}-${rand}_${extension}"

    val from: File = File(this.absolutePath)
    val to: File = File(newPath)
    if (from.exists()) from.renameTo(to)
    return to
}