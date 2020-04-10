package com.example.socialapplication.Fragment


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.*
import android.widget.SearchView
import android.widget.Toast
import androidx.core.view.MenuItemCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.socialapplication.Activity.AddPostActivity
import com.example.socialapplication.Activity.MainActivity
import com.example.socialapplication.Adapter.AdapterGrid
import com.example.socialapplication.Model.ModelPost
import com.example.socialapplication.R
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

@Suppress("ControlFlowWithEmptyBody", "DEPRECATION")
class HomeFragment : BaseFragment() {

    private lateinit var firebaseAuth : FirebaseAuth
    private lateinit var ref : DatabaseReference
    private lateinit var mDatabase: FirebaseDatabase

    private lateinit var shimmerPost : ShimmerFrameLayout
    private lateinit var recyclerView : RecyclerView
    private lateinit var postList : ArrayList<ModelPost>
    private lateinit var adapterPosts : AdapterGrid


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        findView(view)
        initView(view)

        return view
    }

    override fun findView(view: View) {

        recyclerView = view.findViewById(R.id.postsRecyclerview)
        shimmerPost = view.findViewById(R.id.shimmer_view)

    }

    override fun initView(view: View) {

        firebaseAuth = FirebaseAuth.getInstance()
        mDatabase = FirebaseDatabase.getInstance()

        val layoutManager = GridLayoutManager(activity, 2)

        // tampilkan posting terbaru terlebih dahulu, untuk memuat ini dari yang terakhir
//        layoutManager.stackFromEnd = true
//        layoutManager.reverseLayout = true
        //set layout untuk recyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = layoutManager

        postList = arrayListOf()

        loadPosts()

    }

    override fun initListeners(view: View) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun loadPosts() {

        val ref = mDatabase.getReference("Posts")

        val postData : ValueEventListener? = object : ValueEventListener{

            override fun onDataChange(p0: DataSnapshot) {

                postList.clear()
                for (ds : DataSnapshot in p0.children){

                    val modelPost = ds.getValue(ModelPost::class.java)

                    if (modelPost != null) {

                        postList.add(modelPost)
                    }
                    shimmerPost.stopShimmer()
                    shimmerPost.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE

                    //adapter
                    adapterPosts = AdapterGrid(activity, postList)
                    adapterPosts.notifyDataSetChanged()

                }
                //set adapter untuk recyclerView
                recyclerView.adapter = adapterPosts
            }

            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(activity, p0.message, Toast.LENGTH_SHORT).show()
            }
        }
        if (postData != null) {
            ref.addValueEventListener(postData)
        }else{
            Toast.makeText(activity, "Gagal Memuat Data", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onResume() {
        super.onResume()
        shimmerPost.startShimmer()
    }

    override fun onPause() {
        super.onPause()
        shimmerPost.stopShimmer()
    }

    private fun searchPosts(searchQuery : String?){

        ref = mDatabase.getReference("Posts")

        val postData = object : ValueEventListener{

            @SuppressLint("DefaultLocale")
            override fun onDataChange(p0: DataSnapshot) {

                postList.clear()
                for (ds : DataSnapshot in p0.children){

                    val modelPost = ds.getValue(ModelPost::class.java)

                    if (modelPost != null) {

                        if (modelPost.pTitle.toLowerCase().contains(searchQuery!!.toLowerCase()) ||
                            modelPost.pDescr.toLowerCase().contains(searchQuery.toLowerCase())){

                            postList.add(modelPost)

                        }
                    }

                    //adapter
                    adapterPosts = AdapterGrid(activity, postList)
                    adapterPosts.notifyDataSetChanged()
                    //set adapter untuk recyclerView
                    recyclerView.adapter = adapterPosts

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

        ///untuk mencari judul deskripsi postingan
        val item = menu.findItem(R.id.action_search)
        menu.findItem(R.id.action_logout).isVisible = false

        val searchView = MenuItemCompat.getActionView(item) as SearchView



        searchView.isIconified = false

        ///untuk search listener
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{

            override fun onQueryTextSubmit(query: String?): Boolean {

                //dipanggil ketika pengguna menekan tombol pencarian
                if (!TextUtils.isEmpty(query)){

                    searchPosts(query)

                }else{

                    loadPosts()

                }

                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {

                //sipangil ketika pengguna menekan file apa saja
                if (!TextUtils.isEmpty(newText)){

                    searchPosts(newText)

                }else{

                    loadPosts()

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

            //mProfileTv.setText(user.email)

        }else{

            startActivity(Intent(activity, MainActivity::class.java))
            activity!!.finish()

        }

    }

}
