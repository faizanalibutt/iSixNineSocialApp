package com.i69app.ui.screens.main.messenger.chat

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.exception.ApolloException
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.i69app.*
import com.i69app.data.config.Constants
import com.i69app.data.models.ModelGifts
import com.i69app.data.models.User
import com.i69app.databinding.AlertFullImageBinding
import com.i69app.databinding.FragmentNewMessengerChatBinding
import com.i69app.di.modules.AppModule
import com.i69app.firebasenotification.NotificationBroadcast
import com.i69app.gifts.FragmentRealGifts
import com.i69app.gifts.FragmentVirtualGifts
import com.i69app.ui.adapters.NewChatMessagesAdapter
import com.i69app.ui.adapters.UserItemsAdapter
import com.i69app.ui.base.BaseFragment
import com.i69app.ui.screens.ImagePickerActivity
import com.i69app.ui.screens.main.MainActivity.Companion.getMainActivity
import com.i69app.ui.screens.main.search.userProfile.PicViewerFragment
import com.i69app.ui.screens.main.search.userProfile.SearchUserProfileFragment
import com.i69app.ui.viewModels.UserViewModel
import com.i69app.utils.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File


public class MessengerNewChatFragment : BaseFragment<FragmentNewMessengerChatBinding>(),
    NewChatMessagesAdapter.ChatMessageListener {

    private lateinit var adapter: NewChatMessagesAdapter
    private var edges: ArrayList<GetChatMessagesByRoomIdQuery.Edge?> = ArrayList()
    private lateinit var deferred: Deferred<Unit>
    private var userId: String? = null
    private var userToken: String? = null
    private var currentUser: User? = null
    private val viewModel: UserViewModel by activityViewModels()
    var giftUserid: String? = null
    var otherFirstName:String?=null
    var otherUserId:String?=null
    var ChatType: String? = null
    var fragVirtualGifts: FragmentVirtualGifts? = null
    var fragRealGifts: FragmentRealGifts? = null
    private lateinit var GiftbottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private var broadcast: NotificationBroadcast? = null

    var isProgressShow: Boolean = true
    var isMessageSending:Boolean=false

    private val photosLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            val data = activityResult.data
            if (activityResult.resultCode == Activity.RESULT_OK) {
                val result = data?.getStringExtra("result")
                Timber.d("Result $result")
                Log.e("ddd", "" + result)
                if (result != null) {
                    var mediaType: String? = ""
                    if (File(result).getMimeType().toString().startsWith("image")) {
                        mediaType = "image"
                    } else {
                        mediaType = "video"
                    }
                    UploadUtility(this@MessengerNewChatFragment).uploadFile(
                        result,
                        authorization = userToken, upload_type = mediaType
                    ) { url ->
                        Timber.d("responseurll $url")
                        if (url.equals("url")) {

                            binding.root.snackbar(
                                "Not enough coins..",
                                Snackbar.LENGTH_INDEFINITE,
                                callback = {

                                    findNavController().navigate(
                                        destinationId = R.id.actionGoToPurchaseFragment,
                                        popUpFragId = null,
                                        animType = AnimationTypes.SLIDE_ANIM,
                                        inclusive = true,

                                        )
                                })
                        } else {

                            var input = url
                            if (url?.startsWith("/media/chat_files/") == true) {
                                input = "${BuildConfig.BASE_URL}$url"
                            }
                            sendMessageToServer(input)
                        }
                    }
                }
            }
        }


    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNewMessengerChatBinding =
        FragmentNewMessengerChatBinding.inflate(inflater, container, false)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        /*  activity?.window?.decorView?.setOnApplyWindowInsetsListener { view, insets ->
              val insetsCompat = toWindowInsetsCompat(insets, view)
              binding.rvChatMessages.layoutManager?.scrollToPosition(0)
              getMainActivity()?.binding?.bottomNavigation?.isGone = insetsCompat.isVisible(ime())
              view.onApplyWindowInsets(insets)
          }*/
        //broadcast = NotificationBroadcast(this);
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun setupTheme() {

        lifecycleScope.launch {
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!
            Timber.i("usertokenn $userToken")
        }

        /* getTypeActivity<MainActivity>()?.setSupportActionBar(binding.toolbar)
         val supportActionBar = getTypeActivity<MainActivity>()?.supportActionBar
         supportActionBar?.setDisplayHomeAsUpEnabled(false)
         supportActionBar?.setDisplayShowHomeEnabled(false)
         supportActionBar?.title = ""*/
        //currentUser = null
        setHasOptionsMenu(true)
        initInputListener()
        initObservers()
        setupData(true)
        lifecycleScope.launch {
            viewModel.newMessageFlow.collect { message ->
                val avatarPhotos =
                    edges?.find { it?.node?.userId?.id == message?.userId?.id }?.node?.userId?.avatarPhotos
                message?.let { message ->
                    edges?.add(
                        0, GetChatMessagesByRoomIdQuery.Edge(
                            GetChatMessagesByRoomIdQuery.Node(
                                id = message.id,
                                content = message.content,
                                roomId = GetChatMessagesByRoomIdQuery.RoomId(
                                    id = message.roomId.id,
                                    name = message.roomId.name
                                ),
                                timestamp = message.timestamp,
                                userId = GetChatMessagesByRoomIdQuery.UserId(
                                    id = message.userId.id,
                                    username = message.userId.username,
                                    avatarIndex = message.userId.avatarIndex,
                                    avatarPhotos = avatarPhotos
                                ),
                            )
                        )
                    )
                    //notifyOnlyNew(edges = edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?)
                    val chatId = arguments?.getInt("chatId", 0)!!
                    if (message.roomId.id.toInt()==chatId)
                        notifyAdapter(edges2 = edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?)
                }
            }
        }
    }

    private fun initObservers() {
        viewModel.currentUserLiveData.observe(viewLifecycleOwner) { user ->
            Timber.d("User $user")
            user?.let {
                currentUser = it
                val coinsTextResource =
                    if (currentUser!!.purchaseCoins == 0) {

                        binding.coinsCounter.text = currentUser!!.giftCoins.toString()
                        if (currentUser!!.giftCoins == 0 || currentUser!!.giftCoins == 1)
                            R.string.coin_left
                        else
                            R.string.coins_left
                    } else {
                        binding.coinsCounter.text =
                            currentUser!!.purchaseCoins.toString()
                        if (currentUser!!.purchaseCoins == 0 || currentUser!!.purchaseCoins == 1)
                            R.string.coin_left
                        else
                            R.string.coins_left
                    }
                binding.coinsLeftTv.text = getString(coinsTextResource)
            }
        }
    }

    override fun setupClickListeners() {

    }

    private fun getCurrentUserDetails() {
        viewModel.getCurrentUserUpdate(userId!!, token = userToken!!, true)
    }

    fun setupData(isProgressShow: Boolean) {
        this.isProgressShow = isProgressShow
        ChatType = arguments?.getString("ChatType")
        if (ChatType.equals("001")) {
            getFirstMessages()
            binding.inputLayout.setVisibility(View.GONE)
            binding.userName.text = requireArguments().getString("otherUserName")
            otherFirstName=requireArguments().getString("otherUserName")
            binding.userProfileImg.loadCircleImage(R.drawable.logo)
            binding.actionReportMes.visibility=View.GONE
        } else if (ChatType.equals("000")) {
            getBrodcastMessages()
            binding.inputLayout.setVisibility(View.GONE)
            binding.userName.text = requireArguments().getString("otherUserName")
            otherFirstName=requireArguments().getString("otherUserName")
            binding.actionReportMes.visibility=View.GONE
            binding.userProfileImg.loadCircleImage(R.drawable.logo)

        } else /*if(ChatType.equals("Normal"))*/ {
            updateCoinView("")
            initInputListener()
            binding.inputLayout.setVisibility(View.VISIBLE)
            giftUserid = arguments?.getString("otherUserId")
            otherUserId=arguments?.getString("otherUserId")
            otherFirstName=requireArguments().getString("otherUserName")
            binding.userName.text = requireArguments().getString("otherUserName")
            binding.sendgiftto.text =
                "SEND GIFT TO " + requireArguments().getString("otherUserName")

            val url = requireArguments().getString("otherUserPhoto")

            binding.userProfileImg.loadCircleImage(url!!)
            binding.userProfileImgContainer.setOnClickListener { navigateToOtherUserProfile() }

            isOtherUserOnline()

            binding.userProfileImg.setOnClickListener(View.OnClickListener {

                gotoChatUserProfile()
            })

            binding.userName.setOnClickListener(View.OnClickListener {
                gotoChatUserProfile()
            })
            val gender = requireArguments().getInt("otherUserGender", 0)
            binding.input.updateGiftIcon(gender)


        }

        binding.closeBtn.setOnClickListener(View.OnClickListener {
            moveUp()
        })
        binding.actionReportMes.setOnClickListener {
            openMenuItem()
        }



    }
    private fun openMenuItem(){
        val popup = PopupMenu(requireContext(), binding.actionReportMes,10,R.attr.popupMenuStyle,R.style.PopupMenu2)
        popup.getMenuInflater().inflate(R.menu.search_profile_options, popup.getMenu());


        //adding click listener
        popup.setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener { item: MenuItem? ->

            when (item!!.itemId) {

                R.id.nav_item_report -> {
                    reportDialog()
                }
                R.id.nav_item_block -> {
                    blockUserAlert()
                }
            }

            true
        })
        popup.show()
    }
    private fun blockUserAlert(){
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("Are you sure you want to block $otherFirstName ?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                blockAccount()

            }
            .setNegativeButton("No") { dialog, id ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
        alert.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLUE);
        alert.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLUE);
    }
    private fun reportDialog(){
        val builder = AlertDialog.Builder(activity,R.style.CustomAlertDialog).create()
        val view = layoutInflater.inflate(R.layout.report_dialog,null)
        val  edit_text = view.findViewById<EditText>(R.id.report_message)
        val  ok_button = view.findViewById<Button>(R.id.ok_button)
        val  cancel_button = view.findViewById<Button>(R.id.cancel_button)
        builder.setView(view)
        ok_button.setOnClickListener {
            val message=edit_text.text.toString()
            reportAccount(otherUserId,message)
            builder.dismiss()

        }
        cancel_button.setOnClickListener {
            builder.dismiss()
        }
        builder.setCanceledOnTouchOutside(false)
        builder.show()

    }
    private fun blockAccount() {
        lifecycleScope.launch(Dispatchers.Main) {
            when (val response = viewModel.blockUser(userId, otherUserId, token = userToken)) {
                is Resource.Success -> {
                    viewModel.getCurrentUser(userId!!, userToken!!, true)
                    hideProgressView()
                    binding.root.snackbar("${otherFirstName} blocked!")
                    getMainActivity()?.pref?.edit()?.putString("chatListRefresh","true")?.putString("readCount","false")?.apply()
                    findNavController().popBackStack()
                }
                is Resource.Error -> {
                    hideProgressView()
                    Timber.e("${getString(R.string.something_went_wrong)} ${response.message}")
                    binding.root.snackbar("${getString(R.string.something_went_wrong)} ${response.message}")
                }
            }
        }
    }

    private fun reportAccount(otherUserId: String?,reasonMsg: String?) {
        lifecycleScope.launch(Dispatchers.Main) {
            reportUserAccount(token = userToken, currentUserId = userId, otherUserId = otherUserId, reasonMsg = reasonMsg ,mViewModel = viewModel) { message ->
                hideProgressView()
                binding.root.snackbar(message)
            }
        }
    }

    private fun setupOtherUserData() {

        giftUserid = arguments?.getString("otherUserId")

        binding.userName.text = requireArguments().getString("otherUserName")
        binding.sendgiftto.text = "SEND GIFT TO " + requireArguments().getString("otherUserName")

        val url = requireArguments().getString("otherUserPhoto")
        binding.userProfileImg.loadCircleImage(url!!)
        binding.userProfileImgContainer.setOnClickListener { navigateToOtherUserProfile() }
        binding.closeBtn.setOnClickListener(View.OnClickListener {
            moveUp()
        })
        isOtherUserOnline()

        binding.userProfileImg.setOnClickListener(View.OnClickListener {

            gotoChatUserProfile()
        })

        binding.userName.setOnClickListener(View.OnClickListener {
            gotoChatUserProfile()
        })
        val gender = requireArguments().getInt("otherUserGender", 0)
        binding.input.updateGiftIcon(gender)
    }

    private fun navigateToOtherUserProfile() {

    }

    fun gotoChatUserProfile() {
        val bundle = Bundle()
        bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
        bundle.putString("userId", giftUserid)

        findNavController().navigate(
            destinationId = R.id.action_global_otherUserProfileFragment,
            popUpFragId = null,
            animType = AnimationTypes.SLIDE_ANIM,
            inclusive = true,
            args = bundle
        )
    }

    private fun initInputListener() {
        Timber.d("check input string ${arguments?.getInt("chatId")} ${arguments?.getString("otherUserId")}")
        //sendMessageToServer("hardcode text message")
        binding.input.setInputListener { input ->
            binding.input.inputEditText?.hideKeyboard()
            if (!isMessageSending)
                sendMessageToServer(input.toString())
            Timber.d(
                "check input string $input ${arguments?.getInt("chatId")} ${
                    arguments?.getString(
                        "otherUserId"
                    )
                }"
            )
            return@setInputListener false
        }
        binding.input.setAttachmentsListener {
            val intent = Intent(requireActivity(), ImagePickerActivity::class.java)
            intent.putExtra("video_duration_limit", 180)
            photosLauncher.launch(intent)
        }
        binding.input.setGiftButtonListener {


            if (GiftbottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                GiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//            buttonSlideUp.text = "Slide Down";


            } else {
                GiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
//            buttonSlideUp.text = "Slide Up"


            }


//            if (arguments?.getString("otherUserId")?.isEmpty() == true) return@setGiftButtonListener
//
//            val bundle = Bundle()
//            bundle.putString("userId", arguments?.getString-("otherUserId"))
//            Handler(Looper.getMainLooper()).postDelayed({
//                findNavController().navigate(destinationId = R.id.action_to_userGiftsFragment,
//                    popUpFragId = null,
//                    animType = AnimationTypes.SLIDE_ANIM,
//                    inclusive = false,
//                    args = bundle)
//            }, 200)
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
            isProgressShow = true
            val items: MutableList<ModelGifts.Data.AllRealGift> = mutableListOf()
            fragVirtualGifts?.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }
            fragRealGifts?.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }
            lifecycleScope.launchWhenCreated {
                if (items.size > 0) {
                    if (isProgressShow) {
                        showProgressView()
                    }
                    items.forEach { gift ->
                        Log.e("gift.id", "-->" + gift.id)
                        Log.e("giftUserid", "-->" + giftUserid)
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
//                                views.snackbar("Exception ${e.message}")
                            //hideProgressView()
                            //return@launchWhenResumed
                        }

                        Log.e("res", "-->" + Gson().toJson(res))
                        if (res?.hasErrors() == false) {
//                                views.snackbar("You bought ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} successfully!")
                            Toast.makeText(
                                requireContext(),
                                "You bought ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} successfully!",
                                Toast.LENGTH_LONG
                            ).show()
                            updateCoinView("")

                            //     fireGiftBuyNotificationforreceiver(gift.id, giftUserid)

                        }
                        if (res!!.hasErrors()) {
//                                views.snackbar(""+ res.errors!![0].message)
                            Toast.makeText(
                                requireContext(),
                                "" + res.errors!![0].message,
                                Toast.LENGTH_LONG
                            ).show()

                        }
                        Timber.d("apolloResponse ${res.hasErrors()} ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName}")
                    }
                    getCurrentUserDetails()
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

    private fun setupViewPager(viewPager: ViewPager) {
        val adapter = UserItemsAdapter(childFragmentManager)
        fragRealGifts = FragmentRealGifts()
        fragVirtualGifts = FragmentVirtualGifts()

        adapter.addFragItem(fragRealGifts!!, getString(R.string.real_gifts))
        adapter.addFragItem(fragVirtualGifts!!, getString(R.string.virtual_gifts))
        viewPager.adapter = adapter
    }

    fun fireGiftBuyNotificationforreceiver(gid: String, userid: String?) {

        lifecycleScope.launchWhenResumed {


            val queryName = "sendNotification"
            val query = StringBuilder()
                .append("mutation {")
                .append("$queryName (")
                .append("userId: \"${userid}\", ")
                .append("notificationSetting: \"GIFT\", ")
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
    private fun  notifyOnlyNew(edges: ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?) {
        adapter.updateList(edges)
    }

    private fun  notifyAdapter(edges2: ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?) {
        if (binding.rvChatMessages.adapter == null) {
            adapter = NewChatMessagesAdapter(
                requireActivity(),
                userId,
                this@MessengerNewChatFragment,
                //edges
            )
            (binding.rvChatMessages.layoutManager as LinearLayoutManager).apply {
                reverseLayout = true
                stackFromEnd = true
                binding.rvChatMessages.layoutManager = this
            }
        }
        binding.rvChatMessages.adapter = adapter
        adapter.updateList(edges2)
        if (adapter.itemCount > 0) {
            binding.rvChatMessages.layoutManager?.scrollToPosition(0)
        }
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                binding.rvChatMessages.layoutManager?.scrollToPosition(0)
                adapter.unregisterAdapterDataObserver(this)
            }
        })
    }

    private fun isOtherUserOnline() {
        if (isProgressShow) {
            //howProgressView()
        }
        lifecycleScope.launch {
            try {
                val id = requireArguments().getString("otherUserId")
                val res =  apolloClient(requireContext(), userToken!!).query(IsOnlineQuery(id!!)).execute()
                if (!res.hasErrors()) {
                    binding.otherUserOnlineStatus.visibility =
                        if (res.data?.isOnline?.isOnline == true) View.VISIBLE else View.GONE
                    Timber.d("apolloResponse isOnline ${res.data?.isOnline?.isOnline}")
                }
            } catch (e: ApolloException) {
                Timber.d("apolloResponse isOnline ${e.message}")
            }
            //hideProgressView()
        }
    }

    private fun getFirstMessages() {
        //edges = mutableListOf()
        if (isProgressShow) {
            //showProgressView()
        }
        lifecycleScope.launch {
            try {
                val res = apolloClient(requireContext(), userToken!!).query(
                    GetFirstMessageListQuery()
                ).execute()
                val datas = res.data!!.firstmessageMsgs!!.edges

                datas.forEach { Edge ->

                    val msg = GetChatMessagesByRoomIdQuery.Edge(
                        GetChatMessagesByRoomIdQuery.Node(
                            id = Edge!!.node!!.byUserId.id!!,
                            content = Edge.node!!.content,
                            roomId = GetChatMessagesByRoomIdQuery.RoomId(
                                id = "",
                                name = ""
                            ),
                            timestamp = Edge.node.timestamp,
                            userId = GetChatMessagesByRoomIdQuery.UserId(
                                id = Edge.node.byUserId.id,
                                username = Edge.node.byUserId.username,
                                avatarIndex = Edge.node.byUserId.avatarIndex,
                                null
                            ),
                        )
                    )
                    edges!!.add(msg)
                }

                /*edges?.forEach {
                    Timber.d("apolloResponse getChatMessages ${it?.node?.text}")
                }*/
                if (!res.hasErrors()) {
                    Timber.d("apolloResponse success ${edges?.size}")
                    notifyAdapter(edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?)
                } else {
                    Timber.d("apolloResponse error ${res.errors?.get(0)?.message}")
                }
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                //binding.root.snackbar("Exception all moments $GetAllMomentsQuery")
                return@launch
            }
            //hideProgressView()
        }
    }

    private fun getBrodcastMessages() {
        // edges = mutableListOf()
        if (isProgressShow) {
            //showProgressView()
        }
        lifecycleScope.launch {
            try {
                val res = apolloClient(requireContext(), userToken!!).query(
                    GetBroadcastMessageListQuery()
                ).execute()

                val datas = res.data!!.broadcastMsgs!!.edges

                datas.forEach { Edge ->

                    val msg = GetChatMessagesByRoomIdQuery.Edge(
                        GetChatMessagesByRoomIdQuery.Node(
                            id = Edge!!.node!!.byUserId.id!!,
                            content = Edge.node!!.content,
                            roomId = GetChatMessagesByRoomIdQuery.RoomId(
                                id = "",
                                name = ""
                            ),
                            timestamp = Edge.node.timestamp,
                            userId = GetChatMessagesByRoomIdQuery.UserId(
                                id = Edge.node.byUserId.id,
                                username = Edge.node.byUserId.username,
                                avatarIndex = Edge.node.byUserId.avatarIndex,
                                null
                            ),
                        )
                    )
                    edges!!.add(msg)
                }

                /*edges?.forEach {
                    Timber.d("apolloResponse getChatMessages ${it?.node?.text}")
                }*/

                if (!res.hasErrors()) {
                    Timber.d("apolloResponse success ${edges?.size}")
                    notifyAdapter(edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?)
                } else {
                    Timber.d("apolloResponse error ${res.errors?.get(0)?.message}")
                }
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                //binding.root.snackbar("Exception all moments $GetAllMomentsQuery")
                return@launch
            }
            //hideProgressView()
        }
    }

    private fun getChatMessages() {
//        getCurrentUserDetails()
        // if (isProgressShow) {
        //showProgressView()
        //}
        val activity: Activity? = activity
        if (activity!=null){
            lifecycleScope.launch {
                try {
                    val roomId = arguments?.getInt("chatId")

                    Timber.d("apolloResponse roomId $roomId")
                    val res = apolloClient(requireActivity(), userToken!!).query(
                        GetChatMessagesByRoomIdQuery(
                            roomId.toString(),
                            99
                        )
                    ).execute()

                    edges = res.data?.messages?.edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>

                    /*edges?.forEach {
                       Timber.d("apolloResponse getChatMessages ${it?.node?.text}")
                       }*/

                    if (!res.hasErrors()) {
                        Timber.d("apolloResponse success ${edges?.size}")
                        notifyAdapter(edges as ArrayList<GetChatMessagesByRoomIdQuery.Edge?>?)
                    } else {
                        Timber.d("apolloResponse error ${res.errors?.get(0)?.message}")
                    }

                } catch (e: ApolloException) {
                    Timber.d("apolloResponse ${e.message}")
                    //binding.root.snackbar("Exception all moments $GetAllMomentsQuery")
                    return@launch
                }
                //hideProgressView()
            }
        }


    }

    private fun sendMessageToServer(input: String?) {
        isProgressShow = true
        isMessageSending=true
        Timber.d("apolloResponse 1 $input")
        lifecycleScope.launch {
            send(input!!)
        }
        Timber.d("apolloResponse 8")
    }

    suspend fun send(input: String) {
        val chatId = arguments?.getInt("chatId", 0)!!
        Timber.d("apolloResponse 3 c $chatId and crrent id $userId")

        var res: ApolloResponse<SendChatMessageMutation.Data>? = null
        try {
            res = apolloClient(requireActivity(), userToken!!).mutation(
                SendChatMessageMutation(
                    input,
                    chatId
                )
            ).execute()
            Timber.d("apolloResponse 1111 c ${res.hasErrors()} ${res.data?.sendMessage?.message}")
        } catch (e: ApolloException) {
            isMessageSending=false
            Timber.d("apolloResponse ${e.message}")
        } catch (ex: Exception) {
            isMessageSending=false
            Timber.d("General exception ${ex.message}")
        }

        //hideProgressView()
        Log.e("ddd", Gson().toJson(res))
        if (res?.hasErrors() == false) {
            updateCoinView("")
            isMessageSending=false
            Timber.d("apolloResponse ${res.hasErrors()} ${res.data?.sendMessage?.message}")
        } else {

            if (res!!.errors!!.get(0).message.contains("Not enough coins")) {

                binding.root.snackbar(
                    res!!.errors!!.get(0).message,
                    Snackbar.LENGTH_INDEFINITE,
                    callback = {

                        findNavController().navigate(
                            destinationId = R.id.actionGoToPurchaseFragment,
                            popUpFragId = null,
                            animType = AnimationTypes.SLIDE_ANIM,
                            inclusive = true,

                            )
                    })
            } else {

                Toast.makeText(
                    requireContext(),
                    "" + res!!.errors!!.get(0).message,
                    Toast.LENGTH_SHORT
                )
                    .show()
            }

        }
        binding.input.inputEditText.text = null
        isMessageSending=false
    }

    //flow.collect {
    //Timber.d("reealltime ${it.data?.chatSubscription?.message?.text}")
    //val adapter = binding.rvChatMessages.adapter as NewChatMessagesAdapter
    //adapter.addMessage(it.data?.chatSubscription?.message)
    //}

    private suspend fun cancelChatRoom() {
        Timber.d("reealltime detached 3")
        deferred.cancel()
        Timber.d("reealltime detached 4")
        try {
            val result = deferred.await()
            Timber.d("reealltime detached 5")
        } catch (e: CancellationException) {
            Timber.d("reealltime cancel room exception ${e.message}")
            // handle CancellationException if need
        } finally {
            // make some cleanup if need
            Timber.d("reealltime detached 6")
        }
    }

    override fun onChatMessageClick(position: Int, message: GetChatMessagesByRoomIdQuery.Edge?) {

        val url = message?.node?.content
        if (url?.contains("media/chat_files") == true) {
            var fullUrl = url
            if (url.startsWith("/media/chat_files/")) {
                fullUrl = "${BuildConfig.BASE_URL}$url"
            }
            val uri = Uri.parse(fullUrl)
            val lastSegment = uri.lastPathSegment
            val ext = lastSegment?.substring(lastSegment.lastIndexOf(".") + 1)
            Timber.d("clickk $lastSegment $ext")
            if (ext.contentEquals("jpg") || ext.contentEquals("png") || ext.contentEquals("jpeg")) {
//                val w = resources.displayMetrics.widthPixels
//                val h = resources.displayMetrics.heightPixels
//                showImageDialog(w, h, fullUrl)
                val dialog = PicViewerFragment()
                val b = Bundle()
                b.putString("url", fullUrl)
                b.putString("mediatype", "image")

                dialog.arguments = b
                dialog.show(childFragmentManager, "ChatpicViewer")
            } else {
                val dialog = PicViewerFragment()
                val b = Bundle()
                b.putString("url", fullUrl)
                b.putString("mediatype", "video")

                dialog.arguments = b
                dialog.show(childFragmentManager, "ChatvideoViewer")
//                val downloadedFile = File(
//                    requireActivity().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
//                    lastSegment!!
//                )
//                if (downloadedFile.exists()) {
//                    downloadedFile.openFile(requireActivity())
//                } else {
//                    DownloadUtil(this@MessengerNewChatFragment).downloadFile(
//                        fullUrl,
//                        lastSegment
//                    ) { downloadedFile ->
//                        Timber.d("downnfile ${downloadedFile?.exists()} ${downloadedFile?.absolutePath}")
//                        if (downloadedFile?.exists() == true) {
//                            downloadedFile.openFile(requireActivity())
//                        }
//                    }
//                }
            }
        }
    }

    override fun onChatUserAvtarClick() {
        gotoChatUserProfile()
    }

    private fun showImageDialog(width: Int, height: Int, imageUrl: String?) {
        val dialog = Dialog(requireContext())
        val dialogBinding = AlertFullImageBinding.inflate(layoutInflater, null, false)
        dialogBinding.fullImg.loadImage(imageUrl!!, {
            dialogBinding.alertTitle.setViewGone()
        }, {
            dialogBinding.alertTitle.text = it?.message
        })
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setLayout(width, height)
        dialog.show()
        dialogBinding.root.setOnClickListener {
            dialog.cancel()
        }
    }

    override fun onResume() {
        super.onResume()
        val intentFilter = IntentFilter()
        intentFilter.addAction("gift_Received")
//        intentFilter.addAction("sent_message")
        activity?.registerReceiver(broadCastReceiver, intentFilter)
        /*LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            broadcast!!, IntentFilter(Constants.INTENTACTION)
        );*/
    }

    private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt : Context?, intent : Intent?) {
            if (intent!!.action.equals("gift_Received")){
                val extras = intent?.extras
                val state = extras!!.getString("extra")
                updateCoinView(state.toString())
            }
//            if (intent.action.equals("sent_message")){
//                Log.e("Sent_message", "onReceive: " )
//                getChatMessages()
//            }

        }
    }
    fun updateCoinView(toString : String) {
        getCurrentUserDetails()
        getChatMessages()
    }
    override fun onPause() {
        super.onPause()
        Constants.hideKeyboard(requireActivity())
        // LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(broadcast!!);
    }

}