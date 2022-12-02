package com.i69app.firebasenotification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.i69app.BuildConfig
import com.i69app.ChatRoomSubscription
import com.i69app.R
import com.i69app.data.config.Constants
import com.i69app.data.remote.repository.UserUpdateRepository
import com.i69app.singleton.App
import com.i69app.ui.screens.main.MainActivity
import com.i69app.utils.findFileExtension
import com.i69app.utils.isImageFile
import com.i69app.utils.isVideoFile
import com.i69app.utils.loadImage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber
import java.io.FileNotFoundException
import java.net.URL
import javax.inject.Inject
import kotlin.random.Random


@DelicateCoroutinesApi
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var userUpdateRepository: UserUpdateRepository

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        sendRegistrationToServer(s)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        /*Constants.printResponse(
            "onMessageReceived: ",
            "onMessageReceived:Obj Data",
            remoteMessage.getData()
        );
        Constants.printResponse(
            "onMessageReceived: ",
            "onMessageReceived:Obj All Data",
            remoteMessage
        );
        Constants.printResponse(
            "onMessageReceived: ",
            "onMessageReceived:Notification Payload ",
            remoteMessage.getNotification()
        );
        Constants.printResponse(
            "onMessageReceived: ",
            "onMessageReceived:Dataaaa Payload ",
            remoteMessage.getData().get("data")
        );*/

