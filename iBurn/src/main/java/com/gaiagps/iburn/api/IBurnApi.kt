package com.gaiagps.iburn.api

import com.gaiagps.iburn.api.response.Art
import com.gaiagps.iburn.api.response.Camp
import com.gaiagps.iburn.api.response.DataManifest
import com.gaiagps.iburn.api.response.Event
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Streaming

interface IBurnApi {
    @GET("update.json")
    suspend fun getDataManifest(): DataManifest

    @GET("camp.json")
    suspend fun getCamps(): List<Camp>

    @GET("art.json")
    suspend fun getArt(): List<Art>

    @GET("event.json")
    suspend fun getEvents(): List<Event>

    @GET("iburn.mbtiles.jar")
    @Streaming
    suspend fun getTiles(): ResponseBody
}

