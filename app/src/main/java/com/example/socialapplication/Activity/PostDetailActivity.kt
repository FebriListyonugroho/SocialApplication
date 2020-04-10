@file:Suppress("DEPRECATION")

package com.example.socialapplication.Activity

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.*
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialapplication.Adapter.AdapterComments
import com.example.socialapplication.Model.ModelComment
import com.example.socialapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.ArrayList

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class PostDetailActivity : BaseActivity() {

    //get detail dari user
    private var myUid : String? = null
    private var myEmail : String? = null
    private var myName : String? = null
    private var myDp : String? = null
    private var postId : String? = null
    private var pLikes : String? = null
    private var hisDp : String? = null
    private var hisName : String? = null

    private var mProcessComment : Boolean? = false
    private var mProcessLike : Boolean? = false

    //progressDialog
    private lateinit var pd : ProgressDialog

    //FIREBASE
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDatabase: FirebaseDatabase
    private lateinit var mStorage: FirebaseStorage

    private var hisUid : String? = null
    private var pImage : String? = null

    //view
    private var uPictureIv : ImageView? = null
    private var pImageIv : ImageView? = null
    private var uNameTv : TextView? = null
    private var pTimeTv : TextView? = null
    private var pTitleTv : TextView? = null
    private var pDescriptionTv : TextView? = null
    private var pCommentsTv : TextView? = null
    private var pLikesTv : TextView? = null
    private var moreBtn : ImageButton? = null
    private var likeBtn : Button? = null
    private var shareBtn : Button? = null
    private var chatBtn : Button? = null
    private var profileLayout : LinearLayout? = null
    private var recylerView : RecyclerView? = null
    private var hargaTv : TextView? = null

    private var commentList : List<ModelComment>? = null
    private var adapterComment : AdapterComments? = null

    //add comment view layout
    private var commentEt : EditText? = null
    private var sendBtn : ImageButton? = null
    private var cAvatarIv : ImageView? = null
    private var poUid : String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_detail)
        findView()
        initView()
        initListeners()
    }

    override fun findView() {
        uPictureIv = findViewById(R.id.uPictureIv)
        pImageIv = findViewById(R.id.pImageIv)
        uNameTv = findViewById(R.id.uNameTv)
        pTimeTv = findViewById(R.id.pTimeTv)
        pTitleTv = findViewById(R.id.pTitleTv)
        pDescriptionTv = findViewById(R.id.pDescriptionTv)
        pCommentsTv = findViewById(R.id.pCommentsTv)
        pLikesTv = findViewById(R.id.pLikeTv)
        moreBtn = findViewById(R.id.moreBtn)
        likeBtn = findViewById(R.id.likeBtn)
        shareBtn = findViewById(R.id.shareBtn)
        chatBtn = findViewById(R.id.chatBtn)
        profileLayout = findViewById(R.id.profileLayout)
        recylerView = findViewById(R.id.recyclerView)
        hargaTv = findViewById(R.id.hargaTv)

        commentEt = findViewById(R.id.commentEt)
        sendBtn = findViewById(R.id.sendBtn)
        cAvatarIv = findViewById(R.id.cAvatarIv)
    }

    override fun initView() {
        val actionBar = supportActionBar
        actionBar!!.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        actionBar.title = "Detail"

        shareBtn!!.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_share, 0, 0, 0)

        mAuth = FirebaseAuth.getInstance()
        mStorage = FirebaseStorage.getInstance()
        mDatabase = FirebaseDatabase.getInstance()

        //mendapat get intent dari post adapter
        postId = intent.getStringExtra("postId")

        loadPostInfo()
        checkUserStatus()

        loadUserInfo()

        setLikes()

        loadComments()

        //set subtitle pada action bar


    }

    override fun initListeners() {

        //set komen button click
        sendBtn!!.setOnClickListener {

            postComment()
        }

        likeBtn!!.setOnClickListener {

            likePost()
        }

        moreBtn!!.setOnClickListener {

            showMoreOptions()
        }

        if (myUid == hisUid){

            chatBtn!!.visibility = View.INVISIBLE

        }else{

            chatBtn!!.visibility = View.VISIBLE
            chatBtn!!.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_chat_user, 0, 0,0)
            chatBtn!!.setOnClickListener {

                //chat klik
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("hisUid", hisUid)
                startActivity(intent)

            }
        }

        shareBtn!!.setOnClickListener {

            val pTitle = pTitleTv?.text.toString().trim()
            val pDescription = pDescriptionTv?.text.toString().trim()

            val bitmapDrawable: BitmapDrawable? = pImageIv!!.drawable as BitmapDrawable
            if (bitmapDrawable == null){

                //share postingan tanpa image
                shareTextOnly(pTitle, pDescription)
            }else{

                //share postingan menggunakan image
                val bitmap : Bitmap? = bitmapDrawable.bitmap
                shareImageAndText(pTitle, pDescription, bitmap)
            }
        }



        profileLayout!!.setOnClickListener {

            //klik ke thereprofileactivity dengan uid
            val intent = Intent(this, ThereProfileActivity::class.java)
            intent.putExtra("hisUid", poUid)
            startActivity(intent)
        }
    }

    private fun shareImageAndText(pTitle: String?, pDescription: String?, bitmap: Bitmap?) {

        val shareBody  = pTitle +"\n"+ pDescription

        val uri = saveImageToShare(bitmap)

        val sIntent = Intent(Intent.ACTION_SEND)
        sIntent.putExtra(Intent.EXTRA_STREAM, uri)
        sIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here")
        sIntent.type = "image/png"
        startActivity(Intent.createChooser(sIntent, "Berbagi Via"))
    }

    private fun saveImageToShare(bitmap: Bitmap?): Uri? {

        val imageFolder = File(cacheDir, "images")
        var uri : Uri? = null
        try {
            imageFolder.mkdirs()
            val file = File(imageFolder, "share_image.png")

            val stream = FileOutputStream(file)
            bitmap?.compress(Bitmap.CompressFormat.PNG, 90, stream)
            stream.flush()
            stream.close()
            uri = FileProvider.getUriForFile(this, "com.example.socialapplication.fileprovider",
                file)
        }catch (e : Exception){
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }
        return uri
    }

    private fun shareTextOnly(pTitle: String?, pDescription: String?) {

        val shareBody = pTitle +"\n"+ pDescription

        //share intent
        val sIntent : Intent? = Intent(Intent.ACTION_SEND)
        sIntent!!.type = "text/plain"
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here")
        sIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        startActivity(Intent.createChooser(sIntent, "Berbagi Via"))
    }

    private fun loadComments() {

        val layoutManager = LinearLayoutManager(applicationContext)
        //set layout di recyclerView
        recylerView!!.layoutManager = layoutManager

        commentList = ArrayList()

        val ref  = mDatabase.getReference("Posts").child(postId!!).child("Comments")

        val postData = object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {

                (commentList as ArrayList).clear()
                for (ds : DataSnapshot? in p0.children){

                    val modelComment  = ds!!.getValue(ModelComment::class.java)

                    (commentList as ArrayList).add(modelComment!!)

                    //setUpAdapter
                    adapterComment = AdapterComments(applicationContext, commentList, myUid!!, postId!!)
                    //setAdapter
                    recylerView!!.adapter = adapterComment

                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@PostDetailActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        ref.addValueEventListener(postData)

    }

    private fun showMoreOptions() {

        //membuat menu popup saat ini memiliki opsi hapus, kami akan menambahkan lebih banyak opsi nanti
        val popupMenu = PopupMenu(this, moreBtn, Gravity.END)

        //tampil opsi hapus di semua post
        if (hisUid == myUid){
            //add items di menu
            popupMenu.menu.add(Menu.NONE, 0, 0, "Delete")
            popupMenu.menu.add(Menu.NONE, 1, 0, "Edit")

        }
        popupMenu.menu.add(Menu.NONE, 2, 0, "Detail")

        //item klik listener
        popupMenu.setOnMenuItemClickListener { item ->
            val id = item!!.itemId
            if (id == 0){

                //delete di klik
                beginDelete()

            }else if (id == 1){

                val intent = Intent(this, AddPostActivity::class.java)
                intent.putExtra("key", "editPost")
                intent.putExtra("editPostId", postId)
                startActivity(intent)

            }


            false
        }
        //show popup
        popupMenu.show()
    }

    private fun beginDelete() {

        if (pImage == "noImage"){

            //post tanpa gambar
            deleteWithoutImage()

        }else{

            //post dengan gambar
            deleteWithImage()

        }
    }

    private fun deleteWithImage() {

        val pd = ProgressDialog(this)
        pd.setMessage("Menghapus ...")

        //langkah
        //1.) delete image menggunakan uri
        //2.) delete dalam database mengunakan post id

        val picRef = mStorage.getReferenceFromUrl(pImage!!)
        picRef.delete()
            .addOnSuccessListener {

                val fquery = mDatabase.getReference("Posts").orderByChild("pId").equalTo(postId)

                val postData = object : ValueEventListener{

                    override fun onDataChange(p0: DataSnapshot) {

                        for (ds : DataSnapshot in p0.children){

                            ds.ref.removeValue()

                        }
                        //terhapus
                        Toast.makeText(this@PostDetailActivity, "Berhasil Dihapus", Toast.LENGTH_SHORT).show()
                        pd.dismiss()
                    }

                    override fun onCancelled(p0: DatabaseError) {
                        pd.dismiss()
                        Toast.makeText(this@PostDetailActivity, p0.message, Toast.LENGTH_SHORT).show()
                    }
                }
                fquery.addValueEventListener(postData)

            }.addOnFailureListener { p0 ->

                pd.dismiss()
                Toast.makeText(this, p0.message, Toast.LENGTH_SHORT).show()

            }
    }

    private fun deleteWithoutImage() {

        val pd = ProgressDialog(this)
        pd.setMessage("Menghapus ...")

        val fquery = mDatabase.getReference("Posts").orderByChild("pId").equalTo(postId)

        val postData = object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {
                for (ds : DataSnapshot in p0.children){

                    ds.ref.removeValue()

                }
                //terhapus
                Toast.makeText(this@PostDetailActivity, "Berhasil Dihapus", Toast.LENGTH_SHORT).show()
                pd.dismiss()
            }

            override fun onCancelled(p0: DatabaseError) {
                pd.dismiss()
                Toast.makeText(this@PostDetailActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        fquery.addValueEventListener(postData)

    }

    private fun setLikes() {

        val likesRef = mDatabase.reference.child("Likes")

        val postData = object : ValueEventListener{

            @SuppressLint("SetTextI18n")
            override fun onDataChange(p0: DataSnapshot) {
                if (p0.child(postId!!).hasChild(myUid!!)){

                    likeBtn!!.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_red, 0,0,0)
                }else{

                    likeBtn!!.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0,0,0)
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@PostDetailActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        likesRef.addValueEventListener(postData)

    }

    private fun likePost() {

        mProcessLike = true

        val likesRef  = mDatabase.reference.child("Likes")
        val postsRef  = mDatabase.reference.child("Posts")

        val postData = object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {

                if (mProcessLike!!){

                    mProcessLike = if(p0.child(postId!!).hasChild(myUid!!)){

                        //telah dilike, jadi hapus like
                        postsRef.child(postId!!).child("pLikes").setValue("${(Integer.parseInt(pLikes)-1)}")
                        likesRef.child(postId!!).child(myUid!!).removeValue()
                        false

                    }else{

                        //tidak like, melakukan like
                        postsRef.child(postId!!).child("pLikes").setValue("${(Integer.parseInt(pLikes)+1)}")
                        likesRef.child(postId!!).child(myUid!!).setValue("Batal")
                        false

                    }

                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@PostDetailActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        likesRef.addValueEventListener(postData)

    }

    private fun postComment() {

        pd = ProgressDialog(this)
        pd.setMessage("Menambahkan Komentar ...")

        //get data dari edit text komentar
        val comment : String? = commentEt!!.text.toString().trim()

        //validasi
        if (TextUtils.isEmpty(comment)){

            Toast.makeText(this, "Komentar Kosong ...", Toast.LENGTH_SHORT).show()
            return
        }

        val timeStamp  = System.currentTimeMillis().toString()

        //setiap posting akan memiliki child "Comments" yang akan berisi komentar dari posting itu
        val ref  = mDatabase.getReference("Posts").child(postId!!).child("Comments")

        val hashMap : HashMap<String, Any>? = HashMap()
        hashMap?.set("cId", timeStamp)
        hashMap?.set("comment", comment!!)
        hashMap?.set("timestamp", timeStamp)
        hashMap?.set("uid", myUid!!)
        hashMap?.set("uEmail", myEmail!!)
        hashMap?.set("uDp", myDp!!)
        hashMap?.set("uName", myName!!)

        //INPUT DATA DI DATABASE
        ref.child(timeStamp).setValue(hashMap).addOnSuccessListener {
            //JIKA BERHASIL
            pd.dismiss()
            Toast.makeText(this, "Komentar Berhasil Ditambahkan ...", Toast.LENGTH_SHORT).show()
            commentEt!!.setText("")
            updateCommentCount()

        }.addOnFailureListener { exception ->
            //JIKA GAGAL
            pd.dismiss()
            Toast.makeText(this, exception.message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateCommentCount() {

        // kapan pun pengguna menambahkan komentar, tambahkan jumlah komentar seperti yang kami lakukan untuk jumlah yang sama
        mProcessComment = true
        val ref = mDatabase.getReference("Posts").child(postId!!)

        val postData = object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {

                if (mProcessComment!!){

                    val comments : String? = ""+ p0.child("pComments").value
                    val newCommentVal : Int? = Integer.parseInt(comments)+1
                    ref.child("pComments").setValue(""+newCommentVal)
                    mProcessComment = false
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@PostDetailActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        ref.addListenerForSingleValueEvent(postData)

    }

    override fun onSupportNavigateUp(): Boolean {

        onBackPressed()
        return super.onSupportNavigateUp()
    }

    private fun checkUserStatus(){

        val user  = mAuth.currentUser
        if (user != null){

            //user telah terdaftar
            myEmail = user.email
            myUid = user.uid
        }else{

            //user belum terdaftar
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun loadUserInfo() {

        //getInfo User
        val myRef = mDatabase.getReference("Users")

        val postData = object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {

                for (ds : DataSnapshot? in p0.children){

                    myName = ""+ds!!.child("name").value
                    myDp = ""+ds.child("image").value

                    //setData
                    try {

                        Picasso.get().load(myDp).placeholder(R.drawable.ic_default_img).into(cAvatarIv)
                    }catch (e : Exception){

                        Picasso.get().load(R.drawable.ic_default_img).into(cAvatarIv)
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@PostDetailActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        myRef.orderByChild("uid").equalTo(myUid).addListenerForSingleValueEvent(postData)

    }

    private fun loadPostInfo() {

        val ref  = mDatabase.getReference("Posts")
        val query  = ref.orderByChild("pId").equalTo(postId)

        val postData = object : ValueEventListener{

            @SuppressLint("SetTextI18n")
            override fun onDataChange(p0: DataSnapshot) {

                for (ds : DataSnapshot? in p0.children){

                    //getData
                    val pTitle : String?  = ""+ds!!.child("pTitle").value
                    val pDescr : String? = ""+ds.child("pDescr").value
                    pLikes = ""+ds.child("pLikes").value
                    val pTimeStamp : String? = ""+ds.child("pTime").value
                    val pHarga : String? = ""+ds.child("pHarga").value
                    val pMasa : String? = ""+ds.child("pMasa").value
                    pImage = ""+ds.child("pImage").value
                    hisDp = ""+ds.child("uDp").value
                    hisUid = ""+ds.child("uid").value
                    var Email : String? = ""+ds.child("uEmail").value
                    poUid = ""+ds.child("uid").value
                    hisName = ""+ds.child("uName").value
                    val commentCount : String? = ""+ds.child("pComments").value

                    val calender = Calendar.getInstance(Locale.getDefault())
                    calender.timeInMillis = pTimeStamp!!.toLong()
                    val pTime = android.text.format.DateFormat.format("dd/MM/yyyy hh:mm aa", calender).toString()

                    //setData
                    pTitleTv!!.text = pTitle
                    pDescriptionTv!!.text = pDescr
                    pLikesTv!!.text = "$pLikes Likes"
                    pTimeTv!!.text = pTime
                    uNameTv!!.text = hisName
                    hargaTv!!.text = "Rp. $pHarga /$pMasa"
                    pCommentsTv!!.text = "$commentCount Komentar"

                    //set post image
                    //jika tidak ada image postnya
                    if (pImage.equals("noImage")){

                        pImageIv!!.visibility = View.GONE

                    }else{

                        pImageIv!!.visibility = View.VISIBLE

                        try {
                            Picasso.get().load(pImage).into(pImageIv)
                        }catch (e : Exception){

                        }


                    }

                    //set user image di comment
                    try {
                        Picasso.get().load(hisDp).placeholder(R.drawable.ic_default_img).into(uPictureIv)
                    }catch (e: Exception){
                        Picasso.get().load(R.drawable.ic_default_img).into(uPictureIv)
                    }

                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@PostDetailActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        query.addValueEventListener(postData)

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_main, menu)
        //menghilangkan menu item add post dan search
        menu!!.findItem(R.id.action_add_post).isVisible = false
        menu.findItem(R.id.action_search).isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        val id = item!!.itemId
        if (id == R.id.action_logout){

            FirebaseAuth.getInstance().signOut()
            checkUserStatus()

        }
        return super.onOptionsItemSelected(item)
    }
}
