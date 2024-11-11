package com.example.myapitest

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapitest.databinding.ActivityCarDetailBinding
import com.example.myapitest.model.Car
import com.example.myapitest.service.Result
import com.example.myapitest.service.RetrofitClient
import com.example.myapitest.service.safeApiCall
import com.example.myapitest.ui.loadUrl
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CarDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityCarDetailBinding
    private lateinit var car: Car
    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCarDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupView()
        loadCar()
        setupGoogleMap()
    }

    private fun setupView(){

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.deleteCTA.setOnClickListener {
            deleteCar()
        }
        binding.editCTA.setOnClickListener {
            editItem()
        }

    }

    private fun setupGoogleMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (::car.isInitialized) {
            loadItemLocationInGoogleMap()
        }
    }

    private fun loadItemLocationInGoogleMap() {
        car.place?.let {
            binding.googleMapContent.visibility = View.VISIBLE
            val latLong = LatLng(it.lat, it.long)
            mMap.addMarker(
                MarkerOptions().position(latLong)
            )
            mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    latLong,
                    18f
                )
            )
        }
    }

    private fun editItem(){
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall {
                RetrofitClient.apiService.updateCar(car.id, car.copy(licence = binding.licence.text.toString()))
            }

            withContext(Dispatchers.Main){
                when(result){
                    is Result.Error -> {
                        Toast.makeText(this@CarDetailActivity,
                            getString(R.string.erro_atualizacao), Toast.LENGTH_SHORT).show()
                    }
                    is Result.Success -> {
                        Toast.makeText(this@CarDetailActivity,
                            getString(R.string.sucesso_atualizacao), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun deleteCar(){
        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.deleteCar(car.id) }

            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Error -> {
                        Toast.makeText(this@CarDetailActivity,
                            getString(R.string.erro_exclusao), Toast.LENGTH_SHORT).show()
                    }
                    is Result.Success -> {
                        Toast.makeText(this@CarDetailActivity,
                            getString(R.string.sucesso_exclusao), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun loadCar(){

        val carId = intent.getStringExtra(ARG_ID) ?: ""

        CoroutineScope(Dispatchers.IO).launch {
            val result = safeApiCall { RetrofitClient.apiService.getCarValue(carId) }

            withContext(Dispatchers.Main){
                when(result){
                    is Result.Error -> {}
                    is Result.Success -> {
                        car = result.data.value
                        handleOnSuccess()
                    }
                }
            }
        }
    }

    private fun handleOnSuccess(){
        binding.modelo.text = car.name
        binding.ano.text = car.year
        binding.licence.setText(car.licence)
        binding.image.loadUrl(car.imageUrl)
        loadItemLocationInGoogleMap()
    }

    companion object{

        private const val ARG_ID = "ARG_ID"

        fun newIntent(context: Context, carId: String) = Intent(context, CarDetailActivity::class.java).apply {
            putExtra(ARG_ID, carId)
        }

    }

}