package com.i69app.ui.screens.main.moment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.apollographql.apollo3.api.DefaultUpload
import com.apollographql.apollo3.api.content
import com.apollographql.apollo3.exception.ApolloException
import com.google.gson.Gson
import com.i69app.BuildConfig
import com.i69app.MomentMutation
import com.i69app.R
import com.i69app.data.models.User
import com.i69app.databinding.FragmentNewUserMomentBinding
import com.i69app.singleton.App
import com.i69app.ui.base.BaseFragment
import com.i69app.ui.screens.ImagePickerActivity
import com.i69app.ui.screens.SplashActivity
import com.i69app.ui.screens.main.MainActivity
import com.i69app.ui.viewModels.UserViewModel
import com.i69app.utils.*
import com.i69app.utils.KeyboardUtils.SoftKeyboardToggleListener
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File


class NewUserMomentFragment : BaseFragment<FragmentNewUserMomentBinding>() {

    private val viewModel: UserViewModel by activityViewModels()
    private var mFilePath : String? = null
    protected var mUser: User? = null

    private val photosLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
        val data = activityResult.data
        if (activityResult.resultCode == Activity.RESULT_OK) {
            mFilePath = data?.getStringExtra("result")
            Timber.d("fileBase64 $mFilePath")
            val uri=Uri.parse(mFilePath)
            //binding.cropView.setUri(uri);
           /* CropLayout.addOnCropListener(object : OnCropListener {
                override fun onSuccess(bitmap: Bitmap) {
                    // do somethhing with bitmap.
                }

                override fun onFailure(e: Exception) {
                    // do error handling.
                }
            })*/

            //binding.cropView.isOffFrame() // optionally check if the image is off the frame.

            //binding.cropView.crop()

            binding.imgUploadFile.loadCircleImage(mFilePath!!)
        }
    }

    override fun getFragmentBinding(
        inflater: LayoutInflater,
        container: ViewGroup?) = FragmentNewUserMomentBinding.inflate(inflater, container, false)

    @SuppressLint("ClickableViewAccessibility")
    override fun setupTheme() {
        binding.btnSelectFileToUpload.setOnClickListener {
            val intent = Intent(requireActivity(), ImagePickerActivity::class.java)
            photosLauncher.launch(intent)
        }

        /*binding.editWhatsGoing.setOnTouchListener(OnTouchListener { _, event ->

            val DRAWABLE_RIGHT = 2
           
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= binding.editWhatsGoing.getRight() - binding.editWhatsGoing.getCompoundDrawables()
                        .get(DRAWABLE_RIGHT).getBounds().width()
                ) {
                    binding.editWhatsGoing.clearFocus()
                    hideKeyboard(binding.root);
//                    Toast.makeText(requireContext(), "Hello", Toast.LENGTH_SHORT).show()
                    // your action here
                    return@OnTouchListener true
                }
            }
            false
        })*/

        binding.editWhatsGoing.setOnTouchListener(object : DrawableClickListener.BottomDrawableClickListener(binding.editWhatsGoing) {
            override fun onDrawableClick(): Boolean {
                binding.editWhatsGoing.clearFocus()
                hideKeyboard(binding.root);
                return true
            }
        })
        
        KeyboardUtils.addKeyboardToggleListener(requireActivity(),
            SoftKeyboardToggleListener { isVisible: Boolean -> binding.lblPostingMomentTip.setVisibility(if (isVisible) GONE else VISIBLE) })

        binding.btnShareMoment.setOnClickListener {
            if (mFilePath == null) {
                binding.root.snackbar(getString(R.string.you_cant_share_moment))
                return@setOnClickListener
            }
            showProgressView()

            val description = binding.editWhatsGoing.text.toString()
            /*val fileBase64 = BitmapFactory.decodeFile(mFilePath).convertBitmapToString()
            lifecycleScope.launch(Dispatchers.Main) {
                val userToken = getCurrentUserToken()!!
                Timber.d("fileBase64 [${fileBase64.length}] $mFilePath")
                when (val response = mViewModel.shareUserMoment("Moment Image", fileBase64, description, token = userToken)) {
                    is Resource.Success -> {
                        hideProgressView()

                    }
                    is Resource.Error -> onFailureListener(response.message ?: "")
                }
            }*/
            Timber.d("filee $mFilePath")

            lifecycleScope.launchWhenCreated {

                val f = File(mFilePath)
                val buildder = DefaultUpload.Builder()
                buildder.contentType("Content-Disposition: form-data;")
                buildder.fileName(f.name)
                val upload = buildder.content(f).build()
                Timber.d("filee ${f.exists()}")
                val userToken = getCurrentUserToken()!!

                Timber.d("useriddd ${mUser?.id}")
                if (mUser?.id != null) {
                    val response = try {
                        apolloClient(context = requireContext(), token = userToken).mutation(
                            MomentMutation(
                                file = upload,
                                detail = description,
                                userField = mUser?.id!!
                            )
                        ).execute()




                    } catch (e: ApolloException) {
                        hideProgressView()
                        Timber.d("filee Apollo Exception ${e.message}")
                        binding.root.snackbar("ApolloException ${e.message}")
                        return@launchWhenCreated
                    } catch (e: Exception) {
                        hideProgressView()

                        Timber.d("filee General Exception ${e.message} $userToken")
                        binding.root.snackbar("Exception ${e.message}")
                        return@launchWhenCreated
                    }
                    Log.e("222","--->"+Gson().toJson(response))
                    hideProgressView()

                    if(response.hasErrors())
                    {
                        Log.e("ddd1dddww","-->"+response.errors!!.get(0).nonStandardFields!!.get("code"))
                        if(response.errors!!.get(0).nonStandardFields!!.get("code").toString().equals("InvalidOrExpiredToken"))
                        {
                            // error("User doesn't exist")

                          if(activity!=null)
                            {
                                App.userPreferences.clear()
                                //App.userPreferences.saveUserIdToken("","","")
                                val intent = Intent(MainActivity.mContext, SplashActivity::class.java)
                                startActivity(intent)
                                Log.e("ddd1dddwwds","-->"+response.errors!!.get(0).nonStandardFields!!.get("code"))
                                activity?.finishAffinity()
                            }

                        }
                    }
                    else {
                        //val bundle = Bundle()
                        //bundle.putString("fromUser", "1")
                        //findNavController().navigate(R.id.newActionHome,bundle)
                        binding.editWhatsGoing.text=null
                        getMainActivity().mViewModelUser.userMomentsList.clear()
                        getMainActivity().openUserMoments()

                    }

                } else {

                    binding.root.snackbar("username is null")
                    binding.root.snackbar("Exception ${mUser?.id}")
                }
                hideProgressView()
                //binding.root.snackbar("Exception (${response.hasErrors()}) ${response.data?.insertMoment?.moment?.momentDescription}")
                //Timber.d("filee response = (${response.hasErrors()}) ${response.data?.insertMoment?.moment?.momentDescription}")
                //Timber.d("filee response = (${response.hasErrors()}) [${response.errors?.get(0)?.message}] ${response.data?.insertMoment?.moment?.momentDescription}")
                //filee response = com.apollographql.apollo3.api.ApolloResponse@3f798dc
            }
        }

        showProgressView()
        lifecycleScope.launch {
            val userId = getCurrentUserId()!!
            val token = getCurrentUserToken()!!
            viewModel.getCurrentUser(userId, token = token, false).observe(viewLifecycleOwner) { user ->
                user?.let {
                    mUser = it.copy()
                    Timber.d("Userrname ${mUser?.username}")

                    if(mUser != null)
                    {

                            if(mUser!!.avatarPhotos != null && mUser!!.avatarPhotos!!.size != 0)
                            {

                                if (mUser!!.avatarPhotos!!.size!=0)
                                {
                                    binding.imgCurrentUser.loadCircleImage(mUser!!.avatarPhotos!!.get(mUser!!.avatarIndex!!).url.replace("http://95.216.208.1:8000/media/","${BuildConfig.BASE_URL}media/"))

                                }

                            }
                    }

                }
                hideProgressView()
            }
        }
    }

    override fun setupClickListeners() {
        binding.toolbarHamburger.setOnClickListener {
            hideKeyboard(binding.root)
            binding.editWhatsGoing.clearFocus()
            getMainActivity().drawerSwitchState()
        }

    }

    private fun onFailureListener(error: String) {
        hideProgressView()
        Timber.e("${getString(R.string.something_went_wrong)} $error")
        binding.root.snackbar("${getString(R.string.something_went_wrong)} $error")
    }

    fun getMainActivity() = activity as MainActivity
}