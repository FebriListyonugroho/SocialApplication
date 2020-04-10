package com.example.socialapplication.Adapter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.blogspot.atifsoftwares.circularimageview.CircularImageView
import com.example.socialapplication.Activity.ChatActivity
import com.example.socialapplication.Activity.ThereProfileActivity
import com.example.socialapplication.Model.ModelUsers
import com.example.socialapplication.R
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.row_users.view.*
import java.lang.Exception

@Suppress("ControlFlowWithEmptyBody")
class AdapterUsers(val context: Context?, private val userList: List<ModelUsers>?) : RecyclerView.Adapter<AdapterUsers.MyHolder?>(){

    @NonNull
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): MyHolder {

        val view = LayoutInflater.from(context).inflate(R.layout.row_users, parent, false)
        return MyHolder(view)

    }

    override fun getItemCount(): Int {

        return userList!!.size
    }


    override fun onBindViewHolder(@NonNull holder: MyHolder, position: Int) {

        var hisUID= userList!![position].uid!!
        val userImage = userList[position].image
        val userName = userList[position].name
        val userEmail = userList[position].email

        holder.mNameTv.text = userName
        holder.mEmailTv.text = userEmail
        try {

            Picasso.get().load(userImage)
                .placeholder(R.drawable.ic_default_img)
                .into(holder.mAvatarIv)

        }catch (e : Exception){

            Picasso.get().load(R.drawable.ic_default_img).placeholder(R.drawable.ic_default_img).into(holder.mAvatarIv)

        }

        //handle item click
        holder.itemView.setOnClickListener {

            //tampil dialog
            val builder = AlertDialog.Builder(context)
            builder.setItems(arrayOf("Lihat Profile", "Kirim Pesan")) { _, which ->

                if (which == 0){

                    //profile klik
                    //klik ke thereprofileactivity dengan uid
                    val intent = Intent(context, ThereProfileActivity::class.java)
                    intent.putExtra("Uid", hisUID)
                    context!!.startActivity(intent)

                }
                if (which == 1){

                    //chat klik
                    val intent = Intent(context, ChatActivity::class.java)
                    intent.putExtra("hisUid", hisUID)
                    context!!.startActivity(intent)

                }

            }
            builder.create().show()
        }
    }


    class MyHolder(itemView:View):RecyclerView.ViewHolder(itemView) {

       internal var mAvatarIv = itemView.findViewById<CircularImageView>(R.id.avatarIv)
       internal var mNameTv = itemView.findViewById<TextView>(R.id.nameTv)
       internal var mEmailTv = itemView.findViewById<TextView>(R.id.emailTv)

    }

}