package jp.panta.misskeyandroidclient.viewmodel.notes.favorite

import jp.panta.misskeyandroidclient.model.Encryption
import jp.panta.misskeyandroidclient.model.Page
import jp.panta.misskeyandroidclient.model.api.MisskeyAPI
import jp.panta.misskeyandroidclient.model.core.AccountRelation
import jp.panta.misskeyandroidclient.model.fevorite.Favorite
import jp.panta.misskeyandroidclient.model.notes.NoteRequest
import jp.panta.misskeyandroidclient.util.BodyLessResponse
import jp.panta.misskeyandroidclient.viewmodel.MiCore
import jp.panta.misskeyandroidclient.viewmodel.notes.NotePagedStore
import jp.panta.misskeyandroidclient.viewmodel.notes.PlaneNoteViewData
import retrofit2.Response

class FavoriteNotePagingStore(
    override val accountRelation: AccountRelation,
    override val pageableTimeline: Page.Timeline,
    miCore: MiCore,
    private val encryption: Encryption
) : NotePagedStore{

    val favorites = miCore.getMisskeyAPI(accountRelation)!!::favorites

    private val connectionInformation = accountRelation.getCurrentConnectionInformation()!!

    private val builder = NoteRequest.Builder(pageableTimeline)

    override fun loadInit(request: NoteRequest?): Pair<BodyLessResponse, List<PlaneNoteViewData>?> {
        return if(request == null){
            val i = accountRelation.getCurrentConnectionInformation()?.getI(encryption)!!
            val res =favorites(builder.build(i, NoteRequest.Conditions())).execute()
            makeResponse(res, false)
        }else{
            makeResponse(favorites(request).execute(), false)
        }
    }

    override fun loadNew(sinceId: String): Pair<BodyLessResponse, List<PlaneNoteViewData>?> {
        val i = accountRelation.getCurrentConnectionInformation()?.getI(encryption)!!
        val res = favorites(builder.build(i, NoteRequest.Conditions(sinceId = sinceId))).execute()
        return makeResponse(res, true)
    }

    override fun loadOld(untilId: String): Pair<BodyLessResponse, List<PlaneNoteViewData>?> {
        val i = accountRelation.getCurrentConnectionInformation()?.getI(encryption)!!
        val res = favorites(builder.build(i, NoteRequest.Conditions(untilId))).execute()
        return makeResponse(res, false)
    }

    private fun makeResponse(res: Response<List<Favorite>?>, isReversed: Boolean): Pair<BodyLessResponse, List<PlaneNoteViewData>?>{
        val rawList = if(isReversed) res.body()?.asReversed() else res.body()
        val list = rawList?.map{
            FavoriteNoteViewData(it, accountRelation.account) as PlaneNoteViewData
        }
        return Pair(BodyLessResponse(res), list)
    }
}