package dev.ostfalia.iotcam.network.upload

import com.google.gson.annotations.SerializedName


data class S3_Resource(
    @SerializedName("uploadResourceVideo"      ) var uploadResourceVideo      : UploadResourceVideo?      = UploadResourceVideo(),
    @SerializedName("uploadResourceSensorData" ) var uploadResourceSensorData : UploadResourceSensorData? = UploadResourceSensorData()
)

data class Fields (

    @SerializedName("key"                  ) var key                  : String? = null,
    @SerializedName("x-amz-security-token" ) var xamzsecuritytoken    : String? = null,
    @SerializedName("x-amz-credential"     ) var xamzcredential       : String? = null,
    @SerializedName("x-amz-algorithm"      ) var xamzalgorithm        : String? = null,
    @SerializedName("x-amz-date"           ) var xamzdate             : String? = null,
    @SerializedName("policy"               ) var policy               : String? = null,
    @SerializedName("x-amz-signature"      ) var signature            : String? = null

)

data class UploadResourceVideo (

    @SerializedName("url"    ) var url    : String? = null,
    @SerializedName("fields" ) var fields : Fields? = Fields()

)

data class UploadResourceSensorData (

    @SerializedName("url"    ) var url    : String? = null,
    @SerializedName("fields" ) var fields : Fields? = Fields()

)