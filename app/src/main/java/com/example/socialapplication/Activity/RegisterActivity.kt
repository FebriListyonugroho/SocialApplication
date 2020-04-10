package com.example.socialapplication.Activity

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.socialapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

@Suppress("DEPRECATION", "NAME_SHADOWING")
class RegisterActivity : BaseActivity() {

    private lateinit var mEmailEt : EditText
    private lateinit var mPasswordEt : EditText
    private lateinit var mRegisterBtn : Button
    private lateinit var mHaveAccountTv : TextView

    private lateinit var progressDialog : ProgressDialog

    private lateinit var mAuth : FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        findView()
        initView()
        initListeners()

    }

    override fun findView() {

        mEmailEt = findViewById(R.id.emailEt)
        mPasswordEt = findViewById(R.id.passwordEt)
        mRegisterBtn = findViewById(R.id.registerBtn)
        mHaveAccountTv = findViewById(R.id.have_accountTv)

    }

    override fun initView() {

        mAuth = FirebaseAuth.getInstance()

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Registering User ...")

    }

    override fun initListeners() {

        mRegisterBtn.setOnClickListener {

            prosesValidasiRegisterAccount()

        }

        mHaveAccountTv.setOnClickListener {

            startActivity(Intent(this, LoginActivity::class.java))
            finish()

        }

    }

    private fun prosesValidasiRegisterAccount() {

        val email = mEmailEt.text.toString().trim()
        val password = mPasswordEt.text.toString().trim()

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){

            mEmailEt.error = "Invalid Email"
            mEmailEt.isFocusable = true

        }
        else if (password.length < 6){

            mPasswordEt.error = "Password length at least 6 characters"
            mPasswordEt.isFocusable = true

        }
        else {

            registerUser(email, password)

        }

    }

    private fun registerUser(email: String, password: String) {

        progressDialog.show()

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->

            if (task.isSuccessful){

                progressDialog.dismiss()
                val user = mAuth.currentUser

                val email = user!!.email
                val uid = user.uid

                val dbProfile = FirebaseDatabase.getInstance().getReference("Users").child(uid)

                val hashMap : HashMap<String, Any?>? = HashMap()
                hashMap?.set("email", email!!)
                hashMap?.set("uid", uid)
                hashMap?.set("name", "UserName")
                hashMap?.set("onlineStatus", "online")
                hashMap?.set("typingTo", "noOne")
                hashMap?.set("phone", "")
                hashMap?.set("image",
                    "https://firebasestorage.googleapis.com/v0/b/socialapplication-79355.appspot.com/o/Users_Profile_Cover_Imgs%2Fuser_profil_default.png?alt=media&token=43b9ace0-5037-4c14-a5da-5722b0c2949a"
                )
                hashMap?.set("cover", "")

                dbProfile.setValue(hashMap)

                Toast.makeText(this, "Register Sucessfully \n ${user.email}", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()

            }else{

                progressDialog.dismiss()
                Toast.makeText(this, "Authentication Failed", Toast.LENGTH_SHORT).show()

            }

        }.addOnFailureListener { exception ->

            progressDialog.dismiss()
            Toast.makeText(this, "${exception.message}", Toast.LENGTH_SHORT).show()

        }

    }

}
