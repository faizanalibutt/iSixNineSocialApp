package com.i69app.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.i69app.R

private const val MAX_IMAGE_DIMENSION = 512

suspend fun Context.getBitmap(imageSrc: String) =
    withContext(Dispatchers.IO) {
        Glide
            .with(this@getBitmap)
            .asBitmap()
            .fitCenter()
            .load(imageSrc)
            .submit(MAX_IMAGE_DIMENSION, MAX_IMAGE_DIMENSION)
            .get()
    }

fun ImageView.loadCircleImage(imageUrl: String, callback: ((Drawable?) -> Unit)? = null, failure: ((GlideException?) -> Unit)? = null) {
    val requestOptions = RequestOptions().circleCrop().placeholder(R.drawable.ic_default_user).error(R.drawable.ic_default_user)
    loadImage(imageUrl, requestOptions, callback, failure)
}

fun ImageView.loadCircleImage(imageUrl: Int, callback: ((Drawable?) -> Unit)? = null, failure: ((GlideException?) -> Unit)? = null) {
    val requestOptions = RequestOptions().circleCrop().placeholder(R.drawable.ic_default_user).error(R.drawable.ic_default_user)
    loadImage(imageUrl, requestOptions, callback, failure)
}

fun ImageView.loadImage(imageSrc: Any, callback: ((Drawable?) -> Unit)? = null, failure: ((GlideException?) -> Unit)? = null) {
    val requestOptions = RequestOptions().fitCenter()
    loadImage(imageSrc, requestOptions, callback, failure)
}

fun ImageView.loadImage(imageSrc: Any, requestOptions: RequestOptions, callback: ((Drawable?) -> Unit)? = null, failure: ((GlideException?) -> Unit)?) {
    try {
        Glide
            .with(this.context)
            .load(imageSrc)
            .apply(requestOptions)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    failure?.invoke(e)
                    return false
                }

                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    callback?.invoke(resource)
                    return false
                }
            })
            .into(this)

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadImage(context: Context, imageSrc: String, callback: ((Bitmap?) -> Unit)? = null, failure: ((Drawable?) -> Unit)?) {
    try {
        val requestOptions = RequestOptions().placeholder(R.drawable.ic_default_user).error(R.drawable.ic_default_user)
        GlideApp.with(context)
            .asBitmap()
            .apply(requestOptions)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .load(imageSrc)
            .into(object : CustomTarget<Bitmap>(){
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    callback?.invoke(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    failure?.invoke(placeholder)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    failure?.invoke(errorDrawable)
                }
            })

    } catch (e: Exception) {
        e.printStackTrace()
        failure?.invoke(null)
    }
}

fun ImageView.loadImage_bysize(imageSrc: Any, callback: ((Drawable?) -> Unit)? = null, failure: ((GlideException?) -> Unit)? = null) {
    val requestOptions = RequestOptions().override(50,50).circleCrop()
    loadImage_byspecificsize(imageSrc, requestOptions, callback, failure)
}

fun ImageView.loadImage_byspecificsize(imageSrc: Any, requestOptions: RequestOptions, callback: ((Drawable?) -> Unit)? = null, failure: ((GlideException?) -> Unit)?) {
    try {
        Glide
            .with(this.context)
            .load(imageSrc)
            .apply(requestOptions)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    failure?.invoke(e)
                    return false
                }

                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    callback?.invoke(resource)
                    return false
                }
            })
            .into(this)

    } catch (e: Exception) {
        e.printStackTrace()
    }
}