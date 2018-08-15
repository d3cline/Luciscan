package com.objectsyndicate.luciscan

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Service
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast
import android.Manifest.permission
import android.Manifest.permission.WRITE_CALENDAR
import android.support.v4.content.ContextCompat



class LocationTracker(//declaring Context variable
        private val con: Context) : Service(), LocationListener {
    //flag for gps
    internal var isGPSOn = false
    //flag for network location
    internal var isNetWorkEnabled = false
    //flag to getlocation
    var isLocationEnabled = false
        internal set
    //location
    internal var location: Location? = null
    //latitude and longitude
    internal var latitude: Double = 0.toDouble()
    internal var longitude: Double = 0.toDouble()
    //Declaring a LocationManager
    internal var locationManager: LocationManager? = null

    init {
        checkIfLocationAvailable()

    }

    @SuppressLint("MissingPermission")
    fun checkIfLocationAvailable(): Location? {
        try {
            locationManager = con.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            //check for gps availability
            isGPSOn = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
            //check for network availablity
            isNetWorkEnabled = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (!isGPSOn && !isNetWorkEnabled) {
                isLocationEnabled = false
                // no location provider is available show toast to user
                Toast.makeText(con, "No Location Provider is Available", Toast.LENGTH_SHORT).show()
            } else {
                isLocationEnabled = true
                // if network location is available request location update
                if (isNetWorkEnabled) {
                    locationManager!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, MIN_TIME_FOR_UPDATES, MIN_DISTANCE_TO_REQUEST_LOCATION.toFloat(), this)
                    if (locationManager != null) {
                        location = locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        if (location != null) {
                            latitude = location!!.latitude
                            longitude = location!!.longitude
                        }
                    }
                }
                if (isGPSOn) {
                    locationManager!!.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_FOR_UPDATES, MIN_DISTANCE_TO_REQUEST_LOCATION.toFloat(), this)
                    if (locationManager != null) {
                        location = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (location != null) {
                            latitude = location!!.latitude
                            longitude = location!!.longitude
                        }
                    }
                }
            }
        } catch (e: Exception) {
        }

        return location
    }

    // call this to stop using location
    fun stopUsingLocation() {
        if (locationManager != null) {
            locationManager!!.removeUpdates(this@LocationTracker)
        }
    }

    // call this to getLatitude
    fun getLatitude(): Double {
        if (location != null) {
            latitude = location!!.latitude
        }
        return latitude
    }

    //call this to getLongitude
    fun getLongitude(): Double {
        if (location != null) {
            longitude = location!!.longitude
        }
        return longitude
    }

    //call to open settings and ask to enable Location
    fun askToOnLocation() {
        val dialog = AlertDialog.Builder(con)
        //set title
        dialog.setTitle("Settings")
        //set Message
        dialog.setMessage("Location is not Enabled.Do you want to go to settings to enable it?")
        // on pressing this will be called
        dialog.setPositiveButton("Settings") { dialog, which ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            con.startActivity(intent)
        }
        //on Pressing cancel
        dialog.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }
        // show Dialog box
        dialog.show()
    }

    override fun onLocationChanged(location: Location) {
        this.location = location
    }

    override fun onProviderDisabled(provider: String) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        //minimum distance to request for location update
        private val MIN_DISTANCE_TO_REQUEST_LOCATION: Long = 1 // in meters
        // minimum time to request location updates
        private val MIN_TIME_FOR_UPDATES = (1000 * 1).toLong() // 1 sec
    }
}