package com.i69app.ui.base

import android.app.Dialog
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.i69app.R
import com.i69app.data.config.Constants
import kotlinx.coroutines.flow.first
import com.i69app.data.preferences.UserPreferences
import com.i69app.databinding.ActivityMainBinding
import com.i69app.databinding.FragmentMessengerListBinding
import com.i69app.databinding.FragmentNewMessengerChatBinding
import com.i69app.firebasenotification.NotificationBroadcast
import com.i69app.singleton.App
import com.i69app.utils.createLoadingDialog
import com.i69app.utils.transact
import java.util.*

abstract class BaseActivity<dataBinding : ViewDataBinding> : AppCompatActivity() {

    protected lateinit var userPreferences: UserPreferences
    lateinit var binding: dataBinding
    protected lateinit var loadingDialog: Dialog



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val config = resources.configuration
//        val lang = "fr" // your language code
//        val locale = Locale(lang)
//        Locale.setDefault(locale)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1)
//            config.setLocale(locale)
//        else
//            config.locale = locale
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
//            createConfigurationContext(config)
//        resources.updateConfiguration(config, resources.displayMetrics)



        getSupportActionBar()?.hide();

        userPreferences = App.userPreferences
        binding = getActivityBinding(layoutInflater)
        setContentView(binding.root)
        binding.apply {
            lifecycleOwner = this@BaseActivity
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        loadingDialog = createLoadingDialog()
        setupTheme(savedInstanceState)
        setupClickListeners()


    }

    abstract fun getActivityBinding(inflater: LayoutInflater): dataBinding

    abstract fun setupTheme(savedInstanceState: Bundle?)

    abstract fun setupClickListeners()

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    protected fun showProgressView() {
        loadingDialog.show()
    }

    protected fun hideProgressView() {
        loadingDialog.dismiss()
    }
    suspend fun getCurrentUserName() = userPreferences.userName.first()

    suspend fun getCurrentUserId() = userPreferences.userId.first()

    suspend fun getCurrentUserToken() = userPreferences.userToken.first()

    suspend fun getChatUserId() = userPreferences.chatUserId.first()

    fun transact(fr: Fragment, addToBackStack: Boolean = false) = supportFragmentManager.transact(fr, addToBackStack)

}