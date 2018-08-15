package com.objectsyndicate.luciscan

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import com.facebook.login.LoginResult
import com.facebook.login.LoginManager
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.text.TextUtils
import com.facebook.*
import android.view.View
import android.widget.Button
import android.widget.EditText
import com.facebook.AccessToken
import com.facebook.AccessTokenTracker
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import android.widget.Toast

class LoginActivity: Activity() {
    private val callbackManager = CallbackManager.Factory.create()
    private var User: String = ""
    private var Pass: String = ""
    var UserToken = ""
    var LuciListJSON = ""
    var userTokenPref: SharedPreferences? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userTokenPref = this.getSharedPreferences(getString(R.string.preference_file_key), 0)

        setContentView(R.layout.login)
        val skip: Button = findViewById(R.id.skipAuth)
        val login: Button = findViewById(R.id.Login)
        val signup: Button = findViewById(R.id.Signup)

        val user: EditText = findViewById(R.id.Username)
        val pass: EditText = findViewById(R.id.Password)

        skip.setOnClickListener({
            skipAuth()
        })

        login.setOnClickListener({
            userAuth()
        })

/*
        if (BuildConfig.DEBUG) {
            FacebookSdk.setIsDebugEnabled(true)
            FacebookSdk.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS)
        }
*/
        if(Profile.getCurrentProfile() != null && AccessToken.getCurrentAccessToken() != null){
            skip.text = "Continue"
            login.visibility = View.INVISIBLE
            user.visibility = View.INVISIBLE
            pass.visibility = View.INVISIBLE
            signup.visibility = View.INVISIBLE
            skipAuth()
        }
        LoginManager.getInstance().registerCallback(callbackManager,
                object : FacebookCallback<LoginResult> {
                    override fun onSuccess(loginResult: LoginResult) {
                        fbAuth()
                        skip.text = "Continue"
                        login.visibility = View.INVISIBLE
                        user.visibility = View.INVISIBLE
                        pass.visibility = View.INVISIBLE
                        signup.visibility = View.INVISIBLE
                    }

                    override fun onCancel() {
                        // App code
                        skip.text = "Skip Auth"
                        login.visibility = View.VISIBLE
                        user.visibility = View.VISIBLE
                        pass.visibility = View.VISIBLE
                        signup.visibility = View.VISIBLE
                    }

                    override fun onError(exception: FacebookException) {
                        // App code
                        skip.visibility = View.VISIBLE
                        login.visibility = View.VISIBLE
                        user.visibility = View.VISIBLE
                        pass.visibility = View.VISIBLE
                        signup.visibility = View.VISIBLE
                    }
                })

