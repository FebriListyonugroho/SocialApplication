package com.example.socialapplication.Model

class ModelPost (val pId : String, val pTitle : String, val pLikes : String, val pHarga : String, val pMasa : String, val pKategori : String, val pDescr : String, val pComments : String, val pImage : String, val pTime : String, val uid : String, val uEmail : String, val uDp : String, val uName : String){

    constructor() : this("","","","","","","",
        "","","","","","",""){}
}
