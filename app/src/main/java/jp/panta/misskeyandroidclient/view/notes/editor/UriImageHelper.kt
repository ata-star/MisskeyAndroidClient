package jp.panta.misskeyandroidclient.view.notes.editor

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide

object UriImageHelper {
    @JvmStatic
    @BindingAdapter("setSimpleImageUri")
    fun ImageView.setSimpleImageUri(uri: String?){
        Glide.with(this)
            .load(uri)
            .centerCrop()
            .into(this)
    }
}