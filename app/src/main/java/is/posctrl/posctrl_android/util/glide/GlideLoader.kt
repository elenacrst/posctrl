package `is`.posctrl.posctrl_android.util.glide


import android.content.Context
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.io.File


fun ImageView.load(context: Context, file: File) {
    if (file.exists() && file.length() > 1000) {//min 1kb size is valid
        val applicationContext = context.applicationContext
        GlideApp.with(applicationContext)
                .load(File(file.path))
                /*for tall images not to be distorted use these 2 lines*/
                //  .override(SIZE_ORIGINAL, SIZE_ORIGINAL)
                .dontTransform()
                .placeholder(android.R.drawable.gallery_thumb)
                .error(android.R.drawable.gallery_thumb)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(this)
    }

}

fun ImageView.load(context: Context, @DrawableRes res: Int) {
    val applicationContext = context.applicationContext
    GlideApp.with(applicationContext)
            .load(res)
            .dontTransform()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(this)
}

