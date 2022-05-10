package com.daon.finedust_part4_06.data.models.airquality

data class Body(
    val measuredValues: List<MeasuredValue>,
    val numOfRows: Int,
    val pageNo: Int,
    val totalCount: Int
)