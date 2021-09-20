package com.example.livetrackingapp

import android.Manifest
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.livetrackingapp.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.Marker
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var map: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private val LOCATION_PERMISSION_REQUEST = 1
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    lateinit var databaseRef: DatabaseReference
    lateinit var trackingBtn: Button
    lateinit var marker1 : Marker




    //to access mile location
    //whether the device access permission is allowed by the user and asks again if not granted

    private fun getLocationAccess(){
        if(ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    //map.isMyLocationEnabled = true
                    getLocationUpdates()
                    startLocationUpdates()
        }else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST)
        }
    }

    //opens the map if allowed, toast a message otherwise. work with the getLocationAccess()
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST){
            if(grantResults.contains(PackageManager.PERMISSION_GRANTED)){
                getLocationAccess()
            }else{
                Toast.makeText(this, "Grant permission to use this app", Toast.LENGTH_LONG).show()

            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        getLocationUpdates()
        fusedLocationClient =  LocationServices.getFusedLocationProviderClient(this)

    }

    //fetching location from firebase on location path
    val logListener = object : ValueEventListener {
        override fun onCancelled(error: DatabaseError) {
            Toast.makeText(applicationContext, "Could not read from database, Check Internet and On GPS", Toast.LENGTH_SHORT).show()
        }

        //using datasnapshot to get the data from firebase for Gbohunmi
        override fun onDataChange(dataSnapshot: DataSnapshot) {
            if (dataSnapshot.exists()) {
                val locationlog = dataSnapshot.child("Tracker").getValue(LocationModel::class.java)
                val driverLat = locationlog?.latitude
                val driverLong = locationlog?.longitude

                if (driverLat != null  && driverLong != null) {
                    val driverLoc = LatLng(driverLat, driverLong)

                    val markerOptions = MarkerOptions().position(driverLoc).title("Gbohunmi")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.gbohunmi_50x50))
                    map.clear()
                    map.mapType = GoogleMap.MAP_TYPE_HYBRID
                    map.addMarker(markerOptions)
                    marker1.position = driverLoc
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(driverLoc, 30f))
                    //Zoom level - 1: World, 5: Landmass/continent, 10: City, 15: Streets and 20: Buildings
                    Toast.makeText(applicationContext, "Tracking Gbohunmi...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    //the function called when the map loads on the screen
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.mapType = GoogleMap.MAP_TYPE_SATELLITE
        marker1 = map.addMarker(MarkerOptions().position(LatLng(6.2353766, 5.5736061)).title("AbdulRasheed's Location")
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ayo_50x50)))

        trackingBtn = findViewById(R.id.btn_find_location)
        trackingBtn.setOnClickListener {
            getLocationAccess()
        }

    }

    //function to get location updates every 5seconds
    private fun getLocationUpdates() {
        locationRequest = LocationRequest()
        locationRequest.interval = 6_000
        locationRequest.fastestInterval = 5_000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult.locations.isNotEmpty()) {
                    val location = locationResult.lastLocation

                    //storing co-ordinates to firebase
                    databaseRef = Firebase.database.reference
                    val locationlog = LocationModel(location.latitude, location.longitude)
                    databaseRef.addValueEventListener(logListener)
                    databaseRef.child("location").setValue(locationlog)
                        .addOnSuccessListener {
                            //Toast.makeText(applicationContext, "Updates position", Toast.LENGTH_LONG).show()
//                            return@addOnSuccessListener
                        }
                        .addOnFailureListener {
                            Toast.makeText(applicationContext, "Unable to update location, Turn on your GPS and internet", Toast.LENGTH_LONG).show()
                        }

//                    val latLng = LatLng(location.latitude, location.longitude)
//                    val markerOptions = MarkerOptions().position(latLng).title("AbdulRasheed")
//                    //.icon(BitmapDescriptorFactory.fromResource(R.drawable.model_1))
//                    map.clear()
//                    map.addMarker(markerOptions)
//                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }
        }
    }


    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompatRequestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

}













































//
//import android.Manifest
//import android.content.pm.PackageManager
//import android.os.Bundle
//import android.util.Log
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import com.google.android.gms.location.FusedLocationProviderClient
//import com.google.android.gms.location.LocationServices
//import com.google.android.gms.maps.CameraUpdateFactory
//import com.google.android.gms.maps.GoogleMap
//import com.google.android.gms.maps.OnMapReadyCallback
//import com.google.android.gms.maps.SupportMapFragment
//import com.google.android.gms.maps.model.LatLng
//import com.google.android.gms.maps.model.MarkerOptions
//import com.google.firebase.database.DatabaseReference
//import com.google.firebase.database.FirebaseDatabase
//
//class MapsActivity : AppCompatActivity(), OnMapReadyCallback {
//
//    private lateinit var map: GoogleMap
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_maps)
//        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
//        val mapFragment = supportFragmentManager
//            .findFragmentById(R.id.map) as SupportMapFragment
//        mapFragment.getMapAsync(this)
//        setupLocClient()
//
//    }
//
//    private lateinit var fusedLocClient: FusedLocationProviderClient
//    // use it to request location updates and get the latest location
//
//    override fun onMapReady(googleMap: GoogleMap) {
//        map = googleMap //initialise map
//        getCurrentLocation()
//    }
//    private fun setupLocClient() {
//        fusedLocClient =
//            LocationServices.getFusedLocationProviderClient(this)
//    }
//
//    // prompt the user to grant/deny access
//    private fun requestLocPermissions() {
//        ActivityCompat.requestPermissions(this,
//            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), //permission in the manifest
//            REQUEST_LOCATION
//        )
//    }
//
//    companion object {
//        private const val REQUEST_LOCATION = 1 //request code to identify specific permission request
//        private const val TAG = "com.example.livetrackingapp.MapsActivity" // for debugging
//    }
//
//    private fun getCurrentLocation() {
//        // Check if the ACCESS_FINE_LOCATION permission was granted before requesting a location
//        if (ActivityCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION) !=
//            PackageManager.PERMISSION_GRANTED) {
//
//            // call requestLocPermissions() if permission isn't granted
//            requestLocPermissions()
//        } else {
//
//            fusedLocClient.lastLocation.addOnCompleteListener {

//                // lastLocation is a task running in the background
//                val location = it.result //obtain location

//                //reference to the database
//                val database: FirebaseDatabase = FirebaseDatabase.getInstance()
//                val ref: DatabaseReference = database.getReference("location")
//                if (location != null) {
//
//                    val latLng = LatLng(location.latitude, location.longitude)
//                    // create a marker at the exact location
//                    map.addMarker(
//                        MarkerOptions().position(latLng)
//                        .title("You are currently here!"))
//                    // create an object that will specify how the camera will be updated
//                    val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
//
//                    map.moveCamera(update)
//                    //Save the location data to the database
//                    ref.setValue(location)
//                } else {
//                    // if location is null , log an error message
//                    Log.e(TAG, "No location found")
//                }
//
//
//
//            }
//        }
//    }
//
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        //check if the request code matches the REQUEST_LOCATION
//        if (requestCode == REQUEST_LOCATION)
//        {
//            //check if grantResults contains PERMISSION_GRANTED.If it does, call getCurrentLocation()
//            if (grantResults.size == 1 && grantResults[0] ==
//                PackageManager.PERMISSION_GRANTED) {
//                getCurrentLocation()
//            } else {
//                //if it doesn`t log an error message
//                Log.e(TAG, "Location permission has been denied")
//            }
//        }
//    }
//
//}