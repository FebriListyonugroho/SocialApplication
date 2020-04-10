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
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.socialapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.ByteArrayOutputStream
import java.lang.Exception

@Suppress("DEPRECATION", "NAME_SHADOWING")

//permission konstan
private const val CAMERA_REQUEST_CODE = 100
private const val STORAGE_REQUEST_CODE = 200

//image pick konstan
private const val IMAGE_PICK_CAMERA_CODE = 300
private const val IMAGE_PICK_GALLERY_CODE = 400

@Suppress("NAME_SHADOWING", "ControlFlowWithEmptyBody")
class AddPostActivity : BaseActivity() {

    private lateinit var firebaseAuth : FirebaseAuth
    private lateinit var mDatabase : FirebaseDatabase
    private lateinit var mStorage : FirebaseStorage

    private lateinit var titleEt : EditText
    private lateinit var descriptionEt : EditText
    private lateinit var imageIv : ImageView
    private lateinit var hargaEt : EditText
    private lateinit var masaSp : Spinner
    private lateinit var kategoriSp : Spinner
    private lateinit var uploadBtn : Button

    //permission array
    private lateinit var cameraPermissions : Array<String>
    private lateinit var storagePermissions : Array<String>

    //User info data
    private var name : String? = null
    private var email : String? = null
    private var uid : String? = null
    private var dp : String? = null

    //info edit post
    private var editTitle : String? = null
    private var editDescription : String? = null
    private var editImage : String? = null
    private var isUpdateKey : String? = null
    private var editPostId : String? = null


    //progress dialog
    private lateinit var pd : ProgressDialog

