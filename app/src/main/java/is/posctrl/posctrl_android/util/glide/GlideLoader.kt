package `is`.posctrl.posctrl_android.util.glide


import android.content.Context
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target.SIZE_ORIGINAL
import java.io.File


fun ImageView.load(context: Context, file: File) {
    val applicationContext = context.applicationContext
    GlideApp.with(applicationContext)
        .load(file)
        /*for tall images not to be distorted use these 2 lines*/
        .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
        .dontTransform()
        .placeholder(android.R.drawable.gallery_thumb)
        .error(android.R.drawable.gallery_thumb)
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        .into(this)
}

fun ImageView.load(context: Context, @DrawableRes res: Int) {
    val applicationContext = context.applicationContext
    GlideApp.with(applicationContext)
        .load(res)
        .dontTransform()
        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        .into(this)
}

