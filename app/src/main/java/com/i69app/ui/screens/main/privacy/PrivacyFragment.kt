package com.i69app.ui.screens.main.privacy

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.Window
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.i69app.R
import com.i69app.databinding.FragmentPrivacyBinding
import com.i69app.databinding.FragmentPurchaseBinding
import com.i69app.ui.base.BaseFragment
import com.i69app.ui.screens.main.MainActivity.Companion.getMainActivity
import com.i69app.ui.screens.main.notification.NotificationDialogFragment
import com.i69app.utils.startEmailIntent
import kotlinx.coroutines.launch
import timber.log.Timber

class PrivacyFragment:BaseFragment<FragmentPrivacyBinding>() {
    private lateinit var mWebView: WebView
    private var userToken: String? = null
    private var userId: String? = null
    private var url: String? = null

    override fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?) = FragmentPrivacyBinding.inflate(inflater, container, false)

    override fun setupTheme() {
        //requireActivity().window.requestFeature(Window.FEATURE_NO_TITLE)
        lifecycleScope.launch {
            userId = getCurrentUserId()!!
            userToken = getCurrentUserToken()!!
            Timber.i("usertokenn $userToken")
        }
        if (getMainActivity()?.pref?.getString("typeview","privacy").equals("privacy")){
            url=com.i69app.data.config.Constants.URL_PRIVACY_POLICY
        }else{
            url=com.i69app.data.config.Constants.URL_TERMS_AND_CONDITION
        }
        //mWebView = WebView(requireContext())
        binding.privacyWebView.loadUrl(url!!)
        binding.privacyWebView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }
        }

        //requireActivity().setContentView(mWebView)
    }

    override fun setupClickListeners() {
        binding.toolbarHamburger.setOnClickListener {
            getMainActivity()?.drawerSwitchState()
        }
        binding.toolbarLogo.setOnClickListener {
            findNavController().popBackStack()
        }


        binding.bell.setOnClickListener {
            val dialog = NotificationDialogFragment(
                userToken,
                binding.counter,
                userId,
                binding.bell
            )
            dialog.show(childFragmentManager, "notifications")
        }
    }


}