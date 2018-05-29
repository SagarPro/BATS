package sagsaguz.enquirytracking

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BaseTransientBottomBar
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.widget.CardView
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import com.amazonaws.AmazonClientException
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.ScanRequest
import com.amazonaws.services.dynamodbv2.model.ScanResult
import com.google.gson.Gson
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.HtmlEmail
import sagsaguz.enquirytracking.utils.AWSProvider
import sagsaguz.enquirytracking.utils.CenterDO
import sagsaguz.enquirytracking.utils.Config

class LoginActivity : AppCompatActivity() {

    lateinit var rlLogin : RelativeLayout

    lateinit var etEmailAddress : EditText
    lateinit var etPassword : EditText
    lateinit var tvForgotPassword : TextView
    lateinit var cvLogin : CardView

    lateinit var pbLogin : ProgressBar

    lateinit var adminPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_layout)

        rlLogin = findViewById(R.id.rlLogin)

        pbLogin = findViewById(R.id.pbLogin)
        pbLogin.indeterminateDrawable.setColorFilter(resources.getColor(R.color.colorEmerald), android.graphics.PorterDuff.Mode.MULTIPLY)
        pbLogin.visibility = View.GONE

        adminPreferences = getSharedPreferences("AdminDetails", Context.MODE_PRIVATE)

        val loginStatus = adminPreferences.getString("Login", "logout")

        if (loginStatus == "login"){
            startActivity(Intent(baseContext, MainActivity::class.java))
            finish()
        }

        etEmailAddress = findViewById(R.id.etEmailAddress)
        etPassword = findViewById(R.id.etPassword)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvForgotPassword.setOnClickListener {
            if (TextUtils.isEmpty(etEmailAddress.text.toString())){
                basicSnackBar("Enter your registered email address")
            } else {
                PasswordRecovery().execute()
            }
        }
        cvLogin = findViewById(R.id.cvLogin)
        cvLogin.setOnClickListener { validate() }

    }

    private fun validate(){
        if(TextUtils.isEmpty(etEmailAddress.text.toString()) || TextUtils.isEmpty(etPassword.text.toString())){
            basicSnackBar("Enter your registered email and password.")
        } else {
            CenterDetails().execute()
        }
    }

    fun basicSnackBar(message: String) {
        val snackbar = Snackbar.make(rlLogin, message, Snackbar.LENGTH_SHORT)
        val sbView = snackbar.view
        sbView.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.colorPrimary))
        val textView = sbView.findViewById<TextView>(android.support.design.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(baseContext, R.color.colorWhite))
        snackbar.show()
    }

    fun showSnackBar(message: String) {
        val snackbar = Snackbar.make(rlLogin, message, Snackbar.LENGTH_SHORT)
                .setAction("Try Again") {  }
        snackbar.setActionTextColor(ContextCompat.getColor(baseContext, R.color.colorAccent))

        val sbView = snackbar.view
        sbView.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.colorPrimary))
        val textView = sbView.findViewById<TextView>(android.support.design.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(baseContext, R.color.colorWhite))
        snackbar.duration = BaseTransientBottomBar.LENGTH_INDEFINITE
        snackbar.show()
    }



    @SuppressLint("StaticFieldLeak")
    inner class CenterDetails : AsyncTask<Void, Void, String>() {

        val centerDo = CenterDO()

        override fun onPreExecute() {
            pbLogin.visibility = View.VISIBLE
        }

        override fun doInBackground(vararg p0: Void?): String {

            val awsProvider = AWSProvider()
            val dynamoDBClient = AmazonDynamoDBClient(awsProvider.getCredentialsProvider(baseContext))
            dynamoDBClient.setRegion(Region.getRegion(Regions.US_EAST_1))

            try {
                var result: ScanResult? = null
                do {
                    val req = ScanRequest()
                    req.tableName = Config.CENTERTABLE
                    if (result != null) {
                        req.exclusiveStartKey = result.lastEvaluatedKey
                    }
                    result = dynamoDBClient.scan(req)
                    val rows = result.items
                    for (map in rows) {

                        val emailOrPhone = etEmailAddress.text.toString()

                        if (emailOrPhone == map["phone"]!!.s || emailOrPhone == map["emailId"]!!.s){
                            if (etPassword.text.toString() == map["password"]!!.s){

                                if (map["accessType"]!!.s == "Access") {

                                    centerDo.phone = map["phone"]!!.s
                                    centerDo.emailId = map["emailId"]!!.s
                                    centerDo.name = map["name"]!!.s
                                    centerDo.location = map["location"]!!.s
                                    centerDo.password = map["password"]!!.s
                                    centerDo.directions = map["directions"]!!.s
                                    centerDo.accessType = map["accessType"]!!.s

                                    return "success"
                                } else {
                                    return "restricted"
                                }

                            }
                        }
                    }
                } while (result!!.lastEvaluatedKey != null)

                return "unsuccess"
            } catch (e : AmazonClientException){
                showSnackBar("Network connection error!!")
                return "exception"
            }
        }

        override fun onPostExecute(result: String?) {
            pbLogin.visibility = View.GONE
            if (result == "success"){
                val prefsEditor = adminPreferences.edit()
                val gson = Gson()
                val json = gson.toJson(centerDo)
                prefsEditor.putString("Center", json)
                prefsEditor.putString("Login", "login")
                prefsEditor.apply()
                startActivity(Intent(baseContext, MainActivity::class.java))
                finish()
            } else if (result == "unsuccess"){
                basicSnackBar("Please check your login details and try again.")
            } else if (result == "restricted") {
                basicSnackBar("Your account has been restricted. Please contact Head Office for more details.")
            }
        }

    }



    @SuppressLint("StaticFieldLeak")
    inner class PasswordRecovery : AsyncTask<Void, Void, Boolean>() {

        override fun onPreExecute() {
            pbLogin.visibility = View.VISIBLE
        }

        override fun doInBackground(vararg p0: Void?): Boolean {

            val awsProvider = AWSProvider()
            val dynamoDBClient = AmazonDynamoDBClient(awsProvider.getCredentialsProvider(baseContext))
            dynamoDBClient.setRegion(Region.getRegion(Regions.US_EAST_1))

            try {
                var result: ScanResult? = null
                do {
                    val req = ScanRequest()
                    req.tableName = Config.CENTERTABLE
                    if (result != null) {
                        req.exclusiveStartKey = result.lastEvaluatedKey
                    }
                    result = dynamoDBClient.scan(req)
                    val rows = result.items
                    for (map in rows) {

                        val emailOrPhone = etEmailAddress.text.toString()

                        if (emailOrPhone == map["phone"]!!.s || emailOrPhone == map["emailId"]!!.s){
                            sendEmail(map["emailId"]!!.s, map["password"]!!.s)
                            return true
                        }
                    }
                } while (result!!.lastEvaluatedKey != null)

                return false
            } catch (e : AmazonClientException){
                showSnackBar("Network connection error!!")
                return false
            }
        }

        override fun onPostExecute(result: Boolean?) {
            pbLogin.visibility = View.GONE
            if (result!!){
                basicSnackBar("Password has been sent to your email")
            } else {
                basicSnackBar("Enter your registered email address and try again")
            }
        }

    }

    fun sendEmail(toEmail: String, pass: String) {
        val email = HtmlEmail()
        email.hostName = "smtp.googlemail.com"
        email.setSmtpPort(465)
        email.setAuthenticator(DefaultAuthenticator(Config.FROMEMAIL, Config.FROMPASSWORD))
        email.isSSLOnConnect = true
        email.setFrom(Config.FROMEMAIL)
        email.addTo(toEmail)
        email.subject = "Password recovery from BATS"
        email.setTextMsg("Your password is "+ pass)
        //val kotlinLogoURL = URL("https://kotlinlang.org/assets/images/twitter-card/kotlin_800x320.png")
        //val cid = email.embed(kotlinLogoURL, "Kotlin logo")
        //email.setHtmlMsg("<html><h1>Kotlin logo</h1><img src=\"cid:$cid\"></html>")
        email.send()
    }


}
