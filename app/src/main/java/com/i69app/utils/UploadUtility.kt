package com.i69app.utils

import android.app.Activity
import android.app.Dialog
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.i69app.BuildConfig
import com.i69app.ui.screens.main.messenger.chat.MessengerNewChatFragment
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.util.*
import kotlin.math.roundToInt

class UploadUtility(var fragment: Fragment) {

    val activity: Activity
        get() = fragment.requireActivity()

    private lateinit var loadingDialog: Dialog
    var serverURL: String = "${BuildConfig.BASE_URL}chat/image_upload"
    val client = OkHttpClient()

    fun uploadFile(
        sourceFilePath: String,
        uploadedFileName: String? = null,
        authorization: String?,
        upload_type: String?,
        callback: ((String?) -> Unit)
    ) {
        uploadFile(File(sourceFilePath), uploadedFileName, authorization, upload_type, callback)
    }

    private fun uploadFile(
        sourceFile: File,
        uploadedFileName: String? = null,
        authorization: String?,
        upload_type: String?,
        callback: ((String?) -> Unit)
    ) {
        Thread {
            val mimeType = sourceFile.getMimeType() ?: return@Thread;
            val fileName: String = uploadedFileName ?: sourceFile.name
            Log.e("mimeType", "-->" + mimeType)

            Log.e("upload_type", "-->" + upload_type)
            toggleProgressDialog(true)
            try {
                val d = Calendar.getInstance(TimeZone.getTimeZone("GMT")).get(Calendar.DAY_OF_MONTH)
                    .toFloat()
                val result = d * (33333333f / 222f)
                val token = result.roundToInt()

                Timber.d(
                    "checkauth d ${
                        Calendar.getInstance(TimeZone.getTimeZone("GMT")).get(Calendar.DAY_OF_MONTH)
                    }"
                )
                Timber.d("checkauth f $result")
                Timber.d("checkauth t $token")
                val requestBody: RequestBody =
                    MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("token", token.toInt().toString())
                        .addFormDataPart("upload_type", upload_type.toString())
                        .addFormDataPart(
                            "image",
                            fileName,
                            sourceFile.asRequestBody(mimeType.toMediaTypeOrNull())
                        )
                        .build()

                Log.e("TOKENN", token.toString())
                Log.e("UPLOADTYPE", upload_type.toString())
                Timber.d("checkauth type[$mimeType] exist[${sourceFile.exists()}] $token")
                Timber.d("checkauth $serverURL")
                val request: Request = Request.Builder()
                    .addHeader("Authorization", "Token $authorization")
                    .url(serverURL).post(requestBody).build()

                val response: Response = client.newCall(request).execute()

                val responseBody = response.body?.string()
                Timber.d("checkauth $responseBody")
                if (response.isSuccessful) {
                    try {
                        val json = JSONObject(responseBody!!)
                        if (json.getBoolean("success")) {
                            callback.invoke(json.getString("img"))
                        } else {

                            if (json.getString("message").contains("Insufficient coins")) {
                                callback.invoke("error")
                            } else {
                                Log.e(
                                    "File cc",
                                    "-" + "Error: ${response.code} ${json.getString("message")}"
                                )
                                showToast("Error: ${response.code} ${json.getString("message")}")
                            }

                        }
                    } catch (je: JSONException) {
                        showToast("Invalid Json: ${response.code} ${responseBody}")
                    } catch (e: java.lang.Exception) {
                        showToast("Failed: ${response.code} ${responseBody}")
                    }
                    //Log.d("File upload","success, path: $serverUploadDirectoryPath$fileName")
                    //showToast("File uploaded successfully at ${response.body.toString()} $serverUploadDirectoryPath$fileName")
                } else {

                    // if()
                    Log.e("File upload", "-" + responseBody)
                    Log.e("File c", "-" + response.code)
                    showToast("Failed: ${response.code} ${responseBody}")
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
                Log.e("File ex.message", "-" + ex.message)

                //Log.e("File upload", "failed")
                showToast("File uploading failed ${ex.message}")
            }
            toggleProgressDialog(false)
        }.start()
    }

    fun showToast(message: String) {
        activity.runOnUiThread {
            //fragment.view?.snackbar(message)
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun toggleProgressDialog(show: Boolean) {
        activity.runOnUiThread {
            if (show) {
                //dialog = ProgressDialog.show(activity, "", "Uploading file...", true);
                loadingDialog = activity.createLoadingDialog()
                loadingDialog.show()
            } else {
                loadingDialog.dismiss();
            }
        }
    }
}