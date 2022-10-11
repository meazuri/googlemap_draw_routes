package com.example.googlemap

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class Repository private constructor(){

    private val retrofitService: GoogleMapService
    val HTTPS_API_URL = "https://maps.googleapis.com/maps/api/directions/"
    private val mError: MutableLiveData<Map<Int, String>> = MutableLiveData()

    init {
        val logging = HttpLoggingInterceptor()
        // set your desired log level
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        val httpClient = OkHttpClient.Builder()
        // add your other interceptors …
        // add logging as last interceptor
        httpClient.addInterceptor(logging)
        val retrofit = Retrofit.Builder()
            .baseUrl(HTTPS_API_URL)
            .client(httpClient.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        retrofitService = retrofit.create(GoogleMapService::class.java!!)
    }

    companion object {
        private var projectRepository: Repository? = null

        val instance: Repository
            @Synchronized get() {
                if (projectRepository == null) {
                    if (projectRepository == null) {
                        projectRepository = Repository()
                    }
                }
                return projectRepository as Repository
            }
    }

    fun getDirections(origin: LatLng,dest:LatLng,waypoints:ArrayList<LatLng>, dataResponse : APIResponse<GoogleDirection>): MutableLiveData<GoogleDirection> {
        var data = MutableLiveData<GoogleDirection>()

        var waypointList :String = "optimize:true|"
        val waypointsString = waypoints.map { x -> "via:"+ x.latitude.toString() + ","+ x.longitude.toString() }      // 2
            waypointList = waypointList.plus( waypointsString.joinToString("|"))
        Log.i("waypointList",waypointList)
        val queryMap = HashMap<String,String>()
        queryMap.put("origin",origin.latitude.toString().replace(" ","") +","+origin.longitude.toString())
        queryMap.put("waypoints",waypointList)
        queryMap.put("destination",dest.latitude.toString()+","+dest.longitude.toString())
        queryMap.put("mode","driving")
        queryMap.put("sensor","false")
        queryMap.put("key",BuildConfig.MAPS_API_KEY)

        retrofitService.getDirections(queryMap).enqueue(object : Callback<GoogleDirection> {
            override fun onResponse(
                call: Call<GoogleDirection>,
                response: Response<GoogleDirection>
            ) {
                if (response.code() == 200) {
                    Log.i("response",response.body().toString())
                    data.value = response.body()
                    dataResponse.onSuccess(data)
                } else {
                    val message = response.errorBody()?.charStream()?.readText().toString()
                    mError.value = mapOf(Pair(1, message))
                }
            }
            override fun onFailure(call: Call<GoogleDirection>, t: Throwable) {
             val message = t.message.toString().let {
                mError.value = null
                } as String
                mError.value = mapOf(Pair(1,message))            }

        })

        return data
    }


    fun getErrorData(): LiveData<Map<Int, String>> {
        return mError
    }

//    enqueue(object : Callback<List<GoogleDirection>> {
//        override fun onResponse(call: Call<List<GoogleDirection>>, response: Response<List<GoogleDirection>>) {
//
//            if(response.code() == 200 ) {
//                data.value = response.body()
//
//            }else{
//                val message = response.errorBody()?.charStream()?.readText().toString()
//                mError.value = mapOf(Pair(1,message))
//            }
//        }
//
//        override fun onFailure(call: Call<List<GoogleDirection>>, t: Throwable) {
//            data.setValue(null)
//            val message = t.message.toString().let {
//                mError.value = null
//            } as String
//            mError.value = mapOf(Pair(1,message))
//
//        }
//    })


}

