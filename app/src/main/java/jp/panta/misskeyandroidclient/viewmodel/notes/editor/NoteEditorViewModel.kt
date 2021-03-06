package jp.panta.misskeyandroidclient.viewmodel.notes.editor

import android.net.Uri
import android.util.Log
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import jp.panta.misskeyandroidclient.model.Encryption
import jp.panta.misskeyandroidclient.model.api.MisskeyAPI
import jp.panta.misskeyandroidclient.model.core.AccountRelation
import jp.panta.misskeyandroidclient.model.drive.FileProperty
import jp.panta.misskeyandroidclient.model.drive.UploadFile
import jp.panta.misskeyandroidclient.model.meta.Meta
import jp.panta.misskeyandroidclient.model.notes.Note
import jp.panta.misskeyandroidclient.util.eventbus.EventBus
import jp.panta.misskeyandroidclient.view.notes.editor.FileNoteEditorData
import jp.panta.misskeyandroidclient.viewmodel.notes.editor.poll.PollEditor
import jp.panta.misskeyandroidclient.viewmodel.users.UserViewData
import java.util.*
import kotlin.collections.ArrayList

class NoteEditorViewModel(
    private val accountRelation: AccountRelation,
    private val misskeyAPI: MisskeyAPI,
    meta: Meta,
    private val replyToNoteId: String? = null,
    private val quoteToNoteId: String? = null,
    private val encryption: Encryption,
    val note: Note? = null
) : ViewModel(){



    val hasCw = MutableLiveData<Boolean>(note?.cw != null)
    val cw = MutableLiveData<String>(note?.cw)
    val text = MutableLiveData<String>(note?.text)
    val maxTextLength = meta.maxNoteTextLength?: 1500
    val textRemaining = Transformations.map(text){ t: String? ->
        maxTextLength - (t?.codePointCount(0, t.length)?: 0)
    }

    val editorFiles = MediatorLiveData<List<FileNoteEditorData>>().apply{
        this.postValue(note?.files?.map{
            FileNoteEditorData(it)
        }?: emptyList())
    }

    val totalImageCount = MediatorLiveData<Int>().apply{

        this.addSource(editorFiles){
            Log.d("NoteEditorViewModel", "list$it, sizeは: ${it.size}")
            this.value = it.size
        }
    }


    val isPostAvailable = MediatorLiveData<Boolean>().apply{
        this.addSource(textRemaining){
            val totalImageTmp = totalImageCount.value
            this.value =  it in 0 until maxTextLength
                    || (totalImageTmp != null && totalImageTmp > 0 && totalImageTmp <= 4)
                    || quoteToNoteId != null
        }
        this.addSource(totalImageCount){
            val tmpTextSize = textRemaining.value
            this.value = tmpTextSize in 0 until maxTextLength
                    || (it != null && it > 0 && it <= 4)
                    || quoteToNoteId != null
        }
    }

    val visibility = MutableLiveData<PostNoteTask.Visibility>(PostNoteTask.Visibility.values().firstOrNull {
        it.visibility == note?.visibility?.toLowerCase(Locale.US) && it.isLocalOnly == note.localOnly?: false
    }?: PostNoteTask.Visibility.PUBLIC)
    val showVisibilitySelectionEvent = EventBus<Unit>()
    val visibilitySelectedEvent = EventBus<Unit>()

    val address = MutableLiveData<List<UserViewData>>(
        note?.visibleUserIds?.map{ userId ->
            UserViewData(userId).apply{
                setApi(accountRelation.getCurrentConnectionInformation()?.getI(encryption)!!, misskeyAPI)
            }
        }
    )

    val isSpecified = Transformations.map(visibility){
        it == PostNoteTask.Visibility.SPECIFIED
    }

    val poll = MutableLiveData<PollEditor?>(note?.poll?.let{
        PollEditor(it)
    })

    val noteTask = MutableLiveData<PostNoteTask>()

    val showPollDatePicker = EventBus<Unit>()
    val showPollTimePicker = EventBus<Unit>()

    val showPreviewFileEvent = EventBus<FileNoteEditorData>()

    fun post(){
        val noteTask = PostNoteTask(accountRelation.getCurrentConnectionInformation()!!, encryption)
        noteTask.cw = cw.value
        noteTask.files = editorFiles.value
        noteTask.text =text.value
        noteTask.poll = poll.value?.buildCreatePoll()
        noteTask.renoteId = quoteToNoteId
        noteTask.replyId = replyToNoteId
        noteTask.setVisibility(visibility.value, address.value?.map{
            it.userId
        })
        this.noteTask.postValue(noteTask)
    }

    fun add(file: Uri){
        val files = editorFiles.value.toArrayList()
        files.add(FileNoteEditorData(UploadFile(file, true)))
        editorFiles.value = files
    }

    fun add(fp: FileProperty){
        val files = editorFiles.value.toArrayList()
        files.add(FileNoteEditorData(fp))
        editorFiles.value = files
    }

    fun addAllFile(file: List<Uri>){
        val files = editorFiles.value.toArrayList()
        files.addAll(file.map{
            FileNoteEditorData(UploadFile(it, true))
        })
        editorFiles.value = files
    }

    fun addAllFileProperty(fpList: List<FileProperty>){
        val files = editorFiles.value.toArrayList()
        files.addAll(fpList.map{
            FileNoteEditorData(it)
        })
        editorFiles.value = files
    }

    fun removeFileNoteEditorData(data: FileNoteEditorData){
        val files = editorFiles.value.toArrayList()
        files.remove(data)
        editorFiles.value = files
    }

    fun localFileTotal(): Int{
        return editorFiles.value?.filter{
            it.isLocal
        }?.size?: 0
    }

    fun driveFileTotal(): Int{
        return editorFiles.value?.filter{
            !it.isLocal
        }?.size?: 0
    }

    fun fileTotal(): Int{
        return editorFiles.value?.size?: 0
    }

    fun driveFiles(): List<FileProperty>{
        return editorFiles.value?.filter {
            !it.isLocal && it.fileProperty != null
        }?.mapNotNull {
            it.fileProperty
        } ?: emptyList()
    }



    fun changeCwEnabled(){
        hasCw.value = !(hasCw.value?: false)
    }

    fun enablePoll(){
        val p = poll.value
        if(p == null){
            poll.value = PollEditor()
        }
    }

    fun disablePoll(){
        val p = poll.value
        if(p != null){
            poll.value = null
        }
    }

    fun showVisibilitySelection(){
        showVisibilitySelectionEvent.event = Unit
    }

    fun setVisibility(visibility: PostNoteTask.Visibility){
        this.visibility.value = visibility
        this.visibilitySelectedEvent.event = Unit
    }


    private fun List<FileNoteEditorData>?.toArrayList(): ArrayList<FileNoteEditorData>{
        return if(this == null){
            ArrayList<FileNoteEditorData>()
        }else{
            ArrayList<FileNoteEditorData>(this)
        }
    }

    fun setAddress(added: Array<String>, removed: Array<String>){
        val list = address.value?.let{
            ArrayList(it)
        }?: ArrayList()

        list.addAll(
            added.map{
                UserViewData(it).apply{
                    setApi(accountRelation.getCurrentConnectionInformation()?.getI(encryption)!!, misskeyAPI)
                }
            }
        )

        list.removeAll { uv ->
            removed.any{
                uv.userId == it
            }
        }
        address.postValue(list)
    }

    fun showPreviewFile(previewImage: FileNoteEditorData){
        showPreviewFileEvent.event = previewImage
    }


}