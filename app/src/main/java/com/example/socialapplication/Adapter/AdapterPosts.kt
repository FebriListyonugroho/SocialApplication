@file:Suppress("DEPRECATION")

package com.example.socialapplication.Adapter

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.*
import android.widget.*
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.blogspot.atifsoftwares.circularimageview.CircularImageView
import com.example.socialapplication.Activity.AddPostActivity
import com.example.socialapplication.Activity.ThereProfileActivity
import com.example.socialapplication.Model.ModelPost
import com.example.socialapplication.Activity.PostDetailActivity
import com.example.socialapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.Exception

@Suppress("ControlFlowWithEmptyBody", "DEPRECATION",
    "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
)
class AdapterPosts(val context: Context, private val postList: List<ModelPost>) : RecyclerView.Adapter<AdapterPosts.MyHolder?>()  {

    private var likesRef : DatabaseReference? = null
    private var postsRef : DatabaseReference? = null
    private var myUid : String? = null

    private var mProcessLike : Boolean? = false

    init {

        myUid = FirebaseAuth.getInstance().currentUser!!.uid
        likesRef = FirebaseDatabase.getInstance().reference.child("Likes")
        postsRef = FirebaseDatabase.getInstance().reference.child("Posts")

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        val view = LayoutInflater.from(context).inflate(R.layout.row_posts, parent, false)

        return MyHolder(view)

    }

