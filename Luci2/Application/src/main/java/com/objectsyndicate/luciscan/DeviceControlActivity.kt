/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.objectsyndicate.luciscan

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.*
import android.os.AsyncTask
import android.os.Bundle
import android.os.IBinder
import android.text.InputFilter
import android.text.TextUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import java.util.*
import kotlin.concurrent.fixedRateTimer
import android.util.Base64;
import android.view.View
import android.widget.*
import com.github.mikephil.charting.charts.CombinedChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.Math.pow
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.scheduleAtFixedRate

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with `BluetoothLeService`, which in turn interacts with the
 * Bluetooth LE API.
 */
class DeviceControlActivity : Activity() {
    private var mConnectionState: TextView? = null
    private var mDataField: TextView? = null
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null
    private var mGattServicesList: ExpandableListView? = null

    private var mBluetoothLeService: BluetoothLeService? = null
    private var mGattCharacteristics: ArrayList<ArrayList<BluetoothGattCharacteristic>>? = ArrayList()
    private var mConnected = false
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null
    private val LIST_NAME = "NAME"
    private val LIST_UUID = "UUID"
    lateinit var fixedRateTimer: Timer
    var latitude: Double = 0.00
    var longitude: Double = 0.00
    var userTokenPref: SharedPreferences? = null
    //lateinit var tracker: LocationTracker

    var scanning = true
    var char2: BluetoothGattCharacteristic? = null
    /////////////////////////////////////////////////// Render data captured
    var v450 = 0.0F
    var v500 = 0.0F
    var v550 = 0.0F
    var v570 = 0.0F
    var v600 = 0.0F
    var v650 = 0.0F

