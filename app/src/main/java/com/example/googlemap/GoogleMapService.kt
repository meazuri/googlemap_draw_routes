package com.example.googlemap

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.QueryMap


interface GoogleMapService {

    @GET("json")
    fun getDirections(@QueryMap queryMap: Map<String, String>): Call<GoogleDirection>
}