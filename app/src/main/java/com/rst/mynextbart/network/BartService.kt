package com.rst.mynextbart.network

import retrofit2.http.GET
import retrofit2.http.Query

interface BartService {
    @GET("etd.aspx?cmd=etd&key=MW9S-E7SL-26DU-VV8V&json=y")
    suspend fun getRealTimeEstimates(
        @Query("orig") station: String
    ): BartApiResponse

    @GET("route.aspx?cmd=routes&key=MW9S-E7SL-26DU-VV8V&json=y")
    suspend fun getRoutes(): RoutesResponse

    @GET("route.aspx?cmd=routeinfo&key=MW9S-E7SL-26DU-VV8V&json=y")
    suspend fun getRouteInfo(
        @Query("route") routeNumber: String
    ): RouteInfoResponse

    @GET("etd.aspx?cmd=etd&key=MW9S-E7SL-26DU-VV8V&json=y")
    suspend fun getDepartures(
        @Query("orig") orig: String
    ): BartApiResponse

    @GET("stn.aspx?cmd=stninfo&key=MW9S-E7SL-26DU-VV8V&json=y")
    suspend fun getStationInfo(@Query("orig") stationCode: String): StationInfoResponse
} 