package jp.panta.misskeyandroidclient.viewmodel.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import jp.panta.misskeyandroidclient.model.Encryption
import jp.panta.misskeyandroidclient.model.api.MisskeyAPI
import jp.panta.misskeyandroidclient.model.core.AccountRelation
import jp.panta.misskeyandroidclient.model.list.*
import jp.panta.misskeyandroidclient.model.users.User
import jp.panta.misskeyandroidclient.util.eventbus.EventBus
import jp.panta.misskeyandroidclient.viewmodel.MiCore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserListOperateViewModel(
    val accountRelation: AccountRelation,
    val misskeyAPI: MisskeyAPI,
    val encryption: Encryption
) : ViewModel(){

    @Suppress("UNCHECKED_CAST")
    class Factory(val accountRelation: AccountRelation, val miCore: MiCore) : ViewModelProvider.Factory{
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {

            return UserListOperateViewModel(accountRelation, miCore.getMisskeyAPI(accountRelation)!!, miCore.getEncryption()) as T
        }
    }

    private val tag = this.javaClass.simpleName

    private val mPublisher = UserListEventStore(misskeyAPI, accountRelation)

    val updateUserListEvent = EventBus<UserList>()

    fun pushUser(userList: UserList, userId: String){
        misskeyAPI.pushUserToList(
            ListUserOperation(
                i = accountRelation.getCurrentConnectionInformation()?.getI(encryption)!!,
                listId = userList.id,
                userId = userId
            )
        ).enqueue(object : Callback<Unit>{
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if(response.code() in 200 until 300){
                    mPublisher.onPushUser(userList.id, userId)
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.d(tag, "push user error", t)
            }
        })
    }

    fun pullUser(userListId: String, userId: String){
        misskeyAPI.pullUserFromList(
            ListUserOperation(
                i = accountRelation.getCurrentConnectionInformation()?.getI(encryption)!!,
                listId = userListId,
                userId = userId
            )
        ).enqueue(object : Callback<Unit>{
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                if(response.code() in 200 until 300){
                    mPublisher.onPullUser(userListId, userId)
                }else{
                    Log.d(tag, "pull user failure: $response")
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.d(tag, "pull user error", t)
            }
        })
    }

    fun rename(listId: String, name: String){
        Log.d(tag, "update listId:$listId, name:$name")
        misskeyAPI.updateList(
            UpdateList(
                i = accountRelation.getCurrentConnectionInformation()?.getI(encryption)!!,
                name = name,
                listId = listId
            )
        ).enqueue(object : Callback<Unit>{
            override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                Log.d(tag, "update list, response:$response")
                if(response.code() in 200 until 300){
                    mPublisher.onUpdateUserList(listId, name)
                }
            }

            override fun onFailure(call: Call<Unit>, t: Throwable) {
                Log.d(tag, "rename userList error", t)
            }
        })
    }

    fun create(createUserList: CreateList){
        misskeyAPI.createList(
            createUserList
        ).enqueue(object : Callback<UserList>{
            override fun onResponse(call: Call<UserList>, response: Response<UserList>) {
                if(response.code() in 200 until 300){
                    mPublisher.onCreateUserList(response.body()!!)
                }
            }

            override fun onFailure(call: Call<UserList>, t: Throwable) {
                Log.d(tag, "user list create error", t)
            }
        })
    }

    fun delete(userList: UserList){
        misskeyAPI.deleteList(ListId(
            i = accountRelation.getCurrentConnectionInformation()?.getI(encryption)!!,
            listId = userList.id
        )).enqueue(
            object : Callback<Unit>{
                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                    if(response.code() in 200 until 300){
                        mPublisher.onDeleteUserList(userList)
                    }
                }

                override fun onFailure(call: Call<Unit>, t: Throwable) {
                    Log.d(tag, "user list delete error", t)
                }
            }
        )
    }

    fun showUserListUpdateDialog(userList: UserList?){
        userList?.let{ ul ->
            updateUserListEvent.event = ul
        }
    }

}