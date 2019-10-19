package jp.panta.misskeyandroidclient.view.notes.reaction

import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import jp.panta.misskeyandroidclient.MiApplication
import jp.panta.misskeyandroidclient.model.emoji.ConstantEmoji

object ReactionViewHelper {
    @BindingAdapter("reactionImageView", "reactionStringView", "reaction")
    @JvmStatic
    fun LinearLayout.setReaction(reactionImageView: ImageView, reactionStringView: TextView, reaction: Pair<String, Int>) {
        Log.d("ReactionViewHelper", "reaction $reaction")
        if(reaction.first.startsWith(":") && reaction.first.endsWith(":")){
            val miApplication = this.context.applicationContext as MiApplication
            val emoji = miApplication.nowInstanceMeta?.emojis?.firstOrNull{
                it.name == reaction.first.replace(":", "")
            }

            if(emoji != null){
                Log.d("ReactionViewHelper", "カスタム絵文字を発見した: ${emoji}")
                Glide.with(reactionImageView.context)
                    .load(emoji.url?: emoji.uri)
                    .centerCrop()
                    .into(reactionImageView)
                reactionImageView.visibility = View.VISIBLE
                reactionStringView.visibility = View.GONE
                return
            }else{
                Log.d("ReactionViewHelper", "emoji not found")
            }

            reactionImageView.visibility = View.GONE
            reactionStringView.visibility = View.VISIBLE
            if(ConstantEmoji.emojis.contains(reaction.first)){
                reactionStringView.text = ConstantEmoji.textCodeMap[reaction.first]
            }else{
                reactionStringView.text = reaction.first
            }

        }


    }
}