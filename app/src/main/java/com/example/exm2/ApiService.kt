package com.example.exm2

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Path
interface ApiService {
    @GET("search.json")
    suspend fun searchBooks(@Query("q") query: String): BookResponse
    @GET("works/{key}.json")
    suspend fun getBookDetails(@Path("key") key: String): BookDetailsResponse
}