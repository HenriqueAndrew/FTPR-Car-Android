package com.example.myapitest

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.myapitest.databinding.ActivityNewCarBinding
import com.example.myapitest.model.Car
import com.example.myapitest.model.Place
import com.example.myapitest.service.Result
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.safeApiCall
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

class NewCarActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityNewCarBinding
    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var selectedMarker: Marker? = null

    private lateinit var imageUri: Uri
    private var imageFile: File? = null
    private val cameraLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            binding.imageUrl.setText("Imagem Obtida")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNewCarBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setupGoogleMap()
        setupView()
    }

    private fun setupView() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.saveCta.setOnClickListener {
            save()
        }
        binding.takePictureCta.setOnClickListener{
            takePicture()
        }
    }

    private fun setupGoogleMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        binding.mapContent.visibility = View.VISIBLE
        getDeviceLocation()
        mMap.setOnMapClickListener { latLng: LatLng ->
            selectedMarker?.remove()
            selectedMarker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .draggable(true)
                    .title("Lat: ${latLng.latitude}, Long: ${latLng.longitude}")
            )
        }
    }

    private fun getDeviceLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PERMISSION_GRANTED
        ) {
            loadCurrentLocation()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun loadCurrentLocation() {
        mMap.isMyLocationEnabled = true
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location ->
            val currentLocation = LatLng(location.latitude, location.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadCurrentLocation()
            } else {
                Toast.makeText(this, getString(R.string.permissao_negada), Toast.LENGTH_SHORT).show()
            }
        } else if(requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PERMISSION_GRANTED) {
                openCamera()
            } else {
                showToast(getString(R.string.permissaocamera_negada))
            }
        }
    }

    private fun takePicture() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermission()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        imageUri = createImageUri()
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraLauncher.launch(intent)
    }

    private fun createImageUri(): Uri {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_${timeStamp}_"
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        imageFile = File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
        return FileProvider.getUriForFile(
            this,
            "com.example.myapitest.fileprovider",
            imageFile!!
        )
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE
        )
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun uploadImageToFirebase() {
        val storageRef = FirebaseStorage.getInstance().reference
        val imagesRef = storageRef.child("images/${UUID.randomUUID()}.jpg")
        val baos = ByteArrayOutputStream()
        val imageBitmap = BitmapFactory.decodeFile(imageFile!!.path)
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        binding.loadImageProgress.visibility = View.VISIBLE
        binding.takePictureCta.isEnabled = false
        binding.saveCta.isEnabled = false
        imagesRef.putBytes(data)
            .addOnFailureListener {
                binding.loadImageProgress.visibility = View.GONE
                binding.takePictureCta.isEnabled = true
                binding.saveCta.isEnabled = true
                Toast.makeText(this, "Falha ao realizar o upload", Toast.LENGTH_SHORT).show()
            }
            .addOnSuccessListener {
                binding.loadImageProgress.visibility = View.GONE
                binding.takePictureCta.isEnabled = true
                binding.saveCta.isEnabled = true
                imagesRef.downloadUrl.addOnSuccessListener { uri ->
                    saveData(uri.toString())
                }
            }
    }

    private fun save() {

        if (!validateForm()) return
        uploadImageToFirebase()
    }

    private fun saveData(imageUrl: String) {
        val carPosition = selectedMarker?.position?.let {
            Place(it.latitude, it.longitude)
        }
        CoroutineScope(Dispatchers.IO).launch {
            val id = SecureRandom().nextInt().toString()
            val car = Car(
                id = id,
                imageUrl,
                year = binding.ano.text.toString(),
                name = binding.modelo.text.toString(),
                licence = binding.licence.text.toString(),
                place = carPosition
            )
            val result = safeApiCall { RetrofitClient.apiService.saveCar(car) }
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Error -> {
                        Toast.makeText(this@NewCarActivity, getString(R.string.erro_cadastro), Toast.LENGTH_SHORT).show()
                    }
                    is Result.Success -> {
                        Toast.makeText(this@NewCarActivity, getString(R.string.sucesso_cadastro, result.data.id), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun validateForm(): Boolean {
        if (binding.modelo.text.toString().isBlank()) {
            Toast.makeText(this, getString(R.string.erro_exclusao_form, "Modelo"), Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.ano.text.toString().isBlank()) {
            Toast.makeText(this, getString(R.string.erro_exclusao_form, "Ano"), Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.licence.text.toString().isBlank()) {
            Toast.makeText(this, getString(R.string.erro_exclusao_form, "Placa"), Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.imageUrl.text.toString().isBlank()) {
            Toast.makeText(this, getString(R.string.erro_exclusao_form, "Imagem URL"), Toast.LENGTH_SHORT).show()
            return false
        }
        if (imageFile == null) {
            Toast.makeText(this, getString(R.string.error_validate_take_picture), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    companion object {

        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
        private const val CAMERA_REQUEST_CODE = 101

        fun newIntent(context: Context) = Intent(context, NewCarActivity::class.java)
    }

}