@file:Suppress("DEPRECATION")

package com.example.socialapplication.Activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.*
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.socialapplication.Adapter.AdapterChat
import com.example.socialapplication.Model.ModelChat
import com.example.socialapplication.Model.ModelUsers
import com.example.socialapplication.Notification.*
import com.example.socialapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

@Suppress("DEPRECATION")
class ChatActivity : BaseActivity() {

    private lateinit var toolbar : Toolbar
    private lateinit var recyclerView : RecyclerView
    private lateinit var profileIv : ImageView
    private lateinit var nameTv : TextView
    private lateinit var userStatusTv : TextView
    private lateinit var messageEt : EditText
    private lateinit var attachBtn : ImageButton
    private lateinit var sendBtn : ImageButton

    private lateinit var seenListener : ValueEventListener
    private lateinit var userRefForSeen : DatabaseReference

    //permission konstan
    private val CAMERA_REQUEST_CODE = 100
    private val STORAGE_REQUEST_CODE = 200

    //image pick konstan
    private val IMAGE_PICK_CAMERA_CODE = 300
    private val IMAGE_PICK_GALLERY_CODE = 400

    private var imageUri : Uri? = null

    //permission array
    private lateinit var cameraPermissions : Array<String>
    private lateinit var storagePermissions : Array<String>

    private lateinit var chatList: List<ModelChat>
    private lateinit var adapterChat: AdapterChat

    private lateinit var firebaseAuth : FirebaseAuth
    private lateinit var mDatabase : FirebaseDatabase
    private lateinit var usersDbRef : DatabaseReference

    private var requestQueue : RequestQueue? = null

    private var hisUid : String? = null
    private var myUid : String? = null
    private var hisImage : String? = null

