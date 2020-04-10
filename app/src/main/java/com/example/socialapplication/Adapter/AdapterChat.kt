package com.example.socialapplication.Adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.blogspot.atifsoftwares.circularimageview.CircularImageView
import com.example.socialapplication.Model.ModelChat
import com.example.socialapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import java.lang.Exception
import java.util.*

private const val MSG_TYPE_LEFT = 0
private const val MSG_TYPE_RIGHT = 1

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class AdapterChat(val context: Context?, private val chatList: List<ModelChat>?, val imageUrl : String?) : RecyclerView.Adapter<AdapterChat.MyHolder>(){

    private lateinit var fUser : FirebaseUser

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {

        return if (viewType == MSG_TYPE_RIGHT){

            val view = LayoutInflater.from(context).inflate(R.layout.row_chat_right, parent, false)
            MyHolder(view)

        }else{

            val view = LayoutInflater.from(context).inflate(R.layout.row_chat_left, parent, false)
            MyHolder(view)

        }

    }

    override fun getItemCount(): Int {
        return chatList!!.size
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: MyHolder, position: Int) {

        val message = chatList!![position].message
        val timeStamp = chatList[position].timestamp
        val type = chatList[position].type

        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = java.lang.Long.parseLong(timeStamp)

        val dateTime = android.text.format.DateFormat.format("dd/MM/yyyy hh:mm aa", cal).toString()

        if (type.equals("text")){
            //pesan text
            holder.messageTv?.visibility = View.VISIBLE
            holder.messageIv?.visibility = View.GONE

            holder.messageTv!!.text = message
        }else{
            //pesan image
            holder.messageTv?.visibility = View.GONE
            holder.messageIv?.visibility = View.VISIBLE

            Picasso.get().load(message).placeholder(R.drawable.ic_image_black).into(holder.messageIv)
        }

        holder.messageTv!!.text = message
        holder.timeTv!!.text = dateTime

        try {
            Picasso.get().load(imageUrl).into(holder.profileIv)
        }catch (e : Exception){

        }

        ///klik untuk hapus pesan
        holder.messageLayout!!.setOnLongClickListener {

            val builder = AlertDialog.Builder(context)
            builder.setTitle("Hapus Pesan")
            builder.setMessage("Apakah anda yakin hapus pesan ?")

            //button hapus
            builder.setPositiveButton("Hapus") { _, _ ->

                deleteMessage(position)

            }

            builder.setNegativeButton("Tidak") { dialog, _ ->

                dialog.dismiss()

            }

            builder.create().show()

            true
        }

        if (position == chatList.size-1){

            if (chatList[position].isSeen){

                holder.isSeenTv!!.text = "dilihat"

            }else{

                holder.isSeenTv!!.text = "terkirim"

            }

        }else{

            holder.isSeenTv!!.visibility = View.GONE

        }

    }

    private fun deleteMessage(position: Int) {

       val myUID = FirebaseAuth.getInstance().currentUser!!.uid

        /*logika
         *dapatkan timestamp dari pesan yang diklik
         *bandingkan timestamp dari pesan yang diklik dengan semua pesan di chat
         *di mana kedua nilai cocok menghapus pesan*/

        val msgTimeStamp = chatList!![position].timestamp
        val dbRef = FirebaseDatabase.getInstance().getReference("Chats")
        val query = dbRef.orderByChild("timestamp").equalTo(msgTimeStamp)
        query.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {

                if (p0.exists()) {

                    for (ds: DataSnapshot in p0.children) {

                        /*jika Anda ingin agar pengirim hanya menghapus pesannya,
                          bandingkan nilai pengirim dengan pengguna saat ini
                          jika mereka cocok berarti itu adalah pesan pengirim yang mencoba untuk menghapus*/
                        if (ds.child("sender").value!! == myUID) {

                            /*kita dapat melakukan satu dari dua hal di sini.
                            1) hapus pesan dari obrolan
                            2) mengatur nilai pesan "pesan ini telah dihapus ..."
                            jadi lakukan apapun yang kamu mau*/

                            //1) hapus pesan dari obrolan
                            //ds.ref.removeValue()

                            //2) mengatur nilai pesan "pesan ini telah dihapus ..."
                            val hashMap: HashMap<String, Any> = HashMap()
                            hashMap["message"] = "pesan ini telah dihapus ..."
                            ds.ref.updateChildren(hashMap)

                            Toast.makeText(context, "Pesan Dihapus ...", Toast.LENGTH_SHORT).show()

                        } else {

                            Toast.makeText(
                                context,
                                "Anda hanya dapat menghapus pesan Anda",
                                Toast.LENGTH_SHORT
                            ).show()

                        }
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {

                Toast.makeText(context, p0.message, Toast.LENGTH_SHORT).show()
            }

        })

    }

    override fun getItemViewType(position: Int): Int {

        fUser = FirebaseAuth.getInstance().currentUser!!
        return if (chatList!![position].sender.equals(fUser.uid)){

            MSG_TYPE_RIGHT

        }else{

            MSG_TYPE_LEFT

        }

    }

    class MyHolder(@NonNull itemView: View):RecyclerView.ViewHolder(itemView) {

        var profileIv : CircularImageView? = null
        var messageIv : ImageView? = null
        var messageTv : TextView? = null
        var timeTv : TextView? = null
        var isSeenTv : TextView? = null
        var messageLayout : LinearLayout? = null //klik untuk hapus pesan

        init {

            profileIv = itemView.findViewById(R.id.profileIv)
            messageIv = itemView.findViewById(R.id.messageImv)
            messageTv = itemView.findViewById(R.id.messageTv)
            timeTv = itemView.findViewById(R.id.timeTv)
            isSeenTv = itemView.findViewById(R.id.isSeenTv)
            messageLayout = itemView.findViewById(R.id.messageLayout)

        }

    }

}