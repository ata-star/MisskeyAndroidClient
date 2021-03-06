package jp.panta.misskeyandroidclient

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import jp.panta.misskeyandroidclient.model.Page
import jp.panta.misskeyandroidclient.model.core.AccountRelation
import jp.panta.misskeyandroidclient.model.notes.NoteRequest
import jp.panta.misskeyandroidclient.view.SafeUnbox
import jp.panta.misskeyandroidclient.view.notes.ActionNoteHandler
import jp.panta.misskeyandroidclient.view.notes.TimelineFragment
import jp.panta.misskeyandroidclient.viewmodel.MiCore
import jp.panta.misskeyandroidclient.viewmodel.confirm.ConfirmViewModel
import jp.panta.misskeyandroidclient.viewmodel.notes.NotesViewModel
import jp.panta.misskeyandroidclient.viewmodel.notes.NotesViewModelFactory
import kotlinx.android.synthetic.main.activity_search_result.*

class SearchResultActivity : AppCompatActivity() {
    companion object{
        const val EXTRA_SEARCH_WORLD = "jp.panta.misskeyandroidclient.SearchResultActivity.EXTRA_SEARCH_WORLD"
    }

    private var mSearchWord: String? = null
    private var mIsTag: Boolean? = null

    private var mAccountRelation: AccountRelation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme()
        setContentView(R.layout.activity_search_result)
        setSupportActionBar(search_result_toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val keyword: String? = intent.getStringExtra(EXTRA_SEARCH_WORLD)
            ?: intent.data?.getQueryParameter("keyword")

        mSearchWord = keyword

        if(keyword == null){
            finish()
            return
        }
        supportActionBar?.title = keyword

        val isTag = keyword.startsWith("#")
        mIsTag = isTag
        val request = if(isTag){
            Page.SearchByTag(tag = keyword.replace("#", ""))

        }else{
            Page.Search(query = keyword)

        }


        val timelineFragment = TimelineFragment.newInstance(request)
        val ft = supportFragmentManager.beginTransaction()
        ft.replace(R.id.search_result_base, timelineFragment)
        ft.commit()

        (application as MiCore).currentAccount.observe(this, Observer { ar ->
            val notesViewModel = ViewModelProvider(this, NotesViewModelFactory(ar, application as MiApplication))[NotesViewModel::class.java]
            ActionNoteHandler(this, notesViewModel, ViewModelProvider(this)[ConfirmViewModel::class.java]).initViewModelListener()
            mAccountRelation = ar
            invalidateOptionsMenu()
        })

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.activity_search_menu, menu)
        if(menu != null){
            val item = menu.findItem(R.id.nav_search_add_to_tab)

            if(isAddedPage()){
                item.setIcon(R.drawable.ic_remove_to_tab_24px)
            }else{
                item.setIcon(R.drawable.ic_add_to_tab_24px)
            }
            setMenuTint(menu)
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home -> finish()
            R.id.search -> {
                val intent = Intent(this, SearchActivity::class.java)
                intent.putExtra(SearchActivity.EXTRA_SEARCH_WORD, mSearchWord)
                startActivity(intent)
            }
            R.id.nav_search_add_to_tab ->{
                searchAddToTab()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun searchAddToTab(){
        val word = mSearchWord ?: return

        val miCore = application as MiCore
        val samePage = getSamePage()
        if(samePage == null){
            val page = if(mIsTag == true){
                Page(null, word, null, searchByTag = Page.SearchByTag(tag = word.replace("#", "")))
            }else{
                Page(null, mSearchWord?: "", null, search = Page.Search(word))
            }
            miCore.addPageInCurrentAccount(
                page
            )
        }else{
            miCore.removePageInCurrentAccount(samePage)
        }
    }

    private fun isAddedPage(): Boolean{
        return getSamePage() != null
    }

    private fun getSamePage(): Page?{
        return mAccountRelation?.pages?.firstOrNull {
            when (val pageable = it.pageable()) {
                is Page.Search -> {
                    pageable.query == mSearchWord
                }
                is Page.SearchByTag -> {
                    pageable.tag == mSearchWord
                }
                else -> {
                    false
                }
            }
        }
    }


}