    private var imageUri : Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        findView()
        initView()
        initListeners()

    }

    override fun findView() {

        titleEt = findViewById(R.id.pTitleEt)
        descriptionEt = findViewById(R.id.pDescriptionEt)
        imageIv = findViewById(R.id.pImageIv)
        hargaEt = findViewById(R.id.hargaEt)
        masaSp = findViewById(R.id.sewa_spinner)
        kategoriSp = findViewById(R.id.kategori_spinner)
        uploadBtn = findViewById(R.id.pUploadBtn)

    }

    @SuppressLint("SetTextI18n")
    override fun initView() {

        firebaseAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()
        mStorage = FirebaseStorage.getInstance()

        checkUserStatus()

        //get intent dari adapterpost ke activity sebelumya
        isUpdateKey = intent.getStringExtra("key")
        editPostId = intent.getStringExtra("editPostId")

        if (isUpdateKey.equals("editPost")){
            //update
            val actionBar = supportActionBar
            actionBar!!.title = "Update Post"
            uploadBtn.text = "Update"
            loadPostData(editPostId!!)
        }else{
            //add
            val actionBar = supportActionBar
            actionBar!!.title = "Tambah Postingan Baru"
            uploadBtn.text = "Upload"

        }

        ///progress dialog
        pd = ProgressDialog(this)

        //init permissions array
        cameraPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermissions = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        //dapatkan beberapa info dari pengguna saat ini untuk dimasukkan dalam postingan
        val userDbRef = mDatabase.getReference("Users")
        val query = userDbRef.orderByChild("email").equalTo(email)

        val postData = object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {

                for (ds : DataSnapshot in p0.children){

                    name = ""+ ds.child("name").value
                    email = ""+ ds.child("email").value
                    dp = ""+ ds.child("image").value

                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@AddPostActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }

        query.addValueEventListener(postData)

        val actionBar = supportActionBar
        //aktifkan tombol back pada toolbar
        actionBar!!.setDisplayShowHomeEnabled(true)
        actionBar.setDisplayHomeAsUpEnabled(true)

        actionBar.subtitle = email

    }

    override fun initListeners() {

        ///mendapat image dari camera maupun gallery klik
        imageIv.setOnClickListener {

            showImagePickDialog()

        }

        ///Upload button posting
        uploadBtn.setOnClickListener {

            val title : String? = titleEt.text.toString().trim()
            val description: String? = descriptionEt.text.toString().trim()
            val harga: String? = hargaEt.text.toString().trim()
            val masa: String? = masaSp.selectedItem.toString().trim()
            val kategori: String? = kategoriSp.selectedItem.toString().trim()

            if (TextUtils.isEmpty(title)){

                Toast.makeText(this, "harap isi judul ...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener

            }
            if (TextUtils.isEmpty(description)){

                Toast.makeText(this, "harap isi deskripsi ...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener

            }

            if (TextUtils.isEmpty(harga)){

                Toast.makeText(this, "harap form harga diisi ...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (isUpdateKey.equals("editPost")){

                if (title != null && description != null && harga != null) {

                    beginUpdate(title, description, editPostId, harga, masa, kategori)
                }
            }
            else{

                if (title != null && description != null && harga != null){

                    uploadData(title, description, harga, masa, kategori)
                }

            }
        }

    }

    private fun loadPostData(editPostId: String) {

        val reference = mDatabase.getReference("Posts")

        val fQuery  = reference.orderByChild("pId").equalTo(editPostId)

        val postData = object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {

                for(ds in p0.children){
                    //mendapat data
                    editTitle = ""+ds.child("pTitle").value
                    editDescription = ""+ds.child("pDescr").value
                    editImage = ""+ds.child("pImage").value

                    //set data pada view
                    titleEt.setText(editTitle)
                    descriptionEt.setText(editDescription)

                    //set image
                    if (!editImage.equals("noImage")){
                        try {
                            Picasso.get().load(editImage).into(imageIv)
                        }catch (e : Exception){

                        }
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(this@AddPostActivity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        fQuery.addValueEventListener(postData)

    }

    private fun beginUpdate(title: String?, description: String?, editPostId: String?, harga : String?, masa : String?, kategori : String?) {
        pd.setMessage("Update Postingan ...")
        pd.show()

        if (title != null && description != null && harga != null && masa != null && kategori != null){

            if (!editImage.equals("noImage")){
                //menggunakan image
                updateWasWithImage(title, description, editPostId, harga, masa, kategori)
            }else if (imageIv.drawable != null){
                //menggunakan image
                updateWithNowImage(title, description, editPostId, harga, masa, kategori)
            }else{
                //tidak menggunakan image
                updateWithoutImage(title, description, editPostId, harga, masa, kategori)
            }
        }
    }

    private fun updateWithoutImage(title: String?, description: String?, editPostId: String?, harga : String?, masa : String?, kategori : String?) {

        val hashMap : HashMap<String, Any>? = HashMap()
        hashMap?.set("uid", uid!!)
        hashMap?.set("uName", name!!)
        hashMap?.set("uEmail", email!!)
        hashMap?.set("uDp", dp!!)
        hashMap?.set("pTitle", title!!)
        hashMap?.set("pLikes", "0")
        hashMap?.set("pComments", "0")
        hashMap?.set("pHarga",harga!!)
        hashMap?.set("pMasa", masa!!)
        hashMap?.set("pKategori", kategori!!)
        hashMap?.set("pDescr", description!!)
        hashMap?.set("pImage", "noImage")

        val ref = mDatabase.getReference("Posts")
        if (hashMap != null) {
            ref.child(editPostId!!).updateChildren(hashMap)
                .addOnSuccessListener {
                    pd.dismiss()
                    Toast.makeText(this, "Update ...", Toast.LENGTH_SHORT).show()

                }.addOnFailureListener {e ->
                    pd.dismiss()
                    Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateWithNowImage(title: String?, description: String?, editPostId: String?, harga : String?, masa : String?, kategori : String?) {

        val timeStamp = System.currentTimeMillis().toString()
        val filePathAndName = "Posts/post_$timeStamp"

        //get image dari imageView
        val bitmap = (imageIv.drawable as BitmapDrawable).bitmap
        val baos = ByteArrayOutputStream()
        //image compress
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        val data = baos.toByteArray()

        val ref = mStorage.reference.child(filePathAndName)
        ref.putBytes(data).addOnSuccessListener { taskSnapshot ->
            //image terupload get url
            val uriTask = taskSnapshot.storage.downloadUrl
            while (!uriTask.isSuccessful){}

            val downloadUri = uriTask.result.toString()
            if (uriTask.isSuccessful){
                //url didapat, untuk upload firebase

                val hashMap : HashMap<String, Any>? = HashMap()
                hashMap?.set("uid", uid!!)
                hashMap?.set("uName", name!!)
                hashMap?.set("uEmail", email!!)
                hashMap?.set("uDp", dp!!)
                hashMap?.set("pLikes", "0")
                hashMap?.set("pComments", "0")
                hashMap?.set("pHarga",harga!!)
                hashMap?.set("pMasa", masa!!)
                hashMap?.set("pKategori", kategori!!)
                hashMap?.set("pTitle", title!!)
                hashMap?.set("pDescr", description!!)
                hashMap?.set("pImage", downloadUri)

                val ref = mDatabase.getReference("Posts")
                if (hashMap != null) {
                    ref.child(editPostId!!).updateChildren(hashMap)
                        .addOnSuccessListener {
                            pd.dismiss()
                            Toast.makeText(this, "Update ...", Toast.LENGTH_SHORT).show()

                        }.addOnFailureListener {e ->
                            pd.dismiss()
                            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                        }
                }

            }

        }.addOnFailureListener { e ->
            //image tidak terupload
            pd.dismiss()
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }

    }

    private fun updateWasWithImage(title: String?, description: String?, editPostId: String?, harga : String?, masa : String?, kategori : String?) {
        val mPictureRef = mStorage.getReferenceFromUrl(editImage!!)
        mPictureRef.delete().addOnSuccessListener {
            //hapus image, upload image baru
            val timeStamp: String? = System.currentTimeMillis().toString()
            val filePathAndName = "Posts/post_$timeStamp"

            //get image dari imageView
            val bitmap = (imageIv.drawable as BitmapDrawable).bitmap
            val baos = ByteArrayOutputStream()
            //image compress
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val data = baos.toByteArray()

            val ref = FirebaseStorage.getInstance().reference.child(filePathAndName)
            ref.putBytes(data).addOnSuccessListener { taskSnapshot ->
                //image terupload get url
                val uriTask = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful){}

                val downloadUri = uriTask.result.toString()
                if (uriTask.isSuccessful){
                    //url didapat, untuk upload firebase

                    val hashMap : HashMap<String, Any>? = HashMap()
                    hashMap?.set("uid", uid!!)
                    hashMap?.set("uName", name!!)
                    hashMap?.set("uEmail", email!!)
                    hashMap?.set("uDp", dp!!)
                    hashMap?.set("pLikes", "0")
                    hashMap?.set("pComments", "0")
                    hashMap?.set("pHarga",harga!!)
                    hashMap?.set("pMasa", masa!!)
                    hashMap?.set("pKategori", kategori!!)
                    hashMap?.set("pTitle", title!!)
                    hashMap?.set("pDescr", description!!)
                    hashMap?.set("pImage", downloadUri)

                    val ref = FirebaseDatabase.getInstance().getReference("Posts")
                    if (hashMap != null) {
                        ref.child(editPostId!!).updateChildren(hashMap)
                            .addOnSuccessListener {
                                pd.dismiss()
                                Toast.makeText(this, "Update ...", Toast.LENGTH_SHORT).show()

                            }.addOnFailureListener {e ->
                                pd.dismiss()
                                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                            }
                    }

                }

            }.addOnFailureListener { e ->
                //image tidak terupload
                pd.dismiss()
                Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            pd.dismiss()
            Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
        }

    }

    @Suppress("NAME_SHADOWING", "ControlFlowWithEmptyBody")
    private fun uploadData(title: String?, description: String?, harga : String?, masa : String?, kategori : String?) {

        pd.setMessage("Upload Postingan anda ...")
        pd.show()

        //untuk post-image, post-id, post-publish-time
        val timestamp = System.currentTimeMillis().toString()

        val filePathAndName = "Posts/post_$timestamp"

        if (imageIv.drawable != null){

            //get image dari imageView
            val bitmap = (imageIv.drawable as BitmapDrawable).bitmap
            val baos = ByteArrayOutputStream()
            //image compress
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val data = baos.toByteArray()

            //posting dengan image
            val ref = mStorage.reference.child(filePathAndName)
            ref.putBytes(data)
                .addOnSuccessListener { taskSnapshot ->
                    //Berhasil Upload image postingan
                    val uriTask = taskSnapshot.storage.downloadUrl
                    while (!uriTask.isSuccessful){}

                    val downloadUri : String? = uriTask.result.toString()

                    if (uriTask.isSuccessful){

                        // url diterima pos unggah ke database firebase
                        val hashMap : HashMap<String, Any>? = HashMap()
                        hashMap?.set("uid", uid!!)
                        hashMap?.set("uName", name!!)
                        hashMap?.set("uEmail", email!!)
                        hashMap?.set("uDp", dp!!)
                        hashMap?.set("pId", timestamp)
                        hashMap?.set("pLikes", "0")
                        hashMap?.set("pComments", "0")
                        hashMap?.set("pHarga",harga!!)
                        hashMap?.set("pMasa", masa!!)
                        hashMap?.set("pKategori", kategori!!)
                        hashMap?.set("pTitle", title!!)
                        hashMap?.set("pDescr", description!!)
                        hashMap?.set("pImage", downloadUri!!)
                        hashMap?.set("pTime", timestamp)

                        val ref = mDatabase.getReference("Posts")
                        //simpan data pada ref database
                        ref.child(timestamp).setValue(hashMap)
                            .addOnSuccessListener {
                                //jika berhasil upload data posting ke database
                                pd.dismiss()
                                Toast.makeText(this, "Berhasil upload postingan anda", Toast.LENGTH_SHORT).show()

                                //reset kolom data posting view
                                titleEt.setText("")
                                descriptionEt.setText("")
                                imageIv.setImageURI(null)
                                imageUri = null

                            }.addOnFailureListener { exception ->
                                //jika gagal upload data postingan ke database
                                pd.dismiss()
                                Toast.makeText(this, "Gagal upload data postingan \n ${exception.message}", Toast.LENGTH_SHORT).show()

                            }

                    }

                }.addOnFailureListener { exception ->
                    //gagal upload image postingan
                    pd.dismiss()
                    Toast.makeText(this, "Gagal Upload Image Postingan ${exception.message}", Toast.LENGTH_SHORT).show()

                }

        }else{

            //posting tanpa image
            // url diterima pos unggah ke database firebase
            val hashMap : HashMap<String, Any>? = HashMap()
            hashMap?.set("uid", uid!!)
            hashMap?.set("uName", name!!)
            hashMap?.set("uEmail", email!!)
            hashMap?.set("uDp", dp!!)
            hashMap?.set("pId", timestamp)
            hashMap?.set("pLikes", "0")
            hashMap?.set("pComments", "0")
            hashMap?.set("pHarga",harga!!)
            hashMap?.set("pMasa", masa!!)
            hashMap?.set("pKategori", kategori!!)
            hashMap?.set("pTitle", title!!)
            hashMap?.set("pDescr", description!!)
            hashMap?.set("pImage", "noImage")
            hashMap?.set("pTime", timestamp)

            val ref = mDatabase.getReference("Posts")
            //simpan data pada ref database
            ref.child(timestamp).setValue(hashMap)
                .addOnSuccessListener {
                    //jika berhasil upload data posting ke database
                    pd.dismiss()
                    Toast.makeText(this, "Berhasil upload postingan anda", Toast.LENGTH_SHORT).show()
                    //reset kolom data posting view
                    titleEt.setText("")
                    descriptionEt.setText("")
                    imageIv.setImageURI(null)
                    imageUri = null


                }.addOnFailureListener { exception ->
                    //jika gagal upload data postingan ke database
                    pd.dismiss()
                    Toast.makeText(this, "Gagal upload data postingan \n ${exception.message}", Toast.LENGTH_SHORT).show()

                }

        }

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

    override fun onStart() {
        checkUserStatus()
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        checkUserStatus()
    }

    private fun checkUserStatus(){

        val user = firebaseAuth.currentUser

        if(user != null){

            // pengguna sudah masuk tinggal di sini
            email = user.email
            uid = user.uid

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
        menu.findItem(R.id.action_search).isVisible = false
        menu.findItem(R.id.action_logout).isVisible = false

        return super.onCreateOptionsMenu(menu)
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

                // diatur ke tampilan gambar
                imageIv.setImageURI(imageUri)

            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE){

                //gambar diambil dari camera, dapatkan uri gambar
                imageIv.setImageURI(imageUri)

            }

        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}
