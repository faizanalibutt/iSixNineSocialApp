package com.i69app.ui.screens.main.messenger.list

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.fragment.findNavController
import com.apollographql.apollo3.exception.ApolloException
import com.i69app.*
import com.i69app.data.config.Constants
import com.i69app.databinding.FragmentMessengerListBinding
import com.i69app.databinding.ItemRequestPreviewLongBinding
import com.i69app.firebasenotification.NotificationBroadcast
import com.i69app.ui.base.BaseFragment
import com.i69app.ui.screens.SplashActivity
import com.i69app.ui.screens.main.MainActivity
import com.i69app.ui.screens.main.MainActivity.Companion.getMainActivity
import com.i69app.ui.screens.main.messenger.list.MessengerListAdapter.MessagesListListener
import com.i69app.ui.viewModels.UserViewModel
import com.i69app.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber

class MessengerListFragment : BaseFragment<FragmentMessengerListBinding>(), MessagesListListener {

    private lateinit var job: Job
    private var firstMessage: GetAllRoomsQuery.Edge? = null
    private var broadcastMessage: GetAllRoomsQuery.Edge? = null
    private var allRoom: MutableList<GetAllRoomsQuery.Edge?> = mutableListOf()
    private var isRunning = false
    private val viewModel: UserViewModel by activityViewModels()
    private lateinit var messengerListAdapter: MessengerListAdapter
    var endCursor: String = ""
    var hasNextPage: Boolean = false

    private var userId: String? = null
    private var userToken: String? = null
    lateinit var handler: Handler
    private var broadcast: NotificationBroadcast? = null

    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentMessengerListBinding.inflate(inflater, container, false)

