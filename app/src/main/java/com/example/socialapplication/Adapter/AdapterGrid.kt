package com.example.socialapplication.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.*
import android.widget.*
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.example.socialapplication.Model.ModelPost
import com.example.socialapplication.Activity.PostDetailActivity
import com.example.socialapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import java.util.*
import kotlin.Exception

@Suppress("ControlFlowWithEmptyBody", "DEPRECATION",
    "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
)
class AdapterGrid(val context: Context?, private  val postList: List<ModelPost>?) : RecyclerView.Adapter<AdapterGrid.MyHolder?>()  {

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

        val view = LayoutInflater.from(context).inflate(R.layout.grid_post, parent, false)

        return MyHolder(view)

    }

    override fun getItemCount(): Int {

        return postList!!.size

    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val pId = postList!![position].pId
        val pTitle = postList[position].pTitle
        val pDescription = postList[position].pDescr
        val pHarga = postList[position].pHarga
        val pMasa = postList[position].pMasa
        val pImage = postList[position].pImage
        val pTimeStamp = postList[position].pTime


        val calender = Calendar.getInstance(Locale.getDefault())
        calender.timeInMillis = pTimeStamp.toLong()
        val pTime = android.text.format.DateFormat.format("dd/MM/yyyy hh:mm aa", calender).toString()

        //Set data post
        holder.pTitleTv!!.text = pTitle
        holder.pDescriptionTv!!.text = pDescription
        holder.pHargaTv!!.text = "Rp. $pHarga /$pMasa"


        setLikes(holder, pId)


        //set post image
        //jika tidak ada image postnya
        if (pImage == "noImage"){

            holder.pImageIv!!.visibility = View.GONE

        }else{

            holder.pImageIv!!.visibility = View.VISIBLE

            try {
                Picasso.get().load(pImage).into(holder.pImageIv)
            }catch (e : Exception){

            }

        }


        //handle button click like
        holder.likeBtn!!.setOnClickListener {

            val pLikesi =Integer.parseInt(postList[position].pLikes)
            mProcessLike = true

            val postIde = postList[position].pId

            val postData = object : ValueEventListener{

                override fun onDataChange(p0: DataSnapshot) {

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

                override fun onCancelled(p0: DatabaseError) {
                    Toast.makeText(context, p0.message, Toast.LENGTH_SHORT).show()
                }
            }

            likesRef!!.addValueEventListener(postData)

        }
        holder.profileLayout!!.setOnClickListener {

            //klik ke postDetail menggunakan pId
            val intent = Intent(context, PostDetailActivity::class.java)
            intent.putExtra("postId", pId)
            context!!.startActivity(intent)

        }

    }

    private fun setLikes(holder: MyHolder, postKey: String?) {

        val postData = object : ValueEventListener{

            @SuppressLint("SetTextI18n")
            override fun onDataChange(p0: DataSnapshot) {

                if (p0.child(postKey!!).hasChild(myUid!!)){

                    holder.likeBtn!!.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_red, 0,0,0)
                }else{

                    holder.likeBtn!!.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0,0,0)
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(context, p0.message, Toast.LENGTH_SHORT).show()
            }
        }

        likesRef!!.addValueEventListener(postData)
    }

    class MyHolder(@NonNull itemView: View): RecyclerView.ViewHolder(itemView) {

        var pImageIv : ImageView? = null
        var pTitleTv : TextView? = null
        var pHargaTv : TextView? = null
        var pDescriptionTv : TextView? = null
        var likeBtn : Button? = null
        var profileLayout : LinearLayout? = null

        init {

            pImageIv = itemView.findViewById(R.id.pImageIv)
            pTitleTv = itemView.findViewById(R.id.pTitleTv)
            pDescriptionTv = itemView.findViewById(R.id.pDescriptionTv)
            pHargaTv = itemView.findViewById(R.id.phargaTv)
            likeBtn = itemView.findViewById(R.id.likeBtn)
            profileLayout = itemView.findViewById(R.id.profileLayout)


        }

    }

}