    private var notify : Boolean? = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        findView()
        initView()
        initListeners()

    }

    override fun findView() {

        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.chat_recyclerView)
        profileIv = findViewById(R.id.profileIv)
        nameTv = findViewById(R.id.nameTv)
        userStatusTv = findViewById(R.id.userStatusTv)
        messageEt = findViewById(R.id.messageEt)
        sendBtn = findViewById(R.id.sendBtn)
        attachBtn = findViewById(R.id.attachBtn)

    }

    override fun initView() {

        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.stackFromEnd = true

        requestQueue = Volley.newRequestQueue(applicationContext)

        //init permissions array
        cameraPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = linearLayoutManager

        setSupportActionBar(toolbar)
        toolbar.title = ""

        ///mendapat getextraString dari adapterUser
        val intent = intent
        hisUid = intent.getStringExtra("hisUid")

        firebaseAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()
        usersDbRef = mDatabase.getReference("Users")

        readDataToolbarChat()
        readMessages()
        seenMessage()

    }

    override fun initListeners() {

        sendBtn.setOnClickListener {

            notify = true

            cekDataMessage()

        }

        //handel klik attach btn
        attachBtn.setOnClickListener {

            showImagePickDialog()
        }

        ///ketika klik edit text kirim pesan
        messageEt.addTextChangedListener(object : TextWatcher{

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                if (s.toString().trim().isEmpty()){

                    checkTypingStatus("noOne")

                }else{

                    checkTypingStatus(hisUid!!)

                }

            }

            override fun afterTextChanged(s: Editable?) {

            }

        })

    }

    private fun showImagePickDialog() {

        val options = arrayOf("Camera", "Gallery")

        //dialog
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pilih Menu Opsi")

        builder.setItems(options) { _, which ->

            if (which == 0){

                //klik camera
                if (!checkCameraPermission()){

                    requestCameraPermission()

                }else{

                    pickFromCamera()

                }

            }
            if (which == 1){

                //klik gallery
                if (!checkStoragePermission()){

                    requestStoragePermission()

                }else{

                    pickFromGallery()

                }

            }

        }
        //create dan show dialog
        builder.create().show()
    }

    private fun pickFromGallery() {

        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE)

    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun pickFromCamera() {

        //intent untuk memilih gambar dari kamera
        val cv = ContentValues()
        cv.put(MediaStore.Images.Media.TITLE, "Temp Pick")
        cv.put(MediaStore.Images.Media.DESCRIPTION, "Temp Descr")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE)

    }

    private fun checkStoragePermission() : Boolean{

        //periksa apakah izin penyimpanan diaktifkan atau tidak
        //return true jika aktif
        //return false jika tidak aktif

        return ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED)

    }

    private fun requestCameraPermission(){

        //meminta izin camera runtime
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE)

    }

    private fun checkCameraPermission() : Boolean{

        //periksa apakah izin akses camera diaktifkan atau tidak
        //return true jika aktif
        //return false jika tidak aktif
        val result = ContextCompat.checkSelfPermission(this,
            Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED)
        val result1 = ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED)
        return result && result1

    }

    private fun requestStoragePermission(){

        //meminta izin penyimpanan runtime
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE)

    }

    private fun cekDataMessage() {

        val message = messageEt.text.toString().trim()

        if (TextUtils.isEmpty(message)){

            Toast.makeText(this, "Tidak ada pesan dikirim", Toast.LENGTH_SHORT).show()
            return

        }else{

            sendMessage(message)

        }

        messageEt.setText("")

    }

    private fun readDataToolbarChat() {

        val userQuery = usersDbRef.orderByChild("uid").equalTo(hisUid)

        val postData = object :ValueEventListener{
            @SuppressLint("SetTextI18n")
            override fun onDataChange(p0: DataSnapshot) {

                for (ds : DataSnapshot in p0.children){

                    val name = ""+ ds.child("name").value
                    hisImage = ""+ ds.child("image").value
                    val typingStatus = ""+ ds.child("typingTo").value
                    //get data online status
                    val onlineStatus = ""+ ds.child("onlineStatus").value

                    if (typingStatus == myUid){

                        userStatusTv.text = "sedenag mengetik ..."

                    }else{

                        if (onlineStatus == "online"){

                            userStatusTv.text = onlineStatus

                        }else{

                            val cal = Calendar.getInstance(TimeZone.getDefault())
                            cal.timeInMillis = java.lang.Long.parseLong(onlineStatus)

                            val dateTime = android.text.format.DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString()
                            userStatusTv.text = "Terakhir online $dateTime"

                        }

                    }

                    nameTv.text = name

                    ///mendapat data image
                    try {

                        //jika berhasil mengantikan default image
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_default_img).into(profileIv)


                    }catch (e : Exception){

                        //jika gagal akan kembali default image
                        Picasso.get().load(R.drawable.ic_default_img).into(profileIv)

                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@ChatActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        userQuery.addValueEventListener(postData)

    }

    private fun seenMessage() {

        userRefForSeen = mDatabase.getReference("Chats")

        val postData = object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {
                for (ds : DataSnapshot in p0.children){

                    val chat = ds.getValue(ModelChat::class.java)

                    if (chat!!.receiver.equals(myUid) && chat.sender.equals(hisUid)){

                        val infoMsgSeen : HashMap<String, Any> = HashMap()
                        infoMsgSeen["isSeen"] = true
                        ds.ref.updateChildren(infoMsgSeen)

                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@ChatActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        seenListener = userRefForSeen.addValueEventListener(postData)

    }

    private fun readMessages() {

        chatList = ArrayList()

        val dbRef = mDatabase.getReference("Chats")
        //membaca data
        val postChat = object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {

                (chatList as ArrayList).clear()
                for (ds : DataSnapshot in p0.children){

                    val chat = ds.getValue(ModelChat::class.java)

                    if (chat!!.receiver.equals(myUid) && chat.sender.equals(hisUid) ||
                        chat.receiver.equals(hisUid) && chat.sender.equals(myUid)){

                        (chatList as ArrayList).add(chat)
                    }

                }
                adapterChat = AdapterChat(this@ChatActivity, chatList, hisImage!!)
                adapterChat.notifyDataSetChanged()

                recyclerView.adapter = adapterChat
            }

            override fun onCancelled(p0: DatabaseError) {

                Toast.makeText(this@ChatActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        dbRef.addValueEventListener(postChat)

    }

    private fun sendMessage(message: String) {

        val databaseReference = mDatabase.reference

        val timestamp = (System.currentTimeMillis()).toString()

        val hashMap : HashMap<String, Any>? = HashMap()
        hashMap?.set("sender", myUid!!)///uid pengguna
        hashMap?.set("receiver", hisUid!!)//uid penerima
        hashMap?.set("message", message)
        hashMap?.set("timestamp", timestamp)
        hashMap?.set("isSeen", false)
        hashMap?.set("type", "text")

        databaseReference.child("Chats").push().setValue(hashMap)


        val database = mDatabase.getReference("Users").child(myUid!!)

        val postData1 = object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {
                val user = p0.getValue(ModelUsers::class.java)

                if (notify!!){

                    senNotification(hisUid!!, user!!.name, message)

                }
                notify = false
            }


            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@ChatActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        database.addValueEventListener(postData1)

        //membuat chatlist node pada firebase database
        val chatRef1  = mDatabase.getReference("Chatlist")
            .child(myUid!!)
            .child(hisUid!!)

        val postData2 = object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {

                chatRef1.child("id").setValue(hisUid)
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@ChatActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        chatRef1.addValueEventListener(postData2)

        val chatRef2  = mDatabase.getReference("Chatlist")
            .child(hisUid!!)
            .child(myUid!!)
        val postData3 = object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {
                chatRef2.child("id").setValue(myUid)
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@ChatActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        chatRef2.addValueEventListener(postData3)
    }

    private fun senNotification(hisUid: String, name: String?, message: String) {

        val allTokens = mDatabase.getReference("Tokens")
        val query = allTokens.orderByKey().equalTo(hisUid)

        val postData = object :ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {

                for (ds: DataSnapshot in p0.children){

                    val token = ds.getValue(Token::class.java)
                    val data = Data(myUid, "$name : $message", "Pesan Baru", hisUid, R.drawable.ic_default_img)

                    val sender = Sender(data, token!!.token)


                    //fcm json object request
                    val senderJsonObj = JSONObject(Gson().toJson(sender))
                    val jsonObjectRequest = object : JsonObjectRequest("http://fcm.googleapis.com/fcm/send", senderJsonObj,
                        Response.Listener<JSONObject>{response ->

                            //response dari request
                            Log.d("JSON_RESPONSE", "onResponse : $response")

                        },Response.ErrorListener {error ->

                            //error
                            Log.d("JSON_RESPONSE", "onResponse : $error")
                        })
                    {

                        @Throws(AuthFailureError::class)
                        override fun getHeaders(): Map<String, String> {

                            val headers : HashMap<String, String> = HashMap()
                            headers["Content-Type"] = "application/json"
                            headers["Authorization"] = "key=AAAAZv6orus:APA91bHNkS9rviEeX7D2CTdImvBhscIoPt0l7zVLxNCiucgrTw6Vc9ppj0IiBwGdzx4b_4O9h312jjPc5Qt7_p9DQMgATI62GkOjZtgtcA8ng_6oBIybGc9wDcTU81SPLQBKnwynHWI3"

                            return headers
                        }
                    }
                    //add ini untuk request queque
                    requestQueue!!.add(jsonObjectRequest)
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@ChatActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        query.addValueEventListener(postData)

    }

    private fun checkUserStatus(){

        val user = firebaseAuth.currentUser

        if(user != null){

            myUid = user.uid
            //mProfileTv.setText(user.email)

        }else{

            startActivity(Intent(this, MainActivity::class.java))
            finish()

        }

    }

    private fun checkOnlineStatus( status : String){

        val dbRef = mDatabase.getReference("Users").child(myUid!!)

        val hashMap : HashMap<String, Any>? = HashMap()
        hashMap?.set("onlineStatus", status)

        if (hashMap != null) {
            dbRef.updateChildren(hashMap)
        }


    }

    private fun checkTypingStatus(typing : String){

        val dbRef = mDatabase.getReference("Users").child(myUid!!)

        val hashMap : HashMap<String, Any?>? = HashMap()
        hashMap?.set("typingTo", typing)

        if (hashMap != null) {
            dbRef.updateChildren(hashMap)
        }


    }

    override fun onStart() {

        checkUserStatus()
        super.onStart()
        ///ketika buka langsung online
        checkOnlineStatus("online")

    }

    override fun onPause() {
        super.onPause()

        //get time
        val timestamp = System.currentTimeMillis().toString()

        //cek terakhir online berdasarkan time
        checkOnlineStatus(timestamp)
        checkTypingStatus("noOne")
        userRefForSeen.removeEventListener(seenListener)
    }

    override fun onResume() {

        checkOnlineStatus("online")
        super.onResume()
    }

    //menangani hasil izin
    @Suppress("ControlFlowWithEmptyBody")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode){

            CAMERA_REQUEST_CODE ->{

                if (grantResults.isNotEmpty()){

                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED

                    if (cameraAccepted && storageAccepted){
                        // izin akses camera diberikan
                        pickFromCamera()

                    }else{
                        //kamera atau galeri atau kedua izin ditolak
                        Toast.makeText(this, "kamera & penyimpanan kedua izin itu diperlukan", Toast.LENGTH_SHORT).show()

                    }

                }else{
                }
            }

            STORAGE_REQUEST_CODE ->{

                if (grantResults.isNotEmpty()){

                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED

                    if (storageAccepted){
                        // izin penyimpanan diberikan
                        pickFromGallery()

                    }else{
                        //galeri izin ditolak
                        Toast.makeText(this, "akses izin penyimpanan itu diperlukan", Toast.LENGTH_SHORT).show()

                    }


                }else{

                }

            }

        }

    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == Activity.RESULT_OK){

            if (requestCode == IMAGE_PICK_GALLERY_CODE){

                //gambar diambil dari galery, dapatkan uri gambar
                imageUri = data!!.data

                //gunakan image uri untuk upload si firebase
                sendImageMessage(imageUri)

            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE){

                sendImageMessage(imageUri)
            }

        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun sendImageMessage(imageUri: Uri?) {

        notify = true

        //progress dialog
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Sending image ...")
        progressDialog.show()

        val timeStamp = ""+System.currentTimeMillis()
        val fileNameAndPath = "ChatImages/post_$timeStamp"

        //get bitmap untuk image uri
        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val data = baos.toByteArray()
        val ref : StorageReference? = FirebaseStorage.getInstance().reference.child(fileNameAndPath)
        ref?.putBytes(data)?.addOnSuccessListener { taskSnapshot ->

          progressDialog.dismiss()
            //get url untuk upload iamge
            val uriTask = taskSnapshot.storage.downloadUrl
            while (!uriTask.isSuccessful){}
            val downloadUri : String? = uriTask.result.toString()

            if (uriTask.isSuccessful){

                //add image uri dan data info lainya ke database
                val databaseReference = FirebaseDatabase.getInstance().reference

                //setup require data
                val hashMap : HashMap<String, Any> = HashMap()
                hashMap["sender"] = myUid!!
                hashMap["receiver"] = hisUid!!
                hashMap["message"] = downloadUri!!
                hashMap["timestamp"] = timeStamp
                hashMap["type"] = "image"
                hashMap["isSeen"] = false

                databaseReference.child("Chats").push().setValue(hashMap)

                //kirim notification
                val database : DatabaseReference? = FirebaseDatabase.getInstance().getReference("Users").child(myUid!!)
                database?.addValueEventListener(object : ValueEventListener{

                    override fun onDataChange(p0: DataSnapshot) {

                        val user = p0.getValue(ModelUsers::class.java)

                        if (notify!!){

                            senNotification(hisUid!!, user?.name, "mengirim foto ...")
                        }
                        notify = false
                    }

                    override fun onCancelled(p0: DatabaseError) {

                    }
                })

                //membuat chatlist node pada firebase database
                val chatRef1 : DatabaseReference? = FirebaseDatabase.getInstance().getReference("Chatlist")
                    .child(myUid!!)
                    .child(hisUid!!)
                chatRef1?.addValueEventListener(object : ValueEventListener{

                    override fun onDataChange(p0: DataSnapshot) {

                        if (!p0.exists()){

                            chatRef1.child("id").setValue(hisUid)
                        }
                    }

                    override fun onCancelled(p0: DatabaseError) {

                        Toast.makeText(this@ChatActivity, p0.message, Toast.LENGTH_SHORT).show()
                    }
                })

                val chatRef2 : DatabaseReference? = FirebaseDatabase.getInstance().getReference("Chatlist")
                    .child(hisUid!!)
                    .child(myUid!!)
                chatRef2?.addValueEventListener(object : ValueEventListener{

                    override fun onDataChange(p0: DataSnapshot) {

                        if (!p0.exists()){

                            chatRef2.child("id").setValue(myUid)
                        }
                    }

                    override fun onCancelled(p0: DatabaseError) {

                        Toast.makeText(this@ChatActivity, p0.message, Toast.LENGTH_SHORT).show()
                    }
                })

            }
        }?.addOnFailureListener {
            //gagal
            progressDialog.dismiss()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {

        menuInflater.inflate(R.menu.menu_main, menu)
        //menghilangkan menu search dan post pada chat activity
        menu!!.findItem(R.id.action_search).isVisible = false
        menu.findItem(R.id.action_add_post).isVisible = false
        menu.findItem(R.id.action_logout).isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        val id = item!!.itemId
        if (id == R.id.action_logout){

            firebaseAuth.signOut()
            checkUserStatus()

        }
        return super.onOptionsItemSelected(item)
    }

}
