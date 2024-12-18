package com.example.habit_tracker.network

import QuotesApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
//import com.example.habit_tracker.QuotesApiService

object RetrofitInstance {
    private const val BASE_URL = "https://api.api-ninjas.com/"

    val api: QuotesApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuotesApiService::class.java)
    }
}
