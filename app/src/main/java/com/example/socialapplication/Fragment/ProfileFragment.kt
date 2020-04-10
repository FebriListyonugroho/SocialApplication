@file:Suppress("DEPRECATION")

package com.example.socialapplication.Fragment

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.*
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blogspot.atifsoftwares.circularimageview.CircularImageView
import com.example.socialapplication.Activity.AddPostActivity
import com.example.socialapplication.Activity.MainActivity
import com.example.socialapplication.Adapter.AdapterPosts
import com.example.socialapplication.Model.ModelPost
import com.example.socialapplication.R
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage.getInstance
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import java.lang.Exception

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION",
    "ControlFlowWithEmptyBody", "NAME_SHADOWING"
)

class ProfileFragment : BaseFragment() {

    private lateinit var firebaseAuth : FirebaseAuth
    private lateinit var user : FirebaseUser
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference
    private  val storagePath = "Users_Profile_Cover_Imgs/"

    private lateinit var avatarIv : CircularImageView
    private lateinit var coverIv : ImageView
    private lateinit var nameTv : TextView
    private lateinit var emailTv : TextView
    private lateinit var phoneTv : TextView
    private lateinit var fab : FloatingActionButton
    private lateinit var pd : ProgressDialog
    private var postsRecyclerView : RecyclerView? = null
    private lateinit var postList : ArrayList<ModelPost>
    private lateinit var adapterPosts: AdapterPosts
    private var uid : String? = null

    private val CAMERA_REQUEST_CODE = 100
    private val STORAGE_REQUEST_CODE = 200
    private val IMAGE_PICK_GALLERY_CODE = 300
    private val IMAGE_PICK_CAMERA_CODE = 400



    private lateinit var image_uri: Uri
    private lateinit var profileOrCoverPhoto : String

    private lateinit var cameraPermissions: Array<String>
    private lateinit var storagePermission: Array<String>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        findView(view)
        initView(view)
        initListeners(view)