    override fun getItemCount(): Int {

        return postList.size

    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val uid = postList[position].uid
//        val uEmail = postList[position].uEmail
        val uName = postList[position].uName
        val uDp = postList[position].uDp
        val pId = postList[position].pId
        val pTitle = postList[position].pTitle
        val pDescription = postList[position].pDescr
        val pHarga = postList[position].pHarga
        val pMasa = postList[position].pMasa
        val pImage = postList[position].pImage
        val pTimeStamp = postList[position].pTime
        val pLikes = postList[position].pLikes
        val pComments = postList[position].pComments


        val calender = Calendar.getInstance(Locale.getDefault())
        calender.timeInMillis = pTimeStamp.toLong()
        val pTime = android.text.format.DateFormat.format("dd/MM/yyyy hh:mm aa", calender).toString()

        //Set data post
        holder.uNameTv.text = uName
        holder.pTimeTv.text = pTime
        holder.pTitleTv.text = pTitle
        holder.pDescriptionTv.text = pDescription
        holder.pLikesTv.text = "$pLikes Suka"
        holder.pCommentsTv.text = "$pComments Komentar"
        holder.pHargaTv.text = "Rp. $pHarga /$pMasa"


        setLikes(holder, pId)

        //set user dp
        try {
            Picasso.get().load(uDp).placeholder(R.drawable.ic_default_img).into(holder.uPictureIv)
        }catch (e : Exception){

        }

        //set post image
        //jika tidak ada image postnya
        if (pImage == "noImage"){

            holder.pImageIv.visibility = View.GONE

        }else{

            holder.pImageIv.visibility = View.VISIBLE

            try {
                Picasso.get().load(pImage).into(holder.pImageIv)
            }catch (e : Exception){

            }

        }


        holder.moreBtn.setOnClickListener {

            showMoreOptions(holder.moreBtn, uid, myUid, pId, pImage)

        }


        //handle button click like
        holder.likeBtn.setOnClickListener {

            val pLikesi =Integer.parseInt(postList[position].pLikes)
            mProcessLike = true

            val postIde = postList[position].pId

            val postData = object : ValueEventListener{

                override fun onDataChange(p0: DataSnapshot) {

                    if (p0.exists()){

                        if (mProcessLike!!){

                            mProcessLike = if(p0.child(postIde).hasChild(myUid!!)){

                                //telah dilike, jadi hapus like
                                postsRef!!.child(postIde).child("pLikes").setValue("${(pLikesi - 1)}")
                                likesRef!!.child(postIde).child(myUid!!).removeValue()
                                false
                            }else{

                                //tidak like, melakukan like
                                postsRef!!.child(postIde).child("pLikes").setValue("${(pLikesi + 1)}")
                                likesRef!!.child(postIde).child(myUid!!).setValue("Batal")
                                false
                            }

                        }
                    }
                }

                override fun onCancelled(p0: DatabaseError) {
                    Toast.makeText(context, p0.message, Toast.LENGTH_SHORT).show()
                }
            }

            likesRef!!.addValueEventListener(postData)

        }

        //handle button click more
        holder.commentBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_comment_black, 0, 0, 0)
        holder.commentBtn.setOnClickListener {

            val intent = Intent(context, PostDetailActivity::class.java)
            intent.putExtra("postId", pId)
            intent.putExtra("uidPost", uid)
            context.startActivity(intent)

        }
        holder.shareBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_share, 0, 0, 0)
        //handle button click more
        holder.shareBtn.setOnClickListener {

            val bitmapDrawable: BitmapDrawable? = holder.pImageIv.drawable as BitmapDrawable
            if (bitmapDrawable == null){

                //share postingan tanpa image
                shareTextOnly(pTitle, pDescription)
            }else{

                //share postingan menggunakan image
                val bitmap : Bitmap? = bitmapDrawable.bitmap
                shareImageAndText(pTitle, pDescription, bitmap)
            }
        }

        holder.profileLayout.setOnClickListener {

            //klik ke thereprofileactivity dengan uid
            val intent = Intent(context, ThereProfileActivity::class.java)
            intent.putExtra("hisUid", uid)
            context.startActivity(intent)

        }

    }

    private fun shareImageAndText(pTitle: String?, pDescription: String?, bitmap: Bitmap?) {

        val shareBody : String? = pTitle +"\n"+ pDescription

        val uri : Uri? = saveImageToShare(bitmap)

        val sIntent : Intent? = Intent(Intent.ACTION_SEND)
        sIntent!!.putExtra(Intent.EXTRA_STREAM, uri)
        sIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here")
        sIntent.type = "image/png"
        context.startActivity(Intent.createChooser(sIntent, "Berbagi Via"))
    }

    private fun saveImageToShare(bitmap: Bitmap?): Uri? {

        val imageFolder = File(context.cacheDir, "images")
        var uri : Uri? = null
        try {
            imageFolder.mkdirs()
            val file = File(imageFolder, "share_image.png")

            val stream = FileOutputStream(file)
            bitmap?.compress(Bitmap.CompressFormat.PNG, 90, stream)
            stream.flush()
            stream.close()
            uri = FileProvider.getUriForFile(context, "com.example.socialapplication.fileprovider",
                file)
        }catch (e : Exception){
            Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
        }
        return uri
    }

    private fun shareTextOnly(pTitle: String?, pDescription: String?) {

        val shareBody : String? = pTitle +"\n"+ pDescription

        //share intent
        val sIntent : Intent? = Intent(Intent.ACTION_SEND)
        sIntent!!.type = "text/plain"
        sIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here")
        sIntent.putExtra(Intent.EXTRA_TEXT, shareBody)
        context.startActivity(Intent.createChooser(sIntent, "Berbagi Via"))
    }

    private fun setLikes(holder: MyHolder, postKey: String?) {

        val postData = object : ValueEventListener{

            @SuppressLint("SetTextI18n")
            override fun onDataChange(p0: DataSnapshot) {

                if (p0.exists()){

                    if (p0.child(postKey!!).hasChild(myUid!!)){

                        holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_red, 0,0,0)
                    }else{

                        holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0,0,0)
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(context, p0.message, Toast.LENGTH_SHORT).show()
            }
        }

        likesRef!!.addValueEventListener(postData)
    }

    private fun showMoreOptions(moreBtn: ImageButton, uid: String?, myUid: String?, pId: String?, pImage: String?) {

        //membuat menu popup saat ini memiliki opsi hapus, kami akan menambahkan lebih banyak opsi nanti
        val popupMenu = PopupMenu(context, moreBtn, Gravity.END)

        //tampil opsi hapus di semua post
        if (uid == myUid){
            //add items di menu
            popupMenu.menu.add(Menu.NONE, 0, 0, "Delete")
            popupMenu.menu.add(Menu.NONE, 1, 0, "Edit")

        }
        popupMenu.menu.add(Menu.NONE, 2, 0, "Detail")

        //item klik listener
        popupMenu.setOnMenuItemClickListener { item ->
            when (item!!.itemId) {
                0 -> {

                    //delete di klik
                    beginDelete(pId, pImage)

                }
                1 -> {

                    val intent = Intent(context, AddPostActivity::class.java)
                    intent.putExtra("key", "editPost")
                    intent.putExtra("editPostId", pId)
                    context.startActivity(intent)

                }
                2 -> {

                    val intent = Intent(context, PostDetailActivity::class.java)
                    intent.putExtra("postId", pId)
                    context.startActivity(intent)

                }
            }

            false
        }
        //show popup
        popupMenu.show()

    }

    private fun beginDelete(pId: String?, pImage: String?) {

        if (pImage == "noImage"){

            //post tanpa gambar
            deleteWithoutImage(pId)

        }else{

            //post dengan gambar
            deleteWithImage(pId, pImage)

        }

    }

    private fun deleteWithImage(pId: String?, pImage: String?) {

        val pd = ProgressDialog(context)
        pd.setMessage("Menghapus ...")

        //langkah
        //1.) delete image menggunakan uri
        //2.) delete dalam database mengunakan post id

        val picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage!!)
        picRef.delete()
            .addOnSuccessListener {

                val fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId)

                val postData = object : ValueEventListener{

                    override fun onDataChange(p0: DataSnapshot) {

                        for (ds : DataSnapshot in p0.children){

                            ds.ref.removeValue()

                        }
                        //terhapus
                        Toast.makeText(context, "Berhasil Dihapus", Toast.LENGTH_SHORT).show()
                        pd.dismiss()
                    }

                    override fun onCancelled(p0: DatabaseError) {

                        pd.dismiss()
                        Toast.makeText(context, p0.message, Toast.LENGTH_SHORT).show()
                    }
                }

                fquery.addValueEventListener(postData)

            }.addOnFailureListener { p0 ->

                pd.dismiss()
                Toast.makeText(context, p0.message, Toast.LENGTH_SHORT).show()

            }


    }

    private fun deleteWithoutImage(pId: String?) {

        val pd = ProgressDialog(context)
        pd.setMessage("Menghapus ...")

        val fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId)

        val postData = object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {

                for (ds : DataSnapshot in p0.children){

                    ds.ref.removeValue()

                }
                //terhapus
                Toast.makeText(context, "Berhasil Dihapus", Toast.LENGTH_SHORT).show()
                pd.dismiss()
            }

            override fun onCancelled(p0: DatabaseError) {
                pd.dismiss()
                Toast.makeText(context, p0.message, Toast.LENGTH_SHORT).show()
            }
        }

        fquery.addValueEventListener(postData)

    }

    class MyHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        internal var uPictureIv : CircularImageView = itemView.findViewById(R.id.uPictureIv)
        internal var pImageIv : ImageView = itemView.findViewById(R.id.pImageIv)
        internal var uNameTv : TextView = itemView.findViewById(R.id.uNameTv)
        internal var pTimeTv : TextView = itemView.findViewById(R.id.pTimeTv)
        internal var pTitleTv : TextView = itemView.findViewById(R.id.pTitleTv)
        internal var pHargaTv : TextView = itemView.findViewById(R.id.hargaTv)
        internal var pDescriptionTv : TextView = itemView.findViewById(R.id.pDescriptionTv)
        internal var pCommentsTv : TextView = itemView.findViewById(R.id.pCommentsTv)
        internal var pLikesTv : TextView = itemView.findViewById(R.id.pLikeTv)
        internal var moreBtn : ImageButton = itemView.findViewById(R.id.moreBtn)
        internal var likeBtn : Button = itemView.findViewById(R.id.likeBtn)
        internal var commentBtn : Button = itemView.findViewById(R.id.commentBtn)
        internal var shareBtn : Button = itemView.findViewById(R.id.shareBtn)
        internal var profileLayout : LinearLayout = itemView.findViewById(R.id.profileLayout)

    }

}