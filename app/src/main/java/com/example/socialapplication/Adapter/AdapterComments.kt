package com.example.socialapplication.Adapter

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.blogspot.atifsoftwares.circularimageview.CircularImageView
import com.example.socialapplication.Model.ModelComment
import com.example.socialapplication.R
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import java.util.*

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class AdapterComments(val context: Context?, private val commentList: List<ModelComment>?, val myUid : String?, val postId : String?) : RecyclerView.Adapter<AdapterComments.MyHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view : View? = LayoutInflater.from(context).inflate(R.layout.row_comments, parent, false)

        return MyHolder(view!!)
    }

    override fun getItemCount(): Int {
        return commentList!!.size
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val uid = commentList!![position].uid
        val name = commentList[position].uName
        val email = commentList[position].uEmail
        val image = commentList[position].uDp
        val cid = commentList[position].cId
        val comment = commentList[position].comment
        val timestamp = commentList[position].timestamp

        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = java.lang.Long.parseLong(timestamp)

        val pTime = android.text.format.DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString()

        //setData View
        holder.nameTv!!.text =name
        holder.commentTv!!.text = comment
        holder.timeTv!!.text = pTime

        //setUser Dp
        try {
            Picasso.get().load(image).placeholder(R.drawable.ic_default_img).into(holder.avatarIv)
        }catch (e : Exception){

        }

        //comment click Listener
        holder.itemView.setOnLongClickListener {v ->

            if (myUid.equals(uid)){

                val builder : AlertDialog.Builder? = AlertDialog.Builder(v.rootView.context)
                builder!!.setTitle("Hapus")
                builder.setMessage("Apakah anda yakin ingin mengapus komentar ?")
                builder.setPositiveButton("Hapus", DialogInterface.OnClickListener{ _, _ ->

                    deleteComment(cid)
                })
                builder.setNegativeButton("Batal", DialogInterface.OnClickListener{dialog, _ ->
                    dialog.dismiss()
                })
                //show Dialog
                builder.create().show()
            }else{
                Toast.makeText(context, "tidak dapat menghapus komentar orang lain", Toast.LENGTH_SHORT).show()
            }

        true}
    }

    private fun deleteComment(cid : String?) {

        val ref : DatabaseReference? = FirebaseDatabase.getInstance().getReference("Posts").child(postId!!)

        ref!!.child("Comments").child(cid!!).removeValue()

        val postData = object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {

                val comments = ""+ p0.child("pComments").value
                val newCommentVal  = Integer.parseInt(comments)-1
                ref.child("pComments").setValue(""+newCommentVal)
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(context, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        ref.addListenerForSingleValueEvent(postData)

    }

    class MyHolder(@NonNull itemView: View):RecyclerView.ViewHolder(itemView) {

        var avatarIv : CircularImageView? = null
        var nameTv : TextView? = null
        var commentTv : TextView? = null
        var timeTv : TextView? = null

        init {

            avatarIv = itemView.findViewById(R.id.avatarIv)
            nameTv = itemView.findViewById(R.id.nameTv)
            commentTv = itemView.findViewById(R.id.commentTv)
            timeTv = itemView.findViewById(R.id.timeTv)
        }

    }

}