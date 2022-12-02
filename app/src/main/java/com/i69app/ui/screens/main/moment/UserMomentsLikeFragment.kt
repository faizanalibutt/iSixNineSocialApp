package com.i69app.ui.screens.main.moment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.apollographql.apollo3.exception.ApolloException
import com.i69app.GetSelfMomentLikesQuery
import com.i69app.GetUserMomentsQuery
import com.i69app.R
import com.i69app.databinding.FragmentUserMomentsLikeBinding
import com.i69app.ui.adapters.CurrentUserMomentLikesAdapter
import com.i69app.ui.base.BaseFragment
import com.i69app.ui.screens.main.MainActivity
import com.i69app.ui.screens.main.search.userProfile.SearchUserProfileFragment
import com.i69app.utils.AnimationTypes
import com.i69app.utils.apolloClient
import com.i69app.utils.navigate
import com.i69app.utils.snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class UserMomentsLikeFragment : BaseFragment<FragmentUserMomentsLikeBinding>(),
    CurrentUserMomentLikesAdapter.CurrentUserLikesUsers {

    private var userId: String? = null
    private var userToken: String? = null
    private lateinit var currentUserLikeMomentAdapter: CurrentUserMomentLikesAdapter
    var allUserMomentsLike: ArrayList<GetSelfMomentLikesQuery.SelfMomentLike> = ArrayList()
    var layoutManager: LinearLayoutManager? = null

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentUserMomentsLikeBinding =
        FragmentUserMomentsLikeBinding.inflate(inflater, container, false)

    override fun setupTheme() {

        val momentPk = arguments?.getInt("user_id")

        if (momentPk == -1)
        {
            binding.root.snackbar("invalid moement!")
            return
        }

        navController = findNavController()
        allUserMomentsLike = ArrayList()

        currentUserLikeMomentAdapter = CurrentUserMomentLikesAdapter(
            requireActivity(),
            this,
            allUserMomentsLike,
            userId
        )
        layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        binding.rvMomentsLike.layoutManager = layoutManager

        lifecycleScope.launch {
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()
            Timber.i("userToken $userToken")
        }

        // call api fetch list
        showProgressView()
        lifecycleScope.launch(Dispatchers.Main) {
            try {
                // write query in config file first.
                apolloClient(requireContext(), userToken!!)
                    .query(GetSelfMomentLikesQuery(momentPk = momentPk ?: -1))
                    .toFlow()
                    .collect { totalLikes ->
                        if (totalLikes.hasErrors()) {
                            Timber.d(
                                "self Likes= response error = ${totalLikes.errors?.get(0)?.message}"
                            )
                        } else {
                            hideProgressView()

                            // put adapter here
                            val allUserLikeMoments = totalLikes.data?.selfMomentLikes!!

                            if (allUserLikeMoments.isNotEmpty()) {
                                currentUserLikeMomentAdapter.addAll(allUserLikeMoments)
                                binding.rvMomentsLike.adapter = currentUserLikeMomentAdapter
                            } else {
                                binding.root.snackbar("Don't worry you will get Likes")
                            }

                            Timber.tag("SelfLikes").d(
                                "self Likes= response data ${totalLikes.data?.selfMomentLikes.toString()}"
                            )
                        }
                    }

            } catch (exp: ApolloException) {
                Timber.d("self Likes= ${exp.message}")
            }
        }
    }

    override fun setupClickListeners() {
        binding.toolbarHamburger.setOnClickListener {
            hideKeyboard(binding.root)
            MainActivity.getMainActivity()?.drawerSwitchState()
        }
    }

    override fun openLikedUserProfile(id: String?) {
        val bundle = Bundle()
        bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
        bundle.putString("userId", id)
        findNavController().navigate(
            destinationId = R.id.action_global_otherUserProfileFragment,
            popUpFragId = null,
            animType = AnimationTypes.SLIDE_ANIM,
            inclusive = true,
            args = bundle
        )
    }

}