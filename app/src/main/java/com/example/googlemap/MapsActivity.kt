package com.example.googlemap

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import com.example.googlemap.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import org.json.JSONException


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, APIResponse<GoogleDirection> {

    private var locationByNetwork: Location? =null
    private  var locationByGps: Location? = null
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    val bugis = LatLng(1.300385303831001, 103.85614863767253)
    val nationalMuseum = LatLng(1.2969931081996215, 103.8484675042243)
    val  clarkQuay = LatLng(1.2894372961693554, 103.84664091155966)
    val  tampines = LatLng(1.2868724330360888, 103.80117834179211)

    var carMarker :Marker ? = null

    //lateinit var fusedLocationClient : FusedLocationProviderClient

    private var currentLocation: Location? = null
    lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //To show Current Location
       // fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if(isLocationPermissionGranted(this,100)){
            setUpLocationService()
        }




    }
    fun setUpLocationService(){
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
//------------------------------------------------------//
        val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        val gpsLocationListener: LocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationByGps= location
                showCurrentLocation()
            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
//------------------------------------------------------//
        val networkLocationListener: LocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                locationByNetwork= location
                showCurrentLocation()
            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        if (hasGps) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                5000,
                0F,
                gpsLocationListener
            )
        }
//------------------------------------------------------//
        if (hasNetwork) {
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000,
                0F,
                networkLocationListener
            )
        }

    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.addMarker(MarkerOptions().position(bugis).title("Marker in Bugis"))
        mMap.addMarker(MarkerOptions().position(nationalMuseum).title("Marker in National Museum"))
        mMap.addMarker(MarkerOptions().position(clarkQuay).title("Marker in ClarkQuay"))
        mMap.addMarker(MarkerOptions().position(tampines).title("Marker in Tampines"))


        carMarker = mMap.addMarker(
            MarkerOptions().position(nationalMuseum)
                .title("Marker in Sydney") // below line is use to add custom marker on our map.
                .icon(BitmapFromVector(applicationContext,R.drawable.green_car))
        )
        Repository.instance.getDirections(tampines,nationalMuseum,
            arrayListOf(bugis,clarkQuay),this
        )
        carMarker?.let {
            moveMarker(it,bugis,  5000)
        }


    }

    fun  showCurrentLocation(){
        val lastKnownLocationByGps =
            locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        lastKnownLocationByGps?.let {
            locationByGps = lastKnownLocationByGps
        }
//------------------------------------------------------//
        val lastKnownLocationByNetwork =
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        lastKnownLocationByNetwork?.let {
            locationByNetwork = lastKnownLocationByNetwork
        }
//------------------------------------------------------//
        if (locationByGps != null && locationByNetwork != null) {
            if (locationByGps!!.accuracy > locationByNetwork!!.accuracy) {
                currentLocation = locationByGps
                showYourLocatioin()
                // use latitude and longitude as per your need
            } else {
                currentLocation = locationByNetwork
                showYourLocatioin()
                // use latitude and longitude as per your need
            }
        }else if(locationByGps !=null){
            currentLocation = locationByGps
            showYourLocatioin()
        }else{
            currentLocation = locationByNetwork
            showYourLocatioin()
        }
    }
    fun showYourLocatioin(){
        val latitude = currentLocation?.latitude
        val longitude = currentLocation?.longitude

        val currentLocation = if (longitude != null && latitude != null ) {
            LatLng(latitude,longitude)
        } else {
            null
        }
        currentLocation?.let {
            mMap.addMarker(
                MarkerOptions().position(
                    currentLocation
                ).title("It's Me!").icon(BitmapFromVector(applicationContext,R.drawable.ic_user))
            )
            val zoomLevel = 12.0f //This goes up to 21
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation,zoomLevel))

        }
    }
    fun  moveMarker(marker: Marker, newPosition: LatLng, interval: Long){
        val handler = Handler()
        val runnable = Runnable {
            kotlin.run {
                marker.position = newPosition
            }
        }
        handler.postAtTime(runnable,System.currentTimeMillis() +interval)
        handler.postDelayed(runnable, interval);

    }

    private fun drawPath(result: GoogleDirection) {
        try {
            //Tranform the string into a json object
            val route = result.routes.firstOrNull()
            route?.let {
                val encodePolyLine = it.overview_polyline.points
                val list: List<LatLng> = decodePoly(encodePolyLine)
                val line = mMap.addPolyline(
                    PolylineOptions()
                        .addAll(list)
                        .width(12f)
                        .color(Color.parseColor("#05b1fb")) //Google maps blue color
                        .geodesic(true)
                )
            }
        } catch (e: JSONException) {
        }
    }
    private fun BitmapFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        // below line is use to generate a drawable.
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)

        // below line is use to set bounds to our vector drawable.
        vectorDrawable!!.setBounds(
            0,
            0,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )

        // below line is use to create a bitmap for our
        // drawable which we have added.
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        // below line is use to add bitmap in our canvas.
        val canvas = Canvas(bitmap)

        // below line is use to draw our
        // vector drawable in canvas.
        vectorDrawable.draw(canvas)

        // after generating our bitmap we are returning our bitmap.

        val smallMarker = Bitmap.createScaledBitmap(bitmap, 100, 100, false)
        return BitmapDescriptorFactory.fromBitmap(smallMarker)

    }
    private fun decodePoly(encoded: String): List<LatLng> {
        val poly: MutableList<LatLng> = ArrayList()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val p = LatLng(
                lat.toDouble() / 1E5,
                lng.toDouble() / 1E5
            )
            poly.add(p)
        }
        return poly
    }

    override fun onSuccess(data: LiveData<GoogleDirection>) {

        data.value?.let {
            drawPath(it)

        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 100) {
            // Request for permission.
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                setUpLocationService()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    100
                )
            }
        }
    }
}