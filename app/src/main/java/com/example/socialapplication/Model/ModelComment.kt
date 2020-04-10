package com.example.socialapplication.Model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class ModelComment(var cId : String? = "",
                        var comment : String? = "",
                        var timestamp : String? = "",
                        var uid : String? = "",
                        var uEmail : String? = "",
                        var uDp : String? = "",
                        var uName : String? = "")