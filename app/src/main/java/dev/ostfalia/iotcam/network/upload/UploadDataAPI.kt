package dev.ostfalia.iotcam.network.upload;

import com.google.gson.annotations.SerializedName
import dev.ostfalia.iotcam.network.upload.datamodels.RecordingMetadataDataModel
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*


interface UploadDataAPIInterface {
    @POST("resource/")
    fun getS3UploadResource(
        @Header("Authorization") token: String?,
        @Body body: RecordingMetadataDataModel
    ): Call<S3_Resource>

    @Multipart
    @Headers("Content-Encoding: gzip")
    @POST()
    fun uploadToBucket(
        @Url url: String,
        @Part("key") key: RequestBody,
        @Part("x-amz-security-token") xamzsecuritytoken: RequestBody,
        @Part("x-amz-credential") xamzcredential: RequestBody,
        @Part("x-amz-algorithm") xamzalgorithm: RequestBody,
        @Part("x-amz-date") xamzdate: RequestBody,
        @Part("x-amz-signature") signature: RequestBody,
        @Part("policy") policy: RequestBody,
        @Part("file") file: RequestBody
    ): Call<Void>

    companion object {
        //IRELAND
      //  var BASE_URL = "https://prmit4tqhi.execute-api.eu-west-1.amazonaws.com/test/"

        var BASE_URL = "https://lyg6c3bfk0.execute-api.eu-central-1.amazonaws.com/prod/"

        fun create(): UploadDataAPIInterface {

            val incLogger = HttpLoggingInterceptor()
            incLogger.level = HttpLoggingInterceptor.Level.BODY

            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(BASE_URL)
                .build()
            return retrofit.create(UploadDataAPIInterface::class.java)
        }
    }
}
