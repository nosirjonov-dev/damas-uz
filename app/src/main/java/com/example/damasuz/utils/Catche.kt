package com.example.damasuz.utils

import android.content.Context
import android.content.SharedPreferences

object MySharedPrefarance {
    private const val NAME = "KeshXotiraga"
    private const val MODE = Context.MODE_PRIVATE
    private lateinit var preferences: SharedPreferences

    fun init(context: Context?) {
        preferences = context?.getSharedPreferences(NAME, MODE)!!
    }

    private inline fun SharedPreferences.edit(operation: (SharedPreferences.Editor) -> Unit) {
        val editor = edit()
        operation(editor)
        editor.apply()
    }

    var name:String?
        get() = preferences.getString("name", "")
        set(value)= preferences.edit{
            it.putString("name", value)
        }
    var number:String?
        get() = preferences.getString("number", "")
        set(value)= preferences.edit{
            it.putString("number", value)
        }
    var code:Boolean?
        get() = preferences.getBoolean("code", false)
        set(value)= preferences.edit{
            if (value != null) {
                it.putBoolean("code", value)
            }
        }
}