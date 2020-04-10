package com.example.socialapplication.Activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialapplication.Adapter.AdapterPosts
import com.example.socialapplication.Model.ModelPost
import com.example.socialapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.lang.Exception

@Suppress("ControlFlowWithEmptyBody", "DEPRECATION")
class ThereProfileActivity : BaseActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var avatarIv : ImageView
    private lateinit var coverIv : ImageView
    private lateinit var nameTv : TextView
    private lateinit var emailTv : TextView
    private lateinit var phoneTv : TextView

    private var postsRecyclerView : RecyclerView? = null
    private var postList : List<ModelPost>? = null
    private var adapterPosts: AdapterPosts? = null
    private var uid : String? = null

    private lateinit var mDatabase: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_there_profile)

        findView()
        initView()

    }

    override fun findView() {

        postsRecyclerView = findViewById(R.id.recyclerview_posts)
        avatarIv = findViewById(R.id.avatarIv)
        coverIv = findViewById(R.id.coverIv)
        nameTv = findViewById(R.id.nameTv)
        emailTv = findViewById(R.id.emailTv)
        phoneTv = findViewById(R.id.phoneTv)

    }

    override fun initView() {

        mDatabase = FirebaseDatabase.getInstance()

        val actionBar = supportActionBar
        actionBar!!.setBackgroundDrawable(ColorDrawable(Color.parseColor("#ffffff")))
        actionBar.title = "Profile"
        actionBar.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        firebaseAuth = FirebaseAuth.getInstance()

        //get uid klik user untuk melihat postingan
        val intent = intent
        uid = intent.getStringExtra("Uid")

        postList = ArrayList()

        checkUserStatus()

        readHistDataProfile()

        loadHistPosts()

    }

    override fun initListeners() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun readHistDataProfile() {

        val query = mDatabase.getReference("Users").orderByChild("uid").equalTo(uid)

        val postData = object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {

                for (ds in p0.children){

                    val name = "${ds.child("name").value}"
                    val email = "${ds.child("email").value}"
                    val phone = "${ds.child("phone").value}"
                    val image = "${ds.child("image").value}"
                    val cover = "${ds.child("cover").value}"

                    nameTv.text = name
                    emailTv.text = email
                    phoneTv.text = phone
                    try {
                        // ketika mendapat data image
                        Picasso.get().load(image).into(avatarIv)

                    }catch (e : Exception){

                        // jika tidak maka akan tampil image default

                    }

                    try {
                        // ketika mendapat data image
                        Picasso.get().load(cover).into(coverIv)

                    }catch (e : Exception){

                        // jika tidak maka akan tampil image default
                        Picasso.get().load(R.drawable.cover_default).into(coverIv)

                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@ThereProfileActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        query.addValueEventListener(postData)

    }

    private fun loadHistPosts() {

        ///linear layout untuk recyclerview
        val layoutManager = LinearLayoutManager(this)
        //tampilkan posting terbaru terlebih dahulu, untuk memuat ini dari yang terakhir
        layoutManager.stackFromEnd = true
        layoutManager.reverseLayout = true
        //set layout untuk recyclerview
        postsRecyclerView!!.layoutManager = layoutManager

        //mendapat data post
        val ref = mDatabase.getReference("Posts")
        //query untuk load post
        val query = ref.orderByChild("uid").equalTo(uid)
        //dapat semua data

        val postData = object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {

                (postList as ArrayList).clear()
                for (ds : DataSnapshot in p0.children){

                    val myPost = ds.getValue(ModelPost::class.java)

                    //add to list
                    (postList as ArrayList).add(myPost!!)

                    //adapter
                    adapterPosts = AdapterPosts(this@ThereProfileActivity, postList!!)
                    //set adapter untuk recycler view
                    postsRecyclerView!!.adapter = adapterPosts

                }
            }

            override fun onCancelled(p0: DatabaseError) {

                Toast.makeText(this@ThereProfileActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }

        query.addValueEventListener(postData)

    }

    private fun searchHistPosts(searchQuery : String){

        ///linear layout untuk recyclerview
        val layoutManager = LinearLayoutManager(this)
        //tampilkan posting terbaru terlebih dahulu, untuk memuat ini dari yang terakhir
        layoutManager.stackFromEnd = true
        layoutManager.reverseLayout = true
        //set layout untuk recyclerview
        postsRecyclerView!!.layoutManager = layoutManager

        //mendapat data post
        val ref = mDatabase.getReference("Posts")
        //query untuk load post
        val query = ref.orderByChild("uid").equalTo(uid)
        //dapat semua data

        val postData = object : ValueEventListener{

            @SuppressLint("DefaultLocale")
            override fun onDataChange(p0: DataSnapshot) {

                (postList as ArrayList).clear()
                for (ds : DataSnapshot in p0.children){

                    val myPost = ds.getValue(ModelPost::class.java)

                    if (myPost!!.pTitle.toLowerCase().contains(searchQuery.toLowerCase()) ||
                        myPost.pDescr.toLowerCase().contains(searchQuery.toLowerCase())){

                        //add to list
                        (postList as ArrayList).add(myPost)

                    }

                    //adapter
                    adapterPosts = AdapterPosts(this@ThereProfileActivity, postList!!)
                    //set adapter untuk recycler view
                    postsRecyclerView!!.adapter = adapterPosts

                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@ThereProfileActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        query.addValueEventListener(postData)

    }

    private fun checkUserStatus(){

        val user = firebaseAuth.currentUser

        if(user != null){

            //mProfileTv.setText(user.email)

        }else{

            startActivity(Intent(this, MainActivity::class.java))
            finish()

        }

    }

    override fun onSupportNavigateUp(): Boolean {

        onBackPressed()
        return super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu!!.findItem(R.id.action_add_post).isVisible = false
        menu.findItem(R.id.action_logout).isVisible = false

        val item = menu.findItem(R.id.action_search)

        //search postingan user
        val searchView = MenuItemCompat.getActionView(item) as SearchView

        val searchV = object : SearchView.OnQueryTextListener{

            override fun onQueryTextChange(newText: String?): Boolean {

                if (!TextUtils.isEmpty(newText)){

                    searchHistPosts(newText!!)

                }else{

                    loadHistPosts()

                }

                return false
            }

            override fun onQueryTextSubmit(query: String?): Boolean {
                if (!TextUtils.isEmpty(query)){

                    searchHistPosts(query!!)

                }else{

                    loadHistPosts()

                }

                return false
            }
        }

        searchView.setOnQueryTextListener(searchV)

        return super.onCreateOptionsMenu(menu)
    }



}
