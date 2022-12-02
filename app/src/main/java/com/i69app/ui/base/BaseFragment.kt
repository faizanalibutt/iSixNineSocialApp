package com.i69app.ui.base

import android.app.Activity
import android.app.Dialog
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.i69app.R
import com.i69app.data.config.Constants
import com.i69app.data.preferences.UserPreferences
import com.i69app.firebasenotification.NotificationBroadcast
import com.i69app.singleton.App
import com.i69app.utils.createLoadingDialog
import kotlinx.coroutines.flow.first


abstract class BaseFragment<dataBinding : ViewDataBinding> : Fragment() {


    protected lateinit var userPreferences: UserPreferences
    protected lateinit var binding: dataBinding
    private lateinit var loadingDialog: Dialog
    lateinit var navController: NavController
    private var broadcast: NotificationBroadcast? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        userPreferences = App.userPreferences
        setStatusBarColor(getStatusBarColor())
        binding = getFragmentBinding(inflater, container)
        val contentView = binding.root
        broadcast = NotificationBroadcast(this);
        contentView.findViewById<View>(R.id.actionBack)?.setOnClickListener {
             //activity?.onBackPressed()
            //activity?.finishAfterTransition()
            //getMainActivity()?.binding?.bottomNavigation?.selectedItemId= R.id.nav_search_graph
            findNavController().popBackStack()

        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingDialog = requireContext().createLoadingDialog()
        binding.apply {
            lifecycleOwner = this@BaseFragment
        }

         setupTheme()
         setupClickListeners()
    }

    abstract fun getFragmentBinding(inflater: LayoutInflater, container: ViewGroup?): dataBinding

    abstract fun setupTheme()

    abstract fun setupClickListeners()

    suspend fun getCurrentUserId() = userPreferences.userId.first()

    suspend fun getCurrentUserName() = userPreferences.userName.first()

    suspend fun getCurrentUserToken() = userPreferences.userToken.first()

    suspend fun getChatUserId() = userPreferences.chatUserId.first()

    open fun getStatusBarColor() = R.color.colorPrimaryDark

    fun setStatusBarColor(@ColorRes color: Int) {
        val window = requireActivity().window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = ContextCompat.getColor(requireActivity(), color)
    }

    protected fun showProgressView() {
        loadingDialog.show()
    }

    protected fun hideProgressView() {
        if (loadingDialog != null) {
            loadingDialog.dismiss()
        }
    }

    protected fun <T : Activity> getTypeActivity(): T? {
        return if (activity != null) activity as T else null
    }

    fun moveTo(direction: Int, args: Bundle? = null) =
        view?.findNavController()?.navigate(direction, args)

    fun moveTo(direction: NavDirections) = view?.findNavController()?.navigate(direction)

    fun moveUp() = view?.findNavController()?.navigateUp()
    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(requireActivity()).unregisterReceiver(broadcast!!);
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(requireActivity()).registerReceiver(
            broadcast!!, IntentFilter(Constants.INTENTACTION)
        );
    }

    protected fun hideKeyboard(view: View) =
        (requireContext().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE)
                as? InputMethodManager)?.hideSoftInputFromWindow(view.windowToken, 0)

}