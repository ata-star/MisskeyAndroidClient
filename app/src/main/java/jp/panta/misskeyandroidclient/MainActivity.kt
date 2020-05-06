package jp.panta.misskeyandroidclient

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log

import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import jp.panta.misskeyandroidclient.databinding.ActivityMainBinding
import jp.panta.misskeyandroidclient.databinding.NavHeaderMainBinding
import jp.panta.misskeyandroidclient.model.Page
import jp.panta.misskeyandroidclient.model.core.AccountRelation
import jp.panta.misskeyandroidclient.model.core.ConnectionStatus
import jp.panta.misskeyandroidclient.model.notification.Notification
import jp.panta.misskeyandroidclient.model.settings.SettingStore
import jp.panta.misskeyandroidclient.model.users.User
import jp.panta.misskeyandroidclient.util.BottomNavigationAdapter
import jp.panta.misskeyandroidclient.util.getPreferenceName
import jp.panta.misskeyandroidclient.view.ScrollableTop
import jp.panta.misskeyandroidclient.view.account.AccountSwitchingDialog
import jp.panta.misskeyandroidclient.view.messaging.MessagingHistoryFragment
import jp.panta.misskeyandroidclient.view.notes.ActionNoteHandler
import jp.panta.misskeyandroidclient.view.notes.TabFragment
import jp.panta.misskeyandroidclient.view.notes.TimelineFragment
import jp.panta.misskeyandroidclient.view.notification.NotificationFragment
import jp.panta.misskeyandroidclient.view.notification.NotificationMentionFragment
import jp.panta.misskeyandroidclient.view.search.SearchTopFragment
import jp.panta.misskeyandroidclient.view.settings.activities.PageSettingActivity
import jp.panta.misskeyandroidclient.viewmodel.account.AccountViewModel
import jp.panta.misskeyandroidclient.viewmodel.notes.NotesViewModel
import jp.panta.misskeyandroidclient.viewmodel.notes.NotesViewModelFactory
import jp.panta.misskeyandroidclient.viewmodel.notification.NotificationSubscribeViewModel
import jp.panta.misskeyandroidclient.viewmodel.notification.NotificationViewData
import jp.panta.misskeyandroidclient.viewmodel.setting.page.PageableTemplate
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    lateinit var mNotesViewModel: NotesViewModel
    private lateinit var mAccountViewModel: AccountViewModel

    private var mBottomNavigationAdapter: MainBottomNavigationAdapter? = null

    private var mNotificationSubscribeViewModel: NotificationSubscribeViewModel? = null

    private var mNotificationService: NotificationService? = null

    private var mSettingStore: SettingStore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //setTheme(R.style.AppThemeDark)
        setTheme()

        setContentView(R.layout.activity_main)

        val mainBinding: ActivityMainBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        fab.setOnClickListener{
            startActivity(Intent(this, NoteEditorActivity::class.java))
        }

        val miApplication = application as MiApplication

        mAccountViewModel = ViewModelProvider(this, AccountViewModel.Factory(miApplication))[AccountViewModel::class.java]
        initAccountViewModelListener()
        setHeaderProfile(mainBinding)

        var init = false
        miApplication.currentAccount.observe(this, Observer {
            if(!init){
                mNotesViewModel = ViewModelProvider(this, NotesViewModelFactory(it, miApplication)).get(NotesViewModel::class.java)

                Log.d("MainActivity", "NotesViewModelのコネクション情報: ${mNotesViewModel.accountRelation}")
                ActionNoteHandler(this, mNotesViewModel).initViewModelListener()
                init = true
                Log.d("MainActivity", "初期化処理")
            }
            val factory = NotificationSubscribeViewModel.Factory(it, miApplication)
            mNotificationSubscribeViewModel = ViewModelProvider(this, factory).get("${it.account}", NotificationSubscribeViewModel::class.java)
            mNotificationSubscribeViewModel?.currentNotification?.observe(this, notificationObserver)
        })


        miApplication.connectionStatus.observe(this, Observer{ status ->
            when(status){
                ConnectionStatus.SUCCESS -> Log.d("MainActivity", "成功")
                ConnectionStatus.ACCOUNT_ERROR ->{
                    startActivity(Intent(this, AppAuthActivity::class.java))

                    finish()
                }
                ConnectionStatus.NETWORK_ERROR ->{
                    Toast.makeText(this, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
                }
                else -> Log.d("MainActivity", "not initialized")
            }
        })


        startService(Intent(this, NotificationService::class.java))
        mBottomNavigationAdapter = MainBottomNavigationAdapter()

    }



    inner class MainBottomNavigationAdapter
        : BottomNavigationAdapter(bottom_navigation, supportFragmentManager, R.id.navigation_home, R.id.content_main){
        private val home = bottom_navigation.menu.findItem(R.id.navigation_home)
        var currentMenuItem: MenuItem? = null

        override fun viewChanged(menuItem: MenuItem, fragment: Fragment) {
            super.viewChanged(menuItem, fragment)
            when(menuItem.itemId){
                R.id.navigation_home -> changeTitle(getString(R.string.menu_home))
                R.id.navigation_search -> changeTitle(getString(R.string.search))
                R.id.navigation_notification -> changeTitle(getString(R.string.notification))
                R.id.navigation_message_list -> changeTitle(getString(R.string.message))
                R.id.navigation_favorite -> changeTitle(getString(R.string.favorite))
            }
            currentMenuItem = menuItem
        }
        override fun getItem(menuItem: MenuItem): Fragment? {
            return when(menuItem.itemId){
                R.id.navigation_home -> TabFragment()
                R.id.navigation_search -> SearchTopFragment()
                R.id.navigation_notification -> NotificationMentionFragment()
                R.id.navigation_message_list -> MessagingHistoryFragment()
                R.id.navigation_favorite -> TimelineFragment.newInstance(Page.Favorite())
                else -> null
            }
        }

        override fun menuRetouched(menuItem: MenuItem, fragment: Fragment) {
            if(fragment is ScrollableTop){
                fragment.showTop()
            }
        }


    }

    private val notificationObserver = Observer<Notification>{ notify: Notification? ->
        notify?: return@Observer

        val account = (application as MiApplication).currentAccount.value?: return@Observer
        val viewData = NotificationViewData(notify, account.account)
        //Log.d("MainActivity")
        val name = notify.user.name?: notify.user.userName
        val msg = when(viewData.type){
            NotificationViewData.Type.FOLLOW ->  name + getString(R.string.followed_by)
            NotificationViewData.Type.MENTION -> name + getString(R.string.mention_by)
            NotificationViewData.Type.REPLY -> name + getString(R.string.replied_by)
            NotificationViewData.Type.RENOTE -> name + getString(R.string.renoted_by)
            NotificationViewData.Type.QUOTE -> name + getString(R.string.quoted_by)
            NotificationViewData.Type.REACTION -> name + getString(R.string.reacted_by)
            NotificationViewData.Type.POLL_VOTE -> name + getString(R.string.voted_by)
            NotificationViewData.Type.RECEIVE_FOLLOW_REQUEST -> name + getString(R.string.followed_by)
            else -> "もうわかんねぇなこれ"
        }
        val snackBar = Snackbar.make(simple_notification, msg, Snackbar.LENGTH_LONG)
        /*snackBar.setAction(R.string.show){
            mBottomNavigationAdapter?.setCurrentFragment(R.id.navigation_notification)
        }*/
        snackBar.show()

    }

    private val switchAccountButtonObserver = Observer<Int>{
        runOnUiThread{
            drawer_layout.closeDrawer(GravityCompat.START)
            val dialog = AccountSwitchingDialog()
            dialog.show(supportFragmentManager, "mainActivity")
        }
    }

    private val switchAccountObserver = Observer<AccountRelation>{
        Log.d(this.javaClass.simpleName, "アカウントを切り替えようとしている")
        (application as MiApplication).switchAccount(it.account)
    }

    private val showFollowingsObserver = Observer<Unit>{
        closeDrawerWhenOpenedDrawer()
        val intent = Intent(this, FollowFollowerActivity::class.java).apply{
            putExtra(FollowFollowerActivity.EXTRA_VIEW_CURRENT, FollowFollowerActivity.FOLLOWING_VIEW_MODE)
        }
        startActivity(intent)
    }

    private val showFollowersObserver = Observer<Unit>{
        closeDrawerWhenOpenedDrawer()
        val intent = Intent(this, FollowFollowerActivity::class.java).apply {
            putExtra(FollowFollowerActivity.EXTRA_VIEW_CURRENT, FollowFollowerActivity.FOLLOWER_VIEW_MODE)
        }
        startActivity(intent)
    }

    private val showProfileObserver = Observer<User>{
        closeDrawerWhenOpenedDrawer()
        val intent = Intent(this, UserDetailActivity::class.java)
        intent.putExtra(UserDetailActivity.EXTRA_USER_ID, it.id)
        startActivity(intent)
    }
    private fun initAccountViewModelListener(){
        mAccountViewModel.switchAccount.removeObserver(switchAccountButtonObserver)
        mAccountViewModel.switchAccount.observe(this, switchAccountButtonObserver)

        mAccountViewModel.switchTargetConnectionInstance.removeObserver(switchAccountObserver)
        mAccountViewModel.switchTargetConnectionInstance.observe(this, switchAccountObserver)

        mAccountViewModel.showFollowings.observe(this, showFollowingsObserver)
        mAccountViewModel.showFollowers.observe(this, showFollowersObserver)
        mAccountViewModel.showProfile.observe(this, showProfileObserver)
    }
    fun changeTitle(title: String?){
        toolbar.title = title
    }

    private fun setHeaderProfile(activityMainBinding: ActivityMainBinding){

        DataBindingUtil.bind<NavHeaderMainBinding>(activityMainBinding.navView.getHeaderView(0))
        val headerBinding = DataBindingUtil.getBinding<NavHeaderMainBinding>(activityMainBinding.navView.getHeaderView(0))
        headerBinding?.lifecycleOwner = this
        mAccountViewModel
        headerBinding?.accountViewModel = mAccountViewModel

    }

    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        }else if(mBottomNavigationAdapter?.currentMenuItem?.itemId != R.id.navigation_home){
            mBottomNavigationAdapter?.setCurrentFragment(R.id.navigation_home)
        }else{
            super.onBackPressed()
        }
    }

    private fun closeDrawerWhenOpenedDrawer(){
        if(drawer_layout.isDrawerOpen(GravityCompat.START)){
            drawer_layout.closeDrawer(GravityCompat.START)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)

        val isClassicUI = getSettingStore().isClassicUI
        val targetItems = listOf(
            menu.findItem(R.id.action_messaging),
            menu.findItem(R.id.action_notification),
            menu.findItem(R.id.action_search)
        )

        targetItems.forEach{
            it.isVisible = isClassicUI
        }

        setMenuTint(menu)
        return true
    }

    private fun getSettingStore(): SettingStore{
        val store: SettingStore = mSettingStore ?: SettingStore(getSharedPreferences(getPreferenceName(), Context.MODE_PRIVATE))
        mSettingStore = store
        return store
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_tab_setting-> {
                startActivity(Intent(this,
                    PageSettingActivity::class.java))
                true
            }
            R.id.action_notification ->{
                startActivity(Intent(this, NotificationsActivity::class.java))
                true
            }
            R.id.action_messaging ->{
                startActivity(Intent(this, MessagingListActivity::class.java))
                true
            }
            R.id.action_search ->{
                startActivity(Intent(this, SearchActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {

            R.id.nav_setting ->{
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            R.id.nav_drive ->{
                startActivity(Intent(this, DriveActivity::class.java))
            }
            R.id.nav_favorite ->{
                startActivity(Intent(this, FavoriteActivity::class.java))
            }
            R.id.nav_list ->{
                startActivity(
                    Intent(this, ListListActivity::class.java)
                )
            }
        }
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return false
    }
    override fun onStart() {
        super.onStart()

        bindService(Intent(this, NotificationService::class.java), notificationServiceConnection, Context.BIND_AUTO_CREATE)
        setBackgroundImage()
        applyUI()
    }

    override fun onResume(){
        super.onResume()
        (application as MiApplication?)?.currentAccount?.value?.let{
            mNotificationService?.stopShowPushNotification(it.account)
        }
    }

    override fun onPause() {
        super.onPause()
        (application as MiApplication?)?.currentAccount?.value?.let{
            mNotificationService?.startShowPushNotification(it.account)
        }
    }

    override fun onStop() {
        super.onStop()

        mNotificationService = null
        unbindService(notificationServiceConnection)
    }

    private val notificationServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder = p1 as NotificationService.NotificationBinder?
            mNotificationService = binder?.getService()
            (application as MiApplication?)?.currentAccount?.value?.let{
                mNotificationService?.stopShowPushNotification(it.account)
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            mNotificationService = null
        }


    }

    private fun setBackgroundImage(){
        val path = SettingStore(getSharedPreferences(getPreferenceName() ,Context.MODE_PRIVATE)).backgroundImagePath
        Glide.with(this)
            .load(path)
            .into(backgroundImage)
    }

    private fun applyUI(){
        invalidateOptionsMenu()
        bottom_navigation.visibility = if(getSettingStore().isClassicUI){
            View.GONE
        }else{
            View.VISIBLE
        }
        if(getSettingStore().isClassicUI){
            mBottomNavigationAdapter?.setCurrentFragment(R.id.navigation_home)
        }
    }

}
