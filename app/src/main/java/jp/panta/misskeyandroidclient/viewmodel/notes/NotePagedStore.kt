package jp.panta.misskeyandroidclient.viewmodel.notes

import jp.panta.misskeyandroidclient.model.Page
import jp.panta.misskeyandroidclient.model.core.AccountRelation
import jp.panta.misskeyandroidclient.model.notes.NoteRequest
import jp.panta.misskeyandroidclient.util.BodyLessResponse

interface NotePagedStore {
    //val timelineRequestBase: NoteRequest.Setting
    val pageableTimeline: Page.Timeline
    val accountRelation: AccountRelation

    fun loadOld(untilId: String): Pair<BodyLessResponse, List<PlaneNoteViewData>?>
    fun loadNew(sinceId: String): Pair<BodyLessResponse, List<PlaneNoteViewData>?>
    fun loadInit(request: NoteRequest? = null): Pair<BodyLessResponse, List<PlaneNoteViewData>?>
}