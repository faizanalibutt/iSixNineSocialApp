package com.i69app.gifts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.apollographql.apollo3.exception.ApolloException
import com.i69app.GetUserSendGiftQuery
import com.i69app.data.models.ModelGifts
import com.i69app.databinding.FragmentRealGiftsBinding
import com.i69app.ui.adapters.AdapterGifts
import com.i69app.ui.base.BaseFragment
import com.i69app.ui.viewModels.UserViewModel
import com.i69app.utils.apolloClient
import com.i69app.utils.snackbar
import timber.log.Timber

class FragmentSendGifts(val userId: String? = null) : BaseFragment<FragmentRealGiftsBinding>() {

    var giftsAdapter: AdapterGifts? = null
    var list: MutableList<ModelGifts.Data.AllRealGift> = mutableListOf()

    private val viewModel: UserViewModel by activityViewModels()
    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentRealGiftsBinding.inflate(inflater, container, false)

    override fun setupTheme() {
        giftsAdapter = AdapterGifts(requireContext(), list)
        binding.recyclerViewGifts.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recyclerViewGifts.setHasFixedSize(true)
        binding.recyclerViewGifts.adapter = giftsAdapter
        showProgressView()
        getSendGiftIndex()
    }

    private fun getSendGiftIndex() {
        lifecycleScope.launchWhenResumed {
            val res = try {
                apolloClient(
                    requireContext(),
                    userId!!
                ).query(GetUserSendGiftQuery(userId = userId!!))
                    .execute()
            } catch (e: ApolloException) {
                Timber.d("apolloResponse ${e.message}")
                binding.root.snackbar("Exception getGiftIndex ${e.message}")
                return@launchWhenResumed
            }
            Timber.d("apolloResponse getGiftIndex ${res.hasErrors()}")

            val sendGiftList = res.data?.allUserGifts?.edges
            res.data?.allUserGifts?.edges?.forEach { it ->
                Timber.d("apolloResponse getGiftIndex ${it?.node?.gift?.giftName}")
            }
            list.clear()
            sendGiftList?.forEach { edge ->
                edge?.node?.gift?.let {
                    list.add(
                        ModelGifts.Data.AllRealGift(
                            id = it.id,
                            cost = it.cost,
                            giftName = it.giftName,
                            picture = it.picture, type = it.type.rawValue
                        )
                    )
                }
            }
            giftsAdapter?.notifyDataSetChanged()
            hideProgressView()
        }
    }

    override fun setupClickListeners() {}
}