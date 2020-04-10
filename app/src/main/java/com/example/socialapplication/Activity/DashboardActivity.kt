package com.example.socialapplication.Activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Html
import android.widget.SearchView
import androidx.fragment.app.Fragment
import com.example.socialapplication.*
import com.example.socialapplication.Fragment.*
import com.example.socialapplication.Notification.Token
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.iid.FirebaseInstanceId

@Suppress("DEPRECATION")
class DashboardActivity : BaseActivity() {

    private lateinit var firebaseAuth : FirebaseAuth
    private lateinit var mDatabase: FirebaseDatabase
    private var mUID : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        //findView()
        initView()

    }

    override fun findView() {



    }

    override fun initView() {

        firebaseAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()

        val actionBar = supportActionBar
        actionBar!!.setBackgroundDrawable(ColorDrawable(Color.parseColor("#ffffff")))

        val navigationView = findViewById<BottomNavigationView>(R.id.navigation)
        navigationView.setOnNavigationItemSelectedListener(selectedListener)

        actionBar.title = Html.fromHtml("<font color='#D3A537'>Home</font>")
        val fragment = HomeFragment()
        addFragment(fragment)

        checkUserStatus()


    }

    override fun onResume() {
        checkUserStatus()
        super.onResume()
    }

    private fun updateToken(token: String?){

        val ref = mDatabase.getReference("Tokens")
        val mToken = Token(token)
        ref.child(mUID!!).setValue(mToken)

    }

    private val selectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.nav_home -> {
                val actionBar = supportActionBar
                actionBar!!.title = Html.fromHtml("<font color='#D3A537'>Home</font>")
                val fragment =
                    HomeFragment()
                addFragment(fragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_user -> {
                val actionBar = supportActionBar
                actionBar!!.title = Html.fromHtml("<font color='#D3A537'>Search User</font>")
                val fragment =
                    UserFragment()
                addFragment(fragment)
                return@OnNavigationItemSelectedListener true
            }
            R.id.nav_chat -> {
                val actionBar = supportActionBar
                actionBar!!.title = Html.fromHtml("<font color='#D3A537'>Chat</font>")
                val fragment =
                    ChatListFragment()
                addFragment(fragment)
                return@OnNavigationItemSelectedListener true
            }

            R.id.nav_profile -> {
                val actionBar = supportActionBar
                actionBar!!.title = Html.fromHtml("<font color='#D3A537'>Profile</font>")
                val fragment =
                    ProfileFragment()
                addFragment(fragment)
                return@OnNavigationItemSelectedListener true
            }

        }
        false
    }

    private fun addFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content, fragment, "")
            .commit()
    }

    override fun initListeners() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun checkUserStatus(){

        val user = firebaseAuth.currentUser

        if(user != null){

            //mProfileTv.setText(user.email)
            mUID = user.uid

            //simpan uid dari pengguna yang saat ini masuk dalam preferensi bersama
            val sp = getSharedPreferences("SP_USER", Context.MODE_PRIVATE)
            val editor = sp.edit()
            editor.putString("Current_USERID", mUID)
            editor.apply()

            //update Token
            updateToken(FirebaseInstanceId.getInstance().token)

        }else{

            startActivity(Intent(this, MainActivity::class.java))
            finish()

        }

    }

    override fun onBackPressed() {

        super.onBackPressed()
        finish()

    }

    override fun onStart() {

        checkUserStatus()
        super.onStart()

    }
}