    /*
    var v610 = 0.0F
    var v680 = 0.0F
    var v730 = 0.0F
    var v760 = 0.0F
    var v810 = 0.0F
    var v860 = 0.0F
    */
//////////////////////////////////////////////////////////////
    // Code to manage Service lifecycle.
    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                finish()
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService!!.connect(mDeviceAddress)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }
    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private val mGattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                   // println("CONNECT")
                    mConnected = true
                    updateConnectionState(R.string.connected)
                    invalidateOptionsMenu()
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    val progbar = findViewById<ProgressBar>(R.id.progressBar)
                    progbar.visibility = View.VISIBLE

                    mConnected = false
                    updateConnectionState(R.string.disconnected)
                    invalidateOptionsMenu()
                    clearUI()
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> // Show all the supported services and characteristics on the user interface.

                    displayGattServices(mBluetoothLeService!!.supportedGattServices)

                BluetoothLeService.ACTION_DATA_AVAILABLE -> {
                   // println("AVAILABE")

                    displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
                }
            }
        }
    }

    private fun clearUI() {
        //mGattServicesList!!.setAdapter(null as SimpleExpandableListAdapter?)
        try {
            fixedRateTimer.cancel()
        }catch(v: UninitializedPropertyAccessException){
            //print(v)
        }
        //mDataField!!.setText(R.string.no_data)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gatt_services_characteristics)

        userTokenPref = this.getSharedPreferences(getString(R.string.preference_file_key), 0)

        val intent = intent
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)

        //val permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        //val cCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        mConnectionState = findViewById(R.id.connection_state)

        actionBar!!.title = mDeviceName
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)

    }


    override fun onResume() {
        //println("RESUME")
        super.onResume()
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        if (mBluetoothLeService != null) {
            //println("NULL")
            val result = mBluetoothLeService!!.connect(mDeviceAddress)
            Log.d(TAG, "Connect request result=" + result)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mGattUpdateReceiver)
        try {
            fixedRateTimer.cancel()
        }catch(v: UninitializedPropertyAccessException){
            //print(v)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
        mBluetoothLeService = null
        try {
            fixedRateTimer.cancel()
        }catch(v: UninitializedPropertyAccessException){
            //print(v)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gatt_services, menu)
        if (mConnected) {
            menu.findItem(R.id.menu_connect).isVisible = false
            menu.findItem(R.id.menu_disconnect).isVisible = true
        } else {
            menu.findItem(R.id.menu_connect).isVisible = true
            menu.findItem(R.id.menu_disconnect).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_connect -> {
                mBluetoothLeService!!.connect(mDeviceAddress)
                return true
            }
            R.id.menu_disconnect -> {
                onBackPressed()
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateConnectionState(resourceId: Int) {
        runOnUiThread { mConnectionState!!.setText(resourceId) }
    }

    var VioletMol = 0.00
    var BlueMol = 0.00
    var GreenMol = 0.00
    var YellowMol = 0.00
    var OrangeMol = 0.00
    var RedMol = 0.00

    private fun displayData(data: String?) {
    val progbar = findViewById<ProgressBar>(R.id.progressBar)
    progbar.visibility = View.VISIBLE
        if (data != null) {
            //println(data)
            try {
                val o = data.split(":")
                //119.708/nm=Watt/Mol 119.708/500=1/x
                // LightGuidanceNotes.pdf page 12
                when {
                        o[0] == "0" -> {
                            v450 = (100/90)*((o[1].toFloat()))
                            val VioletWatts = v450
                            VioletMol = VioletWatts * 3.759
                            val Vv = findViewById<TextView>(R.id.violet_value)
                            Vv.text = String.format("%,.2f",VioletWatts) +" W/m²"
                            val Vm = findViewById<TextView>(R.id.violet_mol)
                            Vm.text = String.format("%,.2f",VioletMol)+" μMole/m²"
                        }
                        o[0] == "1" -> {
                            v500 = (100/90)*((o[1].toFloat()))
                            val BlueWatts = v500
                            BlueMol = BlueWatts * 4.177
                            val Bv = findViewById<TextView>(R.id.blue_value)
                            Bv.text = String.format("%,.2f",BlueWatts) +" W/m²"
                            val Bm = findViewById<TextView>(R.id.blue_mol)
                            Bm.text = String.format("%,.2f",BlueMol)+" μMole/m²"
                        }
                        o[0] == "2" -> {
                            v550 = (100/90)*((o[1].toFloat()))
                            val GreenWatts = v550
                            GreenMol = GreenWatts * 4.595
                            val Gv = findViewById<TextView>(R.id.green_value)
                            Gv.text = String.format("%,.2f",GreenWatts) +" W/m²"
                            val Gm = findViewById<TextView>(R.id.green_mol)
                            Gm.text = String.format("%,.2f",GreenMol)+" μMole/m²"
                        }
                        o[0] == "3" -> {
                            v570 = (100/90)*((o[1].toFloat()))
                            val YellowWatts =v570
                            YellowMol =  YellowWatts * 4.762
                            val Yv = findViewById<TextView>(R.id.yellow_value)
                            Yv.text = String.format("%,.2f",YellowWatts) +" W/m²"
                            val Ym = findViewById<TextView>(R.id.yellow_mol)
                            Ym.text = String.format("%,.2f",YellowMol)+" μMole/m²"
                        }
                        o[0] == "4" -> {
                            v600 = (100/90)*((o[1].toFloat()))
                            val OrangeWatts = v600
                            OrangeMol = OrangeWatts * 5.012
                            val Ov = findViewById<TextView>(R.id.orange_value)
                            Ov.text = String.format("%,.2f",OrangeWatts) +" W/m² "
                            val Om = findViewById<TextView>(R.id.orange_mol)
                            Om.text = String.format("%,.2f",OrangeMol)+" μMole/m²"
                        }
                        o[0] == "5" -> {
                            v650 = (100/90)*((o[1].toFloat()))
                            val RedWatts = v650
                            RedMol = RedWatts * 5.430
                            val Redv = findViewById<TextView>(R.id.red_value)
                            Redv.text = String.format("%,.2f",RedWatts) +" W/m² "
                            val Redm = findViewById<TextView>(R.id.red_mol)
                            Redm.text = String.format("%,.2f",RedMol) + " μMole/m²"
                        }
                    }


                val lightChart = findViewById<CombinedChart>(R.id.light_chart)
                lightChart.description.isEnabled = false
                lightChart.setBackgroundColor(Color.WHITE)
                lightChart.setDrawGridBackground(false)
                lightChart.setDrawBarShadow(false)

                val left = lightChart.axisLeft;

                left.textSize = 12f
                left.axisMinimum = 0f
                left.axisMaximum = 10f

                if(v450 >= 10f || v500 >= 10f || v550 >= 10f || v570 >= 10f || v600 >= 10f || v650 >= 10f  ){
                    left.axisMaximum = 30f
                }
                if(v450 >= 30f || v500 >= 30f || v550 >= 30f || v570 >= 30f || v600 >= 30f || v650 >= 30f   ){
                    left.axisMaximum = 100f
                }
                if(v450 >= 100f || v500 >= 100f || v550 >= 100f || v570 >= 100f || v600 >= 100f || v650 >= 100f ){
                    left.axisMaximum = 700f
                }

                lightChart.axisRight.setEnabled(false)
                lightChart.setScaleEnabled(false)
                lightChart.setTouchEnabled(false)

                val description = Description()
                description.setText("")
                lightChart.description = description

                val ChlA = ArrayList<Entry>()
                ChlA.add(Entry(428.toFloat(), 1001.toFloat()))
                ChlA.add(Entry(453.toFloat(), 1001.toFloat()))
                ChlA.add(Entry(453.toFloat(), -1.toFloat()))
                ChlA.add(Entry(642.toFloat(), -1.toFloat()))
                ChlA.add(Entry(642.toFloat(), 1001.toFloat()))
                ChlA.add(Entry(661.toFloat(), 1001.toFloat()))
                val chlaArray: IntArray = intArrayOf(Color.argb(0,0,99,0),Color.argb(127,0,99,0))
                val chlgrad = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,chlaArray)
                val chlaset = LineDataSet(ChlA, "Chlorophyll")
                chlaset.setDrawFilled(true)
                chlaset.color = Color.argb(0,0,99,0)
                chlaset.fillDrawable = chlgrad

                val dataSet = ArrayList<ILineDataSet>()

                dataSet.add(chlaset)

                val Carot = ArrayList<Entry>()
                Carot.add(Entry(400.toFloat(), 1001.toFloat()))
                Carot.add(Entry(500.toFloat(), 1001.toFloat()))
                Carot.add(Entry(500.toFloat(), (-1).toFloat()))

                val carotArray: IntArray = intArrayOf(Color.argb(0,255,102,0),Color.argb(127,255,102,0))
                val carotgrad = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,carotArray)
                val carotset = LineDataSet(Carot, "Carotenoids")

                carotset.setDrawFilled(true)
                carotset.color = Color.argb(0,255,102,0)
                carotset.fillDrawable = carotgrad
                dataSet.add(carotset)

                val yAxes = ArrayList<Entry>().apply {
                    add(Entry(400F, -1F))
                    add(Entry(450F, v450))
                    add(Entry(500F, v500))
                    add(Entry(550F, v550))
                    add(Entry(570F, v570))
                    add(Entry(600F, v600))
                    add(Entry(650F, v650))
                    add(Entry(700F, -1F))
                }

                val barAxes = ArrayList<BarEntry>().apply {
                    add(BarEntry(400F, -1F))
                    add(BarEntry(450F, v450))
                    add(BarEntry(500F, v500))
                    add(BarEntry(550F, v550))
                    add(BarEntry(570F, v570))
                    add(BarEntry(600F, v600))
                    add(BarEntry(650F, v650))
                    add(BarEntry(700F, -1F))
                }

                val set1 = LineDataSet(yAxes, "Average")

                val set2 = BarDataSet(barAxes, "40nm FWHM")

                set2.color = Color.argb(97,155,155,155)

                set1.setDrawValues(false)


                set1.mode = LineDataSet.Mode.CUBIC_BEZIER
                set1.setDrawFilled(true)

                val rainArray: IntArray = intArrayOf(Color.MAGENTA, Color.argb(255,127,0,255), Color.BLUE, Color.GREEN, Color.YELLOW, Color.argb(255,255,170,0), Color.RED, Color.argb(255,170,0,0))
                val rainbow = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,rainArray)
                set1.fillDrawable = rainbow
                set1.color = Color.argb(255,0,0,0)

                val DO = arrayOf(CombinedChart.DrawOrder.LINE, CombinedChart.DrawOrder.BAR)
                lightChart.drawOrder = DO

                val l = lightChart.legend
                l.isWordWrapEnabled = true
                l.verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM

                val cdata = CombinedData()

                val ldata = LineData()
                ldata.addDataSet(set1)

                val bdata = BarData()
                bdata.barWidth = 40f
                bdata.addDataSet(set2)


                cdata.setData(ldata)
                cdata.setData(bdata)

                lightChart.data = cdata
                lightChart.invalidate()

                progbar.visibility = View.INVISIBLE

            }catch(e: NullPointerException){
                //println(e)
            }
        }
    }

    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String?

        val unknownCharaString = resources.getString(R.string.unknown_characteristic)

        val progbar = findViewById<ProgressBar>(R.id.progressBar)
        progbar.visibility = View.VISIBLE

        // Loops through available GATT Services.
        for (gattService in gattServices) {

            val gattCharacteristics = gattService.characteristics
            //println(gattCharacteristics)

            val gattCharacteristicGroupData = ArrayList<HashMap<String, String>>()
            mGattCharacteristics = ArrayList()
            val charas = ArrayList<BluetoothGattCharacteristic>()
            for (gattCharacteristic in gattCharacteristics) {

                uuid = gattCharacteristic.uuid.toString()//println(uuid)

                //println(uuid)
                // If there is an active notification on a characteristic, clear
                // it first so it doesn't update the data field on the user interface.

                when (uuid) {

                    SampleGattAttributes.LUCI_LOW -> {

                        charas.add(gattCharacteristic)

                        val currentCharaData = HashMap<String, String>()
                        currentCharaData.put(
                                LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString))
                        currentCharaData.put(LIST_UUID, uuid)
                        gattCharacteristicGroupData.add(currentCharaData)
                        mGattCharacteristics!!.add(charas)
                    }

                    else -> {
                        //println(uuid)
                        //println("Not Luci")
                        // ignore arduino UUIDs too
                    }
                }

            }


            fixedRateTimer = fixedRateTimer(name = "high-timer",
                    initialDelay = 100, period = 100, daemon = true) {

                val characteristic = mGattCharacteristics!![0][0]
                val charaProp = characteristic.properties

                if (charaProp or BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                    // If there is an active notification on a characteristic, clear
                    // it first so it doesn't update the data field on the user interface.
                    if (mNotifyCharacteristic != null) {
                        mBluetoothLeService!!.setCharacteristicNotification(
                                mNotifyCharacteristic!!, false)
                        mNotifyCharacteristic = null
                    }
                    if (scanning) {
                        mBluetoothLeService?.readCharacteristic(characteristic)
                    }

                }
            }

        }

    }

    companion object {
        private val TAG = DeviceControlActivity::class.java.simpleName
       var EXTRAS_DEVICE_NAME = "DEVICE_NAME"
       var EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"

        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            return intentFilter
        }
    }

}