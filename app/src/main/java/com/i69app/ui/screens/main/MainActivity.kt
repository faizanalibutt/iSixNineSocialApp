package com.i69app.ui.screens.main

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.RemoteViews
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_HIGH
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.apollographql.apollo3.exception.ApolloException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.bottomnavigation.BottomNavigationItemView
import com.google.android.material.bottomnavigation.BottomNavigationMenuView
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.i69app.*
import com.i69app.data.models.User
import com.i69app.data.remote.repository.UserDetailsRepository
import com.i69app.data.remote.repository.UserUpdateRepository
import com.i69app.databinding.ActivityMainBinding
import com.i69app.profile.db.dao.UserDao
import com.i69app.singleton.App
import com.i69app.ui.base.BaseActivity
import com.i69app.ui.screens.SplashActivity
import com.i69app.ui.viewModels.UserMomentsModelView
import com.i69app.ui.viewModels.UserViewModel
import com.i69app.ui.views.MyBottomNavigation
import com.i69app.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retryWhen
import timber.log.Timber
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class MainActivity : BaseActivity<ActivityMainBinding>() {

    private val mViewModel: UserViewModel by viewModels()
    lateinit var navController: NavController
    private var navController2: LiveData<NavController>? = null

    @Inject
    lateinit var userDetailsRepository: UserDetailsRepository

    @Inject
    lateinit var userUpdateRepository: UserUpdateRepository

    @Inject
    lateinit var userDao: UserDao
    private var mUser: User? = null
    private var userId: String? = null
    private var userToken: String? = null
    private var chatUserId: Int = 0
    private lateinit var job: Job
    private val viewModel: UserViewModel by viewModels()
    var bottomNav1: MyBottomNavigation? = null
    var userprofile: String = ""
    private val drawerSelectedItemIdKey = "DRAWER_SELECTED_ITEM_ID_KEY"
    private var drawerSelectedItemId = R.id.nav_search_graph
    val mViewModelUser: UserMomentsModelView by viewModels()
    lateinit var pref: SharedPreferences

    private val permissionReqLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
            run {
                val granted = permission.entries.all {
                    it.value == true
                }
                if (granted) {
                    val locationService =
                        LocationServices.getFusedLocationProviderClient(this@MainActivity)
                    locationService.lastLocation.addOnSuccessListener { location: Location? ->
                        val lat: Double? = location?.latitude

                        val lon: Double? = location?.longitude
//                toast("lat = $lat lng = $lon")
                        if (lat != null && lon != null) {
                            // Update Location
                            lifecycleScope.launch(Dispatchers.Main) {
                                var res = mViewModel.updateLocation(
                                    userId = userId!!,
                                    location = arrayOf(lat, lon),
                                    token = userToken!!
                                )

                                //Log.e("aaaaaaaaz",""+res.data!!.errorMessage)
                                Log.e("aaaaaaaaz", "" + res.message)
                            }
                        }
                    }
                }
            }
        }

    override fun getActivityBinding(inflater: LayoutInflater) =
        ActivityMainBinding.inflate(inflater)

    override fun setupTheme(savedInstanceState: Bundle?) {
        window?.decorView?.setOnApplyWindowInsetsListener { view, insets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets, view)
//            MainActivity.getMainActivity()?.binding?.bottomNavigation?.isGone = insetsCompat.isVisible(
//                WindowInsets.Type.ime()
//            )
            view.onApplyWindowInsets(insets)
        }

        mainActivity = this
        pref = PreferenceManager.getDefaultSharedPreferences(mainActivity)

        // navController = Navigation.findNavController(this, R.id.nav_host_fragment)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setViewModel(mViewModel, binding)
        if (savedInstanceState == null) {
            setupBottomNav()
            //drawerSelectedItemId = getInt(drawerSelectedItemIdKey, drawerSelectedItemId)
        }
        savedInstanceState?.let {
            drawerSelectedItemId = it.getInt(drawerSelectedItemIdKey, drawerSelectedItemId)
        }
        setupNavigation()
        notificationOpened = true
