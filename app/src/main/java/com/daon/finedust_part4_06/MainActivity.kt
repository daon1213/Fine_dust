package com.daon.finedust_part4_06

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.daon.finedust_part4_06.data.Repository
import com.daon.finedust_part4_06.data.models.airquality.Grade
import com.daon.finedust_part4_06.data.models.airquality.MeasuredValue
import com.daon.finedust_part4_06.data.models.monitoringstation.MonitoringStation
import com.daon.finedust_part4_06.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var cancellationTokenSource: CancellationTokenSource
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val scope = MainScope() // coroutine scope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initVariables()
        requestLocationPermissions()
    }

    override fun onDestroy() {
        super.onDestroy()
        // app 이 종료되는 시점에 cancel
        cancellationTokenSource.cancel()
        scope.cancel()
    }

    private fun bindViews() {
        binding.refreshLayout.setOnRefreshListener {
            fetchAirQualityData()
        }
    }

    private fun requestLocationPermissions() {
        // fetch Data
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            REQUEST_ACCESS_LOCATION_PERMISSIONS
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestBackgroundLocationPermissions() {
        // fetch Data
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ),
            REQUEST_BACKGROUND_ACCESS_LOCATION_PERMISSIONS
        )
    }

    @SuppressLint("ObsoleteSdkInt")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        val locationPermissionGranted =
            requestCode == REQUEST_ACCESS_LOCATION_PERMISSIONS &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED
        val backgroundLocationPermissionGranted =
            requestCode == REQUEST_BACKGROUND_ACCESS_LOCATION_PERMISSIONS &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED

        /**
         * 실무에서는 실제로 background 기능이 필요할 때, 충분한 설명을 한 뒤에
         * background permission 요청을 하도록 하자.
         */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!backgroundLocationPermissionGranted) {
                requestBackgroundLocationPermissions()
            } else {
                fetchAirQualityData()
            }
        } else {
            if (!locationPermissionGranted) { // 권한 거부
                finish()
            } else {
                fetchAirQualityData()
            }
        }
    }

    private fun initVariables() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    @SuppressLint("MissingPermission")
    private fun fetchAirQualityData() {
        cancellationTokenSource = CancellationTokenSource()
        fusedLocationProviderClient.getCurrentLocation(
            LocationRequest.PRIORITY_HIGH_ACCURACY,
            cancellationTokenSource.token
        ).addOnSuccessListener { location ->
            scope.launch {
                binding.progressBar.isVisible = true
                binding.errorDescriptionTextView.isVisible = false
                try {
                    val monitoringStation =
                        Repository.getNearbyMonitoringStation(location.latitude, location.longitude)
                    val measureValue =
                        Repository.getLatestAirQualityData((monitoringStation?.stationName ?: ""))

                    binding.refreshLayout.isRefreshing = false
                    displayAirQualityData(monitoringStation!!, measureValue!!)
                } catch (exception: Exception) {
                    binding.errorDescriptionTextView.isVisible = true
                    binding.contentsLayout.alpha = 0f
                } finally {
                    binding.progressBar.isVisible = false
                    binding.refreshLayout.isRefreshing = false
                }
            }
        }.addOnFailureListener {
            binding.errorDescriptionTextView.isVisible = true
            binding.contentsLayout.alpha = 0f
            binding.progressBar.isVisible = false
            binding.refreshLayout.isRefreshing = false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun displayAirQualityData(monitoringStation: MonitoringStation, measuredValue: MeasuredValue) {
        binding.contentsLayout.animate()
            .alpha(1F)
            .start()

        binding.measuringStationNameTextView.text = monitoringStation.stationName
        binding.measuringStationAddressTextView.text = monitoringStation.addr

        (measuredValue.khaiGrade ?: Grade.UNKNOWN).let { grade ->
            binding.root.setBackgroundResource(grade.colorResId)
            binding.totalGradeLabelTextView.text = grade.label
            binding.totalGradeEmojiTextView.text = grade.emoji
        }
        with(measuredValue) {
            binding.fineDustInfoTextView.text =
                "미세먼지: $pm10Value ㎍/㎥ ${(pm10Grade ?: Grade.UNKNOWN).emoji}"
            binding.ultraFineDustInfoTextView.text =
                "초미세먼지: $pm25Value ㎍/㎥ ${(pm25Grade ?: Grade.UNKNOWN).emoji}"

            with(binding.so2Item) {
                labelTextView.text = "아황산가스"
                gradeTextView.text = (so2Grade ?: Grade.UNKNOWN).toString()
                valueTextView.text = "$so2Value ppm"
            }
            with(binding.coItem) {
                labelTextView.text = "일산화탄소"
                gradeTextView.text = (coGrade ?: Grade.UNKNOWN).toString()
                valueTextView.text = "$coValue ppm"
            }
            with(binding.o3Item) {
                labelTextView.text = "오존"
                gradeTextView.text = (o3Grade ?: Grade.UNKNOWN).toString()
                valueTextView.text = "$o3Value ppm"
            }
            with(binding.no2Item) {
                labelTextView.text = "이산화질소"
                gradeTextView.text = (no2Grade ?: Grade.UNKNOWN).toString()
                valueTextView.text = "$no2Value ppm"
            }
        }
    }

    companion object {
        private const val REQUEST_ACCESS_LOCATION_PERMISSIONS = 1000
        private const val REQUEST_BACKGROUND_ACCESS_LOCATION_PERMISSIONS = 1001
    }
}