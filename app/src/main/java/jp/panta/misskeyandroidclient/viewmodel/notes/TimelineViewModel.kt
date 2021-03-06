package jp.panta.misskeyandroidclient.viewmodel.notes


import android.util.Log
import androidx.lifecycle.*
import jp.panta.misskeyandroidclient.model.Encryption
import jp.panta.misskeyandroidclient.model.Page
import jp.panta.misskeyandroidclient.model.core.AccountRelation
import jp.panta.misskeyandroidclient.model.notes.NoteRequest
import jp.panta.misskeyandroidclient.model.settings.SettingStore
import jp.panta.misskeyandroidclient.model.streming.TimelineCapture
import jp.panta.misskeyandroidclient.model.streming.note.NoteRegister
import jp.panta.misskeyandroidclient.viewmodel.MiCore
import jp.panta.misskeyandroidclient.viewmodel.notes.favorite.FavoriteNotePagingStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

class TimelineViewModel(
    val accountRelation: AccountRelation,
    private val pageableTimeline: Page.Timeline,
    include: NoteRequest.Include,
    miCore: MiCore,
    private val settingStore: SettingStore,
    encryption: Encryption
) : ViewModel(){



    val tag = "TimelineViewModel"
    val errorState = MediatorLiveData<String>()

    private val noteCaptureRegister = NoteRegister()
    //private val streamingAdapter = miCore.getStreamingAdapter(accountRelation)
    private val noteCapture = miCore.getNoteCapture(accountRelation)
    private val timelineCapture = if(settingStore.isAutoLoadTimeline){
        miCore.getTimelineCapture(accountRelation)
    }else{
        null
    }



    val position = MutableLiveData<Int>()

    private val notePagingStore = when(pageableTimeline){
        is Page.Favorite -> FavoriteNotePagingStore(accountRelation, pageableTimeline, miCore, encryption)
        else -> NoteTimelineStore(
            accountRelation,
            pageableTimeline,
            include,
            miCore,
            encryption
        )
    }


    private val timelineLiveData = object : MutableLiveData<TimelineState>(){
        override fun onActive() {
            super.onActive()

            if(!settingStore.isUpdateTimelineInBackground){
                startNoteCapture()
            }
        }

        override fun onInactive() {
            super.onInactive()

            if(settingStore.isAutoLoadTimeline && !settingStore.isUpdateTimelineInBackground){
                stopTimelineCapture()
                stopNoteCapture()
            }
        }
    }


    private var mObserver: TimelineCapture.TimelineObserver? = null


    private val noteCaptureId = UUID.randomUUID().toString()
    private val timelineCaptureId = UUID.randomUUID().toString()

    val isLoading = MutableLiveData<Boolean>()

    private var isLoadingFlag = false



    fun getTimelineLiveData() : LiveData<TimelineState>{
        return timelineLiveData
    }

    fun loadNew(){
        Log.d("TimelineLiveData", "loadNew")
        if( ! isLoadingFlag ){
            isLoadingFlag = true
            //val sinceId = observableTimelineList.firstOrNull()?.id
            val sinceId = timelineLiveData.value?.getSinceId()
            if(sinceId == null){
                isLoadingFlag = false
                isLoading.postValue(false)
                return loadInit()
            }
            viewModelScope.launch(Dispatchers.IO){
                try{
                    val res = notePagingStore.loadNew(sinceId)
                    val list = res.second
                    if(list == null || list.isEmpty()){
                        isLoadingFlag = false
                        isLoading.postValue(false)
                        return@launch
                    }else{
                        noteCapture.subscribeAll(noteCaptureRegister.registerId, list)


                        val state = timelineLiveData.value
                        val newState = if(state == null){
                            if(settingStore.isAutoLoadTimeline && !settingStore.isUpdateTimelineInBackground){
                                startTimelineCapture()
                            }
                            TimelineState(
                                list,
                                TimelineState.State.LOAD_NEW
                            )

                        }else{
                            if(settingStore.isAutoLoadTimeline && !settingStore.isUpdateTimelineInBackground && list.size < 20){
                                startTimelineCapture()
                            }
                            val newList = ArrayList<PlaneNoteViewData>(state.notes).apply {
                                addAll(0, list)
                            }
                            TimelineState(
                                newList,
                                TimelineState.State.LOAD_NEW
                            )
                        }

                        timelineLiveData.postValue(newState)
                        isLoadingFlag = false
                        isLoading.postValue(false)
                    }
                }catch(e: IOException){
                    isLoadingFlag = false
                    isLoading.postValue(false)
                }catch (e: Exception){
                    Log.d("TimelineLiveData", "タイムライン取得中にエラー発生", e)
                }

            }

        }
    }

    fun loadOld(){
        val untilId = timelineLiveData.value?.getUntilId() ?: return loadInit()
        if( isLoadingFlag){
            return
        }
        isLoadingFlag = true
        viewModelScope.launch(Dispatchers.IO){
            try {
                val res = notePagingStore.loadOld(untilId)
                val list = res.second
                if(list == null || list.isEmpty()){
                    isLoadingFlag = false
                    return@launch
                }else{
                    noteCapture.subscribeAll(noteCaptureRegister.registerId, list)
                    val state = timelineLiveData.value

                    val newState = if(state == null){
                        TimelineState(
                            list,
                            TimelineState.State.LOAD_OLD
                        )
                    }else{
                        val newList = ArrayList<PlaneNoteViewData>(state.notes).apply{
                            addAll(list)
                        }
                        TimelineState(
                            newList,
                            TimelineState.State.LOAD_OLD
                        )
                    }


                    timelineLiveData.postValue(newState)
                    isLoadingFlag = false
                }
            }catch (e: IOException){
                isLoadingFlag = false
            }catch(e: Exception){
                Log.d("TimelineLiveData", "タイムライン取得中にエラー発生", e)
            }
        }
    }

    fun loadInit(){
        this.isLoading.postValue(true)

        if( ! isLoadingFlag ){

            isLoadingFlag = true

            viewModelScope.launch(Dispatchers.IO){
                try{
                    val response = notePagingStore.loadInit()
                    val list = response.second
                    if(list == null || list.isEmpty()){
                        isLoadingFlag = false
                        isLoading.postValue(false)
                        return@launch
                    }else{
                        val state = TimelineState(
                            list,
                            TimelineState.State.INIT
                        )
                        timelineLiveData.postValue(state)
                        noteCapture.subscribeAll(noteCaptureRegister.registerId, list)
                        isLoadingFlag = false

                    }

                    if(settingStore.isAutoLoadTimeline){
                        startTimelineCapture()
                    }

                }catch(e: IOException){
                    isLoadingFlag = false
                    isLoading.postValue(false)
                }catch (e: Exception){
                    Log.d("TimelineLiveData", "タイムライン取得中にエラー発生", e)
                }

            }

        }
    }

    fun start(){
        startNoteCapture()
        if(settingStore.isAutoLoadTimeline){
            startTimelineCapture()
        }
    }

    fun stop(){
        stopNoteCapture()
        stopTimelineCapture()
    }


    private fun startTimelineCapture(){
        if(timelineCapture != null){
            //observer?.updateId()
            val observer = TimelineCapture.TimelineObserver.create(pageableTimeline, this.timelineObserver)
            if(observer != null && mObserver == null){
                Log.d(tag, "タイムラインキャプチャーを開始しようとしている")
                timelineCapture.addChannelObserver(observer)
                mObserver = observer
            }
        }
    }

    private fun stopTimelineCapture(){
        if(timelineCapture != null){
            val observer = mObserver
            if(observer != null){
                Log.d("TimelineViewModel" , "タイムラインキャプチャー停止中")
                timelineCapture.removeChannelObserver(observer)
                mObserver = null
            }

        }
    }

    private fun startNoteCapture(){
        Log.d(tag, "ノートのキャプチャーを開始しようとしている")

        noteCapture.attach(noteCaptureRegister)

    }

    private fun stopNoteCapture(){
        noteCapture.detach(noteCaptureRegister)
    }



    private val timelineObserver = object : TimelineCapture.Observer{
        override fun onReceived(note: PlaneNoteViewData) {
            if(isLoadingFlag){
                return
            }
            noteCapture.subscribe(noteCaptureRegister.registerId, note)
            val notes = timelineLiveData.value?.notes
            val list = if(notes == null){
                arrayListOf(note)
            }else{
                ArrayList<PlaneNoteViewData>(notes).apply{
                    add(0, note)
                }
            }
            timelineLiveData.postValue(
                TimelineState(
                    list,
                    TimelineState.State.RECEIVED_NEW
                )
            )
        }
    }

    private val noteRemovedListener = object : jp.panta.misskeyandroidclient.model.streming.note.NoteCapture.DeletedListener(){
        override fun onDeleted(noteId: String) {
            val list = timelineLiveData.value?.notes
            if(list == null){
                return
            }else{
                val timeline = ArrayList<PlaneNoteViewData>(list)
                timeline.filter{
                    it.toShowNote.id == noteId
                }.forEach{
                    timeline.remove(it)
                }
                timelineLiveData.postValue(
                    TimelineState(
                        timeline,
                        TimelineState.State.REMOVED
                    )
                )
            }

        }
    }.apply{
        if(settingStore.isHideRemovedNote){
            noteCapture.addNoteDeletedListener(this)
        }
    }

}