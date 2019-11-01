package jp.panta.misskeyandroidclient.view.drive

import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import jp.panta.misskeyandroidclient.R
import jp.panta.misskeyandroidclient.viewmodel.drive.file.FileViewData

object FileThumbnailHelper {

    @JvmStatic
    @BindingAdapter("fileThumbnailMain", "fileThumbnailMask", "thumbnailFileViewData")
    fun setFileThumbnail(fileThumbnailMain: ImageView, fileThumbnailMask: ImageView, thumbnailFileViewData: FileViewData){
        val thumbnailUrl = thumbnailFileViewData.thumbnailUrl
        setThumbnail(fileThumbnailMain, thumbnailUrl)

        if(thumbnailFileViewData.type?.contains("image") == true){
            fileThumbnailMask.visibility = View.GONE
        }else if(thumbnailFileViewData.type?.contains("video") == true){
            fileThumbnailMask.visibility = View.VISIBLE
            Glide
                .with(fileThumbnailMask)
                .load(R.drawable.ic_play_circle_outline_black_24dp)
                .centerCrop()
                .into(fileThumbnailMask)
        }
    }

    private fun setThumbnail(imageView: ImageView, url: String?){
        Glide
            .with(imageView.context)
            .load(url)
            .error(R.drawable.ic_cloud_off_black_24dp)
            .centerCrop()
            .into(imageView)

    }
}