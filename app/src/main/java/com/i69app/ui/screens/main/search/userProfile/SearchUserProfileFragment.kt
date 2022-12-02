package com.i69app.ui.screens.main.search.userProfile


import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewpager.widget.ViewPager
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.exception.ApolloException
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayout
import com.google.gson.Gson
import com.i69app.*
import com.i69app.data.models.ModelGifts
import com.i69app.data.models.Photo
import com.i69app.data.models.User
import com.i69app.databinding.AlertFullImageBinding
import com.i69app.databinding.FragmentUserProfileBinding
import com.i69app.di.modules.AppModule
import com.i69app.gifts.FragmentRealGifts
import com.i69app.gifts.FragmentVirtualGifts
import com.i69app.profile.vm.VMProfile
import com.i69app.ui.adapters.UserItemsAdapter
import com.i69app.ui.base.BaseFragment
import com.i69app.ui.screens.SplashActivity
import com.i69app.ui.screens.main.MainActivity
import com.i69app.ui.screens.main.MainActivity.Companion.getMainActivity
import com.i69app.ui.viewModels.UserViewModel
import com.i69app.utils.*
import com.synnapps.carouselview.ViewListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber


@AndroidEntryPoint
class SearchUserProfileFragment : BaseFragment<FragmentUserProfileBinding>() {
    companion object {
        const val ARGS_FROM_CHAT = "from_chat"
    }
    private val viewModel: VMProfile by activityViewModels()
    private val mViewModel: UserViewModel by activityViewModels()

    private var fromChat: Boolean = false
    private var otherUserId: String? = ""
    private var otherUserName: String? = ""
    private var otherFirstName: String? = ""

    private var chatBundle = Bundle()

    private lateinit var GiftbottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    var fragVirtualGifts: FragmentVirtualGifts? = null
    var fragRealGifts: FragmentRealGifts? = null
    private var userToken: String? = null
    private var LuserId: String? = null

    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentUserProfileBinding.inflate(inflater, container, false).apply {
            viewModel.isMyUser = false
            this.vm = viewModel
        }