    override fun setupTheme() {

        navController = findNavController()

        lifecycleScope.launch {
            Log.e("u id", "-->" + getCurrentUserId())
            Log.e("userToken", "-->" + getCurrentUserToken())
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!
            Timber.i("usertokenn $userToken")
        }
        Timber.i("usertokenn 2 $userToken")


        handler.postDelayed(object : Runnable {
            override fun run() {
                messengerListAdapter.notifyDataSetChanged();
                handler.postDelayed(this, 30 * 1000)
            }
        }, 30 * 1000)

        messengerListAdapter = MessengerListAdapter(this@MessengerListFragment, userId)
        binding.messengerList.adapter = messengerListAdapter

        getTypeActivity<MainActivity>()?.enableNavigationDrawer()
        //getFirstMessage()
        //getBroadcastMessage()
        if (getMainActivity().pref.getString("chatListRefresh", "false").equals("true")) {
            getMainActivity().pref.edit()?.putString("chatListRefresh", "false")?.apply()
            allRoom.clear()
        }

        isRunning = false

        lifecycleScope.launch {
            viewModel.shouldUpdateAdapter.collect {
                Timber.tag(MainActivity.CHAT_TAG).i("Collecting Data: Update ($it)")
                //if (it) updateList(true)
            }
        }

        try {
            job = lifecycleScope.launch {
                viewModel.newMessageFlow.collect { message ->
                    message?.let { newMessage ->

                        try{
                            val index = allRoom.indexOfFirst {
                                it?.node?.id == newMessage.roomId.id
                            }
                            val selectedRoom = allRoom[index]!!
                            val room = GetAllRoomsQuery.Edge(
                                GetAllRoomsQuery.Node(
                                    id = selectedRoom.node?.id!!,
                                    name = selectedRoom.node.name,
                                    lastModified = newMessage.timestamp,
                                    blocked = 0,
                                    unread = selectedRoom.node.unread?.toInt()?.plus(1)?.toString(),
                                    messageSet = GetAllRoomsQuery.MessageSet(
                                        edges = selectedRoom.node.messageSet.edges.toMutableList()
                                            .apply {
                                                set(
                                                    0, GetAllRoomsQuery.Edge1(
                                                        GetAllRoomsQuery.Node1(
                                                            content = newMessage.content,
                                                            id = newMessage.id,
                                                            roomId = GetAllRoomsQuery.RoomId(id = newMessage.roomId.id),
                                                            timestamp = newMessage.timestamp,
                                                            read = newMessage.read
                                                        )
                                                    )
                                                )
                                            }
                                    ),
                                    userId = selectedRoom.node.userId,
                                    target = selectedRoom.node.target,
                                )
                            )
                            allRoom.set(index = index, room)

                            Log.e("listtww", "" + allRoom)
                            //messengerListAdapter.updateList(allRoom)
                            messengerListAdapter.submitList1(allRoom)
                        }catch (e:IndexOutOfBoundsException){
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }


        checkForUpdates()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        //broadcast = NotificationBroadcast(this);

        val arguments = arguments
        if (arguments != null) {
            val roomID = arguments.get("roomIDNotify") as String?
            if (roomID != null) {
                getParticularRoom(roomID)
                arguments.run {
                    remove("roomIDNotify")
                    clear()
                }
            }
        }
        handler = Handler()
        return super.onCreateView(inflater, container, savedInstanceState)

    }

    private fun checkForUpdates() {
        if (getMainActivity().pref.getString("roomIDNotify", "false").equals("true")) {
            getMainActivity().pref.edit().putString("roomIDNotify", "false").apply()
            getParticularRoom(getMainActivity().pref.getString("roomID", ""))
        }
        if (getMainActivity().pref.getString("readCount", "false").equals("false")) {
            if (getMainActivity().pref.getString("newChat", "false").equals("true")) {
                getMainActivity().pref.edit().putString("newChat", "false").apply()
                //if (allRoom.size!=0)
                //updateList(true)
            }
        }
        if (getMainActivity().pref.getString("readCount", "false").equals("true")) {
            getMainActivity().pref.edit().putString("readCount", "false").apply()
            if (getMainActivity().pref.getString("type", "").equals("001")) {
                //firstMessage
                val position = getMainActivity().pref.getInt("position", 0)
                val id = getMainActivity().pref.getString("id", "")
                if (position > 0) {
                    if (allRoom.size != 0)
                        getParticularFirstMessageUpdate(position, id!!)
                }

            } else if (getMainActivity().pref.getString("type", "").equals("000")) {
                //braodcastMessage
                val position = getMainActivity().pref.getInt("position", 0)
                val id = getMainActivity().pref.getString("id", "")
                if (allRoom.size != 0)
                    getParticularBraodCastUpdate(position, id!!)

            } else {
                //userMessage
                val position = getMainActivity().pref.getInt("position", 0)
                val id = getMainActivity().pref.getString("id", "")
                if (position > 0) {
                    if (allRoom.size != 0)
                        getParticularRoomForUpdate(position = position, id!!)
                }

            }
            //getMainActivity().pref.edit().putString("roomIDNotify","false").apply()
            //getParticularRoom(getMainActivity().pref.getString("roomID",""))
        }

    }

    override fun onResume() {
        super.onResume()
        if (allRoom.size == 0) {
            allRoom.clear()
            updateList(true)
        }
        if (allRoom.size!=0) {
            allRoom.clear()
            updateList(true)
        }

        /*LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            broadcast!!, IntentFilter(Constants.INTENTACTION)
        );*/
//        val intentFilter = IntentFilter()
//        intentFilter.addAction("com.my.app.onMessageReceived")
//        intentFilter.addAction("gift_Received")
//        activity?.registerReceiver(broadCastReceiver, intentFilter)
    }

    public fun updateView(state: String) {
//        updateList(true)
    }

    private val broadCastReceiver = object : BroadcastReceiver() {
        override fun onReceive(contxt: Context?, intent: Intent?) {
            val extras = intent?.extras
            val state = extras!!.getString("extra")
            Log.e("TAG_Notification_rece", "onReceive: $state")
            updateView(state.toString())
        }
    }

    override fun onPause() {
        if (this::handler.isInitialized){
            handler.removeCallbacksAndMessages(null)
        }
        //job.cancel()
        super.onPause()
        //LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(broadcast!!);
    }

    override fun setupClickListeners() {
        binding.toolbarHamburger.setOnClickListener { (activity as MainActivity).drawerSwitchState() }
        binding.goToSearchBtn.setOnClickListener { activity?.onBackPressed() }
    }

    override fun onDestroyView() {
        if (this::handler.isInitialized){
            handler.removeCallbacksAndMessages(null)
        }
        super.onDestroyView()

    }

    private fun makePreviewAnimation() {

        binding.goToSearchBtn.setViewVisible()
        binding.messengerListPreview.setViewVisible()
        binding.messengerList.setVisibleOrInvisible(false)
        binding.messengerListPreview.setViewVisible()

        val display = requireActivity().windowManager!!.defaultDisplay
        val metrics = DisplayMetrics()
        display.getMetrics(metrics)
        binding.subTitle.defaultAnimate(100, 200)

        setupPreviewItem(binding.firstAnimPreview, R.drawable.icon_boy)
        setupPreviewItem(binding.secondAnimPreview, R.drawable.icon_girl)
        setupPreviewItem(binding.thirdAnimPreview, R.drawable.icon_girl_2)

        binding.firstAnimPreview.root.animateFromLeft(200, 300, metrics.widthPixels / 3)
        binding.secondAnimPreview.root.animateFromLeft(200, 500, metrics.widthPixels / 3)
        binding.thirdAnimPreview.root.animateFromLeft(200, 700, metrics.widthPixels / 3)

    }

    private fun setupPreviewItem(
        requestPreviewBinding: ItemRequestPreviewLongBinding,
        preview: Int
    ) {
        requestPreviewBinding.previewImg.setImageResource(preview)
    }

    private fun getParticularRoom(roomID: String?) {

        Timber.d("ROOMID=" + roomID)
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetparticularRoomsQuery(roomID!!))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception to get room ${e.message}")
                hideProgressView()
                return@launchWhenResumed
            }

            hideProgressView()

            val Rooms = res.data?.room

            val chatBundle = Bundle()
            if (Rooms?.userId!!.id.equals(userId)) {
                chatBundle.putString("otherUserId", Rooms.target.id)
                if (Rooms.target.avatar != null) {
                    chatBundle.putString("otherUserPhoto", Rooms.target.avatar.url ?: "")
                } else {
                    chatBundle.putString("otherUserPhoto", "")
                }

                chatBundle.putString("otherUserName", Rooms.target.fullName)
            } else {
                chatBundle.putString("otherUserId", Rooms.userId.id)
                if (Rooms.userId.avatar != null) {
                    chatBundle.putString("otherUserPhoto", Rooms.userId.avatar.url ?: "")
                } else {
                    chatBundle.putString("otherUserPhoto", "")
                }
                chatBundle.putString("otherUserName", Rooms.userId.fullName)
            }

            chatBundle.putInt("chatId", Rooms.id.toInt())
            findNavController().navigate(R.id.globalUserToNewChatAction, chatBundle)
        }
    }

    private fun getParticularFirstMessageUpdate(position: Int, id: String) {
        lifecycleScope.launchWhenResumed {
            val resFirstMessage = try {
                apolloClient(requireContext(), userToken!!).query(GetFirstMessageQuery())
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception getFirstMessage ${e.message}")
                //hideProgressView()
                return@launchWhenResumed
            }
            if (resFirstMessage.hasErrors()) {

                if (resFirstMessage.errors!!.get(0).nonStandardFields!!.get("code").toString()
                        .equals("InvalidOrExpiredToken")
                ) {
                    // error("User doesn't exist")

                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        //App.userPreferences.saveUserIdToken("","","")
                        val intent = Intent(activity, SplashActivity::class.java)
                        startActivity(intent)
                        activity!!.finishAffinity()
                    }
                }
            }
            if (resFirstMessage.data?.firstmessage != null) {
                firstMessage = GetAllRoomsQuery.Edge(GetAllRoomsQuery.Node(
                    id = "001",
                    name = resFirstMessage.data?.firstmessage?.firstmessageContent!!,
                    lastModified = resFirstMessage.data?.firstmessage?.firstmessageTimestamp,
                    unread = resFirstMessage.data?.firstmessage?.unread?.toString(),
                    blocked = 0,
                    messageSet = GetAllRoomsQuery.MessageSet(
                        edges = mutableListOf<GetAllRoomsQuery.Edge1>().apply {
                            add(
                                GetAllRoomsQuery.Edge1(
                                    GetAllRoomsQuery.Node1(
                                        content = "",
                                        id = "001",
                                        roomId = GetAllRoomsQuery.RoomId(id = ""),
                                        timestamp = resFirstMessage.data?.firstmessage?.firstmessageTimestamp!!,
                                        read = ""
                                    )
                                )
                            )
                        }
                    ),
                    userId = GetAllRoomsQuery.UserId(
                        null,
                        resFirstMessage.data?.firstmessage?.firstmessageContent!!,
                        null,
                        null,
                        null,
                        null
                    ),
                    target = GetAllRoomsQuery.Target(null, null, null, null, null, null),
                ))
            }
            Log.e("params", "" + firstMessage)
            //hideProgressView()

            if (firstMessage != null) {
                try {
                    allRoom[position] = firstMessage!!
                    messengerListAdapter.submitList1(allRoom)
                }catch (e:IndexOutOfBoundsException){
                    e.printStackTrace()
                }

            }
            getMainActivity().updateChatBadge()
        }
    }