//        showProgressView()
        //updateNavItem(R.drawable.ic_default_user)
        updateFirebaseToken(userUpdateRepository)

        lifecycleScope.launch(Dispatchers.Main) {
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!
            Timber.d("UserId $userId!!")
            Timber.d("UserId1 $userToken!!")
            updateChatBadge()
            mViewModel.getCurrentUser(userId!!, token = userToken!!, true)
                .observe(this@MainActivity) { user ->
                    Timber.d("User $user")
                    user?.let {
                        if (mUser == null) {
                            mUser = it
                            mUser!!.id = "$userId"
                            updateNavItem(
                                mUser!!.avatarPhotos!!.get(mUser!!.avatarIndex!!).url.replace(
                                    "${BuildConfig.BASE_URL}media/",
                                    "${BuildConfig.BASE_URL}media/"
                                )
                            )
                            return@observe
                        }
                        mUser = it
                        updateNavItem(
                            mUser!!.avatarPhotos!!.get(mUser!!.avatarIndex!!).url.replace(
                                "${BuildConfig.BASE_URL}media/",
                                "${BuildConfig.BASE_URL}media/"
                            )
                        )
                    }
                }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            userToken = getCurrentUserToken()!!
            try {
                apolloClientSubscription(this@MainActivity, userToken!!).subscription(
                    ChatRoomSubscription(userToken!!)
                ).toFlow()
                    .retryWhen { cause, attempt ->
                        Timber.d("reealltime retry $attempt ${cause.message}")
                        delay(attempt * 1000)
                        true
                    }.collect { newMessage ->
                        if (newMessage.hasErrors()) {
                            Timber.d("reealltime response error = ${newMessage.errors?.get(0)?.message}")
                        } else {
                            Timber.d("reealltime onNewMessage ${newMessage.data?.onNewMessage?.message?.timestamp}")
                            Log.d(
                                "reealltime",
                                "reealltime ${newMessage.data?.onNewMessage?.message?.content}"
                            )
                            Log.e(
                                "reealltime",
                                "reealltime ${newMessage.data}"
                            )
                            viewModel.onNewMessage(newMessage = newMessage.data?.onNewMessage?.message)
                        }
                    }
                Timber.d("reealltime 2")
            } catch (e2: Exception) {
                e2.printStackTrace()
                Timber.d("reealltime exception= ${e2.message}")
            }
        }
    }

    private fun loadUserMomentsData() {
        userToken?.let {
            mViewModelUser.getAllMoments(it, 100, 100, 10, "") { error ->
                if (error == null) {

                } else {

                }

            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(drawerSelectedItemIdKey, drawerSelectedItemId)
        super.onSaveInstanceState(outState)
    }

    fun getmsgsubscriptionlistner() {
        job = lifecycleScope.launch {
            viewModel.newMessageFlow.collect { message ->
                message?.let { newMessage ->
                    Log.e("fff", "fffff")
                    if (userId != message.userId.id) {
                        Log.e("fff", "fffff222")
                        if (mContextTemp != null) {
                            Log.e("fff", "fffff333")
                            sendNotification(message)
                        }
                        updateChatBadge()
                    }

                }
            }
        }
    }

    fun updateFirebaseToken(userUpdateRepository: UserUpdateRepository) {

        Firebase.messaging.token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Timber.d("App", "Fetching FCM registration token failed")
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            Timber.d("FirebaseToken", "" + token)
            token?.let {
                GlobalScope.launch {
                    val userId = App.userPreferences.userId.first()
                    val userToken = App.userPreferences.userToken.first()

                    if (userId != null && userToken != null) {
                        var res = userUpdateRepository.updateFirebasrToken(userId, token, userToken)
                        getmsgsubscriptionlistner()

                        Timber.d("TOKEN")
                    }
                }
            }

//Toast.makeText(mInstance, token, Toast.LENGTH_SHORT).show()
        })
    }

    private fun sendNotification(message: ChatRoomSubscription.Message) {
        notificationOpened = false
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

        //  soundUri = Uri.parse("android.resource://" + getApplicationContext().getPackageName() + "/" + R.raw.iphone_ringtone);

        Log.e("iconn andr", "" + BuildConfig.BASE_URL + message.userId.avatarPhotos?.get(0)!!.url)
        //   val url = URL(BuildConfig.BASE_URL+ message.userId.avatarPhotos?.get(0)!!.url)
        //   val image = BitmapFactory.decodeStream(url.openConnection().getInputStream())
        val mBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(this, getString(R.string.app_name))
                .setSmallIcon(R.drawable.icon_buy_chat_coins)
                .setColor(resources.getColor(R.color.colorPrimary))
//                .setContentTitle(message.roomId.name)
                .setSound(soundUri)
                .setAutoCancel(true)
                //  .setLargeIcon(image)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setPriority(PRIORITY_HIGH)

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.app_name)
            val description = getString(R.string.app_name)
            val importance = IMPORTANCE_HIGH
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

        val myContentView = RemoteViews(packageName, R.layout.layout_notification_content_expanded)
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
            myContentView.setViewVisibility(R.id.myimage, VISIBLE)

            myExpandedContentView.setTextViewText(R.id.content_message, getString(stringResId))
            myExpandedContentView.setImageViewResource(R.id.myimage, icon)
            myExpandedContentView.setViewVisibility(R.id.myimage, VISIBLE)

