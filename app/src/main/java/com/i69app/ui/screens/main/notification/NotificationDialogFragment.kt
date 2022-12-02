package com.i69app.ui.screens.main.notification

import android .content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apollographql.apollo3.exception.ApolloException
import com.google.android.material.textview.MaterialTextView
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.i69app.*
import com.i69app.databinding.DialogNotificationsBinding
import com.i69app.singleton.App
import com.i69app.ui.adapters.NotificationsAdapter
import com.i69app.ui.screens.SplashActivity
import com.i69app.ui.screens.main.MainActivity
import com.i69app.ui.screens.main.MainActivity.Companion.getMainActivity
import com.i69app.ui.screens.main.moment.PlayUserStoryDialogFragment
import com.i69app.ui.screens.main.moment.UserStoryDetailFragment
import com.i69app.utils.apolloClient
import com.i69app.utils.snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class NotificationDialogFragment(
    var userToken: String?,
    var Counters: MaterialTextView,
    var userId: String?,
    var bell: ImageView
) :
    DialogFragment(), NotificationsAdapter.NotificationListener {

    private lateinit var binding: DialogNotificationsBinding
    private lateinit var adapter: NotificationsAdapter
    var endCursor: String=""
    var hasNextPage: Boolean= false
    var navController: NavController? = null
    var allnotification: ArrayList<GetAllNotificationQuery.Edge> = ArrayList()
    var layoutManager: LinearLayoutManager? = null

    var width = 0
    var size = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View
    {
        binding = DialogNotificationsBinding.inflate(inflater, container, false)
        binding.btnCloseNotifications.setOnClickListener { dismiss() }
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        navController = findNavController()
        allnotification = ArrayList()
        adapter = NotificationsAdapter(
            requireActivity(),
            this@NotificationDialogFragment,
            allnotification
        )
        layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        binding.rvNotifications.setLayoutManager(layoutManager)

        getAllNotifications()

        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (hasNextPage) {

                binding.rvNotifications.let {


                    if (it.bottom - (binding.scrollView.height + binding.scrollView.scrollY) == 0)
                        getAllNotificationsNext(10,endCursor)

                }

            }
        })
    }

    private fun  getAllNotifications() {
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(
                    requireContext(),
                    userToken!!
                ).query(GetAllNotificationQuery(10,"")).execute()
            } catch (e: ApolloException) {

                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception ${e.message}")
                return@launchWhenResumed
            }
            Log.e("ddd1ddd","-->"+Gson().toJson(res))
            if(res.hasErrors())
            {
                Log.e("ddd1dddww","-->"+res.errors!!.get(0).nonStandardFields!!.get("code"))
                if(res.errors!!.get(0).nonStandardFields!!.get("code").toString().equals("InvalidOrExpiredToken"))
                {
                    // error("User doesn't exist")
                        if(activity!=null)
                        {
                            App.userPreferences.clear()
                            //App.userPreferences.saveUserIdToken("","","")
                            val intent = Intent(MainActivity.mContext, SplashActivity::class.java)
                            startActivity(intent)
                            Log.e("ddd1dddwwds","-->"+res.errors!!.get(0).nonStandardFields!!.get("code"))
                            activity?.finishAffinity()
                    }
                }
            }
            else {
            val allnotification = res.data?.notifications!!.edges
            if(allnotification != null && allnotification.size != 0 )
            {
                endCursor = res.data?.notifications!!.pageInfo.endCursor!!
                hasNextPage = res.data?.notifications!!.pageInfo.hasNextPage

                if (allnotification.size > 0) {
                    binding.noData.visibility = View.GONE
                    binding.rvNotifications.visibility = View.VISIBLE

                    val allUserMomentsFirst: ArrayList<GetAllNotificationQuery.Edge> = ArrayList()


                    allnotification.indices.forEach { i ->

                        allUserMomentsFirst.add(allnotification[i]!!)
                    }

                    adapter.addAll(allUserMomentsFirst)


                    binding.rvNotifications.adapter = adapter
                    if (binding.rvNotifications.itemDecorationCount == 0) {
                        binding.rvNotifications.addItemDecoration(object :
                            RecyclerView.ItemDecoration() {
                            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State)
                            {
                                outRect.top = 20
                                outRect.bottom = 10
                                outRect.left = 20
                                outRect.right = 20
                            }
                        })
                    }
                    getNotificationIndex()
                } else {
                    binding.noData.visibility = View.VISIBLE
                    binding.rvNotifications.visibility = View.GONE
                }
            }
            else
            {
                binding.noData.visibility = View.VISIBLE
                binding.rvNotifications.visibility = View.GONE
            }
            }

        }

    }

    fun getAllNotificationsNext( i: Int, endCursors: String) {
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetAllNotificationQuery(i,endCursors))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception ${e.message}")
                return@launchWhenResumed
            }
            Log.e("ddd2dddd","-->"+Gson().toJson(res))
            if(res.hasErrors())
            {
                Log.e("ddd2dddww","-->"+res.errors!!.get(0).nonStandardFields!!.get("code"))
                if(res.errors!!.get(0).nonStandardFields!!.get("code").toString().equals("InvalidOrExpiredToken"))
                {
                    // error("User doesn't exist")

                    lifecycleScope.launch(Dispatchers.Main) {
                        App.userPreferences.clear()
                        //App.userPreferences.saveUserIdToken("","","")
                        if(activity!=null)
                        {
                        val intent = Intent(activity, SplashActivity::class.java)
                        startActivity(intent)
                        activity!!.finishAffinity()
                    }
                    }}
            }

            val allnotification = res.data?.notifications!!.edges
            endCursor = res.data?.notifications!!.pageInfo.endCursor!!
            hasNextPage = res.data?.notifications!!.pageInfo.hasNextPage

            if (allnotification.size != 0) {

                val allUserMomentsNext: ArrayList<GetAllNotificationQuery.Edge> = ArrayList()


                allnotification.indices.forEach { i ->


                    allUserMomentsNext.add(allnotification[i]!!)
                }

                adapter.addAll(allUserMomentsNext)

            }



            if (binding.rvNotifications.itemDecorationCount == 0) {
                binding.rvNotifications.addItemDecoration(object : RecyclerView.ItemDecoration() {
                    override fun getItemOffsets(
                        outRect: Rect,
                        view: View,
                        parent: RecyclerView,
                        state: RecyclerView.State
                    ) {
                        outRect.top = 25
                    }
                })
            }
            if (allnotification?.size!! > 0) {
                Timber.d("apolloResponse: ${allnotification?.get(0)?.node!!.id}")
                Timber.d("apolloResponse: ${allnotification?.get(0)?.node!!.createdDate}")
            }
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
            Log.e("ddd4dddd","-->"+Gson().toJson(res))
            Timber.d("apolloResponse NotificationIndex ${res.hasErrors()}")
            if(res.hasErrors())
            {
                Log.e("ddd4dddww","-->"+res.errors!!.get(0).nonStandardFields!!.get("code"))
                if(res.errors!!.get(0).nonStandardFields!!.get("code").toString().equals("InvalidOrExpiredToken"))
                {
                    // error("User doesn't exist")

                    lifecycleScope.launch(Dispatchers.Main) {
                        App.userPreferences.clear()
                        //App.userPreferences.saveUserIdToken("","","")
                       if(activity!=null)
                       {
                        val intent = Intent(activity, SplashActivity::class.java)
                        startActivity(intent)
                        activity!!.finishAffinity()
                    }   }                    }
            }
            val NotificationCount = res.data?.unseenCount
            if (NotificationCount == null || NotificationCount == 0) {
                Counters.visibility = View.GONE
                bell.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.notification1))

            } else {
                Counters.visibility = View.VISIBLE
                Counters.setText("" + NotificationCount)
                bell.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.notification2))

            }


        }
    }

    private fun getMoments(ids: String) {

        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetAllUserParticularMomentsQuery(width,size,ids))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception all moments ${e.message}")
                return@launchWhenResumed
            }
            Log.e("ddd5ddddd","-->"+Gson().toJson(res))
            if(res .hasErrors())
            {
                Log.e("ddd5dddww","-->"+res.errors!!.get(0).nonStandardFields!!.get("code"))
                if(res.errors!!.get(0).nonStandardFields!!.get("code").toString().equals("InvalidOrExpiredToken"))
                {
                    // error("User doesn't exist")

                    lifecycleScope.launch(Dispatchers.Main) {
                        App.userPreferences.clear()
                        if(activity!=null)
                        {
                        //App.userPreferences.saveUserIdToken("","","")
                        val intent = Intent(activity, SplashActivity::class.java)
                        startActivity(intent)
                        activity!!.finishAffinity()
                    }     }                  }
            }
            val allmoments = res.data?.allUserMoments!!.edges


            allmoments.indices.forEach { i ->
                if(ids.equals(allmoments[i]!!.node!!.pk.toString()))
                {
                    val bundle = Bundle().apply {
                        putString("momentID", allmoments[i]?.node!!.pk!!.toString())
                        putString("filesUrl", allmoments[i]?.node!!.file!!)
                        putString("Likes", allmoments[i]?.node!!.like!!.toString())
                        putString("Comments", allmoments[i]?.node!!.comment!!.toString())
                        val gson = Gson()
                        putString("Desc",gson.toJson(allmoments[i]?.node!!.momentDescriptionPaginated))
                        putString("fullnames", allmoments[i]?.node!!.user!!.fullName)
                        if(allmoments[i]!!.node!!.user!!.gender != null)
                        {
                            putString("gender", allmoments[i]!!.node!!.user!!.gender!!.name)

                        }
                        else
                        {
                            putString("gender", null)

                        }
                        putString("momentuserID", allmoments[i]?.node!!.user!!.id.toString())
                    }
                    navController!!.navigate(R.id.momentsAddCommentFragment, bundle)

                    return@forEach
                }


            }


        }
    }

    private fun getStories(pkid: String) {

        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetAllUserStoriesQuery(100,"",pkid))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception all stories ${e.message}")
                return@launchWhenResumed
            }
            Timber.d("apolloResponse allUserStories stories ${res.hasErrors()}")
            Log.e("ddd","-->"+Gson().toJson(res))
            if(res.hasErrors())
            {      Log.e("ddddddww","-->"+res.errors!!.get(0).nonStandardFields!!.get("code"))

                if(res.errors!!.get(0).nonStandardFields!!.get("code").toString().equals("InvalidOrExpiredToken"))
                {
                    // error("User doesn't exist")

                    lifecycleScope.launch(Dispatchers.Main) {
                        App.userPreferences.clear()
                        if(activity!=null)
                        {
                        //App.userPreferences.saveUserIdToken("","","")
                        val intent = Intent(activity, SplashActivity::class.java)
                        startActivity(intent)
                        activity!!.finishAffinity()
                    }                       }}
            }
            val allUserStories = res.data?.allUserStories!!.edges

            allUserStories.indices.forEach { i ->
                if(pkid.equals(allUserStories[i]!!.node!!.pk.toString()))
                {

                    val userStory=allUserStories[i]

                    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }

                    Timber.d("filee ${userStory?.node!!.fileType} ${userStory?.node.file}")
                    val url = "${BuildConfig.BASE_URL}media/${userStory?.node.file}"
                    var userurl = ""
                    if(userStory!!.node!!.user!!.avatar != null && userStory.node.user!!.avatar!!.url != null)
                    {
                        userurl = userStory.node.user.avatar!!.url!!

                    }
                    else
                    {
                        userurl = ""

                    }
                    val username = userStory.node.user!!.fullName
                    val UserID = userId
                    val objectId = userStory.node.pk

                    var text = userStory.node.createdDate.toString()
                    text = text.replace("T", " ").substring(0, text.indexOf("."))
                    val momentTime = formatter.parse(text)
                    val times = DateUtils.getRelativeTimeSpanString(momentTime.time, Date().time, DateUtils.MINUTE_IN_MILLIS)
                    if (userStory?.node.fileType.equals("video")) {
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
                        val dialog = UserStoryDetailFragment(null)
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
                    }
                    return@forEach
                }


            }

        }
    }


    override fun onNotificationClick(
        position: Int,
        notificationdata: GetAllNotificationQuery.Edge?
    ) {

        val titles= notificationdata!!.node!!.notificationSetting!!.title
        Log.e("titles",titles)

        if(titles!!.equals("Moment Liked")|| titles!!.equals("Comment in moment"))
        {
            if (notificationdata!!.node!!.data != null) {
                val resp: JsonObject = JsonParser().parse(notificationdata!!.node!!.data.toString()).asJsonObject

                var momentid = resp.get("momentId").toString()


                getMoments(momentid)
            }
        }

        else if(titles.equals("Story liked"))
        {

            if (notificationdata!!.node!!.data != null) {
                val resp: JsonObject = JsonParser().parse(notificationdata!!.node!!.data.toString()).asJsonObject
                Log.e("ddd",Gson().toJson(resp))

                var pkid = resp.get("pk").toString()
                getStories(pkid)

            }
        }
        else if(titles.equals("Story Commented"))
        {
            if (notificationdata!!.node!!.data != null) {
                val resp: JsonObject = JsonParser().parse(notificationdata!!.node!!.data.toString()).asJsonObject


                var pkid = resp.get("pk").toString()
                getStories(pkid)


            }
        }
        else if(titles.equals("Gift received"))
        {
            //navController!!.navigate(R.id.action_global_user_profile)
            getMainActivity()?.binding?.bottomNavigation?.selectedItemId=R.id.nav_user_profile_graph

        }
        else if(titles.equals("Message received"))
        {
            val resp: JsonObject = JsonParser().parse(notificationdata!!.node!!.data.toString()).asJsonObject
            Log.e("ddd",Gson().toJson(resp))
            // navController!!.navigate(R.id.action_global_user_profile)
            /*val bundle = Bundle()
            bundle.putString("roomIDNotify", resp.get("roomID").toString())
            navController!!.navigate(R.id.messengerListFragment, bundle)*/
            getMainActivity()?.pref?.edit()?.putString("roomIDNotify","true")?.putString("roomID",resp.get("roomID").toString())?.apply()
            getMainActivity()?.binding?.bottomNavigation?.selectedItemId = R.id.nav_chat_graph
        }
        else if(titles.equals("Message Received"))
        {
            val resp: JsonObject = JsonParser().parse(notificationdata!!.node!!.data.toString()).asJsonObject
            Log.e("ddd",Gson().toJson(resp))
            // navController!!.navigate(R.id.action_global_user_profile)
            /*val bundle = Bundle()
            bundle.putString("roomIDNotify", resp.get("roomID").toString())
            navController!!.navigate(R.id.messengerListFragment, bundle)*/
            getMainActivity()?.pref?.edit()?.putString("roomIDNotify","true")?.putString("roomID",resp.get("roomID").toString())?.apply()
            getMainActivity()?.binding?.bottomNavigation?.selectedItemId = R.id.nav_chat_graph
        }
        else if(titles.equals("Congratulations"))
        {
            getMainActivity()?.binding?.bottomNavigation?.selectedItemId=R.id.nav_user_profile_graph
            //navController!!.navigate(R.id.action_global_user_profile)
            /*val resp: JsonObject = JsonParser().parse(notificationdata!!.node!!.data.toString()).asJsonObject
            Log.e("ddd",Gson().toJson(resp))
            // navController!!.navigate(R.id.action_global_user_profile)
            val bundle = Bundle()
            bundle.putString("roomIDNotify", resp.get("roomID").toString())
            navController!!.navigate(R.id.messengerListFragment, bundle)*/
        }
    }
}