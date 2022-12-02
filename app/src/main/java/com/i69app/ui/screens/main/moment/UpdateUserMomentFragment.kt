package com.i69app.ui.screens.main.moment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo3.exception.ApolloException
import com.i69app.UpdateMomentMutation
import com.i69app.databinding.FragmentUpdateUserMomentBinding
import com.i69app.ui.base.BaseFragment
import com.i69app.ui.screens.main.MainActivity.Companion.getMainActivity
import com.i69app.utils.apolloClient
import com.i69app.utils.snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class UpdateUserMomentFragment : BaseFragment<FragmentUpdateUserMomentBinding>() {

    lateinit var desc: String
    var pk: Int = -1

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentUpdateUserMomentBinding =
        FragmentUpdateUserMomentBinding.inflate(inflater, container, false)

    override fun setupTheme() {

        desc = arguments?.getString("moment_desc") ?: ""
        pk = arguments?.getInt("moment_pk") ?: -1
        binding.editWhatsGoing.setText(desc)
    }

    override fun setupClickListeners() {
        binding.btnShareMoment.setOnClickListener {
            // call api
            showProgressView()
            lifecycleScope.launch(Dispatchers.Main) {
                try {
                    apolloClient(
                        requireContext(),
                        getCurrentUserToken()!!
                    ).mutation(UpdateMomentMutation(pk, binding.editWhatsGoing.text.toString()))
                        .execute()
                } catch (e: ApolloException) {
                    Timber.d("apolloResponse ${e.message}")
                    binding.root.snackbar("Exception momentUpdateDesc ${e.message}")
                    hideProgressView()
                    return@launch
                }
                hideProgressView()
                // update specific item in list
                moveUp()
            }
        }

        binding.toolbarHamburger.setOnClickListener {
            hideKeyboard(binding.root)
            binding.editWhatsGoing.clearFocus()
            getMainActivity()?.drawerSwitchState()
        }

    }
}