//            mBuilder.setContentText(message.userId.fullName+" : "+message.content)
        } else {
            myContentView.setTextViewText(R.id.content_message, message.content)
            myContentView.setViewVisibility(R.id.myimage, GONE)

            myExpandedContentView.setTextViewText(R.id.content_message, message.content)
            myExpandedContentView.setViewVisibility(R.id.myimage, GONE)
//            mBuilder.setContentText(message.userId.fullName+" : "+message.content)
        }

        if (!message.userId.avatarPhotos.isNullOrEmpty()) {
            if (message.userId.avatarPhotos[0] != null && message.userId.avatarPhotos[0]!!.url != null) {
                Log.e("rrr", "111111")
                loadImage(
                    this,
                    BuildConfig.BASE_URL + message.userId.avatarPhotos.get(0)!!.url,
                    { bitmap ->
//                    myContentView.setImageViewBitmap(R.id.iv_profile, bitmap)
                        Log.e("rrrccc", "111111")
                        myContentView.setImageViewBitmap(R.id.iv_profile, bitmap)
//                    mBuilder.setLargeIcon(bitmap)
                        mBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
//                    mBuilder.setContentTitle(message.userId.fullName)
                        mBuilder.setCustomContentView(myContentView)
                        //mBuilder.setCustomBigContentView(myExpandedContentView)
                        // notificationId is a unique int for each notification that you must define
                        notificationManager.notify(Random.nextInt(), mBuilder.build())
                    },
                    { drawable ->
                        if (drawable != null) {
                            Log.e("rrrccc", "22222")
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
                        // mBuilder.setCustomBigContentView(myExpandedContentView)
                        // notificationId is a unique int for each notification that you must define
                        notificationManager.notify(Random.nextInt(), mBuilder.build())
                    })
            } else {
                Log.e("rrr", "22222")
                myContentView.setImageViewResource(R.id.iv_profile, R.drawable.login_logo)
                myExpandedContentView.setImageViewResource(R.id.iv_profile, R.drawable.login_logo)
                mBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                myExpandedContentView.setViewVisibility(R.id.iv_profile, GONE)
                myContentView.setViewVisibility(R.id.iv_profile, GONE)
                //This Bitmap conversion code is in working state
//                val bitmap = AppCompatResources.getDrawable(applicationContext, R.drawable.ic_default_user)?.toBitmap()
//                mBuilder.setLargeIcon(bitmap)
                mBuilder.setCustomContentView(myContentView)
                //  mBuilder.setCustomBigContentView(myExpandedContentView)
                // notificationId is a unique int for each notification that you must define
                notificationManager.notify(Random.nextInt(), mBuilder.build())
            }
        } else {
            Log.e("rrr", "3333")
            myContentView.setImageViewResource(R.id.iv_profile, R.drawable.ic_default_user)
            myExpandedContentView.setImageViewResource(R.id.iv_profile, R.drawable.ic_default_user)
            mBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())

            //This Bitmap conversion code is in working state
//                val bitmap = AppCompatResources.getDrawable(applicationContext, R.drawable.ic_default_user)?.toBitmap()
//                mBuilder.setLargeIcon(bitmap)
            mBuilder.setCustomContentView(myContentView)
            //mBuilder.setCustomBigContentView(myExpandedContentView)
            // notificationId is a unique int for each notification that you must define
            notificationManager.notify(Random.nextInt(), mBuilder.build())
        }
        /*if(WEB_URL.matcher(message.content).matches()){
            mBuilder.setContentText(message.userId.fullName+" : ")
            loadImage(this, message.content, { bitmap ->
                mBuilder.setLargeIcon(bitmap)
                mBuilder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
                // notificationId is a unique int for each notification that you must define
                notificationManager.notify(Random.nextInt(), mBuilder.build())
            }, { drawable ->
                if(drawable != null){
                    mBuilder.setSmallIcon(R.drawable.ic_default_user)
                }
                // notificationId is a unique int for each notification that you must define
                notificationManager.notify(Random.nextInt(), mBuilder.build())
            })
        }else{
            mBuilder.setContentText(message.userId.fullName+" : "+message.content)
        }*/
    }

    private fun sendNotification(title: String, message: String) {
        Log.e("fdfdf", "fd")
        notificationOpened = false
        val intent = Intent(App.getAppContext(), MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK
        val id2 = "fdsfsd"

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
                .setSmallIcon(R.drawable.ic_launcher)
//                .setContentTitle(message.roomId.name)
                .setSound(soundUri)

                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setPriority(PRIORITY_HIGH)

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name: CharSequence = getString(R.string.app_name)
            val description = getString(R.string.app_name)
            val importance = IMPORTANCE_HIGH
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

        if (message.contains("media/chat_files")) {
            val ext = message.findFileExtension()
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
            val myContentView =
                RemoteViews(packageName, R.layout.layout_notification_content_expanded)
            myContentView.setImageViewResource(R.id.myimage, icon)
            myContentView.setTextViewText(R.id.content_title, title)
            myContentView.setTextViewText(R.id.content_message, getString(stringResId))
//            mBuilder.setContentText(message.userId.fullName+" : "+message.content)
            mBuilder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            mBuilder.setCustomContentView(myContentView)
            mBuilder.setCustomBigContentView(myContentView)
        } else {
            mBuilder.setContentText("UserName : " + message)
        }
        notificationManager.notify(Random.nextInt(), mBuilder.build())
    }

    fun updateChatBadge() {
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(this@MainActivity, userToken!!).query(GetAllRoomsQuery(20)).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception all room API ${e.message}")
                return@launchWhenResumed
            }

            val allRoom = res.data?.rooms?.edges
            if (allRoom.isNullOrEmpty()) {
                //addBadge(0)
                binding.bottomNavigation.addBadge(0)
                return@launchWhenResumed
            }

            var totoalunread = 0
            allRoom.indices.forEach { i ->
                val data = allRoom[i]
                if (totoalunread == 0) {
                    totoalunread = data!!.node!!.unread!!.toInt()
                } else {
                    totoalunread = totoalunread + data!!.node!!.unread!!.toInt()
                }
            }

            //addBadge(totoalunread)
            try {
                binding.bottomNavigation.addBadge(totoalunread)
                //binding.navView.updateMessagesCount(totoalunread)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun setupClickListeners() {

    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        //unregisterQbChatListeners()
    }

    private fun setupNavigation() {
        updateLocation()
        binding.mainNavView.getHeaderView(0).findViewById<View>(R.id.btnHeaderClose)
            .setOnClickListener {
                //disableNavigationDrawer()
                if (binding.drawerLayout.isOpen) {
                    binding.drawerLayout.close()
                } else {
                    navController.popBackStack()
                }
            }
        /*  KeyboardUtils.addKeyboardToggleListener(this) { isVisible ->
             // binding.navView.visibility = if (isVisible) GONE else VISIBLE
          }*/
        binding.mainNavView.itemIconTintList = null
        binding.mainNavView.setNavigationItemSelectedListener {
            Handler(Looper.getMainLooper()).postDelayed({
                when (it.itemId) {
                    R.id.nav_chat_graph -> openSearchScreen()
                    R.id.nav_coinpurchase_graph -> openCoinScreen()
                    R.id.nav_item_contact -> this.startEmailIntent(
                        com.i69app.data.config.Constants.ADMIN_EMAIL,
                        "",
                        ""
                    )
                    R.id.nav_privacy_graph -> {
                        openPrivacyScreen()
                        /*val intent = Intent(this, PrivacyOrTermsConditionsActivity::class.java)
                         intent.putExtra("type", "privacy")
                         startActivity(intent)*/
                    }
                    R.id.nav_setting_graph -> openSettingsScreen()
                }
            }, 200)

            binding.drawerLayout.closeNavigationDrawer()
            return@setNavigationItemSelectedListener true
        }
    }

    private fun observeNotification() {
        if ((intent.hasExtra("isNotification") && intent.getStringExtra("isNotification") != null)) {
            Log.e("notii", "--> " + "11")
            //val bundle = Bundle()
            //bundle.putString("ShowNotification", "true")
            //navController.navigate(R.id.action_user_moments_fragment, bundle)
            //newActionHome
            pref.edit().putString("ShowNotification", "true").apply()
            binding.bottomNavigation.selectedItemId = R.id.nav_home_graph
            /*val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
            if (currentFragment != null) {
                currentFragment.findNavController().navigate(R.id.newActionHome,bundle)
            }*/
        } else if ((intent.hasExtra("isChatNotification") && intent.getStringExtra("isChatNotification") != null)) {
            Log.e("notii", "--> " + "22")
            Log.e("notii", "--> " + "22-->" + intent.getStringExtra("roomIDs"))

            if ((intent.hasExtra("roomIDs") && intent.getStringExtra("roomIDs") != null)) {
                val rID = intent.getStringExtra("roomIDs")

                /* val bundle = Bundle()
                 bundle.putString("roomIDNotify", rID)
                 //navController.navigate(R.id.messengerListFragment, bundle)
                 val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
                 val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
                 if (currentFragment != null) {
                     currentFragment.findNavController().navigate(R.id.newAction,bundle)
                 }*/
                pref.edit().putString("roomIDNotify", "true").putString("roomID", rID).apply()
                binding.bottomNavigation.selectedItemId = R.id.nav_chat_graph
            }
            //binding.root.snackbar("Chat Message Notification cliked.")
        } else if (intent.hasExtra(ARGS_SCREEN) && intent.getStringExtra(ARGS_SCREEN) != null) {
            Log.e("notii", "--> " + "33")
            if (intent.hasExtra(ARGS_SENDER_ID) && intent.getStringExtra(ARGS_SENDER_ID) != null) {
                val senderId = intent.getStringExtra(ARGS_SENDER_ID)
                onNotificationClick(senderId!!)

            } else {
                openMessagesScreen()
            }
        }
    }

    private fun onNotificationClick(senderId: String) {
//        val msgPreviewModel: MessagePreviewModel? = QbDialogHolder.getChatDialogById(senderId)
//        msgPreviewModel?.let {
//            viewModel?.setSelectedMessagePreview(it)
//            navController.navigate(R.id.globalUserToChatAction)
//        }
    }

    fun drawerSwitchState() {
        binding.drawerLayout.drawerSwitchState()
    }

    fun enableNavigationDrawer() {
        binding.drawerLayout.enableNavigationDrawer()
    }

    fun disableNavigationDrawer() {
        binding.drawerLayout.disableNavigationDrawer()
    }

    private fun updateNavItem(userAvatar: Any?) {
        userprofile = userAvatar.toString()
        // binding.bottomNavigation.selectedItemId = R.id.nav_search_graph
        binding.bottomNavigation.loadImage(
            userprofile, R.id.nav_user_profile_graph, R.drawable.ic_default_user
        )

        /* binding.navView.setItems(
             arrayListOf(
                 Pair(R.drawable.ic_search_inactive, R.drawable.ic_search_active),
                 Pair(R.drawable.ic_home_inactive, R.drawable.ic_home_active),
                 Pair(R.drawable.ic_add_btn, R.drawable.icon_add_black_button),
                 Pair(R.drawable.ic_chat_inactive, R.drawable.ic_chat_active),
                 Pair(userAvatar, userAvatar)
             )
         )*/
    }

    private fun updateLocation() {
        /*Permissions.check(this, Manifest.permission.ACCESS_FINE_LOCATION, null, object : PermissionHandler() {
            override fun onGranted() {
                val locationService = LocationServices.getFusedLocationProviderClient(this@MainActivity)
                locationService.lastLocation.addOnSuccessListener { location: Location? ->
                    val lat: Double? = location?.latitude
                    val lon: Double? = location?.longitude
                    toast("lat = $lat lng = $lon")
                    if (lat != null && lon != null) {
                        // Update Location
                        lifecycleScope.launch(Dispatchers.Main) {
                            mViewModel.updateLocation(userId = userId!!, location = arrayOf(lat, lon), token = userToken!!)
                        }
                    }
                }
            }
        })*/
        if (hasPermissions(applicationContext, locPermissions)) {
            val locationService = LocationServices.getFusedLocationProviderClient(this@MainActivity)
            locationService.lastLocation.addOnSuccessListener { location: Location? ->
                val lat: Double? = location?.latitude
                val lon: Double? = location?.longitude
//                toast("lat = $lat lng = $lon")
                if (lat != null && lon != null) {
                    // Update Location
                    lifecycleScope.launch(Dispatchers.Main) {
                        var res = mViewModel.updateLocation(
                            userId = userId!!,
                            location = arrayOf(lat, lon),
                            token = userToken!!
                        )

                        if (res.message.equals("User doesn't exist")) {
                            //error("User doesn't exist")
                            lifecycleScope.launch(Dispatchers.Main) {
                                userPreferences.clear()
                                //App.userPreferences.saveUserIdToken("","","")
                                val intent = Intent(this@MainActivity, SplashActivity::class.java)
                                startActivity(intent)
                                finishAffinity()
                            }
                        }
                        //  Log.e("aaaaaaaa",""+res.data!!.errorMessage)
                        Log.e("aaaaaaaa", "" + res.message)

                    }
                }
            }
        } else {
            permissionReqLauncher.launch(locPermissions)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    var mContextTemp: Context? = null
    override fun onPause() {
        super.onPause()
        mContextTemp = null


    }

    override fun onResume() {
        super.onResume()
        mContext = this@MainActivity
        mContextTemp = this@MainActivity
        if (!notificationOpened) {
            notificationOpened = true
            observeNotification()
        }

    }

    private fun goToMainActions(position: Int) {
        when (position) {
            0 -> openSearchScreen()
            1 -> openUserMoments()
            2 -> openNewUserMoment()
            3 -> openMessagesScreen()
            4 -> openProfileScreen()
        }
    }

    private fun openSearchScreen() {
        //navController.navigate(R.navigation.nav_search_graph)
        binding.bottomNavigation.selectedItemId = R.id.nav_search_graph
        //navController.setGraph(R.id.nav_chat_graph)
        //navController.navigate(R.id.action_global_search_interested_in)
        //navController.popBackStack(R.id.action_global_search_interested_in,true)
    }

    fun openUserMoments() {
        //val bundle = Bundle()
        //bundle.putString("ShowNotification", "false")
        //navController.navigate(R.id.action_user_moments_fragment, bundle)
        binding.bottomNavigation.selectedItemId = R.id.nav_home_graph
    }

    private fun openNewUserMoment() {
        //navController.navigate(R.id.action_new_user_moment_fragment)
    }

    private fun openMessagesScreen() {
        //navController.navigate(R.id.messengerListAction)
        binding.bottomNavigation.selectedItemId = R.id.nav_chat_graph
    }

    private fun openProfileScreen() {
        //navController.navigate(R.id.action_global_user_profile)
    }

    private fun openPrivacyScreen() {
        pref.edit()?.putString("typeview", "privacy")?.apply()
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
        if (currentFragment != null) {
            currentFragment.findNavController().navigate(R.id.actionGoToPrivacyFragment)
        }
    }

    private fun openCoinScreen() {
        //navController.navigate(R.id.nav_coinpurchase_graph)
        /*val graph = navController.navInflater.inflate(R.navigation.nav_coinpurchase_graph)
         graph.startDestination = R.id.purchaseFragment
         navController.graph = graph*/
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
        if (currentFragment != null) {
            currentFragment.findNavController().navigate(R.id.action_global_purchase)
        }
    }

    private fun openSettingsScreen() {
        //binding.bottomNavigation.selectedItemId = R.id.nav_setting_graph
        //navController.navigate(R.id.actionGoToSettingsFragment)
        //navController = navHostFragment.navController
        //navController.setGraph(R.navigation.nav_setting_graph)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val currentFragment = navHostFragment.childFragmentManager.primaryNavigationFragment
        if (currentFragment != null) {
            currentFragment.findNavController().navigate(R.id.action_global_setting)
        }
    }

    // Chat Section (Companion Object)
    companion object {
        var mContext: Context? = null
        const val CHAT_TAG = "SH_CHAT"
        const val ARGS_SCREEN = "screen"
        const val ARGS_MESSAGE_SCREEN = "message_screen"
        const val ARGS_SENDER_ID = "sender_id"
        const val ARGS_CHANNEL_ID = "5f2f7e32-cf68-4a8e-b27b-41b692aab5b1"

        var notificationOpened = false
        private var viewModel: UserViewModel? = null
        private var binding: ActivityMainBinding? = null

        var mainActivity: MainActivity? = null

        @JvmName("getMainActivity1")
        fun getMainActivity(): MainActivity? {
            return mainActivity
        }

        fun setViewModel(updatedViewModel: UserViewModel, updatedBinding: ActivityMainBinding) {
            viewModel = updatedViewModel
            binding = updatedBinding
        }

        /*private fun updateUnseenMessages(previewMsgList: ArrayList<MessagePreviewModel>?) {
            if (!previewMsgList.isNullOrEmpty()) {
                var unseenMsgCount = 0
                previewMsgList.forEach { msgPreview ->
                    if (msgPreview.chatDialog.unreadMessageCount > 0) unseenMsgCount =
                        unseenMsgCount.plus(1)
                }

                //binding!!.navView.updateMessagesCount(unseenMsgCount)
                binding?.bottomNavigation?.addBadge(unseenMsgCount)
                viewModel!!.updateAdapterFlow()

            }
        }*/


    }

    private val locPermissions = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        setupBottomNav()
        //setupNavigation()
    }

    fun addBadge(number: Int) {
        val badge = bottomNav1?.getOrCreateBadge(R.id.nav_chat_graph)
        if (number > 0) {
            if (badge != null) {
                badge.isVisible = true
                badge.number = number
                badge.badgeTextColor = R.color.white
                badge.backgroundColor = ContextCompat.getColor(this, R.color.colorPrimary)
            }
        } else {
            if (badge != null) {
                badge.isVisible = false
                badge.clearNumber()
            }
        }

    }

    private fun removeBadge() {
        bottomNav1?.removeBadge(R.id.nav_chat_graph)
    }

    private fun setupBottomNav() {
        val graphIds = listOf(
            R.navigation.nav_search_graph,
            R.navigation.nav_home_graph,
            R.navigation.nav_add_new_moment_graph,
            R.navigation.nav_chat_graph,
            R.navigation.nav_user_profile_graph
        )
        val bottomNav = findViewById<MyBottomNavigation>(R.id.bottomNavigation)
        bottomNav1 = bottomNav

        /*val controller = bottomNav.setupWithNavController(
            graphIds,
            supportFragmentManager,
            R.id.nav_host_fragment,
            intent
        )*/
        binding.bottomNavigation.itemIconTintList = null

        val controller = bottomNav.setupWithNavController(
            fragmentManager = supportFragmentManager,
            navGraphIds = graphIds,
            backButtonBehaviour = BackButtonBehaviour.POP_HOST_FRAGMENT,
            containerId = R.id.nav_host_fragment,
            firstItemId = R.id.nav_search_graph, // Must be the same as bottomNavSelectedItemId
            intent = this@MainActivity.intent
        )
        controller.observe(this) {

            setupActionBarWithNavController(it)
        }
        navController2 = controller

        val navigationItemView = bottomNav.getChildAt(0) as BottomNavigationMenuView
        val navigationItemView2 = navigationItemView.getChildAt(4) as BottomNavigationItemView
        val displayMetrics = resources.displayMetrics

        navigationItemView2.setIconSize(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 50f,
                displayMetrics
            ).toInt()
        )

    }

    override fun onSupportNavigateUp(): Boolean {
        return navController2?.value?.navigateUp()!! || super.onSupportNavigateUp()
    }

}