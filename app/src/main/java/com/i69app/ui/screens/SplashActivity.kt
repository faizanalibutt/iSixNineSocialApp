package com.i69app.ui.screens

import android.app.Activity
import android.app.ActivityOptions
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Base64
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.gson.Gson
import com.i69app.databinding.ActivitySplashBinding
import com.i69app.ui.base.BaseActivity
import com.i69app.ui.screens.auth.AuthActivity
import com.i69app.ui.screens.main.MainActivity
import com.i69app.ui.screens.main.search.SearchInterestedInFragment
import com.i69app.utils.defaultAnimate
import com.i69app.utils.startActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException


private const val MY_REQUEST_CODE = 101

class SplashActivity : BaseActivity<ActivitySplashBinding>() {

    private lateinit var appUpdateManager: AppUpdateManager

    override fun getActivityBinding(inflater: LayoutInflater) = ActivitySplashBinding.inflate(inflater)

    fun printKeyHash(context: Activity): String? {
        val packageInfo: PackageInfo
        var key: String? = null
        try {
            //getting application package name, as defined in manifest
            val packageName = context.applicationContext.packageName

            //Retriving package info
            packageInfo = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES
            )
            Log.e("Package Name=", context.applicationContext.packageName)
            for (signature in packageInfo.signatures) {
                val md: MessageDigest = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                key = String(Base64.encode(md.digest(), 0))

                // String key = new String(Base64.encodeBytes(md.digest()));
                Log.e("Key Hash=", key)
            }
        } catch (e1: PackageManager.NameNotFoundException) {
            Log.e("Name not found", e1.toString())
        } catch (e: NoSuchAlgorithmException) {
            Log.e("No such an algorithm", e.toString())
        } catch (e: Exception) {
            Log.e("Exception", e.toString())
        }
        return key
    }
    override fun setupTheme(savedInstanceState: Bundle?) {
        PreferenceManager.getDefaultSharedPreferences(this@SplashActivity).edit().clear().apply();

        if(intent.hasExtra("data"))
        {
            Log.e("data",""+Gson().toJson(intent.extras!!.get("data").toString()))
            Log.e("datatype",""+JSONObject(intent.extras!!.get("data").toString()).getString("notification_type"))
            val intentt = Intent(this, MainActivity::class.java)

var title=JSONObject(intent.extras!!.get("data").toString()).getString("title")
            if (title.equals("Moment Liked") || title.equals("Comment in moment") ||
                title.equals("Story liked") || title.equals("Story Commented") ||
                title.equals("Gift received"))
            {
                intentt.putExtra("isNotification", "yes")
            }
            else
            {
                //Log.e("room id",JSONObject(messageBody.data.toString()).getString("roomID"));
                Log.e("room id",JSONObject(intent.extras!!.get("data").toString()).getString("roomID"));

                intentt.putExtra("isChatNotification", "yes")
                intentt.putExtra("roomIDs", JSONObject(intent.extras!!.get("data").toString()).getString("roomID"))
            }
            startActivity(intentt)
        }
        else
        {
            appUpdateManager = AppUpdateManagerFactory.create(this)
            val appUpdateInfoTask = appUpdateManager.appUpdateInfo

            appUpdateInfoTask
                .addOnSuccessListener { appUpdateInfo ->
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE))
                        startUpdate(appUpdateInfo) else navigate()
                }
                .addOnFailureListener {
                    navigate()
                }
        }


        SearchInterestedInFragment.setShowAnim(true)
        binding.splashLogo.defaultAnimate(400, 500)
        binding.splashTitle.defaultAnimate(300, 700)





//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
//        val channelId = getString(R.string.default_notification_channel_id)
//        val channelName = getString(R.string.default_notification_channel_name)
//        val channelDescription = getString(R.string.default_notification_channel_desc)
//        val importance = NotificationManagerCompat.IMPORTANCE_HIGH
//        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
//
//        val channel = NotificationChannelCompat.Builder(channelId, importance).apply {
//            setName(channelName)
//            setDescription(channelDescription)
//            setSound(alarmSound, Notification.AUDIO_ATTRIBUTES_DEFAULT)
//        }
//        channel.setVibrationEnabled(true)
//        NotificationManagerCompat.from(this).createNotificationChannel(channel.build())





        printKeyHash(this)
    }

    override fun setupClickListeners() {

    }

    private fun startUpdate(appUpdateInfo: AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            AppUpdateType.FLEXIBLE,
            this,
            MY_REQUEST_CODE
        )
    }

    private fun navigate() {
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1200)


            withContext(Dispatchers.Main) {
                if (getCurrentUserId() == null) goToAuthActivity() else startActivity<MainActivity>()
            }
        }
    }

    private fun goToAuthActivity() {
        val transactions = arrayOf<Pair<View, String>>(Pair(binding.splashLogo, "logoView"), Pair(binding.splashTitle, "logoTitle"))
        val options = ActivityOptions.makeSceneTransitionAnimation(this, *transactions)
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent, options.toBundle())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Log.e("request code",""+requestCode)
        Log.e("resultCode code",""+requestCode)
        Log.e("data",""+Gson().toJson(data))


        if (requestCode == MY_REQUEST_CODE) {
            if (resultCode != RESULT_OK) {
                Timber.e("MY_APP", "Update flow failed! Result code: $resultCode")
                navigate()
            } else {
                navigate()
            }
        }
    }

}