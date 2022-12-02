package com.i69app.ui.screens.main.moment

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateUtils
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.DefaultUpload
import com.apollographql.apollo3.api.content
import com.apollographql.apollo3.exception.ApolloException
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.i69app.*
import com.i69app.data.models.ModelGifts
import com.i69app.databinding.FragmentUserMomentsBinding
import com.i69app.di.modules.AppModule
import com.i69app.di.modules.AppModule.provideGraphqlApi
import com.i69app.gifts.FragmentRealGifts
import com.i69app.gifts.FragmentVirtualGifts
import com.i69app.ui.adapters.NearbySharedMomentAdapter
import com.i69app.ui.adapters.UserItemsAdapter
import com.i69app.ui.adapters.UserStoriesAdapter
import com.i69app.ui.base.BaseFragment
import com.i69app.ui.screens.ImagePickerActivity
import com.i69app.ui.screens.SplashActivity
import com.i69app.ui.screens.main.MainActivity
import com.i69app.ui.screens.main.notification.NotificationDialogFragment
import com.i69app.ui.viewModels.UserMomentsModelView
import com.i69app.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class UserMomentsFragment : BaseFragment<FragmentUserMomentsBinding>(),
    UserStoriesAdapter.UserStoryListener,
    NearbySharedMomentAdapter.NearbySharedMomentListener {

    private val mViewModel: UserMomentsModelView by activityViewModels()
    private var ShowNotification: String? = ""
    var width = 0
    var size = 0
    private var userToken: String? = null
    private lateinit var usersAdapter: UserStoriesAdapter
    private lateinit var sharedMomentAdapter: NearbySharedMomentAdapter
    private var mFilePath: String? = null
    var layoutManager: LinearLayoutManager? = null
    var allUserMoments: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()
    var allUserMoments2: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()
    var allUserMoments1: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()

    private var userId: String? = null
    private var userName: String? = null

    var endCursor: String = ""
    var hasNextPage: Boolean = false
    var hasLoaded: Boolean = false

    private lateinit var GiftbottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    var giftUserid: String? = null
    var fragVirtualGifts: FragmentVirtualGifts? = null
    var fragRealGifts: FragmentRealGifts? = null

    private val photosLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            val data = activityResult.data
            if (activityResult.resultCode == Activity.RESULT_OK) {
                mFilePath = data?.getStringExtra("result")
                Timber.d("fileBase64 $mFilePath")
                uploadStory()
            }
        }

    private val videoLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            val contentURI = activityResult.data?.data
            mFilePath = contentURI?.getVideoFilePath(requireContext())
            val f = File(mFilePath!!)
            Timber.d("filee ${f.exists()} ${f.length()} ${f.absolutePath}")
            if (f.exists()) {
                val sizeInMb = (f.length() / 1000) / 1000
                if (sizeInMb < 2) {
                    uploadStory()
                } else {
                    mFilePath = null
                    val ok = resources.getString(R.string.pix_ok)
                    requireContext().showOkAlertDialog(
                        ok,
                        "File Size ${sizeInMb}MB",
                        "Your video file should be less than 11mb"
                    ) { dialog, which -> }
                }
            } else {
                binding.root.snackbar("Wrong path $mFilePath")
            }
        }

    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentUserMomentsBinding.inflate(inflater, container, false)

    override fun setupTheme() {
        navController = findNavController()
        binding.model = mViewModel

        lifecycleScope.launch {
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!
            Timber.i("usertokenn $userToken")
        }
        Timber.i("userID $userId")

        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        width = displayMetrics.widthPixels

        val densityMultiplier = getResources().getDisplayMetrics().density;
        val scaledPx = 14 * densityMultiplier;
        val paint = Paint()
        paint.setTextSize(scaledPx);
        size = paint.measureText("s").roundToInt()

        //allUserMoments = ArrayList()
        //layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        //binding.rvSharedMoments.setLayoutManager(layoutManager)

        getAllUserStories()
        if (mViewModel.userMomentsList.size == 0) {
            endCursor = "";
            sharedMomentAdapter = NearbySharedMomentAdapter(
                requireActivity(),
                this@UserMomentsFragment,
                allUserMoments1,
                userId
            )
        }
        binding.rvSharedMoments.adapter = sharedMomentAdapter
        //binding.rvSharedMoments.setHasFixedSize(true)
        binding.rvSharedMoments.isNestedScrollingEnabled = false


        //getAllUserMoments(width,size)
        if (mViewModel.userMomentsList.size == 0) {
            getMainActivity().pref.edit().putString("checkUserMomentUpdate", "false").apply()
            getUserMomentNextpage(width, size, 10, endCursor)
        }

        if (getMainActivity().pref.getString("checkUserMomentUpdate", "false").equals("true")) {
            getMainActivity().pref.edit().putString("checkUserMomentUpdate", "false").apply()
            getMainActivity().pref.getString("mID", "")
                ?.let { getParticularMoments(getMainActivity().pref.getInt("itemPosition", 0), it) }
        }

        binding.bubble.setOnClickListener {
            try {
                val layabouts: LinearLayoutManager =
                    binding.rvSharedMoments.layoutManager as LinearLayoutManager
                layabouts.scrollToPositionWithOffset(0, 0)
                binding.scrollView.fullScroll(View.FOCUS_UP)
                binding.bubble.isVisible = false
            } catch (e: Exception) {
                Log.e("TAG_Click", "setupTheme: " + e.message.toString())
            }
        }
        binding.btnNewPostCheck.setOnClickListener() {
            try {
                val layabouts: LinearLayoutManager =
                    binding.rvSharedMoments.layoutManager as LinearLayoutManager
                layabouts.scrollToPositionWithOffset(0, 0)
                binding.scrollView.fullScroll(View.FOCUS_UP)
                binding.bubble.isVisible = false
            } catch (e: Exception) {
                Log.e("TAG_Click", "setupTheme: " + e.message.toString())
            }
        }

        if (binding.rvSharedMoments.itemDecorationCount == 0) {
            binding.rvSharedMoments.addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.top = 10
                }
            })
        }

        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (hasNextPage) {

                binding.rvSharedMoments.let {
                    if (it.bottom - (binding.scrollView.height + binding.scrollView.scrollY) == 0)
                        getUserMomentNextpage(width, size, 10, endCursor)
                    //allusermoments1(width,size,10,endCursor)

                    binding.btnNewPostCheck.isVisible = scrollY > height
                    if (binding.bubble.isVisible) {
                        if (scrollY == 0) {
                            binding.bubble.isVisible = false
                        }
                    }
//                   binding.bubble.isVisible=scrollY==height
                }
            }
        })
    }


    override fun setupClickListeners() {
        binding.toolbarHamburger.setOnClickListener {
            getMainActivity().drawerSwitchState()
        }

        binding.bell.setOnClickListener {
            val dialog = NotificationDialogFragment(
                userToken,
                binding.counter,
                userId,
                binding.bell
            )
            dialog.show(childFragmentManager, "notifications")
        }

        GiftbottomSheetBehavior =
            BottomSheetBehavior.from<ConstraintLayout>(binding.giftbottomSheet)
        GiftbottomSheetBehavior.setBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when (newState) {
                    BottomSheetBehavior.STATE_COLLAPSED -> {
                        Timber.d("Slide Up")
                    }
                    BottomSheetBehavior.STATE_HIDDEN -> {

                    }
                    BottomSheetBehavior.STATE_EXPANDED -> {
                        Timber.d("Slide Down")
                    }
                    BottomSheetBehavior.STATE_DRAGGING -> {

                    }
                    BottomSheetBehavior.STATE_SETTLING -> {

                    }
                }
            }
        })

        binding.sendgiftto.setOnClickListener(View.OnClickListener {

            val items: MutableList<ModelGifts.Data.AllRealGift> = mutableListOf()
            //if (fragVirtualGifts?.giftsAdapter!=null)
            fragVirtualGifts?.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }

            fragRealGifts?.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }

            lifecycleScope.launchWhenCreated() {
                if (items.size > 0) {
                    showProgressView()
                    items.forEach { gift ->

                        var res: ApolloResponse<GiftPurchaseMutation.Data>? = null
                        try {
                            res = apolloClient(
                                requireContext(),
                                userToken!!
                            ).mutation(GiftPurchaseMutation(gift.id, giftUserid!!)).execute()
                        } catch (e: ApolloException) {
                            Timber.d("apolloResponse ${e.message}")
                            Toast.makeText(
                                requireContext(),
                                "Exception ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            //views.snackbar("Exception ${e.message}")
                            //hideProgressView()
                            //return@launchWhenResumed
                        }
                        if (res?.hasErrors() == false) {
                            //views.snackbar("You bought ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} successfully!")
                            Toast.makeText(
                                requireContext(),
                                "You bought ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} successfully!",
                                Toast.LENGTH_LONG
                            ).show()
                            // fireGiftBuyNotificationforreceiver(gift.id,giftUserid)

                        }
                        if (res!!.hasErrors()) {
//                                views.snackbar(""+ res.errors!![0].message)
                            Toast.makeText(
                                requireContext(),
                                "" + res.errors!![0].message,
                                Toast.LENGTH_LONG
                            ).show()

                        }
                        Log.e("rr3rr", "-->" + Gson().toJson(res))
                        Timber.d("apolloResponse ${res.hasErrors()} ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName}")
                    }
                    hideProgressView()
                }
            }

        })


        binding.giftsTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab) {
                binding.giftsPager.currentItem = tab.position
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab?) {
            }
        })
        binding.giftsTabs.setupWithViewPager(binding.giftsPager)
        setupViewPager(binding.giftsPager)

    }

    fun fireGiftBuyNotificationforreceiver(gid: String, userid: String?) {

        lifecycleScope.launchWhenResumed {
            val queryName = "sendNotification"
            val query = StringBuilder()
                .append("mutation {")
                .append("$queryName (")
                .append("userId: \"${userid}\", ")
                .append("notificationSetting: \"GIFT RLVRTL\", ")
                .append("data: {giftId:${gid}}")
                .append(") {")
                .append("sent")
                .append("}")
                .append("}")
                .toString()

            val result = AppModule.provideGraphqlApi().getResponse<Boolean>(
                query,
                queryName, userToken
            )
            Timber.d("RSLT", "" + result.message)
        }
    }


    private fun setupViewPager(viewPager: ViewPager) {
        val adapter = UserItemsAdapter(childFragmentManager)
        fragRealGifts = FragmentRealGifts()
        fragVirtualGifts = FragmentVirtualGifts()

        adapter.addFragItem(fragRealGifts!!, getString(R.string.real_gifts))
        adapter.addFragItem(fragVirtualGifts!!, getString(R.string.virtual_gifts))
        viewPager.adapter = adapter
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val arguments = arguments
        if (arguments != null) {
            ShowNotification = arguments.get("ShowNotification") as String?

            if (ShowNotification.equals("true")) {

                Handler().postDelayed({ binding.bell.performClick() }, 500)
            }
        }
        ShowNotification = getMainActivity().pref.getString("ShowNotification", "false")
        if (ShowNotification.equals("true")) {
            getMainActivity().pref.edit().putString("ShowNotification", "false").apply()
            ShowNotification = "false"
            Handler().postDelayed({ binding.bell.performClick() }, 500)
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    private fun getAllUserMoments(width: Int, size: Int) {

        showProgressView()
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(
                    GetAllUserMomentsQuery(
                        width,
                        size,
                        10,
                        "",
                        ""
                    )
                ).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception all moments ${e.message}")
                hideProgressView()
                return@launchWhenResumed
            }

            hideProgressView()
            Log.e("rr2rr", "-->" + Gson().toJson(res))

            if (res.hasErrors()) {
                Log.e(
                    "rr2rrr",
                    "-->" + res.errors!!.get(0).nonStandardFields!!.get("code").toString()
                )
                if (res.errors!!.get(0).nonStandardFields!!.get("code").toString()
                        .equals("InvalidOrExpiredToken")
                ) {
                    // error("User doesn't exist")

                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        if (activity != null) {
                            //App.userPreferences.saveUserIdToken("","","")
                            val intent = Intent(activity, SplashActivity::class.java)
                            startActivity(intent)
                            activity!!.finishAffinity()
                        }
                    }
                }
            }

            val allmoments = res.data?.allUserMoments!!.edges
            if (allmoments.size != 0) {
                endCursor = res.data?.allUserMoments!!.pageInfo.endCursor!!
                hasNextPage = res.data?.allUserMoments!!.pageInfo.hasNextPage!!


                val allUserMomentsFirst: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()


                allmoments.indices.forEach { i ->
                    allUserMomentsFirst.add(allmoments[i]!!)
                }
                sharedMomentAdapter = NearbySharedMomentAdapter(
                    requireActivity(),
                    this@UserMomentsFragment,
                    allUserMomentsFirst,
                    userId
                )
                /*if (!hasLoaded) {
                    sharedMomentAdapter.addAll(allUserMomentsFirst)
                    hasLoaded=true
               }else{
                   sharedMomentAdapter.updateList(allUserMomentsFirst,userId)
                }*/

                binding.rvSharedMoments.adapter = sharedMomentAdapter
            }

            if (binding.rvSharedMoments.itemDecorationCount == 0) {
                binding.rvSharedMoments.addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        outRect.top = 10
                    }
                })
            }
            if (allmoments.size > 0) {
                Timber.d("apolloResponse: ${allmoments.get(0)?.node!!.file}")
                Timber.d("apolloResponse: ${allmoments.get(0)?.node!!.id}")
                Timber.d("apolloResponse: ${allmoments.get(0)?.node!!.createdDate}")
                Timber.d("apolloResponse: ${allmoments.get(0)?.node!!.momentDescriptionPaginated}")
                Timber.d("apolloResponse: ${allmoments.get(0)?.node!!.user?.fullName}")
            }
            //binding.root.snackbar("apolloResponse ${allusermoments?.get(0)?.file}")
        }
    }

    private fun getNotificationIndex() {

        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetNotificationCountQuery())
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception NotificationIndex ${e.message}")
                return@launchWhenResumed
            }
            Timber.d("apolloResponse NotificationIndex ${res.hasErrors()}")
            Log.e("rr1rr", "-->" + Gson().toJson(res))

            if (res.hasErrors()) {
                Log.e(
                    "rr1rrr",
                    "-->" + res.errors!!.get(0).nonStandardFields!!.get("code").toString()
                )

                if (res.errors!!.get(0).nonStandardFields!!.get("code").toString()
                        .equals("InvalidOrExpiredToken")
                ) {
                    // error("User doesn't exist")

                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        if (activity != null) {
                            //App.userPreferences.saveUserIdToken("","","")
                            val intent = Intent(activity, SplashActivity::class.java)
                            startActivity(intent)
                            activity!!.finishAffinity()
                        }
                    }
                }
            }
            val NotificationCount = res.data?.unseenCount
            if (NotificationCount == null || NotificationCount == 0) {
                binding.counter.visibility = View.GONE
                binding.bell.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.notification1
                    )
                )
            } else {
                binding.counter.visibility = View.VISIBLE
                binding.counter.setText("" + NotificationCount)
                binding.bell.setImageDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.notification2
                    )
                )
            }
        }
    }

    override fun onResume() {
        getNotificationIndex()
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction("moment_added")
        intentFilter.addAction("moment_deleted")
        intentFilter.addAction("moment_updated")
        intentFilter.addAction("story_deleted")
        intentFilter.addAction("story_added")
        activity?.registerReceiver(broadCastReceiver, intentFilter)
    }

    private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            val extras = intent?.extras
            val state = extras!!.getString("extra")
            Timber.e("Tag_Moment_added onReceive: $state")
            Timber.e("Tag_Moment_deleted onReceive: $state")
            Timber.e("Tag_Moment_updated onReceive: $state")
            Timber.e("Tag_Story_deleted onReceive: $state")
            Timber.e("Tag_Story_added onReceive: $state")

            if (state.equals("Story Deleted")
                || state.equals("New Story added")
            ) {
                getAllUserStories()
                return
            }

            val listSize = mViewModel.userMomentsList.size - 1
            mViewModel.userMomentsList.clear()

            if (state.equals("Moment Added"))
                binding.bubble.isVisible = binding.scrollView.scrollY == 0

            getUserMomentNextpage(width, size, listSize, "")

