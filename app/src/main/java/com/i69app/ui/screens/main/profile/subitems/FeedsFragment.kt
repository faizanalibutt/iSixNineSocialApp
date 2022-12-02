package com.i69app.ui.screens.main.profile.subitems

import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.apollographql.apollo3.exception.ApolloException
import com.google.gson.Gson
import com.i69app.*
import com.i69app.databinding.FragmentFeedBinding
import com.i69app.di.modules.AppModule
import com.i69app.ui.adapters.NearbySharedMomentAdapter
import com.i69app.ui.adapters.UserStoriesAdapter
import com.i69app.ui.base.BaseFragment
import com.i69app.utils.*
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.roundToInt

class FeedsFragment : BaseFragment<FragmentFeedBinding>(), UserStoriesAdapter.UserStoryListener,
    NearbySharedMomentAdapter.NearbySharedMomentListener {

    private var userToken: String? = null
    private var userName: String? = null

    private lateinit var sharedMomentAdapter: NearbySharedMomentAdapter

    var width = 0
    var size = 0
    var endCursor: String = ""
    var hasNextPage: Boolean = false
    var allUserMoments: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()
    var layoutManager: LinearLayoutManager? = null

    private var userId: String? = null


    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentFeedBinding.inflate(inflater, container, false)

    override fun setupTheme() {
        navController = findNavController()

        val displayMetrics = DisplayMetrics()
        requireActivity().windowManager.defaultDisplay.getMetrics(displayMetrics)
        val height = displayMetrics.heightPixels
        width = displayMetrics.widthPixels

        val densityMultiplier = getResources().getDisplayMetrics().density;
        val scaledPx = 14 * densityMultiplier;
        val paint = Paint()
        paint.setTextSize(scaledPx);
        size = paint.measureText("s").roundToInt()

        setUpData()
    }

    override fun setupClickListeners() {
    }

    private fun setUpData() {

        lifecycleScope.launch {
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!
            userName = getCurrentUserName()
        }

        allUserMoments = ArrayList()
        sharedMomentAdapter = NearbySharedMomentAdapter(
            requireActivity(),
            this,
            allUserMoments,
            userId
        )
        layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        binding.rvMoments.setLayoutManager(layoutManager)



        getAllUserMoments(width, size)

        binding.scrollView.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            if (hasNextPage) {

                binding.rvMoments?.let {


                    if (it.bottom - (binding.scrollView.height + binding.scrollView.scrollY) == 0)
                        allusermoments1(width, size, 10, endCursor)
                }

            }
        })

    }


    private fun getAllUserMoments(width: Int, size: Int) {
        lifecycleScope.launch() {
            val res = try {

                apolloClient(requireContext(), userToken!!).query(
                    GetAllUserMomentsQuery(
                        width, size, 10, "", ""
                    )
                ).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")

                return@launch
            }


            val allmoments = res.data?.allUserMoments!!.edges
            if (allmoments.size != 0) {
                endCursor = res.data?.allUserMoments!!.pageInfo.endCursor!!
                hasNextPage = res.data?.allUserMoments!!.pageInfo.hasNextPage!!


                val allUserMomentsFirst: ArrayList<GetAllUserMomentsQuery.Edge> = ArrayList()

                allmoments.indices.forEach { i ->


                    allUserMomentsFirst.add(allmoments[i]!!)
                }

                // sharedMomentAdapter.addAll(allUserMomentsFirst)

                binding.rvMoments.adapter = sharedMomentAdapter
                allUserMoments.addAll(allUserMomentsFirst)
                sharedMomentAdapter.submitList1(allUserMoments)
            }

            if (binding.rvMoments.itemDecorationCount == 0) {
                binding.rvMoments.addItemDecoration(object : RecyclerView.ItemDecoration() {
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
        }
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
                    allUserMoments[pos] = allmoments[i]!!
                    sharedMomentAdapter.notifyItemChanged(pos)
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
                .append("userId: \"${item!!.node!!.user!!.id}\", ")
                .append("notificationSetting: \"LIKE\", ")
                .append("data: {momentId:${item!!.node!!.pk}}")
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

    fun allusermoments1(width: Int, size: Int, i: Int, endCursors: String) {
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(
                    GetAllUserMomentsQuery(
                        width,
                        size,
                        10,
                        endCursors,
                        ""
                    )
                )
                    .execute()
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
                allUserMoments.addAll(allUserMomentsNext)
                sharedMomentAdapter.submitList1(allUserMoments)

                // sharedMomentAdapter.addAll(allUserMomentsNext)

            }


            if (binding.rvMoments.itemDecorationCount == 0) {
                binding.rvMoments.addItemDecoration(object : RecyclerView.ItemDecoration() {
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
        position: Int,
        item: GetAllUserMomentsQuery.Edge?
    ) {
        val bundle = Bundle()
        bundle.putString("momentID", item?.node!!.pk!!.toString())

        bundle.putString("filesUrl", item?.node!!.file!!)
        bundle.putString("Likes", item?.node!!.like!!.toString())
        bundle.putString("Comments", item?.node!!.comment!!.toString())
        val gson = Gson()
        bundle.putString("Desc", gson.toJson(item.node!!.momentDescriptionPaginated))
        if (item.node.user!!.gender != null) {
            bundle.putString("gender", item.node.user.gender!!.name)

        } else {
            bundle.putString("gender", null)

        }
        bundle.putString("fullnames", item.node!!.user!!.fullName)
        bundle.putString("momentuserID", item.node!!.user!!.id.toString())



        navController.navigate(R.id.momentsAddCommentFragment, bundle)
    }

    override fun onMomentGiftClick(position: Int, item: GetAllUserMomentsQuery.Edge?) {
//        var bundle = Bundle()
//        bundle.putString("userId", userId)
//        navController.navigate(R.id.action_userProfileFragment_to_userGiftsFragment,bundle)
    }


    override fun onDotMenuofMomentClick(
        position: Int,
        item: GetAllUserMomentsQuery.Edge?,
        types: String
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

                val positionss = allUserMoments.indexOf(item)
                allUserMoments.remove(item)
                sharedMomentAdapter.notifyItemRemoved(position)
            }
        } else if (types.equals("report")) {
            showProgressView()
            lifecycleScope.launchWhenResumed {
                val res = try {
                    apolloClient(
                        requireContext(),
                        userToken!!
                    ).mutation(
                        ReportonmomentMutation(
                            item?.node!!.pk!!.toString(),
                            "This is not valid post"
                        )
                    )
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponse ${e.message}")
                    binding.root.snackbar("Exception ${e.message}")
                    hideProgressView()
                    return@launchWhenResumed
                }

                hideProgressView()
            }
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


    override fun onMoreShareMomentClick() {

    }

    override fun onSharedMomentClick(position: Int, item: GetAllUserMomentsQuery.Edge?) {
    }


    override fun onUserStoryClick(position: Int, userStory: GetAllUserStoriesQuery.Edge?) {
    }

    override fun onAddNewUserStoryClick() {
    }
}