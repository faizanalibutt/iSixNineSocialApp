package com.i69app.gifts

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.apollographql.apollo3.exception.ApolloException
import com.i69app.GetSentGiftsQuery
import com.i69app.R
import com.i69app.databinding.FragmentReceivedSentGiftsBinding
import com.i69app.ui.adapters.AdapterSentGifts
import com.i69app.ui.base.BaseFragment
import com.i69app.ui.screens.SplashActivity
import com.i69app.ui.screens.main.search.userProfile.PicViewerFragment
import com.i69app.ui.screens.main.search.userProfile.SearchUserProfileFragment
import com.i69app.utils.AnimationTypes
import com.i69app.utils.apolloClient
import com.i69app.utils.navigate
import com.i69app.utils.snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import timber.log.Timber

class FragmentSentGifts: BaseFragment<FragmentReceivedSentGiftsBinding>(),AdapterSentGifts.SentGiftPicUserPicInterface {
    lateinit var giftsAdapter: AdapterSentGifts

    var list : MutableList<GetSentGiftsQuery.Edge?> = mutableListOf()

    private var userId: String? = null
    private var userToken: String? = null

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentReceivedSentGiftsBinding.inflate(inflater, container, false)

    override fun setupTheme() {

        lifecycleScope.launch {
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!
            Timber.i("usertokenn $userToken")
        }
        Timber.i("userID $userId")


        giftsAdapter = AdapterSentGifts(requireContext(),this@FragmentSentGifts, list)
        binding.recyclerViewGifts.setHasFixedSize(true)
        binding.recyclerViewGifts.adapter = giftsAdapter


        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(requireContext(), userToken!!).query(GetSentGiftsQuery(userId!!)).execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception received gift ${e.message}")
                hideProgressView()
                return@launchWhenResumed
            }

            if(res.hasErrors())
            {

                if(JSONObject(res.errors!!.get(0).toString()).getString("code").equals("InvalidOrExpiredToken"))
                {
                    // error("User doesn't exist")

                    lifecycleScope.launch(Dispatchers.Main) {
                        userPreferences.clear()
                        //App.userPreferences.saveUserIdToken("","","")
                        val intent = Intent(activity, SplashActivity::class.java)
                        startActivity(intent)
                        activity!!.finishAffinity()
                    }                       }
            }

            val allreceivedgift = res.data?.allUserGifts!!.edges
            if(allreceivedgift.size!=0)
            {
                list.clear()
                list.addAll(allreceivedgift)
                giftsAdapter?.notifyDataSetChanged()
            }

            if (binding.recyclerViewGifts.itemDecorationCount == 0) {
                binding.recyclerViewGifts.addItemDecoration(object : RecyclerView.ItemDecoration() {
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

    override fun setupClickListeners() {}
    override fun onpiclicked(url: String) {



        val dialog = PicViewerFragment()
        val b = Bundle()
        b.putString("url", url)
        b.putString("mediatype", "image")

        dialog.arguments = b
        dialog.show(childFragmentManager, "GiftpicViewer")

    }

    override fun onuserpiclicked(userid: String) {
        val bundle = Bundle()
        bundle.putBoolean(SearchUserProfileFragment.ARGS_FROM_CHAT, false)
        bundle.putString("userId", userid)
        findNavController().navigate(
            destinationId = R.id.action_global_otherUserProfileFragment,
            popUpFragId = null,
            animType = AnimationTypes.SLIDE_ANIM,
            inclusive = true,
            args = bundle
        )
    }
}