//            updateView(state.toString())
        }
    }

    private fun getAllUserStories() {

        //showProgressView()
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(
                    GetAllUserStoriesQuery(
                        100,
                        "",
                        ""
                    )
                ).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception all stories ${e.message}")
                //hideProgressView()
                return@launchWhenResumed
            }
            Timber.d("apolloResponse allUserStories stories ${res.hasErrors()}")
            Log.e("rr4rr", "-->" + Gson().toJson(res))

            if (res.hasErrors()) {
                Log.e("rr4rrrr", "-->" + res.errors!!.get(0).nonStandardFields!!.get("code"))
                if (res.errors!!.get(0).nonStandardFields!!.get("code").toString()
                        .equals("InvalidOrExpiredToken")
                ) {
                    // error("User doesn't exist")

                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        if (activity != null) {
                            //App.userPreferences.saveUserIdToken("","","")
                            val intent = Intent(activity, SplashActivity::class.java)
                            startActivity(intent)
                            activity!!.finishAffinity()
                        }
                    }
                }
            }

            // hideProgressView()
            val allUserStories = res.data?.allUserStories!!.edges.also {

                usersAdapter = UserStoriesAdapter(requireActivity(), this@UserMomentsFragment, it)
            }
            binding.rvUserStories.adapter = usersAdapter
            if (binding.rvUserStories.itemDecorationCount == 0) {
                binding.rvUserStories.addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        outRect.top = 20
                        outRect.bottom = 10
                        outRect.left = 20
                    }
                })
            }

            if (allUserStories?.size!! > 0) {
                Timber.d("apolloResponse: stories ${allUserStories?.size}")
                Timber.d("apolloResponse: stories ${allUserStories?.get(0)?.node!!.file}")
                Timber.d("apolloResponse: stories ${allUserStories?.get(0)?.node!!.id}")
                Timber.d("apolloResponse: stories ${allUserStories?.get(0)?.node!!.createdDate}")
            }
        }
    }

    private fun uploadStory() {

        showProgressView()
        lifecycleScope.launchWhenCreated {

            val f = File(mFilePath!!)
            val buildder = DefaultUpload.Builder()
            buildder.contentType("Content-Disposition: form-data;")
            buildder.fileName(f.name)
            val upload = buildder.content(f).build()
            Timber.d("filee ${f.exists()} ${f.length()}")
            val userToken = getCurrentUserToken()!!

            val response = try {
                apolloClient(context = requireContext(), token = userToken).mutation(
                    StoryMutation(file = upload)
                ).execute()
            } catch (e: ApolloException) {
                Timber.d("filee Apollo Exception ${e.message}")
                binding.root.snackbar("ApolloException ${e.message}")
                return@launchWhenCreated
            } catch (e: Exception) {

                Timber.d("filee General Exception ${e.message} $userToken")
                binding.root.snackbar("Exception ${e.message}")
            } finally {
                hideProgressView()
            }
            Timber.d("filee hasError= ${response}")
            getAllUserStories()
        }
    }

    override fun onUserStoryClick(position: Int, userStory: GetAllUserStoriesQuery.Edge?) {

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        //findNavController().navigate(R.id.action_user_story_detail_fragment)
        //requireActivity().openUserStoryDialog(userStory)
        Timber.d("filee ${userStory?.node!!.fileType} ${userStory?.node.file}")
        val url = "${BuildConfig.BASE_URL}media/${userStory.node.file}"
        var userurl = ""
        if (userStory.node!!.user!!.avatar != null && userStory.node.user!!.avatar!!.url != null) {
            userurl = userStory.node.user.avatar!!.url!!
        } else {
            userurl = ""
        }
        val username = userStory.node.user!!.fullName
        val UserID = userId
        val objectId = userStory.node.pk


        var text = userStory.node.createdDate.toString()
        text = text.replace("T", " ").substring(0, text.indexOf("."))
        val momentTime = formatter.parse(text)
        val times = DateUtils.getRelativeTimeSpanString(
            momentTime.time,
            Date().time,
            DateUtils.MINUTE_IN_MILLIS
        )
        if (userStory.node.fileType.equals("video")) {
            val dialog = PlayUserStoryDialogFragment()
            val b = Bundle()

            b.putString("Uid", UserID)
            b.putString("url", url)
            b.putString("userurl", userurl)
            b.putString("username", username)
            b.putString("times", times.toString())
            b.putString("token", userToken)
            b.putInt("objectID", objectId!!)

            dialog.arguments = b
            dialog.show(childFragmentManager, "story")

        } else {
            val dialog = UserStoryDetailFragment(
                object : UserStoryDetailFragment.DeleteCallback {
                    override fun deleteCallback(objectId: Int) {
                        // call api for delete
                        showProgressView()
                        Handler(Looper.getMainLooper()).postDelayed({
                            lifecycleScope.launch {
                                try {
                                    apolloClient(requireContext(), userToken!!)
                                        .mutation(
                                            DeleteStoryMutation(objectId)
                                        ).execute()
                                } catch (e: ApolloException) {
                                    Timber.d("apolloResponse ${e.message}")
                                    binding.root.snackbar("Exception ${e.message}")
                                    hideProgressView()
                                    return@launch
                                }
                            }
                            hideProgressView()
                            getAllUserStories()
                        }, 1000)
                    }
                }
            )

            val b = Bundle()
            b.putString("Uid", UserID)
            b.putString("url", url)
            b.putString("userurl", userurl)
            b.putString("username", username)
            b.putString("times", times.toString())
            b.putString("token", userToken)
            b.putInt("objectID", objectId!!)
            b.putBoolean("showDelete", userId == userStory.node.user.id)
            dialog.arguments = b
            dialog.show(childFragmentManager, "story")

        }
    }

    override fun onAddNewUserStoryClick() {
        val intent = Intent(requireActivity(), ImagePickerActivity::class.java)
        photosLauncher.launch(intent)
        //val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        // videoLauncher.launch(intent)
    }

    override fun onSharedMomentClick(position: Int, item: GetAllUserMomentsQuery.Edge?) {

    }

    override fun onLikeofMomentClick(position: Int, item: GetAllUserMomentsQuery.Edge?) {

        if (userId == item?.node?.user?.id) {

            val bundleArgs = Bundle()
            bundleArgs.putInt("user_id", item?.node?.pk ?: -1)

            // open new screen with user likes
            navController.navigate(R.id.currentUserMomentLikesFragment, bundleArgs)

        } else {
            showProgressView()
            lifecycleScope.launchWhenResumed {
                userName = getCurrentUserName()!!
                userId = getCurrentUserId()!!

                val res = try {
                    apolloClient(
                        requireContext(),
                        userToken!!
                    ).mutation(LikeOnMomentMutation(item?.node!!.pk!!.toString()))
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponse ${e.message}")
                    binding.root.snackbar("Exception ${e.message}")
                    hideProgressView()
                    return@launchWhenResumed
                }
                hideProgressView()
                Timber.d("Size", "" + allUserMoments.size)
                fireLikeNotificationforreceiver(item)
                getParticularMoments(position, item.node.pk.toString())
            }
        }

    }

    private fun getParticularMoments(pos: Int, ids: String) {

        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(
                    GetAllUserMomentsQuery(
                        width,
                        size,
                        1,
                        "",
                        ids
                    )
                )
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception all moments ${e.message}")
                return@launchWhenResumed
            }

            val allmoments = res.data?.allUserMoments!!.edges

            allmoments.indices.forEach { i ->
                if (ids.equals(allmoments[i]!!.node!!.pk.toString())) {
                    if (mViewModel.userMomentsList[pos] != null) {
                        mViewModel.userMomentsList[pos] = allmoments[i]!!
                        sharedMomentAdapter.submitList1(mViewModel.userMomentsList)
                    }

                    //sharedMomentAdapter.notifyItemChanged(pos)
                    return@forEach
                }
            }
        }
    }


    fun fireLikeNotificationforreceiver(item: GetAllUserMomentsQuery.Edge) {

        lifecycleScope.launchWhenResumed {

            val queryName = "sendNotification"
            val query = StringBuilder()
                .append("mutation {")
                .append("$queryName (")
                .append("userId: \"${item.node!!.user!!.id}\", ")
                .append("notificationSetting: \"LIKE\", ")
                .append("data: {momentId:${item.node.pk}}")
                .append(") {")
                .append("sent")
                .append("}")
                .append("}")
                .toString()

            val result = provideGraphqlApi().getResponse<Boolean>(
                query,
                queryName, userToken
            )
            Timber.d("RSLT", "" + result.message)
        }
    }

    fun getUserMomentNextpage(width: Int, size: Int, i: Int, endCursors: String) {
        userToken?.let {
            mViewModel.getAllMoments(it, width, size, i, endCursors) { error ->
                if (error == null) {
                    activity?.runOnUiThread(Runnable {
                        //allUserMoments2.addAll(mViewModel.userMomentsList)
                        //sharedMomentAdapter.setData(mViewModel.userMomentsList)
                        sharedMomentAdapter.submitList1(mViewModel.userMomentsList)

                    })
                    hasNextPage = mViewModel.hasNextPageN
                    endCursor = mViewModel.endCursorN
                } else {

                }
            }
        }
        /* mViewModel.arrayListLiveData.observe(viewLifecycleOwner) { arrayList ->
             allUserMoments2.addAll(arrayList)
             sharedMomentAdapter.setData(allUserMoments2)
         }*/
    }

    fun allusermoments1(width: Int, size: Int, i: Int, endCursors: String) {
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(
                    GetAllUserMomentsQuery(
                        width,
                        size,
                        i,
                        endCursors,
                        ""
                    )
                ).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception all moments ${e.message}")
                return@launchWhenResumed
            }

            val allusermoments = res.data?.allUserMoments!!.edges

            endCursor = res.data?.allUserMoments!!.pageInfo.endCursor!!
            hasNextPage = res.data?.allUserMoments!!.pageInfo.hasNextPage

            if (allusermoments.size != 0) {
                val allUserMomentsNext: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()

                allusermoments.indices.forEach { i ->
                    allUserMomentsNext.add(allusermoments[i]!!)
                }
                sharedMomentAdapter.addAll(allUserMomentsNext)
            }
            if (binding.rvSharedMoments.itemDecorationCount == 0) {
                binding.rvSharedMoments.addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        outRect.top = 15
                    }
                })
            }
            if (allusermoments?.size!! > 0) {
                Timber.d("apolloResponse: ${allusermoments?.get(0)?.node!!.file}")
                Timber.d("apolloResponse: ${allusermoments?.get(0)?.node!!.id}")
                Timber.d("apolloResponse: ${allusermoments?.get(0)?.node!!.createdDate}")
                Timber.d("apolloResponse: ${allusermoments?.get(0)?.node!!.momentDescriptionPaginated}")
                Timber.d("apolloResponse: ${allusermoments?.get(0)?.node!!.user?.fullName}")
            }
        }
    }

    override fun onCommentofMomentClick(

        position: Int, item: GetAllUserMomentsQuery.Edge?
    ) {
        val bundle = Bundle().apply {
            putString("momentID", item?.node!!.pk.toString())
            putInt("itemPosition", position)
            putString("filesUrl", item.node.file!!)
            putString("Likes", item.node.like!!.toString())
            putString("Comments", item.node.comment!!.toString())
            val gson = Gson()
            putString("Desc", gson.toJson(item.node.momentDescriptionPaginated))
            putString("fullnames", item.node.user!!.fullName)
            if (item.node.user.gender != null) {
                putString("gender", item.node.user.gender!!.name)
            } else {
                putString("gender", null)
            }
            putString("momentuserID", item.node.user.id.toString())
        }
        navController.navigate(R.id.momentsAddCommentFragment, bundle)
    }

    override fun onMomentGiftClick(position: Int, item: GetAllUserMomentsQuery.Edge?) {
//        var bundle = Bundle()
//        bundle.putString("userId", userId)
//        navController.navigate(R.id.action_userProfileFragment_to_userGiftsFragment,bundle)

        if (!userId!!.equals(item!!.node!!.user!!.id)) {
            giftUserid = item.node!!.user!!.id.toString()
            binding.sendgiftto.text = "SEND GIFT TO " + item!!.node!!.user!!.fullName
            if (GiftbottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                GiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//            buttonSlideUp.text = "Slide Down";

            } else {
                GiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED;
//            buttonSlideUp.text = "Slide Up"

            }
        } else {
            Toast.makeText(requireContext(), "User can't bought gift yourself", Toast.LENGTH_LONG)
                .show()
        }
    }

    override fun onDotMenuofMomentClick(
        position: Int,
        item: GetAllUserMomentsQuery.Edge?, types: String
    ) {
        if (types.equals("delete")) {

            showProgressView()
            lifecycleScope.launchWhenResumed {
                val res = try {
                    apolloClient(
                        requireContext(),
                        userToken!!
                    ).mutation(DeletemomentMutation(item?.node!!.pk!!.toString()))
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponse ${e.message}")
                    binding.root.snackbar("Exception ${e.message}")
                    hideProgressView()
                    return@launchWhenResumed
                }
                hideProgressView()

                val result = res.data?.deleteMoment?.moment?.pk != 0
                if (result)
                    binding.root.snackbar("Moment Deleted!", duration = Snackbar.LENGTH_SHORT)

                val positionss = allUserMoments.indexOf(item)
                allUserMoments.remove(item)
                sharedMomentAdapter.notifyItemRemoved(position)
            }
        } else if (types.equals("report")) {
            reportDialog(item)
        } else if (types == "edit") {
            val sb = StringBuilder()
            item?.node?.momentDescriptionPaginated?.forEach { sb.append(it) }
            val desc = sb.toString().replace("", "")
            val args = Bundle()
            args.putString("moment_desc", desc)
            args.putInt("moment_pk", item?.node?.pk ?: -1)
            findNavController().navigate(
                destinationId = R.id.userMomentUpdateFragment,
                popUpFragId = null,
                animType = AnimationTypes.SLIDE_ANIM,
                inclusive = true,
                args = args
            )
        }
    }

    private fun reportDialog(item: GetAllUserMomentsQuery.Edge?) {
        val builder = AlertDialog.Builder(activity, R.style.CustomAlertDialog).create()
        val view = layoutInflater.inflate(R.layout.report_dialog, null)
        val edit_text = view.findViewById<EditText>(R.id.report_message)
        val ok_button = view.findViewById<Button>(R.id.ok_button)
        val cancel_button = view.findViewById<Button>(R.id.cancel_button)
        builder.setView(view)
        ok_button.setOnClickListener {
            val message = edit_text.text.toString()
            showProgressView()
            lifecycleScope.launchWhenResumed {
                val res = try {
                    apolloClient(
                        requireContext(),
                        userToken!!
                    ).mutation(ReportonmomentMutation(item?.node!!.pk!!.toString(), message))
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponse ${e.message}")
                    binding.root.snackbar("Exception ${e.message}")
                    hideProgressView()
                    builder.dismiss()
                    return@launchWhenResumed
                }
                hideProgressView()
                builder.dismiss()
            }

        }
        cancel_button.setOnClickListener {
            builder.dismiss()
        }
        builder.setCanceledOnTouchOutside(false)
        builder.show()

    }


    override fun onMoreShareMomentClick() {
    }

    fun getMainActivity() = activity as MainActivity
}