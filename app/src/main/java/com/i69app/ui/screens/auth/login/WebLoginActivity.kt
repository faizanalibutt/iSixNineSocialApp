package com.i69app.ui.screens.auth.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.webkit.*
import com.i69app.data.config.Constants
import com.i69app.databinding.ActivityWebLoginBinding
import com.i69app.ui.base.BaseActivity
import timber.log.Timber
import java.net.URLDecoder

class WebLoginActivity : BaseActivity<ActivityWebLoginBinding>() {

    companion object {
        const val ARGS_ACCESS_TOKEN = "access_token"
        const val ARGS_ACCESS_VERIFIER = "access_verifier"
    }

    override fun getActivityBinding(inflater: LayoutInflater) = ActivityWebLoginBinding.inflate(inflater)

    override fun setupTheme(savedInstanceState: Bundle?) {
        loadingDialog.show()

        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
        }
        WebStorage.getInstance().deleteAllData()

        with(binding.webView) {
            clearHistory()
            clearFormData()
            clearMatches()
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            webViewClient = mWebViewClient
            webChromeClient = mWebChromeClient
            loadUrl(Constants.TWITTER_SIGN_IN_URL)
        }
    }

    override fun setupClickListeners() {
    }

    private val mWebViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            if (request?.url.toString().startsWith(Constants.TWITTER_CALLBACK_TEMP_URL)) {
                val decodedUrl = URLDecoder.decode(request?.url.toString(), "UTF-8")
                Timber.d("Decode Url $decodedUrl")

                if (decodedUrl.contains("oauth_token=")) {
                    val uri = Uri.parse(decodedUrl)
                    val accessToken = uri.getQueryParameter("oauth_token")
                    val accessVerifier = uri.getQueryParameter("oauth_verifier")
                    Timber.d("Access Token: $accessToken")

                    val intent = Intent()
                    intent.putExtra(ARGS_ACCESS_TOKEN, accessToken)
                    intent.putExtra(ARGS_ACCESS_VERIFIER, accessVerifier)
                    setResult(RESULT_OK, intent)
                    finish()
                }
                return true
            }
            return false
        }
    }

    private val mWebChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            if (newProgress == 100) loadingDialog.dismiss()
        }
    }

}