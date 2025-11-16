package com.example.damasuz.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Point
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.activity.result.contract.ActivityResultContracts
import com.example.damasuz.R
import com.example.damasuz.databinding.FragmentMapsBinding
import com.example.damasuz.databinding.ItemDialogMarkerBinding
import com.example.damasuz.models.*
import com.example.damasuz.models.myLatLngToLatLng
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*

class MapsFragment : Fragment() {

    private val TAG = "MapsFragment"
    var mMap: GoogleMap? = null
    lateinit var geocoder: Geocoder
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    lateinit var locationRequest: LocationRequest

    lateinit var binding: FragmentMapsBinding
    lateinit var liniya: Liniya
    lateinit var shopir: SHopir
    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var referenceShopir: DatabaseReference
    lateinit var referenceYolovchi: DatabaseReference

    lateinit var shList: ArrayList<SHopir>
    lateinit var yList: ArrayList<Yolovchi>

    val markerListSh = HashMap<String, Marker>()
    val markerListY = HashMap<String, Marker>()

    var polyline1: Polyline? = null
    var userSwipe = false
    var userLocationMarker: Marker? = null
    var userLocationAcuracyCircle: Circle? = null


    // ---------------------- PERMISSION LAUNCHER ----------------------

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fine || coarse) {
                startLocationUpdate()
            } else {
                showPermissionDialog()
            }
        }

    private fun checkPermissionsAndStart() {
        val fine = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdate()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage("Joylashuv ruxsatlari kerak. Sozlamalardan yoqing.")
            .setPositiveButton("Sozlamalar") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }

    // ---------------------- END PERMISSION ----------------------


    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap
        writePolyline(myLatLngToLatLng(liniya.locationListYoli!!))

        binding.imgMapType.setOnClickListener {
            mMap?.mapType = if (mMap?.mapType == GoogleMap.MAP_TYPE_HYBRID)
                GoogleMap.MAP_TYPE_NORMAL else GoogleMap.MAP_TYPE_HYBRID
        }

        mMap?.setOnMarkerClickListener {
            if (markerListSh.values.contains(it)) {
                var shp = SHopir()
                for (sh in shList) {
                    if (markerListSh[sh.id] == it) {
                        shp = sh
                        break
                    }
                }

                val dialog = AlertDialog.Builder(context, R.style.NewDialog).create()
                val d = ItemDialogMarkerBinding.inflate(layoutInflater)

                d.tvName.text = shp.name
                d.tvNumber.text = shp.phoneNumber
                d.tvAvtoNumber.text = shp.avtoNumber
                d.tvEmpty.text = "Odam soni: ${shp.boshJoy}"

                d.tvNumber.setOnClickListener {
                    val uri = "tel:${shp.phoneNumber}"
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(uri)))
                }

                dialog.setView(d.root)
                dialog.show()

            } else if (markerListY.values.contains(it)) {

                var yol = Yolovchi()
                for (sh in yList) {
                    if (markerListY[sh.id] == it) {
                        yol = sh
                        break
                    }
                }

                val dialog = AlertDialog.Builder(context, R.style.NewDialog).create()
                val d = ItemDialogMarkerBinding.inflate(layoutInflater)

                d.tvName.text = yol.name
                d.tvNumber.text = yol.number
                d.tvAvtoNumber.visibility = View.GONE
                d.tvEmpty.visibility = View.GONE

                d.tvNumber.setOnClickListener {
                    val uri = "tel:${yol.number}"
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse(uri)))
                }

                dialog.setView(d.root)
                dialog.show()

            } else {
                Toast.makeText(context, "Bu marker sizni emas!", Toast.LENGTH_SHORT).show()
            }
            true
        }

        mMap?.setOnPolylineClickListener {
            Toast.makeText(context, "${liniya.name} — asosiy yo‘l", Toast.LENGTH_LONG).show()
        }
    }


    fun writePolyline(list: List<LatLng>) {
        polyline1?.remove()

        polyline1 = mMap?.addPolyline(
            PolylineOptions().geodesic(true).clickable(true).addAll(list)
        )
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMapsBinding.inflate(layoutInflater)

        liniya = arguments?.getSerializable("keyLiniya") as Liniya
        shopir = arguments?.getSerializable("keyShopir") as SHopir

        firebaseDatabase = FirebaseDatabase.getInstance()
        referenceShopir = firebaseDatabase.getReference("shopir")
        referenceYolovchi = firebaseDatabase.getReference("yolovchi")

        geocoder = Geocoder(requireContext())
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        locationRequest = LocationRequest.create()
        locationRequest.interval = 500
        locationRequest.fastestInterval = 500
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        /** Shopirlar realtime */
        referenceShopir.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                shList = ArrayList()

                for (child in snapshot.children) {
                    val value = child.getValue(SHopir::class.java)
                    if (value != null) {
                        if (value.id != shopir.id && value.isOnline && value.liniyaId == liniya.id) {
                            shList.add(value)
                            addMarker(value)
                        }
                        if (!value.isOnline && markerListSh.containsKey(value.id)) {
                            markerListSh[value.id]?.remove()
                            markerListSh.remove(value.id)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Internetni tekshiring...", Toast.LENGTH_SHORT).show()
            }
        })

        /** Yo‘lovchilar realtime */
        referenceYolovchi.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                yList = ArrayList()

                for (child in snapshot.children) {
                    val value = child.getValue(Yolovchi::class.java)
                    if (value != null) {
                        if (value.location != null && value.liniyaId == liniya.id) {
                            yList.add(value)
                            addMarker(value)
                        }
                        if (value.location == null && markerListY.containsKey(value.id)) {
                            markerListY[value.id]?.remove()
                            markerListY.remove(value.id)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        binding.imgSwipeUser.setOnClickListener {
            userSwipe = !userSwipe
            binding.imgSwipeUser.setImageResource(
                if (userSwipe) R.drawable.ic_swipe_user_1 else R.drawable.ic_swipe_user_0
            )
        }

        binding.btnPlus.setOnClickListener {
            shopir.boshJoy++
            binding.tvCount.text = shopir.boshJoy.toString()
        }
        binding.btnMinus.setOnClickListener {
            shopir.boshJoy--
            binding.tvCount.text = shopir.boshJoy.toString()
        }

        return binding.root
    }


    fun addMarker(shopir: SHopir) {
        if (mMap != null) {
            if (markerListSh.containsKey(shopir.id)) {
                val marker = markerListSh[shopir.id]
                marker?.position = shopir.location!!.latitude?.let { shopir.location!!.longitude?.let { it1 ->
                    LatLng(it,
                        it1
                    )
                } }!!
                marker?.title = "${shopir.name}\n${shopir.phoneNumber}\nodam soni: ${shopir.boshJoy}"
            } else {
                val marker =
                    shopir.location!!.latitude?.let { shopir.location!!.longitude?.let { it1 ->
                        LatLng(it,
                            it1
                        )
                    } }?.let {
                        MarkerOptions()
                            .position(it)
                            .title("${shopir.name}\n${shopir.phoneNumber}\nodam soni: ${shopir.boshJoy}")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_damas))
                    }?.let {
                        mMap!!.addMarker(
                            it
                        )
                    }
                markerListSh[shopir.id!!] = marker!!
            }
        }
    }

    fun addMarker(yol: Yolovchi) {
        if (mMap != null) {
            if (!markerListY.containsKey(yol.id)) {
                val marker = yol.location!!.latitude?.let { yol.location!!.longitude?.let { it1 ->
                    LatLng(it,
                        it1
                    )
                } }?.let {
                    MarkerOptions()
                        .position(it)
                        .title("${yol.name}\n${yol.number}")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.yolovchi))
                }?.let {
                    mMap!!.addMarker(
                        it
                    )
                }
                markerListY[yol.id!!] = marker!!
            } else {
                markerListY[yol.id]?.position =
                    yol.location!!.latitude?.let { yol.location!!.longitude?.let { it1 ->
                        LatLng(it,
                            it1
                        )
                    } }!!
            }
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }


    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            if (mMap != null) setUserLocationMarker(p0.lastLocation!!)
        }
    }


    fun setUserLocationMarker(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)

        if (userLocationMarker == null) {
            val markerOptions = MarkerOptions()
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.located))
            markerOptions.anchor(0.5F, 0.5F)
            markerOptions.position(latLng)
            userLocationMarker = mMap?.addMarker(markerOptions)
        } else {
            userLocationMarker?.position = latLng
        }

        binding.tvCount.text = shopir.boshJoy.toString()

        val projection = mMap!!.projection
        val markerPoint = projection.toScreenLocation(latLng)
        val targetPoint = Point(markerPoint.x, markerPoint.y - view?.height!! / 3)
        val targetPosition = projection.fromScreenLocation(targetPoint)

        if (!userSwipe) {
            val currentPlace = CameraPosition.Builder()
                .target(targetPosition)
                .bearing(location.bearing)
                .zoom(18f)
                .build()

            mMap?.animateCamera(CameraUpdateFactory.newCameraPosition(currentPlace), 500, null)
        }

        val myLatLng = MyLatLng(latLng.latitude, latLng.longitude)
        shopir.location = myLatLng
        shopir.isOnline = true
        referenceShopir.child(shopir.id!!).setValue(shopir)

        if (userLocationAcuracyCircle == null) {
            val circleOptions = CircleOptions()
            circleOptions.center(latLng)
            circleOptions.strokeWidth(4F)
            circleOptions.strokeColor(Color.argb(255, 255, 0, 0))
            circleOptions.fillColor(Color.argb(32, 255, 0, 0))
            circleOptions.radius(location.accuracy.toDouble())
            userLocationAcuracyCircle = mMap?.addCircle(circleOptions)
        } else {
            userLocationAcuracyCircle?.center = latLng
            userLocationAcuracyCircle?.radius = location.accuracy.toDouble()
        }
    }


    // ---------------- LOCATION UPDATES --------------------

    @SuppressLint("MissingPermission")
    fun startLocationUpdate() {
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdate() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    // ---------------- LIFECYCLE --------------------

    override fun onStart() {
        super.onStart()
        checkPermissionsAndStart()
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdate()
        shopir.location = null
        shopir.isOnline = false
        referenceShopir.child(shopir.id!!).setValue(shopir)
    }
}
