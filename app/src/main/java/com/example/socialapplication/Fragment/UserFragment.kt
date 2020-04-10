package com.example.socialapplication.Fragment


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.SearchView
import android.widget.Toast
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialapplication.Activity.MainActivity
import com.example.socialapplication.Adapter.AdapterUsers
import com.example.socialapplication.Model.ModelUsers
import com.example.socialapplication.R
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

@Suppress("ControlFlowWithEmptyBody", "DEPRECATION")
class UserFragment : BaseFragment() {

    private var recyclerView: RecyclerView? = null
    private var adapterUsers : AdapterUsers? = null
    private lateinit var userList : ArrayList<ModelUsers>

    private var shimmerPost : ShimmerFrameLayout? = null

    private lateinit var mAuth : FirebaseAuth
    private lateinit var mDatabase: FirebaseDatabase
    private lateinit var ref : DatabaseReference

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_user, container, false)

        findView(view)
        initView(view)

        return view
    }

    override fun findView(view: View) {

        recyclerView = view.findViewById(R.id.users_recyclerView)
        shimmerPost = view.findViewById(R.id.shimmer_view)
    }

    override fun initView(view: View) {

        val layoutManager = LinearLayoutManager(activity)

        mAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()

        recyclerView!!.setHasFixedSize(true)
        recyclerView!!.layoutManager = layoutManager

        userList = arrayListOf()

        getAllUsers()

    }

    override fun initListeners(view: View) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun getAllUsers() {

        val fUser = mAuth.currentUser

        ref = mDatabase.reference.child("Users")

        val postData = object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {

                    userList.clear()

                    for (ds : DataSnapshot in p0.children){

                        val modelUser = ds.getValue(ModelUsers::class.java)

                        if (modelUser != null){

                            if (modelUser.uid != fUser!!.uid){

                                userList.add(modelUser)

                            }
                        }
                        shimmerPost?.stopShimmer()
                        shimmerPost?.visibility = View.GONE
                        recyclerView?.visibility = View.VISIBLE
                    }
                    //adapter
                    adapterUsers = AdapterUsers(activity!!, userList)
                    //set adapter untuk recyclerview
                    recyclerView!!.adapter = adapterUsers
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(activity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        ref.addValueEventListener(postData)

    }

    override fun onResume() {
        super.onResume()
        shimmerPost?.startShimmer()
    }

    override fun onPause() {
        super.onPause()
        shimmerPost?.stopShimmer()
    }

    private fun searchUser(query: String) {

        val fUser = mAuth.currentUser

        val ref = mDatabase.getReference("Users")

        val postData = object : ValueEventListener{

            @SuppressLint("DefaultLocale")
            override fun onDataChange(p0: DataSnapshot) {

                userList.clear()

                for (ds : DataSnapshot in p0.children){

                    val modelUser = ds.getValue(ModelUsers::class.java)

                    if (modelUser!!.uid != fUser!!.uid){

                        if (modelUser.name!!.toLowerCase().contains(query.toLowerCase()) ||
                            modelUser.email!!.toLowerCase().contains(query.toLowerCase())){

                            userList.add(modelUser)

                        }

                    }

                    //adapter
                   val  adapterUsers = AdapterUsers(activity!!, userList)
                    //refresh adapter
                    adapterUsers.notifyDataSetChanged()
                    //set adapter untuk recyclerview
                    recyclerView!!.adapter = adapterUsers

                }
            }

            override fun onCancelled(p0: DatabaseError) {

                Toast.makeText(activity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }

        ref.addValueEventListener(postData)


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

        //search listener
        val item = menu.findItem(R.id.action_search)
        val searchView = MenuItemCompat.getActionView(item) as SearchView

        val searchV = object : SearchView.OnQueryTextListener{

            override fun onQueryTextChange(s: String?): Boolean {

                if (!TextUtils.isEmpty(s!!.trim())){

                    searchUser(s)

                }else{

                    getAllUsers()

                }

                return false
            }

            override fun onQueryTextSubmit(s: String?): Boolean {

                if (!TextUtils.isEmpty(s!!.trim())){

                    searchUser(s)

                }else{

                    getAllUsers()

                }

                return false
            }
        }
        searchView.setOnQueryTextListener(searchV)

        super.onCreateOptionsMenu(menu, inflater)

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val id = item.itemId
        if (id == R.id.action_logout){

            mAuth.signOut()
            checkUserStatus()

        }
        return super.onOptionsItemSelected(item)

    }

    private fun checkUserStatus(){

        val user = mAuth.currentUser

        if(user != null){

            //mProfileTv.setText(user.email)

        }else{

            startActivity(Intent(activity, MainActivity::class.java))
            activity!!.finish()

        }

    }

}
