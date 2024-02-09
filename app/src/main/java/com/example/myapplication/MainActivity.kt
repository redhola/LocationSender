package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// Uygulamanın ana sınıfı
class MainActivity : ComponentActivity() {

    // Seçilen kişinin telefon numarasını saklamak için kullanılan değişken
    private var selectedContactNumber: String? = null
    private lateinit var locationManager: LocationManager
    private var locationListener: LocationListener? = null
    private lateinit var contactPickerLauncher: ActivityResultLauncher<Intent>
    private val REQUEST_SMS_PERMISSION = 101
    private val REQUEST_LOCATION_PERMISSION = 102

    private var currentLocation: Location? = null
    private var isSMSSent: Boolean = false

    // Uygulama başladığında çalışan fonksiyon
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Kişi seçme işlemi için ActivityResultLauncher'ı ayarla
        contactPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Kişi seçildiğinde çalışacak kod
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val contactUri: Intent? = result.data
                processSelectedContact(contactUri)
            }
        }

        // Uygulamanın kullanıcı arayüzünü ayarla
        setContent {
            MainContent { pickContact() }
        }

        // İzinleri kontrol et ve gerekirse izin iste
        checkPermissions()
    }

    // İzin kontrolü ve izin isteme işlemleri
    private fun checkPermissions() {
        // Konum ve SMS izinlerini kontrol et, eksikse kullanıcıdan iste
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), REQUEST_SMS_PERMISSION)
        }
        // Konum almayı başlat
        getLocation()
    }

    // Kullanıcı arayüzünü oluşturan Composable fonksiyon
    @Composable
    fun MainContent(onSelectPersonClicked: () -> Unit) {
        // Arayüzdeki elemanların yerleşimi
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Konum ve SMS Uygulaması", modifier = Modifier.padding(bottom = 8.dp))
            // Kişi seçme butonu
            Button(onClick = onSelectPersonClicked, modifier = Modifier.padding(top = 8.dp)) {
                Text("Kişi Seç")
            }
            // Konum gönderme butonu
            Button(onClick = {
                // Eğer SMS gönderilmediyse ve bir kişi seçildiyse konum gönder
                if (!isSMSSent && selectedContactNumber != null) {
                    currentLocation?.let { location ->
                        selectedContactNumber?.let { phoneNumber ->
                            sendSMSWithLocation(location, phoneNumber)
                        }
                    }
                } else {
                    Toast.makeText(applicationContext, "Konum alınıyor veya kişi seçilmedi", Toast.LENGTH_LONG).show()
                }
            }, modifier = Modifier.padding(top = 8.dp)) {
                Text("Konum Gönder")
            }
        }
    }

    // Kişi seçmek için Intent başlatan fonksiyon
    private fun pickContact() {
        val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI).apply {
            type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
        }
        contactPickerLauncher.launch(pickContactIntent)
    }

    // Seçilen kişiyi işleyip, telefon numarasını alıp saklayan fonksiyon
    private fun processSelectedContact(contactUri: Intent?) {
        contactUri?.data?.let { uri ->
            val cursor = contentResolver.query(uri, arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER), null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                selectedContactNumber = cursor.getString(numberIndex)
                cursor.close()
            }
        }
    }

    // Cihazın konumunu almayı başlatan fonksiyon
    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener = LocationListener { location ->
            currentLocation = location
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener!!)
        }
    }

    // Seçilen kişiye konumu SMS ile gönderen fonksiyon
    private fun sendSMSWithLocation(location: Location, phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            val message = "Benim konumum: Enlem: ${location.latitude}, Boylam: ${location.longitude}"
            SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, null, null)
            Toast.makeText(this, "SMS gönderildi.", Toast.LENGTH_LONG).show()
            isSMSSent = true
        }
    }

    // Kullanıcıdan alınan izinlerin sonuçlarını işleyen fonksiyon
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // İzin sonuçlarına göre işlem yap
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLocation()
            }
        } else if (requestCode == REQUEST_SMS_PERMISSION) {
            if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "SMS izni gerekiyor.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
