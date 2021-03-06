package jp.panta.misskeyandroidclient.viewmodel.auth.signin

import android.util.Log
import androidx.lifecycle.*
import jp.panta.misskeyandroidclient.MiApplication
import jp.panta.misskeyandroidclient.model.MisskeyAPIServiceBuilder
import jp.panta.misskeyandroidclient.model.api.MisskeyAPI
import jp.panta.misskeyandroidclient.model.auth.signin.SignIn
import jp.panta.misskeyandroidclient.model.core.Account
import jp.panta.misskeyandroidclient.model.core.AccountRelation
import jp.panta.misskeyandroidclient.model.core.EncryptedConnectionInformation
import jp.panta.misskeyandroidclient.model.meta.Meta
import jp.panta.misskeyandroidclient.model.meta.RequestMeta
import jp.panta.misskeyandroidclient.model.users.RequestUser
import jp.panta.misskeyandroidclient.model.users.User
import jp.panta.misskeyandroidclient.viewmodel.MiCore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.IllegalArgumentException


class SignInViewModel(
    miCore: MiCore
) : ViewModel(){
    companion object{
        private const val TAG = "SignInViewModel"
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(val miApplication: MiApplication, val mode: Int) : ViewModelProvider.Factory{
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if(modelClass == SignInViewModel::class.java){
                return SignInViewModel(miApplication) as T
            }
            throw IllegalArgumentException("use SignInViewModel::class.java")
        }
    }
    val encryption = miCore.getEncryption()

    val userName = MutableLiveData<String>()
    val password = MutableLiveData<String>()

    val user = MediatorLiveData<User>()
    val token = MutableLiveData<String>()

    val isValidDomain = MutableLiveData<Boolean>(false)

    var misskeyAPI: MisskeyAPI? = null

    val connectionInformation = MutableLiveData<Pair<Account, EncryptedConnectionInformation>>()

    val instanceDomain = MediatorLiveData<String>()
    //val i = MutableLiveData<String>()
    val isValidityOfAuth = MutableLiveData<Boolean>(false)

    //val me = MutableLiveData<User?>()
    val isNeed2FA = Transformations.map(user){
        it?.twoFactorEnabled == true
    }

    init{
        instanceDomain.observeForever {
            viewModelScope.launch{
                try{
                    misskeyAPI = MisskeyAPIServiceBuilder.build("https://$it")
                }catch(e: Exception){
                    isValidDomain.postValue(false)
                }
            }

            misskeyAPI?.getMeta(RequestMeta(false))?.enqueue(object : Callback<Meta?>{
                override fun onResponse(call: Call<Meta?>, response: Response<Meta?>) {
                    isValidDomain.postValue(
                        response.code() == 200
                    )
                }
                override fun onFailure(call: Call<Meta?>, t: Throwable) {
                    isValidDomain.postValue(false)
                }
            })
        }

        user.addSource(userName){
            misskeyAPI?.showUser(RequestUser(
                i = null,
                userName = it,
                userId = null
            ))?.enqueue(object : Callback<User>{
                override fun onResponse(call: Call<User>, response: Response<User>) {
                    user.postValue(response.body())
                }
                override fun onFailure(call: Call<User>, t: Throwable) {
                    user.postValue(null)
                }
            })
        }
    }

    fun signIn(){
        val un = userName.value
        val pw = password.value
        val isNeedToken = isNeed2FA.value
        val token = token.value

        val domain = "https://" + (instanceDomain.value?: return)
        if(un == null || pw == null)
            return

        viewModelScope.launch(Dispatchers.IO) {
            val i = misskeyAPI?.signIn(SignIn(
                username = un,
                password = pw,
                token = if(isNeedToken == true){
                        token
                    }else{
                        null
                    }
                )
            )?.execute()?.body()
            isValidityOfAuth.postValue(i != null)

            if(i == null){
                Log.d(TAG, "認証に失敗")
                return@launch
            }
            val me = misskeyAPI?.i(i)?.execute()?.body()
            if(me == null){
                Log.d(TAG, "自己情報取得に失敗")
                return@launch
            }
            val creator = EncryptedConnectionInformation.Creator(encryption)
            val ci = creator.create(i.i, me, domain)
            connectionInformation.postValue(Account(me.id) to ci)
        }

    }


    /*private fun makeConnectionInstance(domain: String, i: I){
        viewModelScope.launch(Dispatchers.IO){
            try{
                val res = misskeyAPI?.i(i)?.execute()
                if(res == null){
                    Log.e(TAG, "Retrofitが初期されていない")
                    return@launch
                }
                val user = res.body()
                if(user == null){
                    Log.d(TAG, "api/iの取得に失敗した, code:${res.code()}, error:${res.errorBody()?.string()}")
                    return@launch
                }

                val creator = EncryptedConnectionInformation.Creator(encryption)
                creator.create(i, domain)

                this@SignInViewModel.connectionInstance.postValue(connectionInstance)
            }catch(e: Exception){
                Log.e(TAG, "ConnectionInstance作成中にエラーが発生しました", e)
            }

        }

    }*/

}