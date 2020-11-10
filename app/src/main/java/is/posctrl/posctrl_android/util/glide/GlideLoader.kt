package `is`.posctrl.posctrl_android.util.glide


import android.content.Context
import android.graphics.Bitmap
import android.widget.ImageView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL


fun ImageView.load(context: Context, bitmap: Bitmap) {
    val applicationContext = context.applicationContext
    GlideApp.with(applicationContext)
        .load(bitmap)
        /*for tall images not to be distorted use these 2 lines*/
        .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
        .dontTransform()

        .placeholder(android.R.drawable.gallery_thumb)
        .error(android.R.drawable.gallery_thumb)
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        .into(this)
}

