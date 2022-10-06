package com.example.googlemap

import androidx.lifecycle.LiveData


interface APIResponse<T> {

    fun onSuccess( data: LiveData<T>)
}