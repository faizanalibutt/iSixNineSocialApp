package com.i69app.ui.screens.main.search.result

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView.OnEditorActionListener
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.i69app.R
import com.i69app.data.models.User
import com.i69app.data.remote.requests.SearchRequestNew
import com.i69app.databinding.FragmentPageSearchResultBinding
import com.i69app.ui.adapters.UsersSearchListAdapter
import com.i69app.ui.base.BaseFragment
import com.i69app.ui.screens.main.MainActivity.Companion.getMainActivity
import com.i69app.ui.screens.main.search.userProfile.SearchUserProfileFragment
import com.i69app.ui.viewModels.SearchViewModel
import com.i69app.utils.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import timber.log.Timber


@AndroidEntryPoint
class PageSearchResultFragment : BaseFragment<FragmentPageSearchResultBinding>(), UsersSearchListAdapter.UserSearchListener {
    private var userToken: String? = null
    private var userId: String? = null
    companion object {
        private const val ARG_DATA_BY_PAGE_ID = "ARG_PAGE_ID"

        fun newInstance(page: Int): PageSearchResultFragment {
            val args = Bundle()
            args.putInt(ARG_DATA_BY_PAGE_ID, page)
            val fragment = PageSearchResultFragment()
            fragment.arguments = args
            return fragment
        }
    }

    private var mPage: Int = 0
    private val mViewModel: SearchViewModel by activityViewModels()
    private lateinit var usersAdapter: UsersSearchListAdapter


    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentPageSearchResultBinding.inflate(inflater, container, false)

    override fun setupTheme() {

        mPage = requireArguments().getInt(ARG_DATA_BY_PAGE_ID)
        navController = findNavController()
        initSearch()

        lifecycleScope.launch {
            val userToken = getCurrentUserToken()!!

            mViewModel.getDefaultPickers(userToken).observe(viewLifecycleOwner, { pickers ->
                pickers?.let { picker ->
                    usersAdapter = UsersSearchListAdapter(this@PageSearchResultFragment, picker)
                    binding.usersRecyclerView.adapter = usersAdapter

                    val users: ArrayList<User> = when (mPage) {
                        0 -> mViewModel.getRandomUsers()

                        1 -> mViewModel.getPopularUsers()

                        else -> mViewModel.getMostActiveUsers()
                    }

                    if (users.isNullOrEmpty()) {
                        binding.noUsersLabel.setViewVisible()

                    } else {
                        binding.noUsersLabel.setViewGone()
                        usersAdapter.updateItems(users)
                    }
                }
            })
        }
    }

    override fun setupClickListeners() {
    }

