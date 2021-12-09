package com.example.forager.misc

import android.content.Context
import android.widget.ImageView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners

object ImageUtil {

    // Loading images with rounded corners and a loading bar if needed
    fun loadImagesWithFlair(context: Context, photoUrl: String, loadInto: ImageView) {
        val circularProgressDrawable = CircularProgressDrawable(context)
        circularProgressDrawable.strokeWidth = 5f
        circularProgressDrawable.centerRadius = 30f
        circularProgressDrawable.start()

        Glide.with(context).load(photoUrl)
            .placeholder(circularProgressDrawable)
            .transform(RoundedCorners(20))
            .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
            .into(loadInto)
    }

}