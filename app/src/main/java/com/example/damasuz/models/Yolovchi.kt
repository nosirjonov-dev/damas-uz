package com.example.damasuz.models

import java.io.Serializable

class Yolovchi : Serializable{
    var id:String? = null
    var name:String? = null
    var number:String? = null
    var location:MyLatLng? = null
    var liniyaId:String? = null


    constructor()
    constructor(id: String?, name: String?, number: String?) {
        this.id = id
        this.name = name
        this.number = number
    }

    constructor(id: String?, name: String?, number: String?, location: MyLatLng?) {
        this.id = id
        this.name = name
        this.number = number
        this.location = location
    }
}

class MyLatLng: Serializable {
    var latitude:Double? = null
    var longitude:Double? = null

    constructor(latitude: Double?, longitude: Double?) {
        this.latitude = latitude
        this.longitude = longitude
    }

    constructor()

    override fun toString(): String {
        return "MyLatLng(latitude=$latitude, longitude=$longitude)"
    }
}