    private fun initSearch() {

        binding.keyInput.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                binding.keyInput.hideKeyboard()
                lifecycleScope.launch {
                    userToken = getCurrentUserToken()!!
                    userId = getCurrentUserId()!!

                    Timber.i("usertokenn $userToken")
                }

                val searchRequest = SearchRequestNew(

                    name = binding.keyInput.text.toString()

                )

                Log.e("search params", Gson().toJson(searchRequest))
                mViewModel.getSearchUsersTemp(
                    _searchRequest = searchRequest,
                    token = userToken!!
                ) { error ->
                    if (error == null) {
                        hideProgressView()

                        if (mViewModel.getRandomUsersSearched().isNullOrEmpty()) {
                            binding.noUsersLabel.setViewVisible()

                        } else {
                            binding.noUsersLabel.setViewGone()

                        }
                        usersAdapter.updateItems(mViewModel.getRandomUsersSearched())
                        //  navController.navigate(R.id.action_searchFiltersFragment_to_searchResultFragment)
                    } else {
                        hideProgressView()
                        //binding.root.snackbar(error)
                    }
                }
                return@OnEditorActionListener true
            }
            false
        })

        binding.interestsIcon.setOnClickListener {


            if(binding.keyInput.text.toString().length>0)
            {
                binding.keyInput.hideKeyboard()
                lifecycleScope.launch {
                    userToken = getCurrentUserToken()!!
                    userId = getCurrentUserId()!!

                    Timber.i("usertokenn $userToken")
                }

                val searchRequest = SearchRequestNew(

                    name = binding.keyInput.text.toString()

                )

                Log.e("search params", Gson().toJson(searchRequest))
                mViewModel.getSearchUsersTemp(
                    _searchRequest = searchRequest,
                    token = userToken!!
                ) { error ->
                    if (error == null) {
                        hideProgressView()

                        if (mViewModel.getRandomUsersSearched().isNullOrEmpty()) {
                            binding.noUsersLabel.setViewVisible()

                        } else {
                            binding.noUsersLabel.setViewGone()

                        }
                        usersAdapter.updateItems(mViewModel.getRandomUsersSearched())
                        //  navController.navigate(R.id.action_searchFiltersFragment_to_searchResultFragment)
                    } else {
                        hideProgressView()
                        //binding.root.snackbar(error)
                    }
                }
            }
            else
            {
                binding.keyInput.requestFocus()
                binding.keyInput.showKeyboard()
            }


        }

        binding.keyInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val filteredUsers = when (mPage) {
                    0 ->
                    {
                        val searchKey: String = binding.keyInput.text.toString()
                        if(searchKey.length==0)
                        {
                            binding.keyInput.hideKeyboard()
                            Log.e("iff","iff")
                            if (mViewModel.getRandomUsers().isNullOrEmpty()) {
                                binding.noUsersLabel.setViewVisible()

                            } else {
                                binding.noUsersLabel.setViewGone()

                            }
                            usersAdapter.updateItems(mViewModel.getRandomUsers())
                        }
                        else
                        {

                  /*      lifecycleScope.launch {
                            userToken = getCurrentUserToken()!!
                            userId = getCurrentUserId()!!

                            Timber.i("usertokenn $userToken")
                        }

                        val searchRequest = SearchRequestNew(

                            name = searchKey

                        )

                        Log.e("search params", Gson().toJson(searchRequest))
                        mViewModel.getSearchUsersTemp(
                            _searchRequest = searchRequest,
                            token = userToken!!
                        ) { error ->
                            if (error == null) {
                                hideProgressView()
                                usersAdapter.updateItems(mViewModel.getRandomUsersSearched())
                              //  navController.navigate(R.id.action_searchFiltersFragment_to_searchResultFragment)
                            } else {
                                hideProgressView()
                                //binding.root.snackbar(error)
                            }
                        }*/
                    }

                    }
                    1 ->
                    {
                       // filterUsers(s.toString(), mViewModel.getPopularUsers())
                        val searchKey: String = binding.keyInput.text.toString()
                        if(searchKey.length==0)
                        {
                            if (mViewModel.getPopularUsers().isNullOrEmpty()) {
                                binding.noUsersLabel.setViewVisible()

                            } else {
                                binding.noUsersLabel.setViewGone()

                            }
                            usersAdapter.updateItems(mViewModel.getPopularUsers())
                        }
                        else
                        {

                       /*     lifecycleScope.launch {
                                userToken = getCurrentUserToken()!!
                                userId = getCurrentUserId()!!

                                Timber.i("usertokenn $userToken")
                            }

                            val searchRequest = SearchRequestNew(

                                name = searchKey

                            )

                            Log.e("search params", Gson().toJson(searchRequest))
                            mViewModel.getSearchUsersTemp(
                                _searchRequest = searchRequest,
                                token = userToken!!
                            ) { error ->
                                if (error == null) {
                                    hideProgressView()
                                    usersAdapter.updateItems(mViewModel.getRandomUsersSearched())
                                    //  navController.navigate(R.id.action_searchFiltersFragment_to_searchResultFragment)
                                } else {
                                    hideProgressView()
                                  //  binding.root.snackbar(error)
                                }
                            }*/
                        }
                    }
                    2 -> {

                      //  filterUsers(s.toString(), mViewModel.getMostActiveUsers())
                        val searchKey: String = binding.keyInput.text.toString()
                        if(searchKey.length==0)
                        {
                            if (mViewModel.getMostActiveUsers().isNullOrEmpty()) {
                                binding.noUsersLabel.setViewVisible()

                            } else {
                                binding.noUsersLabel.setViewGone()

                            }
                            usersAdapter.updateItems(mViewModel.getMostActiveUsers())
                        }
                        else
                        {

                     /*       lifecycleScope.launch {
                                userToken = getCurrentUserToken()!!
                                userId = getCurrentUserId()!!

                                Timber.i("usertokenn $userToken")
                            }

                            val searchRequest = SearchRequestNew(

                                name = searchKey

                            )

                            Log.e("search params", Gson().toJson(searchRequest))
                            mViewModel.getSearchUsersTemp(
                                _searchRequest = searchRequest,
                                token = userToken!!
                            ) { error ->
                                if (error == null) {
                                    hideProgressView()
                                    usersAdapter.updateItems(mViewModel.getRandomUsersSearched())
                                    //  navController.navigate(R.id.action_searchFiltersFragment_to_searchResultFragment)
                                } else {
                                    hideProgressView()
                                   // binding.root.snackbar(error)
                                }
                            }*/
                        }
                    }
                    else -> mViewModel.getRandomUsers()
                }

                /*  val filteredUsers = when (mPage) {
                    0 -> filterUsers(s.toString(), mViewModel.getRandomUsers())
                    1 -> filterUsers(s.toString(), mViewModel.getPopularUsers())
                    2 -> filterUsers(s.toString(), mViewModel.getMostActiveUsers())
                    else -> arrayListOf()
                }*/

            }

            override fun afterTextChanged(s: Editable?) {

            }
        })
    }

    private fun filterUsers(text: String, fullListOfUsers: List<User>): List<User> {
        return if (text.trim().isEmpty()) {
            fullListOfUsers

        } else {

            val filteredList: ArrayList<User> = ArrayList()
            fullListOfUsers.forEach {
                if (it.fullName.lowercase().contains(text.lowercase())) filteredList.add(it)
            }
            filteredList
        }
    }

    override fun onItemClick(position: Int, user: User) {
        mViewModel.selectedUser.value = user
        var bundle = Bundle()
        bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
        bundle.putString("userId", user.id)
        if (userId==user.id){
            getMainActivity()?.binding?.bottomNavigation?.selectedItemId = R.id.nav_user_profile_graph

        }else {
            navController.navigate(
                destinationId = R.id.action_global_otherUserProfileFragment,
                popUpFragId = null,
                animType = AnimationTypes.SLIDE_ANIM,
                inclusive = true,
                args = bundle
            )
        }
    }

    override fun onUnlockFeatureClick() {
        navController.navigate(R.id.actionGoToPurchaseFragment)
    }


}