// Check if message contains a data payload.
        /*if (remoteMessage.data.isNotEmpty()) {
            Log.e(TAG, "Message data payload:" + "--" + Gson().toJson(remoteMessage.data))
            Log.e(TAG, "Message room id:" + "--" + JSONObject(remoteMessage.data.toString()))
            sendNotification(remoteMessage)
        }


        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.e(TAG, "Message Notification Body:  " + Gson().toJson(it))
            if (remoteMessage.notification!!.title!!.contains("Welcome") || remoteMessage.notification!!.body!!.contains(
                    "offered you"
                )
            ) {
                sendNotification(remoteMessage)
            }
        }*/
        if (remoteMessage.data.isNotEmpty())
            sendNotification(remoteMessage)
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM registration token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: String?) {
        // TODO: Implement this method to send token to your app server.
        Log.d(TAG, "sendRegistrationTokenToServer($token)")


        token?.let {
            GlobalScope.launch {
                val userId = App.userPreferences.userId.first()
                val userToken = App.userPreferences.userToken.first()

                if (userId != null && userToken != null) {
                    userUpdateRepository.updateFirebasrToken(userId, token, userToken)
                }
            }
        }
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private fun sendNotification(messageBody: RemoteMessage) {

        /**
         * Added by AM COIN DEDUCTION AND ADDED */

        Log.e("TAG_notification_data", "sendNotification: " + messageBody.data)
        Log.e("TAG_notification_title", "sendNotification: " + messageBody.notification!!.title)

        if (messageBody.notification!!.title != null) {

//            val title= messageBody.data["title"]
//            if (title!!.isNotEmpty()){

            MainActivity.notificationOpened = false
            val intent = Intent(App.getAppContext(), MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK


            if (JSONObject(messageBody.data.toString()).getJSONObject("data").has("title")) {
                if (JSONObject(messageBody.data.toString()).getJSONObject("data").getString("title")
                        .equals("Received Gift") ||
                    JSONObject(messageBody.data.toString()).getJSONObject("data").getString("title")
                        .equals("Sent message")
                ) {
                    intent.putExtra("isNotification", "yes")
                    val intentsend = Intent()
                    intentsend.putExtra("extra", messageBody.notification!!.title)
                    intentsend.action = "gift_Received"
                    sendBroadcast(intentsend, "com.my.app.onMessageReceived")
                }
            }
            if (messageBody.notification!!.title!!.contains("Gifted coins deduction") ||
                messageBody.notification!!.title!!.contains("Gifted coins added")
            ) {
                intent.putExtra("isNotification", "yes")
                val intentsend2 = Intent()
                intentsend2.putExtra("extra", messageBody.notification!!.title)
                intentsend2.action = "gift_Received"
                sendBroadcast(intentsend2, "com.my.app.onMessageReceived")
            }

            if (messageBody.notification!!.title!!.contains("New Moment added")) {
                intent.putExtra("isNotification", "yes")
                val intentsend2 = Intent()
                intentsend2.putExtra("extra", messageBody.notification!!.title)
                intentsend2.action = "moment_added"
                sendBroadcast(intentsend2, "com.my.app.onMessageReceived")
            }

            if (messageBody.notification!!.title!!.contains("Moment Deleted")) {
                intent.putExtra("isNotification", "yes")
                val intentDelete = Intent()
                intentDelete.putExtra("extra", messageBody.notification!!.title)
                intentDelete.action = "moment_deleted"
                sendBroadcast(intentDelete, "com.my.app.onMessageReceived")
                return
            }

            if (messageBody.notification!!.title!!.contains("Moment Updated")) {
                intent.putExtra("isNotification", "yes")
                val intentDelete = Intent()
                intentDelete.putExtra("extra", messageBody.notification!!.title)
                intentDelete.action = "moment_updated"
                sendBroadcast(intentDelete, "com.my.app.onMessageReceived")
                return
            }

            if (messageBody.notification!!.title!!.contains("Story Deleted")) {
                intent.putExtra("isNotification", "yes")
                val intentDelete = Intent()
                intentDelete.putExtra("extra", messageBody.notification!!.title)
                intentDelete.action = "story_deleted"
                sendBroadcast(intentDelete, "com.my.app.onMessageReceived")
                return
            }

            if (messageBody.notification!!.title!!.contains("New Story added")) {
                intent.putExtra("isNotification", "yes")
                val intentDelete = Intent()
                intentDelete.putExtra("extra", messageBody.notification!!.title)
                intentDelete.action = "story_added"
                sendBroadcast(intentDelete, "com.my.app.onMessageReceived")
            }


//            }
            //            if (JSONObject(messageBody.data.toString()).getJSONObject("data").getString("title").equals("Sent message") ||
            //            JSONObject(messageBody.data.toString()).getJSONObject("data").getString("title").equals("Received Gift")) {
//                val intent = Intent()
//                intent.putExtra("extra", messageBody.notification!!.title)
//                intent.action = "sent_message"
//                sendBroadcast(intent, "com.my.app.onMessageReceived")
//            }

            if (messageBody.notification!!.title.equals("Moment Liked") || messageBody.notification!!.title.equals(
                    "Comment in moment"
                ) ||
                messageBody.notification!!.title.equals("Story liked") || messageBody.notification!!.title.equals(
                    "Story Commented"
                ) ||
                messageBody.notification!!.title.equals("Gift received") ||
                messageBody.notification!!.title.equals("Welcome to the i69") ||
                messageBody.notification!!.body!!.contains("offered you")
            ) {
                intent.putExtra("isNotification", "yes")

            } else if (messageBody.notification!!.title!!.contains("Gifted coins deduction") ||
                messageBody.notification!!.title!!.contains("Gifted coins added")
            ) {
                intent.putExtra("isNotification", "yes")
                Log.e(
                    "onMessageReceived",
                    "sendNotification coinDedication: " + messageBody.notification!!.title
                )
                val intent = Intent()
                intent.putExtra("extra", messageBody.notification!!.title)
                intent.action = "com.my.app.onMessageReceived"
                sendBroadcast(intent, "com.my.app.onMessageReceived")

            } else {
                //Log.e("room id",JSONObject(messageBody.data.toString()).getString("roomID"));
                /* Log.e(
                     "room id",
                     JSONObject(messageBody.data.toString()).getJSONObject("data")
                         .getString("roomID")
                 );*/
                try {
                    var obj = JSONObject(messageBody.data.toString())
                    intent.putExtra("isChatNotification", "yes")
                    if (obj.length() != 0) {
                        var obj1 = JSONObject(messageBody.data.toString()).getJSONObject("data")
                        if (obj1.length() != 0) {
                            intent.putExtra(
                                "roomIDs",
                                JSONObject(messageBody.data.toString()).getJSONObject("data")
                                    .getString("roomID")
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }


            }

            var notify_id = Random.nextInt()

            intent.action = Constants.INTENTACTION
            intent.putExtra("notify_id", notify_id)

            val textTitle: String = messageBody.notification!!.title!!

//            PendingIntent.FLAG_IMMUTABLE


            //Second Time Tune
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

            var pendingIntent: PendingIntent? = null
            pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
            } else {
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            }


            val soundUri: Uri

            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            var mBuilder: NotificationCompat.Builder? = null

            if (JSONObject(messageBody.data.toString()).getJSONObject("data").has("title")) {
                if (JSONObject(messageBody.data.toString()).getJSONObject("data")
                        .getString("title").equals("Received Gift")
                ) {
                    val url = URL(
                        BuildConfig.BASE_URL + JSONObject(messageBody.data.toString()).getJSONObject(
                            "data"
                        )
                            .getString("giftUrl")
                    )
                    var image = drawableToBitmap(resources.getDrawable(R.drawable.ic_launcher))
                    try {
                        image = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    mBuilder = NotificationCompat.Builder(this, getString(R.string.app_name))
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setLargeIcon(image)
                        .setContentTitle(textTitle)
                        .setContentText(messageBody.notification!!.body)
                        .setSound(soundUri)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setDefaults(Notification.DEFAULT_SOUND)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)

                }
                /* if (messageBody.notification!!.icon != null) {
                     Log.e("iconn", "" + messageBody.notification!!.icon)
                     val url = URL(messageBody.notification!!.icon)
                     val image = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                     mBuilder = NotificationCompat.Builder(this, getString(R.string.app_name))
                         .setSmallIcon(R.drawable.ic_launcher)
                         .setLargeIcon(image)
                         .setStyle(NotificationCompat.DecoratedCustomViewStyle())
                         .setContentTitle(textTitle)
                         .setContentText(messageBody.notification!!.body)
                         .setSound(soundUri)
                         .setAutoCancel(true)
                         .setContentIntent(pendingIntent)
                         .setDefaults(Notification.DEFAULT_SOUND)
                         .setPriority(NotificationCompat.PRIORITY_HIGH)

                 } else {

                     mBuilder = NotificationCompat.Builder(this, getString(R.string.app_name))
                         .setSmallIcon(R.drawable.ic_launcher)

                         .setContentTitle(textTitle)
                         .setContentText(messageBody.notification!!.body)
                         .setSound(soundUri)
                         .setAutoCancel(true)
                         .setContentIntent(pendingIntent)
                         .setDefaults(Notification.DEFAULT_SOUND)
                         .setPriority(NotificationCompat.PRIORITY_HIGH)
                 }*/
            } else {
                if (messageBody.notification!!.icon != null) {
                    Log.e("iconn", "" + messageBody.notification!!.icon)
                    val url = URL(messageBody.notification!!.icon)
                    val image = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    mBuilder = NotificationCompat.Builder(this, getString(R.string.app_name))
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setLargeIcon(image)
                        .setContentTitle(textTitle)
                        .setContentText(messageBody.notification!!.body)
                        .setSound(soundUri)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setDefaults(Notification.DEFAULT_SOUND)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)

                } else if (JSONObject(messageBody.data.toString()).getJSONObject("data")
                        .has("img_url")
                ) {
                    val url = URL(
                        BuildConfig.BASE_URL + JSONObject(messageBody.data.toString())
                            .getJSONObject("data").getString("img_url")
                    )
                    var image: Bitmap? = null
                    try {
                        image = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    } catch (exp: FileNotFoundException) {
                        Timber.d(exp.message)
                    }
                    mBuilder = NotificationCompat.Builder(this, getString(R.string.app_name))
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(textTitle)
                        .setContentText(messageBody.notification!!.body)
                        .setSound(soundUri)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setDefaults(Notification.DEFAULT_SOUND)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                    if (image != null) mBuilder.setLargeIcon(image)
                } else {

                    mBuilder = NotificationCompat.Builder(this, getString(R.string.app_name))
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle(textTitle)
                        .setContentText(messageBody.notification!!.body)
                        .setSound(soundUri)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        .setDefaults(Notification.DEFAULT_SOUND)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                }
            }


            //builder.setLargeIcon(getCircleBitmap(image))


            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name: CharSequence = getString(R.string.app_name)
                val description = getString(R.string.app_name)
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(getString(R.string.app_name), name, importance)
                channel.description = description
                val attributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                channel.enableLights(true)
                channel.enableVibration(true)
                channel.setSound(soundUri, attributes)

                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                val notificationManager = getSystemService(
                    NotificationManager::class.java
                )
                notificationManager.createNotificationChannel(channel)
            }

            val notificationManager = NotificationManagerCompat.from(this)


            // notificationId is a unique int for each notification that you must define
            if (mBuilder != null) {
                notificationManager.notify(Random.nextInt(), mBuilder.build())
            }


        }

        /* else
         {
             val senderId = messageBody.notification!!..additionalData[MainActivity.ARGS_SENDER_ID].toString()

             if (notification.launchURL != null) {
                 MainActivity.notificationOpened = false
                 val intent = Intent(App.getAppContext(), MainActivity::class.java)
                 intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK
                 intent.putExtra(MainActivity.ARGS_SCREEN, notification.launchURL)
                 intent.putExtra(MainActivity.ARGS_SENDER_ID, senderId)
                 startActivity(intent)
             }
         }
     }
     else
     {
         val senderId = notification.additionalData[MainActivity.ARGS_SENDER_ID].toString()

         if (notification.launchURL != null) {
             MainActivity.notificationOpened = false
             val intent = Intent(App.getAppContext(), MainActivity::class.java)
             intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK
             intent.putExtra(MainActivity.ARGS_SCREEN, notification.launchURL)
             intent.putExtra(MainActivity.ARGS_SENDER_ID, senderId)
             startActivity(intent)
         }
     }*/


    }

    private fun sendNotification(message: ChatRoomSubscription.Message) {
        MainActivity.notificationOpened = false
        val intent = Intent(App.getAppContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK
        val id2 = message.roomId.id

        intent.putExtra("isChatNotification", "yes")
        intent.putExtra("roomIDs", id2)


        var pendingIntent: PendingIntent? = null
        pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        val soundUri: Uri

        soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

//        soundUri = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.iphone_ringtone);

        val mBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, getString(R.string.app_name))
                .setSmallIcon(R.drawable.icon_buy_chat_coins)
                .setWhen(System.currentTimeMillis())
                .setColor(resources.getColor(R.color.colorPrimary))
                .setContentInfo(resources.getString(R.string.app_name))
//                .setContentTitle(message.roomId.name)
                .setSound(soundUri)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.app_name)
            val description = getString(R.string.app_name)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(getString(R.string.app_name), name, importance)
            channel.description = description
            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            channel.enableLights(true)
            channel.enableVibration(true)
            channel.setSound(soundUri, attributes)

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notificationManager = NotificationManagerCompat.from(this)

        val myContentView = RemoteViews(packageName, R.layout.layout_notification_content)
        val myExpandedContentView =
            RemoteViews(packageName, R.layout.layout_notification_content_expanded)
        myContentView.setTextViewText(R.id.content_title, message.userId.fullName)
        myExpandedContentView.setTextViewText(R.id.content_title, message.userId.fullName)
        if (message.content.contains("media/chat_files")) {
            val ext = message.content.findFileExtension()
            val stringResId =
                if (ext.isImageFile()) {
                    R.string.photo
                } else if (ext.isVideoFile()) {
                    R.string.video
                } else {
                    R.string.file
                }
            val icon =
                if (ext.isImageFile()) {
                    R.drawable.ic_photo
                } else if (ext.isVideoFile()) {
                    R.drawable.ic_video
                } else {
                    R.drawable.ic_baseline_attach_file_24
                }
            myContentView.setTextViewText(R.id.content_message, getString(stringResId))
            myContentView.setImageViewResource(R.id.myimage, icon)
            myContentView.setViewVisibility(R.id.myimage, View.VISIBLE)

            myExpandedContentView.setTextViewText(R.id.content_message, getString(stringResId))
            myExpandedContentView.setImageViewResource(R.id.myimage, icon)
            myExpandedContentView.setViewVisibility(R.id.myimage, View.VISIBLE)

//            mBuilder.setContentText(message.userId.fullName+" : "+message.content)
        } else {
            myContentView.setTextViewText(R.id.content_message, message.content)
            myContentView.setViewVisibility(R.id.myimage, View.GONE)

            myExpandedContentView.setTextViewText(R.id.content_message, message.content)
            myExpandedContentView.setViewVisibility(R.id.myimage, View.GONE)
//            mBuilder.setContentText(message.userId.fullName+" : "+message.content)
        }

        if (!message.userId.avatarPhotos.isNullOrEmpty()) {
            if (message.userId.avatarPhotos[0] != null && message.userId.avatarPhotos[0]!!.url != null) {
                loadImage(this, message.userId.avatarPhotos[0]!!.url!!, { bitmap ->
//                    myContentView.setImageViewBitmap(R.id.iv_profile, bitmap)
                    myExpandedContentView.setImageViewBitmap(R.id.iv_profile, bitmap)
//                    mBuilder.setLargeIcon(bitmap)
                    mBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
//                    mBuilder.setContentTitle(message.userId.fullName)
                    mBuilder.setCustomContentView(myContentView)
                    mBuilder.setCustomBigContentView(myExpandedContentView)
                    // notificationId is a unique int for each notification that you must define
                    notificationManager.notify(Random.nextInt(), mBuilder.build())
                }, { drawable ->
                    if (drawable != null) {
                        //This Bitmap conversion code is in working state
//                        val bitmap = AppCompatResources.getDrawable(applicationContext, R.drawable.ic_default_user)?.toBitmap()
//                        mBuilder.setLargeIcon(bitmap)
                        myContentView.setImageViewResource(
                            R.id.iv_profile,
                            R.drawable.ic_default_user
                        )
                        myExpandedContentView.setImageViewResource(
                            R.id.iv_profile,
                            R.drawable.ic_default_user
                        )
                    }
                    mBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
//                    mBuilder.setContentTitle(message.userId.fullName)
                    mBuilder.setCustomContentView(myContentView)
                    mBuilder.setCustomBigContentView(myExpandedContentView)
                    // notificationId is a unique int for each notification that you must define
                    notificationManager.notify(Random.nextInt(), mBuilder.build())
                })
            } else {
                myContentView.setImageViewResource(R.id.iv_profile, R.drawable.ic_default_user)
                myExpandedContentView.setImageViewResource(
                    R.id.iv_profile,
                    R.drawable.ic_default_user
                )
                mBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())

                //This Bitmap conversion code is in working state
//                val bitmap = AppCompatResources.getDrawable(applicationContext, R.drawable.ic_default_user)?.toBitmap()
//                mBuilder.setLargeIcon(bitmap)
                mBuilder.setCustomContentView(myContentView)
                mBuilder.setCustomBigContentView(myExpandedContentView)
                // notificationId is a unique int for each notification that you must define
                notificationManager.notify(Random.nextInt(), mBuilder.build())
            }
        }
    }

    companion object {

        private const val TAG = "MyFirebaseMsgService"
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap? {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }
        var width = drawable.intrinsicWidth
        width = if (width > 0) width else 1
        var height = drawable.intrinsicHeight
        height = if (height > 0) height else 1
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}