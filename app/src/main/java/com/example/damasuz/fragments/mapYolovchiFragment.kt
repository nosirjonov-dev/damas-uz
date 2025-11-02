package com.example.damasuz.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.damasuz.R
import com.example.damasuz.databinding.FragmentMapYolovchiBinding
import com.example.damasuz.databinding.ItemDialogMarkerBinding
import com.example.damasuz.models.*
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.database.*

class mapYolovchiFragment : Fragment() {

    private val TAG = "MapsFragment"
    private var mMap: GoogleMap? = null
    private lateinit var geocoder: Geocoder
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    lateinit var binding: FragmentMapYolovchiBinding
    lateinit var liniya: Liniya
    lateinit var yolovchi: Yolovchi
    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var referenceShopir: DatabaseReference
    lateinit var referenceYolovchi: DatabaseReference
    lateinit var shList: ArrayList<SHopir>
    lateinit var yList: ArrayList<Yolovchi>
    var userSwipe = false

    // 🔐 Permission launcher o‘rniga
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

            if (fineGranted || coarseGranted) {
                startLocationUpdate()
            } else {
                showPermissionDialog()
            }
        }

    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap
        writePolyline(myLatLngToLatLng(liniya.locationListYoli!!))

        binding.imgMapType.setOnClickListener {
            mMap?.mapType =
                if (mMap?.mapType == GoogleMap.MAP_TYPE_HYBRID)
                    GoogleMap.MAP_TYPE_NORMAL
                else
                    GoogleMap.MAP_TYPE_HYBRID
        }

        // Marker bosilganda dialog
        mMap?.setOnMarkerClickListener { marker ->
            when {
                markerListSh.containsValue(marker) -> showDriverDialog(marker)
                markerListY.containsValue(marker) -> showPassengerDialog(marker)
                else -> Toast.makeText(context, "Bu sizning marker", Toast.LENGTH_SHORT).show()
            }
            true
        }

        mMap?.setOnPolylineClickListener {
            Toast.makeText(context, "Bu chiziq ${liniya.name} liniyasi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(requireContext())
            .setMessage("Joylashuv ruxsatlari kerak. Sozlamalardan yoqing.")
            .setPositiveButton("Sozlamalarga kirish") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireActivity().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Bekor qilish", null)
            .show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapYolovchiBinding.inflate(layoutInflater)

        liniya = arguments?.getSerializable("keyLiniya") as Liniya
        yolovchi = arguments?.getSerializable("keyYolovchi") as Yolovchi

        firebaseDatabase = FirebaseDatabase.getInstance()
        referenceShopir = firebaseDatabase.getReference("shopir")
        referenceYolovchi = firebaseDatabase.getReference("yolovchi")

        geocoder = context?.let { Geocoder(it) }!!
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 10_000L
        ).setMinUpdateIntervalMillis(5_000L).build()

        listenFirebaseData()
        setupSwipeButton()

        return binding.root
    }

    private fun setupSwipeButton() {
        binding.imgSwipeUser.setOnClickListener {
            userSwipe = !userSwipe
            binding.imgSwipeUser.setImageResource(
                if (userSwipe) R.drawable.ic_swipe_user_1 else R.drawable.ic_swipe_user_0
            )
        }
    }

    private fun listenFirebaseData() {
        // 🔹 Haydovchilar
        referenceShopir.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                shList = ArrayList()
                for (child in snapshot.children) {
                    val sh = child.getValue(SHopir::class.java)
                    if (sh != null) {
                        if (sh.isOnline && sh.liniyaId == yolovchi.liniyaId) {
                            shList.add(sh)
                            addMarker(sh)
                        } else if (!sh.isOnline && markerListSh.containsKey(sh.id)) {
                            markerListSh[sh.id]?.remove()
                            markerListSh.remove(sh.id)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Internetni tekshiring", Toast.LENGTH_SHORT).show()
            }
        })

        // 🔹 Yo‘lovchilar
        referenceYolovchi.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                yList = ArrayList()
                for (child in snapshot.children) {
                    val yol = child.getValue(Yolovchi::class.java)
                    if (yol != null && yol.id != yolovchi.id && yol.liniyaId == yolovchi.liniyaId) {
                        addMarker(yol)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 🔹 Polyline
    private var polyline1: Polyline? = null
    private fun writePolyline(list: List<LatLng>) {
        polyline1?.remove()
        polyline1 = mMap?.addPolyline(
            PolylineOptions()
                .geodesic(true)
                .color(Color.BLUE)
                .clickable(true)
                .addAll(list)
        )
    }

    // 🔹 Marker qo‘shish
    private val markerListSh = HashMap<String, Marker>()
    private val markerListY = HashMap<String, Marker>()

    private fun addMarker(shopir: SHopir) {
        if (mMap == null || shopir.location == null) return

        val pos = shopir.location!!.latitude?.let { shopir.location!!.longitude?.let { it1 ->
            LatLng(it,
                it1
            )
        } }
        val marker = markerListSh[shopir.id]
        if (marker == null) {
            val newMarker = pos?.let {
                MarkerOptions()
                    .position(it)
                    .title("${shopir.name} (${shopir.phoneNumber})")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_damas))
            }?.let {
                mMap!!.addMarker(
                    it
                )
            }
            markerListSh[shopir.id!!] = newMarker!!
        } else {
            if (pos != null) {
                marker.position = pos
            }
        }
    }

    private fun addMarker(yol: Yolovchi) {
        if (mMap == null || yol.location == null) return
        val pos = yol.location!!.latitude?.let { yol.location!!.longitude?.let { it1 ->
            LatLng(it,
                it1
            )
        } }
        val marker = markerListY[yol.id]
        if (marker == null) {
            val newMarker = pos?.let {
                MarkerOptions()
                    .position(it)
                    .title("${yol.name} (${yol.number})")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.yolovchi))
            }?.let {
                mMap!!.addMarker(
                    it
                )
            }
            markerListY[yol.id!!] = newMarker!!
        } else {
            if (pos != null) {
                marker.position = pos
            }
        }
    }

    private fun showDriverDialog(marker: Marker) {
        val sh = shList.firstOrNull { markerListSh[it.id] == marker } ?: return
        val dialog = AlertDialog.Builder(context, R.style.NewDialog).create()
        val dialogBinding = ItemDialogMarkerBinding.inflate(layoutInflater)
        dialogBinding.tvName.text = sh.name
        dialogBinding.tvNumber.text = sh.phoneNumber
        dialogBinding.tvAvtoNumber.text = sh.avtoNumber
        dialogBinding.tvEmpty.text = "Odam soni: ${sh.boshJoy}"

        dialogBinding.tvNumber.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${sh.phoneNumber}")))
        }

        dialog.setView(dialogBinding.root)
        dialog.show()
    }

    private fun showPassengerDialog(marker: Marker) {
        val yol = yList.firstOrNull { markerListY[it.id] == marker } ?: return
        val dialog = AlertDialog.Builder(context, R.style.NewDialog).create()
        val dialogBinding = ItemDialogMarkerBinding.inflate(layoutInflater)
        dialogBinding.tvName.text = yol.name
        dialogBinding.tvNumber.text = yol.number
        dialogBinding.tvAvtoNumber.visibility = View.GONE
        dialogBinding.tvEmpty.visibility = View.GONE

        dialogBinding.tvNumber.setOnClickListener {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${yol.number}")))
        }

        dialog.setView(dialogBinding.root)
        dialog.show()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map_y) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    // 🔹 Joylashuv yangilanishi
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            setUserLocationMarker(loc)
        }
    }

    private var userMarker: Marker? = null
    private var userCircle: Circle? = null

    private fun setUserLocationMarker(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        if (userMarker == null) {
            val opts = MarkerOptions()
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_red_yolovchi))
                .anchor(0.5f, 0.5f)
                .position(latLng)
            userMarker = mMap?.addMarker(opts)
        } else {
            userMarker!!.position = latLng
        }

        if (!userSwipe) {
            val cam = CameraPosition.Builder()
                .target(latLng).zoom(17f).tilt(60f).build()
            mMap?.animateCamera(CameraUpdateFactory.newCameraPosition(cam))
        }

        yolovchi.location = MyLatLng(latLng.latitude, latLng.longitude)
        yolovchi.liniyaId = liniya.id
        referenceYolovchi.child(yolovchi.id!!).setValue(yolovchi)

        if (userCircle == null) {
            val circleOpts = CircleOptions()
                .center(latLng)
                .strokeWidth(4f)
                .strokeColor(Color.RED)
                .fillColor(Color.argb(50, 255, 0, 0))
                .radius(location.accuracy.toDouble())
            userCircle = mMap?.addCircle(circleOpts)
        } else {
            userCircle!!.center = latLng
            userCircle!!.radius = location.accuracy.toDouble()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdate() {
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }

    private fun stopLocationUpdate() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    override fun onStart() {
        super.onStart()
        checkPermissionsAndStart()
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdate()
        yolovchi.location = null
        yolovchi.liniyaId = null
        referenceYolovchi.child(yolovchi.id!!).setValue(yolovchi)
    }

    private fun checkPermissionsAndStart() {
        val fine = ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
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
}
