package com.example.socialapplication.Model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ModelChat (var message : String? = "",
                      var receiver : String? = "",
                      var sender : String? = "",
                      var timestamp : String? = "",
                      var type : String? = "",
                      var isSeen : Boolean = false )