        val accessTokenTracker: AccessTokenTracker = object : AccessTokenTracker() {
            override fun onCurrentAccessTokenChanged(accessToken: AccessToken?, accessToken2: AccessToken?) {
                if (accessToken == null) {
                    // Log in Logic
                    skip.text = "Continue"

                    login.visibility = View.INVISIBLE
                    user.visibility = View.INVISIBLE
                    pass.visibility = View.INVISIBLE
                    signup.visibility = View.INVISIBLE
                } else if (accessToken2 == null) {
                    // Log out logic
                    skip.text = "Skip Auth"
                    login.visibility = View.VISIBLE
                    user.visibility = View.VISIBLE
                    pass.visibility = View.VISIBLE
                    signup.visibility = View.VISIBLE
                }
            }
        }
     }

    override fun onResume() {
        super.onResume()
        val skip: Button = findViewById(R.id.skipAuth)
        val login: Button = findViewById(R.id.Login)
        val signup: Button = findViewById(R.id.Signup)

        val user: EditText = findViewById(R.id.Username)
        val pass: EditText = findViewById(R.id.Password)

    }

    fun skipAuth() {
        val intent = Intent(this, DeviceScanActivity::class.java)
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    fun userAuth() {
        // intent spawns BLE Scan screen
        val intent = Intent(this, DeviceScanActivity::class.java)
        // URL is used for asynctask
        val urlString = "https://api.objectsyndicate.com/api/obtain_token/"
        val urlListString = "https://api.objectsyndicate.com/api/v1/luci/list/"
        val urlAddString = "https://api.objectsyndicate.com/api/v1/luci/add/"

        // are the fields populated?
        try {
            val username =  findViewById(R.id.Username) as EditText
            User = username.text.toString()
            if (TextUtils.isEmpty(User)) {
                username.error = "Enter Username"
                return
            }

            val password =  findViewById(R.id.Password) as EditText
            Pass = password.text.toString()
            if (TextUtils.isEmpty(Pass)) {
                password.error = "Enter Password"
                return
            }

        } catch (e: Exception) {
            println(e)
        }

        // first async task gets USERTOKEN.
        val sendURL = URL(urlString)
        val tokentask = ObtainTokenTask().execute(sendURL).get()

        // Was auth OK?
        if(tokentask == 200){
            // If auth was OK, do you have any luciscans?

            val listURL = URL(urlListString)
            val listtask = ListLuciTask().execute(listURL).get()
            if(listtask == 200){
                val obj = JSONObject(LuciListJSON)

// Do you have any lucis?
                if (obj.length() == 0){
                    val lucilist = ArrayList<String>()
                    val addURL = URL(urlAddString)
                    val addtask = AddLuciTask().execute(addURL).get()

                    if (addtask == 201){


                        val listURL2 = URL(urlListString)
                        val listtask2 = ListLuciTask().execute(listURL2).get()
                        if(listtask2 == 200){
                            val obj2 = JSONObject(LuciListJSON)
                            for (uuid in obj2.keys()){
                                lucilist.add(obj2[uuid].toString())
                            }
                        }


                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Pick device storage")
                        builder.setItems(lucilist.toTypedArray(), { dialog, which ->
                            val editor = userTokenPref!!.edit()
                            editor.putString("LuciToken", lucilist[which])
                            editor.apply()

                            startActivity(intent)
                        })
                        builder.show()
                    }
                }
                else{
                    val lucilist = ArrayList<String>()
                    for (uuid in obj.keys()){
                        lucilist.add(obj[uuid].toString())
                    }

                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Pick device storage")
                    builder.setItems(lucilist.toTypedArray(), { dialog, which ->
                        // the user clicked on colors[which]
                        val editor = userTokenPref!!.edit()
                        editor.putString("LuciToken", lucilist[which])
                        editor.apply()

                        startActivity(intent)
                    })
                    builder.show()

                }

            }

        }else if(tokentask == 400) {
            Toast.makeText(this, "Authentication Failed", Toast.LENGTH_LONG).show()
        }
    }

    private inner class ObtainTokenTask : AsyncTask<URL, Int, Int>() {
        override fun doInBackground(vararg urls: URL): Int? {
            var responseCode = 0
            try {
                val url = urls[0]
                val conn = url.openConnection() as HttpsURLConnection
                conn.doOutput = true
                conn.doInput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.requestMethod = "POST"
                conn.connectTimeout = 1 * 60 * 1000

                val jsonRequest = "{\"username\":\"$User\", \"password\": \"$Pass\"}"

                val out = DataOutputStream(conn.outputStream)
                out.writeBytes(jsonRequest)
                conn.connect()
                responseCode = conn.responseCode
                if (conn.responseCode == 200) {
                    val input = conn.inputStream
                    val reader = BufferedReader(InputStreamReader(input))
                    val line: String = reader.readLine()

                    val obj = JSONObject(line)
                    try {
                        UserToken = obj["token"].toString()

                        val editor = userTokenPref!!.edit()
                        editor.putString("UserToken", UserToken)
                        editor.apply()
                    }
                    catch(e: Exception){
                        //println("bad")
                    }

                } else {
                    return conn.responseCode
                }
                out.flush()
                out.close()
            } catch (e: Exception) {
                //println(e)
            }
            return responseCode
        }
    }

    private inner class AddLuciTask : AsyncTask<URL, Int, Int>() {
        override fun doInBackground(vararg urls: URL): Int? {
            var responseCode = 0
            try {
                val url = urls[0]
                val conn = url.openConnection() as HttpsURLConnection
                conn.doOutput = true
                conn.doInput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Token " + UserToken)
                conn.requestMethod = "POST"
                conn.connectTimeout = 1 * 60 * 1000

                val jsonRequest = "{\"add\":\"TRUE\"}"

                val out = DataOutputStream(conn.outputStream)
                out.writeBytes(jsonRequest)
                conn.connect()
                responseCode = conn.responseCode
                if (conn.responseCode == 200) {
                    val input = conn.inputStream
                    val reader = BufferedReader(InputStreamReader(input))
                    val line: String = reader.readLine()
                    val obj = JSONObject(line)

                } else {
                    return conn.responseCode
                }
                out.flush()
                out.close()
            } catch (e: Exception) {
                //println(e)
            }
            return responseCode
        }
    }

    private inner class ListLuciTask : AsyncTask<URL, Int, Int>() {
        override fun doInBackground(vararg urls: URL): Int? {
            var responseCode = 0
            try {
                val url = urls[0]
                val conn = url.openConnection() as HttpsURLConnection
                conn.doOutput = true
                conn.doInput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Token " + UserToken)
                conn.requestMethod = "POST"
                conn.connectTimeout = 1 * 60 * 1000

                val jsonRequest = "{\"list_tokens\": \"TRUE\"}"

                val out = DataOutputStream(conn.outputStream)
                out.writeBytes(jsonRequest)
                conn.connect()
                responseCode = conn.responseCode
                if (conn.responseCode == 200) {
                    val input = conn.inputStream
                    val reader = BufferedReader(InputStreamReader(input))
                    val line: String = reader.readLine()

                    LuciListJSON = line

                    //UserToken = obj["token"].toString()

                    //println(UserToken)

                    //val editor = userTokenPref!!.edit()
                    //editor.putString("UserToken", UserToken)
                    //editor.commit()

                } else {
                    //println( conn.responseCode)
                }
                out.flush()
                out.close()
            } catch (e: Exception) {
                //println(e)
            }
            return responseCode
        }
    }

    fun fbAuth() {
        val urlString = "https://api.objectsyndicate.com/api/fbtoken/"
        val urlListString = "https://api.objectsyndicate.com/api/v1/luci/list/"
        val urlAddString = "https://api.objectsyndicate.com/api/v1/luci/add/"
        val intent = Intent(this, DeviceScanActivity::class.java)

        val sendURL = URL(urlString)
        val task = FacebookTokenTask().execute(sendURL).get()
        if(task == 200){
            // If auth was OK, do you have any luciscans?

            val listURL = URL(urlListString)
            val listtask = ListLuciTask().execute(listURL).get()
            if(listtask == 200){
                val obj = JSONObject(LuciListJSON)

// Do you have any lucis?
                if (obj.length() == 0){
                    val lucilist = ArrayList<String>()
                    val addURL = URL(urlAddString)
                    val addtask = AddLuciTask().execute(addURL).get()

                    if (addtask == 201){
                        val listURL2 = URL(urlListString)
                        val listtask2 = ListLuciTask().execute(listURL2).get()
                        if(listtask2 == 200){
                            val obj2 = JSONObject(LuciListJSON)
                            for (uuid in obj2.keys()){
                                lucilist.add(obj2[uuid].toString())
                            }
                        }


                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Pick device storage")
                        builder.setItems(lucilist.toTypedArray(), { dialog, which ->
                            val editor = userTokenPref!!.edit()
                            editor.putString("LuciToken", lucilist[which])
                            editor.apply()
                            startActivity(intent)
                        })
                        builder.show()
                    }
                }
                else{
                    val lucilist = ArrayList<String>()
                    for (uuid in obj.keys()){
                        lucilist.add(obj[uuid].toString())
                    }

                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Pick device storage")
                    builder.setItems(lucilist.toTypedArray(), { dialog, which ->
                        // the user clicked on colors[which]
                        val editor = userTokenPref!!.edit()
                        editor.putString("LuciToken", lucilist[which])
                        editor.apply()
                        startActivity(intent)
                    })
                    builder.show()
                }
            }

        }else if(task == 400) {
            Toast.makeText(this, "Authentication Failed", Toast.LENGTH_LONG).show()
        }

    }

    private inner class FacebookTokenTask : AsyncTask<URL, Int, Int>() {
        override fun doInBackground(vararg urls: URL): Int? {
            var responseCode = 0
            try {
                val url = urls[0]
                val conn = url.openConnection() as HttpsURLConnection
                conn.doOutput = true
                conn.doInput = true
                conn.setRequestProperty("Content-Type", "application/json")
                conn.requestMethod = "POST"
                conn.connectTimeout = 1 * 60 * 1000

                val FBToken = AccessToken.getCurrentAccessToken().token
                val jsonRequest = "{\"token\":\"$FBToken\"}"

                val out = DataOutputStream(conn.outputStream)
                out.writeBytes(jsonRequest)
                conn.connect()

                responseCode = conn.responseCode
                if (conn.responseCode == 200) {
                    val input = conn.inputStream
                    val reader = BufferedReader(InputStreamReader(input))
                    val line: String = reader.readLine()

                    val obj = JSONObject(line)
                    UserToken = obj["token"].toString()

                    val editor = userTokenPref!!.edit()
                    editor.putString("UserToken", UserToken)
                    editor.apply()

                } else {
                    return conn.responseCode
                }
                out.flush()
                out.close()
            } catch (e: Exception) {
                println(e)
            }
            return responseCode
        }

    }

}