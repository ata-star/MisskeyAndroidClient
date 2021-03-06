package jp.panta.misskeyandroidclient.viewmodel.notes.editor.poll

import androidx.lifecycle.MutableLiveData
import jp.panta.misskeyandroidclient.model.notes.poll.CreatePoll
import jp.panta.misskeyandroidclient.model.notes.poll.Poll
import java.util.*
import kotlin.collections.ArrayList

class PollEditor(val poll: Poll? = null) {
    private val exChoices = poll?.choices?.map {
        PollChoice(it)
    } ?: listOf(
            PollChoice(),
            PollChoice()
        )
    enum class DeadLineType{
        INDEFINITE_PERIOD,
        DATE_AND_TIME
    }
    val choices = MutableLiveData<List<PollChoice>>(exChoices)
    val isMutable = MutableLiveData<Boolean>()

    val expiresAt = MutableLiveData<Date>()

    val deadLineType = MutableLiveData<DeadLineType>(DeadLineType.INDEFINITE_PERIOD)

    fun makeAndAddChoice(){
        val choices = this.choices.value
        if(choices.isNullOrEmpty()){
            this.choices.value = listOf(PollChoice())
        }else{
            this.choices.value = ArrayList<PollChoice>(choices).apply{
                add(PollChoice())
            }
        }
    }
    fun removeChoice(choice: PollChoice){
        val choices = this.choices.value
        if(!choices.isNullOrEmpty()){
            this.choices.value = ArrayList<PollChoice>(choices).apply{
                remove(choice)
            }
        }
    }
    fun buildCreatePoll(): CreatePoll?{
        val choices = this.choices.value?.map{
            it.text.value
        }?.filter{
            it != null && it.isNotBlank()
        }?.filterNotNull()

        val expiresAt = if(deadLineType.value == DeadLineType.DATE_AND_TIME){
            this.expiresAt.value
        }else{
            null
        }
        if(choices != null && choices.size >= 2){
            return CreatePoll(
                choices,
                isMutable.value?: false,
                expiresAt?.time
            )
        }
        return null
    }

}
