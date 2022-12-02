package com.i69app.ui.screens.main.search.userProfile

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.DialogFragment
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.slider.RangeSlider
import com.i69app.R
import com.i69app.utils.CountDownTimerExt
import com.i69app.utils.createLoadingDialog
import com.i69app.utils.loadImage
import timber.log.Timber


class PicViewerFragment : DialogFragment() {

    private lateinit var views: View
    private lateinit var loadingDialog: Dialog


    private var timer1: CountDownTimerExt? = null

    var exoPlayer: SimpleExoPlayer? = null
    var videoDisable: View? = null
    var videoview: PlayerView? = null
    var videoLayout: RelativeLayout? = null

    lateinit var progressBar1: RangeSlider
    lateinit var imgplay: AppCompatImageView
    lateinit var progress_bar: CircularProgressIndicator

    override fun getTheme(): Int {
        return R.style.DialogTheme
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        views = inflater.inflate(R.layout.fragment_pic_viewer, container, false)

        loadingDialog = requireActivity().createLoadingDialog()
        videoview = views.findViewById<PlayerView>(com.i69app.R.id.videoview)
        videoLayout = views.findViewById<RelativeLayout>(com.i69app.R.id.videoLayout)
        videoDisable = views.findViewById<View>(com.i69app.R.id.videoDisable)
        imgplay = views.findViewById<AppCompatImageView>(com.i69app.R.id.img_play)
        progressBar1 = views.findViewById<RangeSlider>(R.id.progressBar1)
        val imgUserStory = views.findViewById<ImageView>(R.id.imgUserStory)
        val img_close = views.findViewById<ImageView>(com.i69app.R.id.img_close)
        progress_bar = views.findViewById<CircularProgressIndicator>(com.i69app.R.id.progress_bar)

        val type = arguments?.getString("mediatype")
        val url = arguments?.getString("url", "")

        progressBar1.setOnTouchListener(OnTouchListener { v, event -> true })

        if (type.equals("image")) {
            imgUserStory.visibility = View.VISIBLE
            imgplay.visibility = View.GONE
            videoLayout!!.visibility = View.GONE
            if (!url.equals("")) {
                imgUserStory.loadImage(url!!)

            } else {
                imgUserStory.loadImage(R.drawable.ic_default_user)
            }
        } else if (type.equals("video")) {
            videoview!!.visibility = View.VISIBLE
            videoLayout!!.visibility = View.VISIBLE
            progressBar1.visibility = View.VISIBLE
            imgplay.visibility = View.GONE
            imgplay.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_pause_circle_outline))

            val uri = Uri.parse(url)

            initExoPlayer(uri)

        }

        img_close.setOnClickListener(View.OnClickListener {
            releasePlayer()
            dismiss()
        })

        videoDisable!!.setOnClickListener(View.OnClickListener {
            if (exoPlayer != null) {
                if (exoPlayer!!.isPlaying) {
                    imgplay.visibility = View.VISIBLE
                    imgplay.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_play_circle_outline_24))
                    exoPlayer!!.pause()
                    timer1!!.pause()
                } else {
                    imgplay.visibility = View.GONE
                    imgplay.setImageDrawable(resources.getDrawable(R.drawable.ic_baseline_pause_circle_outline))
                    exoPlayer!!.play()
                    timer1!!.start()
                }
            }
        })


        return views
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("onDestroyView")
        releasePlayer()
        dismiss()
    }


    protected fun showProgressView() {
        loadingDialog.show()
    }

    protected fun hideProgressView() {
        loadingDialog.dismiss()
    }

    var isFirst: Boolean = false

    fun initExoPlayer(videouri: Uri?) {

        // showProgressView()
        progress_bar.visibility=View.VISIBLE

        exoPlayer = SimpleExoPlayer.Builder(requireActivity()).build()
        videoview!!.setPlayer(exoPlayer)
        val mediaItem = MediaItem.fromUri(videouri!!)
        exoPlayer!!.setMediaItem(mediaItem)
        exoPlayer!!.prepare()
        exoPlayer!!.play()

        var durationSet = false

        exoPlayer!!.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == ExoPlayer.STATE_READY && !durationSet) {

                    //    hideProgressView()
                    progress_bar.visibility=View.GONE

                    if (!isFirst) {
                        isFirst = true
                        val realDurationMillis: Long = exoPlayer!!.getDuration()
                        progressBar1!!.valueFrom = 0.0f
                        progressBar1!!.valueTo = realDurationMillis.toFloat()

                        val millisInFuture = realDurationMillis
                        timer1 = object : CountDownTimerExt(millisInFuture, 100) {
                            override fun onTimerTick(millisUntilFinished: Long) {

                                val diffTime: Long = millisInFuture - millisUntilFinished
                                val elepsedTime: Long = 0 + diffTime

                                Log.d("MainActivity!!", "onTimerTick $millisUntilFinished")
                                onTickProgressUpdate(elepsedTime)
                            }

                            override fun onTimerFinish() {
                                Log.d("MainActivity!", "onTimerFinish")
                                dismiss()
                            }
                        }
                        timer1!!.start()
                    } else {
                        if (exoPlayer!!.isPlaying) {
                            timer1!!.pause()
                        } else {
                            timer1!!.start()
                        }
                    }

                }
            }
        })
    }

    private fun playView(mediaItem: MediaItem) {

        /* showProgressView()

         exoPlayer = SimpleExoPlayer.Builder(requireActivity()).build().apply {
             playWhenReady = isPlayerPlaying
             seekTo(currentWindow, playbackPosition)
             setMediaItem(mediaItem, false)
             prepare()
         }
         player_view!!.player = exoPlayer
         var durationSet = false

         exoPlayer!!.addListener(object : Player.Listener {
             override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                 if (playbackState == ExoPlayer.STATE_READY && !durationSet) {

                     hideProgressView()

                     if (!isFirst) {
                         isFirst = true
                         val realDurationMillis: Long = exoPlayer!!.getDuration()
                         progressBar1!!.valueFrom = 0.0f
                         progressBar1!!.valueTo = realDurationMillis.toFloat()

                         val millisInFuture = realDurationMillis
                         timer1 = object : CountDownTimerExt(millisInFuture, 100) {
                             override fun onTimerTick(millisUntilFinished: Long) {

                                 val diffTime: Long = millisInFuture - millisUntilFinished
                                 val elepsedTime: Long = 0 + diffTime

                                 Log.d("MainActivity!!", "onTimerTick $millisUntilFinished")
                                 onTickProgressUpdate(elepsedTime)
                             }

                             override fun onTimerFinish() {
                                 Log.d("MainActivity!", "onTimerFinish")
                                 dismiss()
                             }

                         }
                         timer1!!.start()
                     } else {
                         if (exoPlayer!!.isPlaying) {
                             timer1!!.pause()
                         } else {
                             timer1!!.start()
                         }
                     }

                 }
             }

             fun onPlayWhenReadyCommitted() {
                 // No op.

             }


             override fun onPlayerError(error: ExoPlaybackException) {
                 // No op.
             }
         })*/

    }

    private fun onTickProgressUpdate(long: Long) {
//        countUp += 100
//        val progress = countUp
//        Timber.d("prggress $progress")
        progressBar1!!.setValues(long.toFloat())
    }

    private fun releasePlayer() {
        if (exoPlayer != null) {
            exoPlayer!!.stop();
            exoPlayer!!.release();
            /*if (videoview!!.getPlayer() != null) {
                videoview!!.getPlayer()!!.stop();
                videoview!!.getPlayer()!!.release();
            }*/
        }
        if (timer1 != null) {
            timer1!!.pause()
        }
    }

}







