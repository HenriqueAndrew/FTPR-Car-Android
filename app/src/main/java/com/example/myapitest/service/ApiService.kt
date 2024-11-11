package com.example.myapitest.service

import com.example.myapitest.model.Car
import com.example.myapitest.model.CarValue
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @GET("car")
    suspend fun getCar(): List<Car>

    @GET("car/{id}")
    suspend fun getCarValue(@Path("id") id: String): CarValue

    @DELETE("car/{id}")
    suspend fun deleteCar(@Path("id") id: String)

    @POST("car")
    suspend fun saveCar(@Body car: Car) : CarValue

    @PATCH("car/{id}")
    suspend fun updateCar(@Path("id") id: String, @Body car: Car) : CarValue

}