    private val addSliderImageIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            Timber.d("RESULT" + result)
        }


    override fun setupTheme() {

        lifecycleScope.launch {
            userToken = getCurrentUserToken()!!
            LuserId = getCurrentUserId()!!
            Timber.i("usertokenn $userToken")
        }

        fromChat = requireArguments().getBoolean(ARGS_FROM_CHAT)
        otherUserId = requireArguments().getString("userId")
        chatBundle.putString("otherUserId", otherUserId)
        chatBundle.putString("otherUserPhoto", "")

        viewModel.getProfile(otherUserId)


        viewModel.data.observe(this) { data ->
            Timber.d("observe: $data")

            if(data==null)
            {
                lifecycleScope.launch(Dispatchers.Main) {
                    userPreferences.clear()
                    //App.userPreferences.saveUserIdToken("","","")
                    val intent = Intent(activity, SplashActivity::class.java)
                    startActivity(intent)
                    activity!!.finishAffinity()
                }
            }

            if (data != null) {
                if (data.user != null) {
                    otherUserName = data.user?.username
                    otherFirstName = data.user?.fullName
                    binding.sendgiftto.text = "SEND GIFT TO " + otherFirstName!!
                    if (data.user!!.avatarPhotos != null && data.user!!.avatarPhotos!!.size != 0) {
                        binding.userImgHeader.setIndicatorVisibility(View.VISIBLE)


                        try {

                            binding.userImgHeader.setViewListener(ViewListener {

                                var view: View = layoutInflater.inflate(
                                    com.i69app.R.layout.custom_imageview,
                                    null
                                )

                                var pos = it
                                var imageView = view.findViewById<ImageView>(com.i69app.R.id.userIv)

                                if (it <= data.user!!.avatarPhotos!!.size) {
                                    data.user?.avatarPhotos?.get(it)?.let {
                                        imageView.loadImage(
                                            it.url.replace(
                                                "${BuildConfig.BASE_URL_REP}media/",
                                                "${BuildConfig.BASE_URL}media/"
                                            )
                                        )
                                    }
                                }

                                imageView.setOnClickListener {
                                    if (data.user!!.avatarPhotos!! != null && data.user!!.avatarPhotos!!.size != 0) {

                                        val dataarray: ArrayList<Photo> = ArrayList()
                                        data.user!!.avatarPhotos!!.indices.forEach { i ->

                                            val photo_ = data.user!!.avatarPhotos!![i]
                                            dataarray.add(photo_)
                                        }
                                        addSliderImageIntent.launch(
                                            getimageSliderIntent(
                                                requireActivity(),
                                                Gson().toJson(dataarray),
                                                pos
                                            )
                                        )
//
//                                        data.user?.avatarPhotos?.get(position)?.let { it1 ->
//                                            showImageDialog(
//                                                it1.url.replace(
//                                                    "http://95.216.208.1:8000/media/",
//                                                    "${BuildConfig.BASE_URL}media/"
//                                                )
//                                            )
//                                        }
                                    }
                                }


                                view
                            })


                            /*        binding.userImgHeader.setImageListener { position, imageView ->

                                        if(position<= data.user!!.avatarPhotos!!.size)
                                        {
                                            data.user?.avatarPhotos?.get(position)?.let {
                                                imageView.loadImage(it.url.replace("http://95.216.208.1:8000/media/","${BuildConfig.BASE_URL}media/"))
                                            }
                                        }
                                        imageView.setOnClickListener {
                                            if(data.user!!.avatarPhotos!! != null && data.user!!.avatarPhotos!!.size != 0) {

                                                val dataarray: ArrayList<Photo> = ArrayList()
                                                data.user!!.avatarPhotos!!.indices.forEach { i ->

                                                    val photo_ = data.user!!.avatarPhotos!![i]
                                                    dataarray.add(photo_)
                                                }


                                                addSliderImageIntent.launch(
                                                    getimageSliderIntent(requireActivity(), Gson().toJson(dataarray),position))


        //
        //                                        data.user?.avatarPhotos?.get(position)?.let { it1 ->
        //                                            showImageDialog(
        //                                                it1.url.replace(
        //                                                    "http://95.216.208.1:8000/media/",
        //                                                    "${BuildConfig.BASE_URL}media/"
        //                                                )
        //                                            )
        //                                        }
                                            }
                                        }

                                    }*/
                            binding.userImgHeader.pageCount = data.user!!.avatarPhotos!!.size

                        } catch (e: Exception) {
                        }
                    } else {

                        for (f in 0 until binding.userImgHeader.pageCount) {


                            binding.userImgHeader.removeViewAt(f)
                            binding.userImgHeader.setIndicatorVisibility(View.GONE)


                        }

                    }
                    chatBundle.putString("otherUserName", data.user?.fullName)
                    chatBundle.putInt("otherUserGender", data.user?.gender!!)
                }
            }


            if (data != null) {
                if (data.user != null) {
                    if (data.user!!.avatarPhotos != null) {

                        if (data.user?.avatarPhotos!!.size != 0) {
                            if ((data.user?.avatarPhotos?.size!! > 0) && (data.user?.avatarIndex!! < data.user!!.avatarPhotos!!.size)) {
                                chatBundle.putString(
                                    "otherUserPhoto", data?.user!!.avatarPhotos!!.get(
                                        data?.user!!.avatarIndex!!
                                    ).url.replace(
                                        "http://95.216.208.1:8000/media/",
                                        "${BuildConfig.BASE_URL}media/"
                                    )
                                )

                                binding.userProfileImg.loadCircleImage(
                                    data?.user!!.avatarPhotos!!.get(
                                        data?.user!!.avatarIndex!!
                                    ).url.replace(
                                        "http://95.216.208.1:8000/media/",
                                        "${BuildConfig.BASE_URL}media/"
                                    )
                                )
                            }
                        }

                    }
                }
            }
//            binding.userImgHeader.pageCount = data?.user?.avatarPhotos?.size ?: 1
            binding.profileTabs.setupWithViewPager(binding.userDataViewPager)
            binding.userDataViewPager.adapter =
                viewModel.setupViewPager(childFragmentManager, data?.user, data?.defaultPicker)
           /* if (!checkUserIsAlreadyInChat()) {
                binding.initChatMsgBtn.visibility = View.VISIBLE
            } else binding.initChatMsgBtn.visibility = View.GONE*/
        }
        binding.actionGifts1.visibility = View.GONE
//        binding.actionGifts.visibility = View.VISIBLE
        binding.actionCoins.visibility = View.GONE

        adjustScreen()

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
            fragVirtualGifts?.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }
            fragRealGifts?.giftsAdapter?.getSelected()?.let { it1 -> items.addAll(it1) }

            lifecycleScope.launchWhenCreated() {
                if (items.size > 0) {
                    showProgressView()
                    items.forEach { gift ->
                    Log.e("gift.id",gift.id)
                    Log.e("otherUserId","--> "+otherUserId)
                        var res: ApolloResponse<GiftPurchaseMutation.Data>? = null
                        try {
                            res = apolloClient(
                                requireContext(),
                                userToken!!
                            ).mutation(GiftPurchaseMutation(gift.id, otherUserId!!)).execute()
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

                        Log.e("res","--> "+Gson().toJson(res))
                        if (res?.hasErrors() == false) {
//                                views.snackbar("You bought ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} successfully!")
                            Toast.makeText(
                                requireContext(),
                                "You bought ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} successfully!",
                                Toast.LENGTH_LONG
                            ).show()

                        //    fireGiftBuyNotificationforreceiver(gift.id, otherUserId!!)

                        }
                        if (res!!.hasErrors()) {
                            Log.e("resc","--> "+Gson().toJson(res))
//                                views.snackbar(""+ res.errors!![0].message)
                            Toast.makeText(
                                requireContext(),
                                "" + res.errors!![0].message,
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("resd","--> "+Gson().toJson(res))
                        }
                        if(res.hasErrors())
                        {
                            try {
                                Log.e("rese","--> "+JSONObject(res.errors!!.get(0).toString()).getString("code"))
                                if(JSONObject(res.errors!!.get(0).toString()).getString("code")!=null)
                                {   Log.e("resf","--> "+Gson().toJson(res))
                                    if(JSONObject(res.errors!!.get(0).toString()).getString("code").equals("InvalidOrExpiredToken"))
                                    {   Log.e("resg","--> "+Gson().toJson(res))
                                        // error("User doesn't exist")

                                        lifecycleScope.launch(Dispatchers.Main) {
                                            userPreferences.clear()
                                            //App.userPreferences.saveUserIdToken("","","")
                                            val intent = Intent(activity, SplashActivity::class.java)
                                            startActivity(intent)
                                            activity!!.finishAffinity()
                                        }             }          }
                            }
                            catch (e:Exception)
                            {
                                Log.e("resez","--> "+e)
                            }

                        }
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
    private fun blockAccount() {
        lifecycleScope.launch(Dispatchers.Main) {
            when (val response = mViewModel.blockUser(LuserId, otherUserId, token = userToken)) {
                is Resource.Success -> {
                    mViewModel.getCurrentUser(LuserId!!, userToken!!, true)
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
            reportUserAccount(token = userToken, currentUserId = LuserId, otherUserId = otherUserId, reasonMsg = reasonMsg ,mViewModel = mViewModel) { message ->
                hideProgressView()
                binding.root.snackbar(message)
            }
        }
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

    override fun onDetach() {
        super.onDetach()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun showImageDialog(imageUrl: String) {
        val dialog = Dialog(
            requireContext(),
            android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen
        )
        val dialogBinding = AlertFullImageBinding.inflate(layoutInflater, null, false)
        dialogBinding.fullImg.loadImage(imageUrl) {
            dialogBinding.alertTitle.setViewGone()
        }
        dialog.setContentView(dialogBinding.root)
        dialog.show()
        dialogBinding.root.setOnClickListener {
            dialog.cancel()
        }
        dialogBinding.alertTitle.setViewGone()

    }

    private fun navToUserChat() {
        /*findNavController().navigate(
            destinationId = R.id.globalUserToChatAction,
            popUpFragId = R.id.searchUserProfileFragment,
            animType = null,
            inclusive = true
        )*/
    }

    override fun setupClickListeners() {
        /*binding.actionBack.setOnClickListener { requireActivity().onBackPressed() }*/
        with(viewModel) {
            onReport = {
                /*showProgressView()
                reportUser(otherUserId).observe(this@SearchUserProfileFragment) {
                    hideProgressView()
                    //binding.userMainContent.snackbar(it)
                }*/
                openMenuItem()
            }
            onSendMsg = {
//                goToMessageScreen()
                createNewChat()

            }
            onGift = {
                //findNavController().navigate(R.id.action_userProfileFragment_to_userGiftsFragment)

//                var bundle = Bundle()
//                bundle.putString("userId", otherUserId)
//                Handler(Looper.getMainLooper()).postDelayed({
//                    findNavController().navigate(
//                        destinationId = R.id.action_userProfileFragment_to_userGiftsFragment,
//                        popUpFragId = null,
//                        animType = AnimationTypes.SLIDE_ANIM,
//                        inclusive = false,
//                        args = bundle
//                    )
//                }, 200)
                binding.purchaseButton.visibility = View.VISIBLE
                binding.topl.visibility = View.GONE
                if (GiftbottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                    GiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//            buttonSlideUp.text = "Slide Down";

                } else {
                    GiftbottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED;
//            buttonSlideUp.text = "Slide Up"

                }
            }
            onBackPressed = {
                onDetach()
                requireActivity().onBackPressed()
            }
        }
    }

    private fun openMenuItem(){
        val popup = PopupMenu(requireContext(), binding.actionReport,10,R.attr.popupMenuStyle,R.style.PopupMenu2)
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


    private fun createNewChat() {

        lifecycleScope.launchWhenCreated() {
            showProgressView()

            var res: ApolloResponse<CreateChatMutation.Data>? = null
            val idOfUserYouWantToChatWith = otherUserName
            Timber.d("apolloResponse $idOfUserYouWantToChatWith")
            try {
                res = apolloClient(
                    requireContext(),
                    getCurrentUserToken()!!
                ).mutation(CreateChatMutation(idOfUserYouWantToChatWith!!)).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                //binding.root.snackbar("Exception ${e.message}")
                //hideProgressView()
                //return@launchWhenResumed
            }
            if (res?.hasErrors() == false) {
                //binding.root.snackbar("You bought ${res?.data?.giftPurchase?.giftPurchase?.gift?.giftName} successfully!")
                val chatroom = res.data?.createChat?.room
                Timber.d("apolloResponse success ${chatroom?.id}")
                chatBundle.putInt("chatId", chatroom?.id?.toInt()!!)
                chatBundle.putString("ChatType", "normal")
                findNavController().navigate(R.id.globalUserToNewChatAction, chatBundle)
            }
            if (res?.hasErrors() == true && res.errors?.size!! > 0) {
                binding.root.snackbar("${res.errors?.get(0)?.message}")
                Timber.d("apolloResponse end ${res.hasErrors()} ${res.errors?.get(0)?.message}")
            }
            //Timber.d("apolloResponse ${res?.hasErrors()} ${res?.data?.createChat?.room} ${res?.data?.createChat?.room?.isNew}")
            hideProgressView()
        }
    }


    /*private fun goToMessageScreen() {
        if (checkUserIsAlreadyInChat()) {
            if (fromChat) {
                navToUserChat()
            } else {
                val msgPreviewModel: MessagePreviewModel? =
                    QbDialogHolder.getChatDialogByUserId(otherUserId)
                msgPreviewModel?.let {
                    viewModel.selectedMsgPreview = it
                    findNavController().navigate(R.id.globalUserToChatAction)
                }
            }
        } else {
            findNavController().navigate(R.id.action_global_sendFirstMessengerChatFragment)
        }
    }*/

   /* private fun checkUserIsAlreadyInChat(): Boolean {
        val dialogsUsersIds = ArrayList(QbDialogHolder.dialogsMap.values).map { it.user }.map { it?.id }
        return dialogsUsersIds.contains(otherUserId)
    }*/

    private fun adjustScreen() {
        var height = Utils.getScreenHeight()
        var calculated = (height * 70) / 100
        Timber.d("Height: $height Calculated 75%: $calculated")

        var params = binding?.userCollapseToolbar?.layoutParams
        params.height = calculated
        binding?.userCollapseToolbar?.layoutParams = params
    }
}