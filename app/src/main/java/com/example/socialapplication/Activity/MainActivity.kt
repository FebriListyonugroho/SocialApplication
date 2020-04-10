package com.example.socialapplication.Activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import com.example.socialapplication.R

class MainActivity : BaseActivity() {

    private lateinit var mRegisterBtn : Button
    private lateinit var mLoginBtn : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findView()
        initView()
        initListeners()

    }

    override fun findView() {

        mLoginBtn = findViewById(R.id.login_btn)
        mRegisterBtn = findViewById(R.id.register_btn)

    }

    override fun initView() {

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            val w = window
//            w.setFlags(
//                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
//                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
//            )
//        }
    }

    override fun initListeners() {

        mLoginBtn.setOnClickListener {

            startActivity(Intent(this, LoginActivity::class.java))

        }

        mRegisterBtn.setOnClickListener {

            startActivity(Intent(this, RegisterActivity::class.java))

        }

    }
}
