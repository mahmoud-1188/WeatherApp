package com.example.weatherapp.models

import java.io.Serializable

data class main (
    val temp : Double,
    val feels_like :Double,
    val temp_min : Double,
    val temp_max : Double,
    val pressure : Int,
    val humidity : Int
): Serializable