    private fun getParticularBraodCastUpdate(position: Int, id: String) {
        lifecycleScope.launchWhenResumed {
            val resBroadcast = try {
                apolloClient(requireContext(), userToken!!).query(GetBroadcastMessageQuery())
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception getBroadcastMessage ${e.message}")
                //hideProgressView()
                return@launchWhenResumed
            }
            if (resBroadcast.hasErrors()) {

                if (resBroadcast.errors!!.get(0).nonStandardFields!!.get("code").toString()
                        .equals("InvalidOrExpiredToken")
                ) {
                    // error("User doesn't exist")

                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        //App.userPreferences.saveUserIdToken("","","")
                        val intent = Intent(activity, SplashActivity::class.java)
                        startActivity(intent)
                        activity!!.finishAffinity()
                    }
                }
            }
            if (resBroadcast.data?.broadcast != null) {
                broadcastMessage = GetAllRoomsQuery.Edge(GetAllRoomsQuery.Node(
                    id = "000",
                    name = resBroadcast.data?.broadcast?.broadcastContent!!,
                    lastModified = resBroadcast.data?.broadcast?.broadcastTimestamp,
                    unread = resBroadcast.data?.broadcast?.unread,
                    blocked = 0,
                    messageSet = GetAllRoomsQuery.MessageSet(
                        edges = mutableListOf<GetAllRoomsQuery.Edge1>().apply {
                            add(
                                GetAllRoomsQuery.Edge1(
                                    GetAllRoomsQuery.Node1(
                                        content = "",
                                        id = "000",
                                        roomId = GetAllRoomsQuery.RoomId(id = ""),
                                        timestamp = resBroadcast.data?.broadcast?.broadcastTimestamp!!,
                                        read = ""
                                    )
                                )
                            )
                        }
                    ),
                    userId = GetAllRoomsQuery.UserId(null, "Team i69", null, null, null, null),
                    target = GetAllRoomsQuery.Target(null, null, null, null, null, null),
                ))
            }
            if (broadcastMessage != null) {
                //allRoom.add(0, broadcastMessage)
                allRoom[position] = broadcastMessage!!
                messengerListAdapter.submitList1(allRoom)
            }
            getMainActivity().updateChatBadge()
        }
    }

    private fun getParticularRoomForUpdate(position: Int, id: String) {
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetAllRoomsQuery(20))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception to get room ${e.message}")
                // hideProgressView()
                return@launchWhenResumed
            }

            //hideProgressView()
            val room = res.data?.rooms?.edges
            var totoalunread = 0

            room?.indices?.forEach { i ->
                try {
                    if (id.equals(room[i]!!.node!!.id.toString())) {
                        //temp until unread count not fixed
                        allRoom[position] = room[i]!!
                        messengerListAdapter.submitList1(allRoom)
                        //sharedMomentAdapter.notifyItemChanged(pos)
                        return@forEach
                    }
                }catch (e:IndexOutOfBoundsException){

                }


            }

            getMainActivity().updateChatBadge()
        }
    }

    fun updateList(isProgressShow: Boolean) {
        /* if (isProgressShow) {
             showProgressView()
         }*/
        allRoom.clear()
        lifecycleScope.launchWhenResumed {
            val resBroadcast = try {
                apolloClient(requireContext(), userToken!!).query(GetBroadcastMessageQuery())
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception getBroadcastMessage ${e.message}")
                //hideProgressView()
                return@launchWhenResumed
            }
            if (resBroadcast.hasErrors()) {

                if (resBroadcast.errors!!.get(0).nonStandardFields!!.get("code").toString()
                        .equals("InvalidOrExpiredToken")
                ) {
                    // error("User doesn't exist")

                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        //App.userPreferences.saveUserIdToken("","","")
                        val intent = Intent(activity, SplashActivity::class.java)
                        startActivity(intent)
                        activity!!.finishAffinity()
                    }
                }
            }
            if (resBroadcast.data?.broadcast != null) {
                broadcastMessage = GetAllRoomsQuery.Edge(GetAllRoomsQuery.Node(
                    id = "000",
                    name = resBroadcast.data?.broadcast?.broadcastContent!!,
                    lastModified = resBroadcast.data?.broadcast?.broadcastTimestamp,
                    unread = resBroadcast.data?.broadcast?.unread,
                    blocked = 0,
                    messageSet = GetAllRoomsQuery.MessageSet(
                        edges = mutableListOf<GetAllRoomsQuery.Edge1>().apply {
                            add(
                                GetAllRoomsQuery.Edge1(
                                    GetAllRoomsQuery.Node1(
                                        content = "",
                                        id = "000",
                                        roomId = GetAllRoomsQuery.RoomId(id = ""),
                                        timestamp = resBroadcast.data?.broadcast?.broadcastTimestamp!!,
                                        read = ""
                                    )
                                )
                            )
                        }
                    ),
                    userId = GetAllRoomsQuery.UserId(null, "Team i69", null, null, null, null),
                    target = GetAllRoomsQuery.Target(null, null, null, null, null, null),
                ))
            }
            if (broadcastMessage != null) {
                allRoom.add(0, broadcastMessage)
                // allRoom = allRoom.filter { it?.node?.messageSet?.edges?.isNotEmpty() == true }.toMutableList()
                //Log.e("listt", "" + allRoom)
                //messengerListAdapter.updateList(allRoom)
                messengerListAdapter.submitList1(allRoom)
            }
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetAllRoomsQuery(20)).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception all moments ${e.message}")
                //hideProgressView()
                return@launchWhenResumed
            }
            Log.e("dddfff", "1111")
            if (res.hasErrors()) {
                Log.e("dddfff", "2222")
                Log.e("dddfff", "-->" + res.errors!!.get(0))
                Log.e("dddfff", "-->" + res.errors!!.get(0).nonStandardFields)
                Log.e("dddfff", "-->" + res.errors!!.get(0).nonStandardFields!!.get("code"))
                if (res.errors!!.get(0).nonStandardFields!!.get("code").toString()
                        .equals("InvalidOrExpiredToken")
                ) {
                    Log.e("dddfff", "33333")
                    // error("User doesn't exist")
                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        Log.e("dddfff", "444444")
                        //App.userPreferences.saveUserIdToken("","","")
                        val intent = Intent(activity, SplashActivity::class.java)
                        startActivity(intent)
                        activity!!.finishAffinity()
                        Log.e("dddfff", "555555")
                    }
                }
            }
            allRoom.addAll(res.data?.rooms!!.edges)
            allRoom =
                allRoom.filter { it?.node?.messageSet?.edges?.isNotEmpty() == true }.toMutableList()
            allRoom = allRoom.filter { it?.node?.blocked == 0 }.toMutableList()

            //Log.e("listt", "" + allRoom)
            //messengerListAdapter.updateList(allRoom)
            messengerListAdapter.submitList1(allRoom)

            val resFirstMessage = try {
                apolloClient(requireContext(), userToken!!).query(GetFirstMessageQuery())
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception getFirstMessage ${e.message}")
                //hideProgressView()
                return@launchWhenResumed
            }
            if (resFirstMessage.hasErrors()) {

                if (resFirstMessage.errors!!.get(0).nonStandardFields!!.get("code").toString()
                        .equals("InvalidOrExpiredToken")
                ) {
                    // error("User doesn't exist")

                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        //App.userPreferences.saveUserIdToken("","","")
                        val intent = Intent(activity, SplashActivity::class.java)
                        startActivity(intent)
                        activity!!.finishAffinity()
                    }
                }
            }
            if (resFirstMessage.data?.firstmessage != null) {
                firstMessage = GetAllRoomsQuery.Edge(GetAllRoomsQuery.Node(
                    id = "001",
                    name = resFirstMessage.data?.firstmessage?.firstmessageContent!!,
                    lastModified = resFirstMessage.data?.firstmessage?.firstmessageTimestamp,
                    unread = resFirstMessage.data?.firstmessage?.unread?.toString(),
                    blocked = 0,
                    messageSet = GetAllRoomsQuery.MessageSet(
                        edges = mutableListOf<GetAllRoomsQuery.Edge1>().apply {
                            add(
                                GetAllRoomsQuery.Edge1(
                                    GetAllRoomsQuery.Node1(
                                        content = "",
                                        id = "001",
                                        roomId = GetAllRoomsQuery.RoomId(id = ""),
                                        timestamp = resFirstMessage.data?.firstmessage?.firstmessageTimestamp!!,
                                        read = ""
                                    )
                                )
                            )
                        }
                    ),
                    userId = GetAllRoomsQuery.UserId(
                        null,
                        resFirstMessage.data?.firstmessage?.firstmessageContent!!,
                        null,
                        null,
                        null,
                        null
                    ),
                    target = GetAllRoomsQuery.Target(null, null, null, null, null, null),
                ))
            }
            Log.e("params", "" + firstMessage)
            Log.e("resss", "" + res.data?.rooms!!.edges.toMutableList())

            //hideProgressView()

            if (firstMessage != null) {
                allRoom.add(firstMessage)
            }

            //allRoom = allRoom.filter { it?.node?.messageSet?.edges?.isNotEmpty() == true }.toMutableList()
            //Log.e("listt", "" + allRoom)
            //messengerListAdapter.updateList(allRoom)
            messengerListAdapter.submitList1(allRoom)


            //allRoom = res.data?.rooms!!.edges.toMutableList()

            /*if (allRoom.isNullOrEmpty()) {
                    if (isRunning) return@launchWhenResumed
                    isRunning = true
                    makePreviewAnimation()
                    return@launchWhenResumed
                }*/

            if (allRoom.size != 0) {
                if (res.data?.rooms?.pageInfo?.endCursor != null) {
                    endCursor = res.data?.rooms!!.pageInfo.endCursor!!
                    hasNextPage = res.data?.rooms!!.pageInfo.hasNextPage
                }
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

            try {
                //getMainActivity().binding.navView.updateMessagesCount(totoalunread)
                getMainActivity().binding.bottomNavigation.addBadge(totoalunread)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            if (allRoom.size > 0) {
                Timber.d("apolloResponse: ${allRoom.get(0)?.node!!.name}")
                Timber.d("apolloResponse: ${allRoom.get(0)?.node!!.id}")
                Timber.d("apolloResponse: ${allRoom.get(0)?.node!!.lastModified}")
                Timber.d("apolloResponse: ${allRoom.get(0)?.node!!.target.username}")
            }
        }
    }

    override fun onItemClick(AllroomEdge: GetAllRoomsQuery.Edge, position: Int) {
        /*if (AllroomEdge.node?.id == "firstName" || AllroomEdge.node?.id == "broadcast") {
            Toast.makeText(requireContext(),"work in progress",Toast.LENGTH_SHORT).show()
            return
        }*/
        viewModel.setSelectedMessagePreview(AllroomEdge)
        val chatBundle = Bundle()
        if (AllroomEdge.node?.id == "001") {
            if (AllroomEdge.node.unread?.toInt()!! > 0) {
                getMainActivity().pref.edit().putString("readCount", "true")
                    .putString("type", "001").putString("id", AllroomEdge.node.id)
                    .putInt("position", position).apply()
            }
            chatBundle.putString("otherUserId", "")
            chatBundle.putString("otherUserPhoto", "")
            chatBundle.putString("otherUserName", AllroomEdge.node.userId.fullName)
            chatBundle.putInt("otherUserGender", 0)
            chatBundle.putString("ChatType", "001")
            chatBundle.putInt("chatId", 0)
            findNavController().navigate(R.id.globalUserToNewChatAction, chatBundle)
        } else if (AllroomEdge.node?.id == "000") {
            if (AllroomEdge.node.unread?.toInt()!! > 0) {
                getMainActivity().pref.edit().putString("readCount", "true")
                    .putString("type", "000").putString("id", AllroomEdge.node.id)
                    .putInt("position", position).apply()
            }
            chatBundle.putString("otherUserId", "")
            chatBundle.putString("otherUserPhoto", "")
            chatBundle.putString("otherUserName", AllroomEdge.node.userId.fullName)
            chatBundle.putInt("otherUserGender", 0)
            chatBundle.putString("ChatType", "000")
            chatBundle.putInt("chatId", 0)
            findNavController().navigate(R.id.globalUserToNewChatAction, chatBundle)
        } else {
            if (AllroomEdge.node?.unread?.toInt()!! > 0) {
                getMainActivity().pref.edit().putString("readCount", "true")
                    .putString("type", "User").putString("id", AllroomEdge.node.id)
                    .putInt("position", position).apply()
            }

            if (AllroomEdge.node!!.userId.id.equals(userId)) {
                chatBundle.putString("otherUserId", AllroomEdge.node.target.id)
                if (AllroomEdge.node.target.avatar != null) {
                    chatBundle.putString("otherUserPhoto", AllroomEdge.node.target.avatar.url ?: "")
                } else {
                    chatBundle.putString("otherUserPhoto", "")
                }
                chatBundle.putString("otherUserName", AllroomEdge.node.target.fullName)
                chatBundle.putInt("otherUserGender", AllroomEdge.node.target.gender ?: 0)
                chatBundle.putString("ChatType", "Normal")

            } else {
                chatBundle.putString("otherUserId", AllroomEdge.node.userId.id)
                if (AllroomEdge.node.userId.avatar != null) {
                    chatBundle.putString("otherUserPhoto", AllroomEdge.node.userId.avatar.url ?: "")
                } else {
                    chatBundle.putString("otherUserPhoto", "")
                }
                chatBundle.putString("otherUserName", AllroomEdge.node.userId.fullName ?: "")
                chatBundle.putInt("otherUserGender", AllroomEdge.node.userId.gender ?: 0)
                chatBundle.putString("ChatType", "Normal")
            }

            chatBundle.putInt("chatId", AllroomEdge.node.id.toInt())
            findNavController().navigate(R.id.globalUserToNewChatAction, chatBundle)
        }
    }

    fun getMainActivity() = activity as MainActivity

}