package com.example.googlemap

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import com.example.googlemap.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import org.json.JSONException
import java.util.*
import kotlin.collections.ArrayList


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, APIResponse<GoogleDirection> {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    val bugis = LatLng(1.300385303831001, 103.85614863767253)
    val nationalMuseum = LatLng(1.2969931081996215, 103.8484675042243)
    val  clarkQuay = LatLng(1.2894372961693554, 103.84664091155966)
    val  tampines = LatLng(1.2868724330360888, 103.80117834179211)

    var carMarker :Marker ? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)


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
        //mMap.addMarker(MarkerOptions().position(nationalMuseum).title("Marker in National Museum"))
        mMap.addMarker(MarkerOptions().position(clarkQuay).title("Marker in ClarkQuay"))
        mMap.addMarker(MarkerOptions().position(tampines).title("Marker in Tampines"))


        carMarker = mMap.addMarker(
            MarkerOptions().position(nationalMuseum)
                .title("Marker in Sydney") // below line is use to add custom marker on our map.
                .icon(BitmapFromVector(applicationContext,R.drawable.green_car))
        )
        val zoomLevel = 16.0f //This goes up to 21
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bugis,zoomLevel))
        Repository.instance.getDirections(tampines,nationalMuseum,
            arrayListOf(bugis,clarkQuay),this
        )
        carMarker?.let {
            moveMarker(it,bugis,  5000)
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
}