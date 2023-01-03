package dev.ostfalia.iotcam.network.upload.datamodels
import com.google.gson.annotations.SerializedName

open class RecordingMetadataDataModel (

    @SerializedName("name"              ) var name              : String?                = null,
    @SerializedName("recordingStarted"  ) var recordingStarted  : Long?                   = null,
    @SerializedName("recordingStopped"  ) var recordingStopped  : Long?                   = null,
    @SerializedName("usedSensors"       ) var usedSensors       : ArrayList<UsedSensors> = arrayListOf(),
    @SerializedName("recordingDevice"   ) var recordingDevice   : RecordingDevice?       = RecordingDevice(),
    @SerializedName("projectID"         ) var projectID         : String?                = null,
    @SerializedName("appVersion"        ) var appVersion        : String?                = null,
    @SerializedName("camera"            ) var camera            : Camera?                = Camera(),
    @SerializedName("user"              ) var user              : String?                = null,
    @SerializedName("freeText"          ) var freeText          : String?                = null,
    @SerializedName("recordingLocation" ) var recordingLocation : RecordingLocation?     = RecordingLocation()

)

data class UsedSensors (

    @SerializedName("sensorType"        ) var sensorType        : String? = null,
    @SerializedName("unitOfMeasurement" ) var unitOfMeasurement : String? = null,
    @SerializedName("samplingFrequency" ) var samplingFrequency : Int?    = null,
    @SerializedName("manufacturer")        var manufacturer: String?

)

data class RecordingDevice (

    @SerializedName("operatingSystem" ) var operatingSystem : String? = null,
    @SerializedName("manufacturer"    ) var manufacturer    : String? = null,
    @SerializedName("deviceName"      ) var deviceName      : String? = null

)

data class Resolution (

    @SerializedName("y" ) var y : Int? = null,
    @SerializedName("x" ) var x : Int? = null

)

data class Camera (

    @SerializedName("orientation" ) var orientation : String?     = null,
    @SerializedName("resolution"  ) var resolution  : Resolution? = Resolution()

)

data class RecordingLocation (

    @SerializedName("latitude"  ) var latitude  : Double? = null,
    @SerializedName("longitude" ) var longitude : Double? = null

)