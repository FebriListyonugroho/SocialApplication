package com.example.socialapplication.Fragment


import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.socialapplication.Activity.MainActivity
import com.example.socialapplication.Adapter.AdapterChatlist
import com.example.socialapplication.Model.ModelChat
import com.example.socialapplication.Model.ModelChatlist
import com.example.socialapplication.Model.ModelUsers

import com.example.socialapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

@Suppress("ControlFlowWithEmptyBody")
class ChatListFragment : BaseFragment() {

    private lateinit var firebaseAuth : FirebaseAuth
    private lateinit var mDatabase: FirebaseDatabase
    private lateinit var reference: DatabaseReference

    private var recyclerView : RecyclerView? = null
    private var chatlistList : List<ModelChatlist>? = null
    private var adapterChatlist : AdapterChatlist? = null
    private var userList : List<ModelUsers>? = null
    private var currentUser : FirebaseUser? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_chat_list, container, false)
        findView(view)
        initView(view)

        return view
    }

    override fun findView(view: View) {

        recyclerView = view.findViewById(R.id.recyclerView)
    }

    override fun initView(view: View) {

        firebaseAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()
        currentUser = FirebaseAuth.getInstance().currentUser

        chatlistList = ArrayList()
        reference = FirebaseDatabase.getInstance().getReference("Chatlist").child(currentUser!!.uid)
        reference.addValueEventListener(object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {

                if (p0.exists()){

                    (chatlistList as ArrayList).clear()
                    for (ds : DataSnapshot? in p0.children){

                        val chatlist = ds!!.getValue(ModelChatlist::class.java)
                        (chatlistList as ArrayList).add(chatlist!!)
                    }
                    loadChats()
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(activity, p0.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun initListeners(view: View) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun loadChats() {

        userList = ArrayList()
        reference = mDatabase.getReference("Users")

        val postData = object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {

                (userList as ArrayList).clear()
                for (ds : DataSnapshot? in p0.children){

                    val user = ds?.getValue(ModelUsers::class.java)
                    for (chatlist : ModelChatlist? in chatlistList!!){

                        if (user?.uid != null && user.uid.equals(chatlist?.id)){

                            (userList as ArrayList).add(user)
                            break
                        }
                    }
                    //adapter
                    adapterChatlist = AdapterChatlist(context, userList)
                    recyclerView?.adapter = adapterChatlist

                    for (i in userList!!.indices){

                        lastMessage(userList?.get(i)?.uid)
                    }
                }
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(activity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        reference.addValueEventListener(postData)

    }

    private fun lastMessage(uid: String?) {

        val reference = mDatabase.getReference("Chats")

        val postData = object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {

                var theLastMessage = "default"

                if (p0.exists()){

                    for (ds : DataSnapshot? in p0.children){

                        val chat = ds!!.getValue(ModelChat::class.java) ?: continue

                        val sender = chat.sender
                        val receiver = chat.receiver
                        if (sender==null || receiver==null){
                            continue
                        }
                        if (chat.receiver.equals(currentUser?.uid) &&
                            chat.sender.equals(uid) || chat.receiver.equals(uid)
                            && chat.sender.equals(currentUser?.uid)){

                            //jika last message image
                            theLastMessage = if (chat.type.equals("image")){

                                "menerima foto"
                            }else{

                                chat.message!!
                            }
                        }
                    }
                }
                adapterChatlist?.setLastMessageMap(uid, theLastMessage)
                adapterChatlist?.notifyDataSetChanged()
            }

            override fun onCancelled(p0: DatabaseError) {
                 Toast.makeText(activity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        reference.addValueEventListener(postData)

    }

    override fun onCreate(savedInstanceState: Bundle?) {

        setHasOptionsMenu(true)//untuk menampilkan menu di fragmen class

        super.onCreate(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {

        inflater.inflate(R.menu.menu_main, menu)

        ///menghilangkan tampilan icon pada userfragment
        menu.findItem(R.id.action_add_post).isVisible = false
        menu.findItem(R.id.action_logout).isVisible = false

        super.onCreateOptionsMenu(menu, inflater)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val id = item.itemId
        if (id == R.id.action_logout){

            firebaseAuth.signOut()
            checkUserStatus()

        }
        return super.onOptionsItemSelected(item)

    }

    private fun checkUserStatus(){

        val user = firebaseAuth.currentUser

        if(user != null){

            //mProfileTv.setText(user.email)

        }else{

            startActivity(Intent(activity, MainActivity::class.java))
            activity!!.finish()

        }

    }

}
