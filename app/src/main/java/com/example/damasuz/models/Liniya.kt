package com.example.damasuz.models

import java.io.Serializable

class Liniya : Serializable {
    var id:String? = null
    var name:String? = null
    var locationListYoli:ArrayList<MyLatLng>? = null

    constructor(id: String?, name: String?, locationListYoli: ArrayList<MyLatLng>?) {
        this.id = id
        this.name = name
        this.locationListYoli = locationListYoli
    }

    constructor()

    override fun toString(): String {
        return "Liniya(id=$id, name=$name, locationListYoli=$locationListYoli)"
    }
}