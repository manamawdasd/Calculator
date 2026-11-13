package com.example.data

import com.squareup.moshi.Json
import retrofit2.http.GET

data class ExchangeRateResponse(
    val result: String?,
    @Json(name = "time_last_update_unix") val timeLastUpdateUnix: Long?,
    @Json(name = "base_code") val baseCode: String?,
    val rates: Map<String, Double>?
)

interface ExchangeRateApi {
    @GET("v6/latest/USD")
    suspend fun getLatestUsdRates(): ExchangeRateResponse
}
