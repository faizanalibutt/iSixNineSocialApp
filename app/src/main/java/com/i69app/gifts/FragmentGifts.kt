package com.i69app.gifts

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.exception.ApolloException
import com.i69app.GiftPurchaseMutation
import com.i69app.data.models.ModelGifts
import com.i69app.databinding.FragmentGiftsBinding
import com.i69app.di.modules.AppModule
import com.i69app.ui.base.profile.BaseGiftsFragment
import com.i69app.utils.apolloClient
import com.i69app.utils.getResponse
import com.i69app.utils.snackbar
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class FragmentGifts: BaseGiftsFragment() {

    var purchaseGiftFor: String?=""

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentGiftsBinding.inflate(inflater, container, false)

    override fun setupClickListeners() {
        binding.purchaseButton.setOnClickListener {
            var items: MutableList<ModelGifts.Data.AllRealGift> = mutableListOf()
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
                                getCurrentUserToken()!!
                            ).mutation(GiftPurchaseMutation(gift.id, purchaseGiftFor!!)).execute()
                        } catch (e: ApolloException) {
                            Timber.d("apolloResponse ${e.message}")
                            binding.root.snackbar("Exception ${e.message}")
                            //hideProgressView()
                            //return@launchWhenResumed
                        }
                        if (res?.hasErrors() == false) {
                            binding.root.snackbar("You bought ${res.data?.giftPurchase?.giftPurchase?.gift?.giftName} successfully!")
                            //fireGiftBuyNotificationforreceiver(gift.id)

                        }
                        if(res!!.hasErrors())
                        {
                            binding.root.snackbar(""+ res.errors!![0].message)

                        }
                        Timber.d("apolloResponse ${res?.hasErrors()} ${res?.data?.giftPurchase?.giftPurchase?.gift?.giftName}")
                    }
                    hideProgressView()
                }
            }
        }
    }
    fun fireGiftBuyNotificationforreceiver(gid: String) {

        lifecycleScope.launchWhenResumed {


            val queryName = "sendNotification"
            val query = StringBuilder()
                .append("mutation {")
                .append("$queryName (")
                .append("userId: \"${purchaseGiftFor!!}\", ")
                .append("notificationSetting: \"GIFT\", ")
                .append("data: {giftId:${gid}}")
                .append(") {")
                .append("sent")
                .append("}")
                .append("}")
                .toString()

            val result= AppModule.provideGraphqlApi().getResponse<Boolean>(
                query,
                queryName, getCurrentUserToken()!!)
            Timber.d("RSLT",""+result.message)

        }








    }
    override fun setupScreen() {
        purchaseGiftFor = requireArguments().getString("userId")
    }
}