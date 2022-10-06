package com.example.googlemap

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import com.example.googlemap.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONException


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, APIResponse<GoogleDirection> {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    val bugis = LatLng(1.300385303831001, 103.85614863767253)
    val nationalMuseum = LatLng(1.2969931081996215, 103.8484675042243)
    val  clarkQuay = LatLng(1.2894372961693554, 103.84664091155966)
    val  tampines = LatLng(1.2868724330360888, 103.80117834179211)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        Repository.instance.getDirections(tampines,nationalMuseum,
            arrayListOf(bugis,clarkQuay),this
        )

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

        // Add a marker in Sydney and move the camera
        //val sydney = LatLng(-34.0, 151.0)
        //mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))


        mMap.addMarker(MarkerOptions().position(bugis).title("Marker in Bugis"))
        mMap.addMarker(MarkerOptions().position(nationalMuseum).title("Marker in National Museum"))
        mMap.addMarker(MarkerOptions().position(clarkQuay).title("Marker in ClarkQuay"))
        mMap.addMarker(MarkerOptions().position(tampines).title("Marker in Tampines"))

        val points: ArrayList<LatLng> = ArrayList()


//        try {
//            //here is where it will draw the polyline in your map
//            val lineOptions = PolylineOptions()
//            lineOptions.add(bugis,
//                clarkQuay, nationalMuseum, tampines)
//            lineOptions.addAll(points);
//            lineOptions.width(12f);
//            lineOptions.color(Color.RED);
//            mMap.addPolyline(lineOptions);
//
//        } catch (e: NullPointerException) {
//            Log.e("Error", "NullPointerException onPostExecute: $e")
//        } catch (e2: Exception) {
//            Log.e("Error", "Exception onPostExecute: $e2")
//        }
        val zoomLevel = 16.0f //This goes up to 21
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bugis,zoomLevel))

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

            /*
           for(int z = 0; z<list.size()-1;z++){
                LatLng src= list.get(z);
                LatLng dest= list.get(z+1);
                Polyline line = mMap.addPolyline(new PolylineOptions()
                .add(new LatLng(src.latitude, src.longitude), new LatLng(dest.latitude,   dest.longitude))
                .width(2)
                .color(Color.BLUE).geodesic(true));
            }
           */
        } catch (e: JSONException) {
        }
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