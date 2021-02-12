package com.example.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object constans{


    const val APP_ID :String = "509c71d4b989cd63182921c147fb07ea"
    const val BASE_URL:String = "http://api.openweathermap.org/data/"
    const val METRIC_UNIT :String = "metric"

    const val PREFERENCE_NAME = "weather app preference"
    const val WEATHER_PREFERENCE_DATA = "preference data"


    // chick if the Internet connection is Available...
       fun isNetworkAvilable(contex:Context):Boolean{

           val connectivitymanager = contex.getSystemService(Context.CONNECTIVITY_SERVICE)
                                                     as ConnectivityManager

           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){

               val network = connectivitymanager.activeNetwork ?: return false
               val activeNetwork = connectivitymanager.getNetworkCapabilities(network) ?: return false

               return when{

                   activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                   activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->true
                   activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->true
                   else -> false
               }

           }else{

               val networkinfo = connectivitymanager.activeNetworkInfo
               return networkinfo != null && networkinfo.isConnectedOrConnecting
           }
       }
 }