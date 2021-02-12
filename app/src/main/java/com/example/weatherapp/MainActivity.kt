package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

     var mdialog :Dialog?= null
    private lateinit var mlocationfused :FusedLocationProviderClient
    private lateinit var msharedpreferences :SharedPreferences
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        msharedpreferences = getSharedPreferences(constans.PREFERENCE_NAME, MODE_PRIVATE)

        mlocationfused = LocationServices.getFusedLocationProviderClient(this)

        setupUI()

        if (!islocationenabled()){

            Toast.makeText(this,"your provider location turned off. please turn it",Toast.LENGTH_LONG).show()

           val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)

        }else{

            // use Dexter to ask permission from the user..
            Dexter.withContext(this).withPermissions(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            ).withListener(object :MultiplePermissionsListener{
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()){

                       RequestLocation()
                    }
                    if (report.isAnyPermissionPermanentlyDenied){

                        Toast.makeText(this@MainActivity,
                                "you have denied. please enable theme as it is mandatory for the app to work ",
                                Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(p0: MutableList<PermissionRequest>?, p1: PermissionToken?) {
                    showRationalDialog()
                }
            }).onSameThread().check()
        }
    }

    override fun onResume() {

        if (mdialog != null) {
            closedialoge()
            Log.i("on resume", "on resume")
        }
        super.onResume()
    }
    override fun onPause() {
        if (mdialog != null) {
            Log.i("on pause", "on pause")
            closedialoge()
        }
        super.onPause()
    }

    // inflate menu items..
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)

        return super.onCreateOptionsMenu(menu)
    }

    //action when item selected from menu...
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){

            R.id.refresh_action ->{
                RequestLocation()
                true
            }else -> return super.onOptionsItemSelected(item)
        }
    }

   //method to get location weather details an use retrofit...retrofit.
    private fun getLocationweatherdetails(latitude: Double, longitude:Double){

        if (constans.isNetworkAvilable(this)){   // chick if internet is available..


            // prepare retrofit with base url..
            val retrofit :Retrofit = Retrofit.Builder().baseUrl(constans.BASE_URL).
                          addConverterFactory(GsonConverterFactory.create()).build()
            // prepare service ...retrofit
            val service :WeatherService = retrofit.create<WeatherService>(WeatherService::class.java)
            // prepare call.. retrofit
            val listCall :Call<WeatherResponse> = service.getweather(latitude,longitude,
                          constans.METRIC_UNIT,constans.APP_ID)

            showdialog() // show progress bare ...

           // make call object and execute.. retrofit.
            listCall.enqueue(object :Callback<WeatherResponse> {

                @RequiresApi(Build.VERSION_CODES.N)
                override fun onResponse(
                    call: Call<WeatherResponse>,response: Response<WeatherResponse>
                ) {
                    // chick if connection done successful..
                    if (response.isSuccessful){

                        val weatherlist = response.body()


                        // convert weather object from json to string and add it to preference variable..
                        val weatherresponsejsonstring = Gson().toJson(weatherlist)
                        val editor = msharedpreferences.edit()
                            editor.putString(constans.WEATHER_PREFERENCE_DATA ,weatherresponsejsonstring)
                            editor.apply()

                        setupUI()

                        closedialoge()
                    }else{

                        val rc = response.code()
                        when(rc) {

                            400 -> {
                                Log.i("400 error", "bad connection")
                            }
                            404 -> {
                                Log.i("404 error", "not found")
                            }
                            else -> {

                                Log.i("error", "general error")
                            }
                        }

                    }
                }
               // method on failure..
                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {

                    Log.i("errorrr","${t.message}")

                   closedialoge()
                }
            })
        }else{
            Toast.makeText(this,"No Internet connection Available",Toast.LENGTH_SHORT).show()

        }

    }

  // Rational Dialog... Dexter permission.
    private fun showRationalDialog (){

        AlertDialog.Builder(this).setMessage("it looks you have turned off permissions required for this feature." +
                " it can be enabled under application setting "
        ).setPositiveButton("GO TO SETTING "){
            _,_ ->

            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)

            }catch (e:ActivityNotFoundException){
                e.printStackTrace()
            }
        }.setNegativeButton("Cancel"){
            dialog,_ ->

            dialog.dismiss()
        }.show()
    }


    // put weather details from API into UI...
    @RequiresApi(Build.VERSION_CODES.N)
    @SuppressLint("SetTextI18n")
    private fun setupUI (){

        // get weather string object from preference...
        val weatherresponsejsonstring = msharedpreferences.getString(constans.WEATHER_PREFERENCE_DATA,"")

       // chick if weather object is not null or empty...
        if (!weatherresponsejsonstring.isNullOrEmpty()){

            // convert weather object from string to json object..
            val weatherlist = Gson().fromJson(weatherresponsejsonstring,WeatherResponse ::class.java)

            for (i in weatherlist.weather.indices){

                Log.i("weather list", weatherlist.toString())

                // prepare UI elements ...
                weather.text = weatherlist.weather[i].main
                condition.text = weatherlist.weather[i].description
                degree.text =  weatherlist.main.temp.toString()+getunit(application.resources.configuration.locales.toString())
                sunrise.text = unixtime(weatherlist.sys.sunrise)
                sunset.text = unixtime(weatherlist.sys.sunset)
                minimum.text = weatherlist.main.temp_min.toString() +" min"
                maximum.text = weatherlist.main.temp_max.toString() +" max"
                name.text = weatherlist.name
                percent.text = weatherlist.main.humidity.toString()+ " per cent"
                country.text = weatherlist.sys.country
                wind.text = weatherlist.wind.speed.toString()
                when(weatherlist.weather[i].icon){

                    // images of weather condition....
                    "02d" ->{icon.setImageResource(R.drawable.cloud)}
                    "02n" ->{icon.setImageResource(R.drawable.cloud)}
                    "03d" ->{icon.setImageResource(R.drawable.cloud)}
                    "03n" ->{icon.setImageResource(R.drawable.cloud)}
                    "04d" ->{icon.setImageResource(R.drawable.cloud)}
                    "04n" ->{icon.setImageResource(R.drawable.cloud)}
                    "13d" ->{icon.setImageResource(R.drawable.snowflake)}
                    "10d" ->{icon.setImageResource(R.drawable.rain)}
                    "09d" ->{icon.setImageResource(R.drawable.rain)}
                    "11d" ->{icon.setImageResource(R.drawable.storm)}
                    "01d" ->{icon.setImageResource(R.drawable.cleare)}
                    "50d" ->{icon.setImageResource(R.drawable.haze)}

                }
            }
        }



    }
   //get degree unit 'c' or 'f'...
    private fun getunit (value:String):String{

        var values = "°C"

        if ("US" == value || "LR" == value || "MM" == value){

            values = "°F"
        }
                return values
    }

    // convert time from API to readable time...
    private fun unixtime(timx:Long):String{

        val date = (timx*1000L)
        val sdf = SimpleDateFormat("HH:mm",Locale.UK)
            sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)

    }

    // Request location method...
    @SuppressLint ("Missingpermission")
    private fun RequestLocation (){

        val locationrequest = LocationRequest()
        locationrequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mlocationfused.requestLocationUpdates(locationrequest,locationcallback, Looper.myLooper())
    }

    // location callback variable ...
    private val locationcallback = object : LocationCallback(){

        override fun onLocationResult(locationresult: LocationResult?) {

            val mlastlocation = locationresult!!.lastLocation

            val latitude = mlastlocation.latitude
            Log.i("latitude","$latitude")
            val longitude = mlastlocation.longitude
            Log.i("longitude","$longitude")

            getLocationweatherdetails(latitude,longitude)
        }

    }

    // show progress bar..
    private fun  showdialog (){


       mdialog = Dialog(this@MainActivity)
       mdialog!!.setContentView(R.layout.progreesdialog)
       mdialog!!.show()
    }

    private fun closedialoge (){

        mdialog?.dismiss()
    }

    // chick if location provider is enabled or not...
    private fun islocationenabled ():Boolean{
        val locationmanager :LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return locationmanager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
               locationmanager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}