        return view
    }

    override fun findView(view: View) {

        avatarIv = view.findViewById(R.id.avatarIv)
        coverIv = view.findViewById(R.id.coverIv)
        nameTv = view.findViewById(R.id.nameTv)
        emailTv = view.findViewById(R.id.emailTv)
        phoneTv = view.findViewById(R.id.phoneTv)
        fab = view.findViewById(R.id.fab)
        postsRecyclerView = view.findViewById(R.id.recyclerview_posts)

    }

    override fun initView(view: View) {

        pd = ProgressDialog(activity)

        firebaseAuth = FirebaseAuth.getInstance()
        user = firebaseAuth.currentUser!!
        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReference = firebaseDatabase.getReference("Users")

        storageReference = getInstance().reference

        postList = arrayListOf()

        cameraPermissions = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        storagePermission = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        readMyDataProfile()
        checkUserStatus()
        loadMyPosts()

    }

    override fun initListeners(view: View) {

        fab.setOnClickListener {

            showEditProfileDialog()

        }
    }

    private fun readMyDataProfile() {

        val query = databaseReference.orderByChild("email").equalTo(user.email)

        val postData = object : ValueEventListener{

            @SuppressLint("InflateParams")
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
                Toast.makeText(activity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        query.addValueEventListener(postData)

    }

    private fun loadMyPosts() {

        ///linear layout untuk recyclerview
        val layoutManager = LinearLayoutManager(activity)
        //tampilkan posting terbaru terlebih dahulu, untuk memuat ini dari yang terakhir
        layoutManager.stackFromEnd = true
        layoutManager.reverseLayout = true
        //set layout untuk recyclerview
        postsRecyclerView!!.layoutManager = layoutManager

        //mendapat data post
        val ref  = firebaseDatabase.getReference("Posts")
        //query untuk load post
        val query  = ref.orderByChild("uid").equalTo(uid)
        //dapat semua data

        val postData = object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {

                if (p0.exists()){

                    postList.clear()
                    for (ds in p0.children){

                        val myPost = ds.getValue(ModelPost::class.java)

                        //add to list
                        postList.add(myPost!!)

                        //adapter
                        adapterPosts = AdapterPosts(activity!!, postList)
                        //set adapter untuk recycler view
                        postsRecyclerView!!.adapter = adapterPosts

                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(activity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        query.addValueEventListener(postData)

    }

    private fun searchMyPosts(searchQuery : String){

        ///linear layout untuk recyclerview
        val layoutManager = LinearLayoutManager(activity)
        //tampilkan posting terbaru terlebih dahulu, untuk memuat ini dari yang terakhir
        layoutManager.stackFromEnd = true
        layoutManager.reverseLayout = true
        //set layout untuk recyclerview
        postsRecyclerView!!.layoutManager = layoutManager

        //mendapat data post
        val ref = firebaseDatabase.getReference("Posts")
        //query untuk load post
        val query = ref.orderByChild("uid").equalTo(uid)
        //dapat semua data

        val postData = object : ValueEventListener{

            @SuppressLint("DefaultLocale")
            override fun onDataChange(p0: DataSnapshot) {

                postList.clear()
                for (ds : DataSnapshot in p0.children){

                    val myPost = ds.getValue(ModelPost::class.java)

                    if (myPost!!.pTitle.toLowerCase().contains(searchQuery.toLowerCase()) ||
                        myPost.pDescr.toLowerCase().contains(searchQuery.toLowerCase())){

                        //add to list
                        postList.add(myPost)

                    }

                    //adapter
                    adapterPosts = AdapterPosts(activity!!, postList)
                    //set adapter untuk recycler view
                    postsRecyclerView!!.adapter = adapterPosts

                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(activity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        query.addValueEventListener(postData)

    }

    private fun checkStoragePermission() : Boolean{

        return (ContextCompat.checkSelfPermission(activity!!, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            == (PackageManager.PERMISSION_GRANTED))

    }

    private fun requestCameraPermission(){

        requestPermissions(cameraPermissions, CAMERA_REQUEST_CODE)

    }

    private fun checkCameraPermission() : Boolean{

        val result = (ContextCompat.checkSelfPermission(activity!!, android.Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED))

        val result1 = (ContextCompat.checkSelfPermission(activity!!, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED))

        return result && result1

    }

    private fun requestStoragePermission(){

        requestPermissions(storagePermission, STORAGE_REQUEST_CODE)

    }

    private fun showEditProfileDialog() {

        val options = arrayOf("Edit Profile Picture", "Edit Cover Photo", "Edit Name", "Edit Phone")

        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Choose Action")
        builder.setItems(options) { _, which ->

            when (which) {
                0 -> {

                    pd.setMessage("Updating Profile Picture")
                    profileOrCoverPhoto = "image"
                    showImagePicDialog()

                }
                1 -> {

                    pd.setMessage("Updating Cover Picture")
                    profileOrCoverPhoto = "cover"
                    showImagePicDialog()

                }
                2 -> {

                    pd.setMessage("Updating Name")
                    showNamePhoneUpdateDialog("name")

                }
                3 -> {

                    pd.setMessage("Updating Phone")
                    showNamePhoneUpdateDialog("phone")

                }
            }

        }
        builder.create().show()

    }

    private fun showNamePhoneUpdateDialog(s: String?) {

        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Update $s")

        val linearLayout = LinearLayout(activity)
        linearLayout.orientation = LinearLayout.VERTICAL

        val editText = EditText(activity)
        editText.hint = "Enter $s"
        linearLayout.addView(editText)

        builder.setView(linearLayout)

        builder.setPositiveButton("Update") { _, _ ->

            val value: String? = editText.text.toString().trim()

            if (!TextUtils.isEmpty(value)){

                pd.show()
                val result : HashMap<String, Any> = HashMap()
                result[s!!] = value!!

                databaseReference.child(user.uid).updateChildren(result)
                    .addOnSuccessListener {

                        pd.dismiss()
                        Toast.makeText(activity, "Updated ...", Toast.LENGTH_SHORT).show()

                    }.addOnFailureListener { exception ->

                        pd.dismiss()
                        Toast.makeText(activity, "${exception.message}", Toast.LENGTH_SHORT).show()

                    }

                // jika pengguna mengedit namanya, ubah juga dari postingan nya
                if (s == "name"){

                    val ref = firebaseDatabase.getReference("Posts")

                    val query = ref.orderByChild("uid").equalTo(uid)

                    val postData = object : ValueEventListener{

                        override fun onDataChange(p0: DataSnapshot) {

                            for (ds : DataSnapshot in p0.children){

                                val child = ds.key
                                p0.ref.child(child!!).child("uName").setValue(value)

                            }
                        }

                        override fun onCancelled(p0: DatabaseError) {
                            Toast.makeText(activity, p0.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    query.addValueEventListener(postData)

                    //update nama di komentar pengguna saat ini pada posting

                    val postData1 = object : ValueEventListener{

                        override fun onDataChange(p0: DataSnapshot) {

                            for (ds : DataSnapshot? in p0.children){

                                val child = ds?.key
                                if (p0.child(child!!).hasChild("Comments")){

                                    val child1 : String? = ""+p0.child(child).key
                                    val child2 : Query? = FirebaseDatabase.getInstance().getReference("Posts").child(child1!!)
                                        .child("Comments").orderByChild("uid").equalTo(uid)

                                    child2!!.addValueEventListener(object : ValueEventListener{

                                        override fun onDataChange(p0: DataSnapshot) {

                                            for (ds : DataSnapshot? in p0.children){

                                                val child : String? = ds?.key
                                                p0.ref.child(child!!).child("uName").setValue(value)

                                            }
                                        }

                                        override fun onCancelled(p0: DatabaseError) {

                                            Toast.makeText(activity, p0.message, Toast.LENGTH_SHORT).show()
                                        }
                                    })
                                }
                            }
                        }

                        override fun onCancelled(p0: DatabaseError) {
                            Toast.makeText(activity, p0.message, Toast.LENGTH_SHORT).show()
                        }
                    }

                    ref.addValueEventListener(postData1)

                }

            }else{

                Toast.makeText(activity, "Please Enter $s", Toast.LENGTH_SHORT).show()

            }

        }

        builder.setNegativeButton("Cancel") { dialog, _ ->

            dialog.dismiss()

        }

        builder.create().show()

    }

    private fun showImagePicDialog() {

        val options = arrayOf("Camera", "Gallery")

        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Pick Image From")
        builder.setItems(options) { _, which ->

            if (which == 0){

                if (!checkCameraPermission()){

                    requestCameraPermission()

                }else{

                    pickFromCamera()

                }

            }else if (which == 1){

                if (!checkStoragePermission()){

                    requestStoragePermission()

                }else{

                    pickFromGallery()

                }

            }

        }
        builder.create().show()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        /*Metode ini digunakan ketika pengguna menekan untuk memperbolehkan atau menolak dari dialog permintaan izin akses,
         *di sini akan menangani kasus izin (diizinkan & ditolak)*/

        when (requestCode){

            CAMERA_REQUEST_CODE -> {
                //memilih dari kamera, periksa terlebih dahulu apakah izin kamera dan penyimpanan diizinkan atau tidak
                if (grantResults.isNotEmpty()){

                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED

                    if (cameraAccepted && writeStorageAccepted){
                        //izin akses aktif
                        pickFromCamera()

                    }else{
                        //izin akses tidak aktif
                        Toast.makeText(activity, "Harap aktifkan izin kamera & penyimpanan",Toast.LENGTH_LONG).show()

                    }

                }

            }

            STORAGE_REQUEST_CODE -> {

                if (grantResults.isNotEmpty()){

                    val writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED

                    if (writeStorageAccepted){
                        //izin akses aktif
                        pickFromGallery()

                    }else{
                        //izin akses tidak aktif
                        Toast.makeText(activity, "Harap aktifkan izin penyimpanan",Toast.LENGTH_LONG).show()

                    }

                }

            }

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (resultCode == RESULT_OK){

            if (requestCode == IMAGE_PICK_GALLERY_CODE){

                image_uri = data!!.data

                uploadProfileCoverPhoto(image_uri)

            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE){

                uploadProfileCoverPhoto(image_uri)

            }

        }

        super.onActivityResult(requestCode, resultCode, data)

    }

    private fun uploadProfileCoverPhoto(imageUri: Uri?) {

        pd.show()

        val filePathAndName = storagePath+ ""+ profileOrCoverPhoto +""+ user.uid

        val storageReference2nd  = filePathAndName.let {
            storageReference.child(
                it
            )
        }
        storageReference2nd.putFile(imageUri!!)
            .addOnSuccessListener { taskSnapshot ->

                val uriTask = taskSnapshot.storage.downloadUrl
                while (!uriTask.isSuccessful) {}
                val downloadUri = uriTask.result

                if (uriTask.isSuccessful){

                    val result : HashMap<String, Any?> = HashMap()
                    result[profileOrCoverPhoto] = downloadUri.toString()

                    databaseReference.child(user.uid).updateChildren(result)
                        .addOnSuccessListener {

                            pd.dismiss()
                            Toast.makeText(activity, "Image Updating ...", Toast.LENGTH_SHORT).show()

                        }.addOnFailureListener { exception ->

                            pd.dismiss()
                            Toast.makeText(activity, "Updating Image Error ${exception.message}", Toast.LENGTH_SHORT).show()

                        }

                    // jika pengguna mengedit foto profil maupuncover , ubah juga dari postingan nya
                    if (profileOrCoverPhoto == "image"){

                        val ref  = firebaseDatabase.getReference("Posts")

                        val query = ref.orderByChild("uid").equalTo(uid)

                        val postData = object : ValueEventListener{

                            override fun onDataChange(p0: DataSnapshot) {

                                for (ds : DataSnapshot in p0.children){

                                    val child = ds.key
                                    p0.ref.child(child!!).child("uDp").setValue(downloadUri.toString())

                                }
                            }

                            override fun onCancelled(p0: DatabaseError) {
                                Toast.makeText(activity, p0.message, Toast.LENGTH_SHORT).show()
                            }
                        }

                        query.addValueEventListener(postData)

                        //update profile di komentar pengguna saat ini pada posting

                        val postData1 = object : ValueEventListener{

                            override fun onDataChange(p0: DataSnapshot) {

                                for (ds : DataSnapshot? in p0.children){

                                    val child = ds!!.key
                                    if (p0.child(child!!).hasChild("Comments")){

                                        val child1 : String? = ""+p0.child(child).key
                                        val child2 : Query? = FirebaseDatabase.getInstance().getReference("Posts").child(child1!!)
                                            .child("Comments").orderByChild("uid").equalTo(uid)

                                        child2!!.addValueEventListener(object : ValueEventListener{

                                            override fun onDataChange(p0: DataSnapshot) {

                                                for (ds : DataSnapshot? in p0.children){

                                                    val child  = ds!!.key
                                                    p0.ref.child(child!!).child("uDp").setValue(downloadUri.toString())

                                                }
                                            }

                                            override fun onCancelled(p0: DatabaseError) {

                                                Toast.makeText(activity, p0.message, Toast.LENGTH_SHORT).show()
                                            }
                                        })
                                    }
                                }
                            }

                            override fun onCancelled(p0: DatabaseError) {
                                Toast.makeText(activity, p0.message, Toast.LENGTH_SHORT).show()
                            }
                        }

                        ref.addValueEventListener(postData1)

                    }

                }else{

                    pd.dismiss()
                    Toast.makeText(activity, "Some error occured", Toast.LENGTH_SHORT).show()

                }

            }.addOnFailureListener { exception ->

                pd.dismiss()
                Toast.makeText(activity, "${exception.message}", Toast.LENGTH_SHORT).show()

            }

    }

    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun pickFromCamera() {

        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description")

        image_uri = activity!!.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE)

    }

    private fun pickFromGallery() {

        val galleryIntent = Intent(Intent.ACTION_PICK)
        galleryIntent.type = "image/*"
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE)

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        setHasOptionsMenu(true)//untuk menampilkan menu di fragmen class

        super.onCreate(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.menu_main, menu)

        val item = menu.findItem(R.id.action_search)

        //search postingan user
        val searchView = MenuItemCompat.getActionView(item) as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {

                if (!TextUtils.isEmpty(query)){

                    searchMyPosts(query!!)

                }else{

                    loadMyPosts()

                }

                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                if (!TextUtils.isEmpty(newText)){

                    searchMyPosts(newText!!)

                }else{

                    loadMyPosts()

                }

                return false
            }

        })

        super.onCreateOptionsMenu(menu, inflater)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        ///menu toolbar
        val id = item.itemId
        if (id == R.id.action_logout){

            firebaseAuth.signOut()
            checkUserStatus()

        }

        if (id == R.id.action_add_post){

            startActivity(Intent(activity, AddPostActivity::class.java))

        }

        return super.onOptionsItemSelected(item)

    }

    private fun checkUserStatus(){

        val user = firebaseAuth.currentUser

        if(user != null){

            uid = user.uid

        }else{

            startActivity(Intent(activity, MainActivity::class.java))
            activity!!.finish()

        }

    }

}
