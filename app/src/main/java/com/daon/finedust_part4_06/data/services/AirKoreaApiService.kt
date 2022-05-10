package com.daon.finedust_part4_06.data.services

import androidx.viewbinding.BuildConfig
import com.daon.finedust_part4_06.data.models.airquality.AirQualityResponse
import com.daon.finedust_part4_06.data.models.monitoringstation.MonitoringStationsResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface AirKoreaApiService {

    @GET("B552584/MsrstnInfoInqireSvc/getNearbyMsrstnList" +
            "?serviceKey=${com.daon.finedust_part4_06.BuildConfig.AIR_KOREA_SERVICE_KEY}" +
            "&returnType=json")
    suspend fun getNearbyMonitoringStation(
        @Query("tmX") tmX: Double,
        @Query("tmY") tmY: Double
    ): Response<MonitoringStationsResponse>

    @GET("B552584/ArpltnInforInqireSvc/getMsrstnAcctoRltmMesureDnsty" +
            "?serviceKey=${com.daon.finedust_part4_06.BuildConfig.AIR_KOREA_SERVICE_KEY}" +
            "&returnType=json" +
            "&dataTerm=Daily" +
            "&ver=1.3")
    suspend fun getRealtimeAirQualities(
        @Query("stationName") stationName: String
    ): Response<AirQualityResponse>
}