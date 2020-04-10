package com.example.socialapplication.Fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

abstract class BaseFragment : Fragment(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    abstract fun findView(view : View)
    abstract fun initView(view: View)
    abstract fun initListeners(view: View)

}