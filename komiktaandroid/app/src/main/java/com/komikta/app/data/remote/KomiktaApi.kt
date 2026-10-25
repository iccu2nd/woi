package com.komikta.app.data.remote

import com.google.gson.JsonElement
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class ApiEnvelope<T>(val status: Boolean, val data: T? = null, val message: String? = null)
data class ExplorePage(
    val items: List<JsonElement> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pages: Int = 1,
    val sort: String = "latest"
)

interface KomiktaApi {
    @GET("api/home") suspend fun home(): ApiEnvelope<JsonElement>
    @GET("api/update") suspend fun updates(): ApiEnvelope<JsonElement>
    @GET("api/search") suspend fun search(@Query("q") query: String): ApiEnvelope<JsonElement>
    @GET("api/explore") suspend fun explore(
        @Query("q") query: String = "",
        @Query("genre") genre: String = "",
        @Query("format") format: String = "",
        @Query("sort") sort: String = "latest",
        @Query("page") page: Int = 1,
        @Query("size") size: Int = 60
    ): ApiEnvelope<ExplorePage>
    @GET("api/detail") suspend fun detail(@Query("url") sourceUrl: String): ApiEnvelope<JsonElement>
    @GET("api/read") suspend fun chapter(@Query("url") sourceUrl: String): ApiEnvelope<JsonElement>

    companion object {
        // Set this to the deployed Komikta backend URL, including trailing slash.
        fun create(baseUrl: String): KomiktaApi = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(KomiktaApi::class.java)
    }
}
