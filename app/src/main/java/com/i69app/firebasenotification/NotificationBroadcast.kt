package com.i69app.firebasenotification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import androidx.fragment.app.Fragment
import com.i69app.ui.base.BaseFragment
import com.i69app.ui.screens.main.MainActivity
import com.i69app.ui.screens.main.MainActivity.Companion.getMainActivity
import com.i69app.ui.screens.main.messenger.chat.MessengerNewChatFragment
import com.i69app.ui.screens.main.messenger.list.MessengerListFragment
import org.json.JSONObject
import timber.log.Timber


class NotificationBroadcast(var activity: Fragment?) : BroadcastReceiver() {
    var NOTIFY_ID = 0 // ID of notification

    override fun onReceive(context: Context?, intent: Intent) {
        try {
            if (intent.extras?.getString("data") != null) {
                if (TextUtils.isEmpty(intent.extras!!.getString("data"))) {
                    Timber.d(
                        "onMessageReceived", "onReceive: Data Received  ${
                            intent.extras?.getString("data")?.let { JSONObject(it) }
                        }"
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        Timber.tag("onMessageReceived")
            .d("onReceive: Data Recived ${intent.extras?.getString("type")}")
        getAction(intent)
    }

    fun getAction(intent: Intent) {
        if (intent.extras != null) {
            //getMainActivity()?.binding?.bottomNavigation?.addBadge(1);

            //  Constants.printResponse("Daaaaa", "Datasss", intent.getExtras())

            if ((intent.hasExtra("isNotification") && intent.getStringExtra("isNotification") != null)) {
                Handler(Looper.getMainLooper()).postDelayed(Runnable() {
                    kotlin.run {
                        val bundle = Bundle()
                        bundle.putString("ShowNotification", "true")
                        // MainActivity.getMainActivity()!!.navController.navigate(R.id.action_user_moments_fragment, bundle)
                    }
                }, 500);
            } else if ((intent.hasExtra("isChatNotification") && intent.getStringExtra("isChatNotification") != null)) {
                if ((intent.hasExtra("roomIDs") && intent.getStringExtra("roomIDs") != null)) {
                    Handler(Looper.getMainLooper()).postDelayed(Runnable() {
                        kotlin.run {
                            val rID = intent.getStringExtra("roomIDs")
                            val bundle = Bundle()
                            bundle.putString("roomIDNotify", rID)
                            if (activity is MessengerListFragment) {
                                soundPlay(activity as MessengerListFragment)
                                clearNotification(activity as MessengerListFragment)
                                //(activity as MessengerListFragment).updateList(false)
                            } else if (activity is MessengerNewChatFragment) {
                                soundPlay(activity as MessengerNewChatFragment)
                                clearNotification(activity as MessengerNewChatFragment)
                                //(activity as MessengerNewChatFragment).setupData(false)
                            } else if (activity is BaseFragment<*>) {
                                soundPlay(activity as BaseFragment<*>)
                                //getMainActivity()?.updateChatBadge()
                                getMainActivity()?.pref?.edit()?.putString("newChat", "true")
                                    ?.putString("roomIDS", rID)?.apply()
                                //clearNotification(activity as BaseFragment<*>)
                                //(activity as BaseFragment<*>).setupData(false)
                            }
//                            MainActivity.getMainActivity()!!.navController.navigate(R.id.messengerListFragment, bundle)
                        }
                    }, 500);
                }
            } else if (intent.hasExtra(MainActivity.ARGS_SCREEN) && intent.getStringExtra(
                    MainActivity.ARGS_SCREEN
                ) != null
            ) {
                if (intent.hasExtra(MainActivity.ARGS_SENDER_ID) && intent.getStringExtra(
                        MainActivity.ARGS_SENDER_ID
                    ) != null
                ) {
                    Handler(Looper.getMainLooper()).postDelayed(Runnable() {
                        kotlin.run {
                            val senderId = intent.getStringExtra(MainActivity.ARGS_SENDER_ID)
                            onNotificationClick(senderId!!)
                        }
                    }, 500);
                } else {
                    Handler(Looper.getMainLooper()).postDelayed(Runnable() {
                        kotlin.run {
                            if (activity is MessengerListFragment) {
                                soundPlay(activity as MessengerListFragment)
                                clearNotification(activity as MessengerListFragment)
                                //(activity as MessengerListFragment).updateList(false)
                            } else if (activity is MessengerNewChatFragment) {
                                soundPlay(activity as MessengerNewChatFragment)
                                clearNotification(activity as MessengerNewChatFragment)
                                //(activity as MessengerNewChatFragment).setupData(false)
                            }
//                            MainActivity.getMainActivity()!!.navController.navigate(R.id.messengerListAction)
                        }
                    }, 500);
                }
            }

        }
    }


    fun soundPlay(fragment: Fragment) {
        try {
            val notification: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val r = RingtoneManager.getRingtone(fragment.requireActivity(), notification)
            r.play()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun onNotificationClick(senderId: String) {
//        val msgPreviewModel: MessagePreviewModel? = QbDialogHolder.getChatDialogById(senderId)
//        msgPreviewModel?.let {
//            viewModel?.setSelectedMessagePreview(it)
//            navController.navigate(R.id.globalUserToChatAction)
//        }
    }

    fun clearNotification(fragment: Fragment) {
        val notificationManager = fragment.requireActivity()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//        notificationManager.cancel(NOTIFY_ID)
        notificationManager.cancelAll()
    }

}