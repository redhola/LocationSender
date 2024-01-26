package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.telephony.SmsManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import android.widget.Toast


class MainActivity : ComponentActivity() {

    private lateinit var locationManager: LocationManager
    private var locationListener: LocationListener? = null
    private val REQUEST_SMS_PERMISSION = 101
    private val REQUEST_LOCATION_PERMISSION = 102
    private var currentLocation: Location? = null
    private var isSMSSent: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent{
            MainContent()
        }
        checkLocationPermission()
        checkSMSPermission()
    }

    @Composable
    fun MainContent() {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Konum ve SMS Uygulaması", modifier = Modifier.padding(bottom = 8.dp))
            Button(onClick = {
                if (!isSMSSent) {
                    checkLocationPermission()
                    checkSMSPermission()
                    Toast.makeText(applicationContext, "Butona basıldı: İzinler kontrol ediliyor ve konum alınıp SMS gönderilecek", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(applicationContext, "SMS zaten gönderildi", Toast.LENGTH_LONG).show()
                }
            }) {
                Text("Konum Al ve SMS Gönder")
            }
        }
    }

    private fun checkSMSPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), REQUEST_SMS_PERMISSION)
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
        } else {
            getLocation()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getLocation()
                }
            }
            REQUEST_SMS_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    currentLocation?.let { sendSMSWithLocation(it) }
                }
            }
        }
    }

    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = LocationListener { location ->
            currentLocation = location
            sendSMSWithLocation(location)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener!!)
        }
    }

    private fun sendSMSWithLocation(location: Location) {
        if (!isSMSSent && ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            val phoneNo = "5456652875"  // Alıcı telefon numarası
            val message = "Benim konumum: Enlem: ${location.latitude}, Boylam: ${location.longitude}"

            try {
                SmsManager.getDefault().sendTextMessage(phoneNo, null, message, null, null)
                isSMSSent = true  // SMS gönderildi olarak işaretle
                Toast.makeText(this, "SMS gönderildi", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


}
