package com.rst.mynextbart.network

import retrofit2.http.GET
import retrofit2.http.Query

interface BartService {
    companion object {
        private const val API_KEY = "MW9S-E7SL-26DU-VV8V"
    }

    @GET("etd.aspx")
    suspend fun getRealTimeEstimates(
        @Query("cmd") cmd: String = "etd",
        @Query("orig") station: String,
        @Query("json") json: String = "y",
        @Query("key") key: String = API_KEY
    ): BartApiResponse

    @GET("route.aspx")
    suspend fun getRoutes(
        @Query("cmd") cmd: String = "routes",
        @Query("json") json: String = "y",
        @Query("key") key: String = API_KEY
    ): RoutesResponse
    
    @GET("route.aspx")
    suspend fun getRouteInfo(
        @Query("cmd") cmd: String = "routeinfo",
        @Query("route") route: String,
        @Query("json") json: String = "y",
        @Query("key") key: String = API_KEY
    ): RouteInfoResponse
} 