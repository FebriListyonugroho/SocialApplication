package com.example.socialapplication.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.blogspot.atifsoftwares.circularimageview.CircularImageView
import com.example.socialapplication.Activity.ChatActivity
import com.example.socialapplication.Model.ModelUsers
import com.example.socialapplication.R
import com.squareup.picasso.Picasso
import java.lang.Exception

class AdapterChatlist(context: Context?, userList: List<ModelUsers>?) : RecyclerView.Adapter<AdapterChatlist.MyHolder>() {

    private var userList : List<ModelUsers>? = null
    private var context : Context? = null
    private var lastMessageMap : HashMap<String, String>? = null

    init {

        this.userList = userList
        this.context = context
        lastMessageMap = HashMap()

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.row_chatlist, parent, false)
        return MyHolder(view)
    }

    override fun getItemCount(): Int {
        return userList!!.size
    }

    fun setLastMessageMap(userId : String?, lastMessage : String?){

        lastMessageMap?.put(userId!!, lastMessage!!)
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val hisUid = userList!![position].uid
        val userImage = userList!![position].image
        val userName = userList!![position].name
        val lastMessage = lastMessageMap?.get(hisUid)

        //set Data
        holder.nameTv?.text = userName
        if (lastMessage == null || lastMessage == "default"){

            holder.lastMessageTv!!.visibility = View.GONE
        }else{
            holder.lastMessageTv!!.visibility = View.VISIBLE
            holder.lastMessageTv!!.text = lastMessage
        }
        try {
            Picasso.get().load(userImage).placeholder(R.drawable.ic_default_img).into(holder.profileIv)
        }catch (e: Exception){
            Picasso.get().load(userImage).into(holder.profileIv)
        }
        //set online status
        if (userList!![position].onlineStatus.equals("online")){

            //online
            holder.onlineStatusIv?.setImageResource(R.drawable.circle_online)
        }else{

            //offline
            holder.onlineStatusIv?.setImageResource(R.drawable.circle_offline)
        }
        // handle klik chatlist
        holder.itemView.setOnClickListener {

            val intent : Intent? = Intent(context, ChatActivity::class.java)
            intent?.putExtra("hisUid", hisUid)
            context!!.startActivity(intent)
        }
    }

    class MyHolder(@NonNull itemView: View):RecyclerView.ViewHolder(itemView) {

        var profileIv : CircularImageView? = null
        var onlineStatusIv : ImageView? = null
        var nameTv : TextView? = null
        var lastMessageTv : TextView? = null

        init {

            profileIv = itemView.findViewById(R.id.profileIv)
            onlineStatusIv = itemView.findViewById(R.id.onlineStatusIv)
            nameTv = itemView.findViewById(R.id.nameTv)
            lastMessageTv = itemView.findViewById(R.id.lastMessageTv)

        }

    }
}