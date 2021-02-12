package com.example.weatherapp.models

import java.io.Serializable

data class WeatherResponse (
    val coord :coord,
    val weather : List<weather>,
    val base :String,
    val main : main,
    val visibility : Int,
    val wind : wind,
    val clouds : clouds,
    val dt:Double,
    val sys : sys,
    val timezone : Double,
    val id : Int,
    val name : String,
    val cod : Int
): Serializable