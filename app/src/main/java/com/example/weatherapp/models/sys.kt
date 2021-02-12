package com.example.weatherapp.models

import java.io.Serializable

data class sys (
    val type :Int,
    val id : Int,
    val message: Double,
    val country : String,
    val sunrise : Long,
    val sunset : Long
):Serializable