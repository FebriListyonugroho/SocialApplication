package com.example.socialapplication.Model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ModelUsers(var name : String? = "",
                      var email: String? = "",
                      var search : String? = "",
                      var phone : String? = "",
                      var image : String? = "",
                      var cover : String? = "",
                      var typingTo : String? = "",
                      var onlineStatus : String? = "",
                      var uid : String? = "")