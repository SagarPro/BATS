package sagsaguz.enquirytracking

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.Dialog
import android.app.ProgressDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.support.design.widget.BaseTransientBottomBar
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.telephony.PhoneNumberUtils
import android.telephony.SmsManager
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.amazonaws.AmazonClientException
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.google.gson.Gson
import kotlinx.android.synthetic.main.new_enquiry_layout.*
import sagsaguz.enquirytracking.MainActivity.Companion.mainActivity
import sagsaguz.enquirytracking.utils.AWSProvider
import sagsaguz.enquirytracking.utils.CenterDO
import sagsaguz.enquirytracking.utils.CustomerDetailsDO
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class CustomerActivitiesActivity : AppCompatActivity() {

    lateinit var userName : TextView
    lateinit var userPhone : TextView
    lateinit var llRating : LinearLayout

    lateinit var rlCustomerActivities : RelativeLayout

    lateinit var lvActivities : ListView

    lateinit var llOptions : LinearLayout
    lateinit var ibSMS : ImageButton
    lateinit var btnUpdate : Button
    lateinit var ibCall : ImageButton

    var dateList = ArrayList<String>()
    var fActivities = HashMap<String, String>()

    lateinit var dialog: Dialog

    lateinit var user : ActivitiesAdapter
    lateinit var customerDetails: CustomerDetailsDO

    lateinit var adminPreferences: SharedPreferences
    lateinit var center: CenterDO

    var updateStatus = "noUpdate"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.customer_activities_layout)

        title = "Admission Tracking"

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val intent = intent
        customerDetails = intent.getSerializableExtra("customer") as CustomerDetailsDO

        adminPreferences = getSharedPreferences("AdminDetails", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = adminPreferences.getString("Center", "")
        center = gson.fromJson<CenterDO>(json, CenterDO::class.java)

        dialog = Dialog(this)

        rlCustomerActivities = findViewById(R.id.rlCustomerActivities)

        userName = findViewById(R.id.user_name)
        userName.text = customerDetails.parentName
        userPhone = findViewById(R.id.user_phone)
        userPhone.text = customerDetails.phone
        llRating = findViewById(R.id.llRating)
        val ratingPoint = customerDetails.rating!!.toInt()
        for (i in 0 until ratingPoint)
        {
            val imageView = ImageView(baseContext)
            imageView.setImageResource(R.drawable.icon_rating)
            imageView.layoutParams = LinearLayout.LayoutParams(40, 40)
            llRating.addView(imageView)
        }

        lvActivities = findViewById(R.id.lvActivities)

        user = ActivitiesAdapter(baseContext, dateList, fActivities)
        lvActivities.adapter = user

        customerActivities()

        llOptions = findViewById(R.id.llOptions)
        if (customerDetails.status != "open"){
            llOptions.visibility = View.GONE
        }
        ibSMS = findViewById(R.id.ibSMS)
        ibSMS.setOnClickListener {
            smsOrVideo()
        }
        btnUpdate = findViewById(R.id.btnUpdate)
        btnUpdate.setOnClickListener { updateNFD() }
        ibCall = findViewById(R.id.ibCall)
        ibCall.setOnClickListener { telephonePermissionCheck(customerDetails.phone.toString()) }

    }

    fun basicSnackBar(message: String) {
        val snackbar = Snackbar.make(rlCustomerActivities, message, Snackbar.LENGTH_SHORT)
        val sbView = snackbar.view
        sbView.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.colorPrimary))
        val textView = sbView.findViewById<TextView>(android.support.design.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(baseContext, R.color.colorWhite))
        snackbar.show()
    }

    private fun customerActivities(){

        dateList.clear()
        fActivities.clear()

        val nfdVal = customerDetails.nfd
        val nfdKeys = nfdVal!!.keys
        for (key1 in nfdKeys) {
            val key = key1
            dateList.add(key)
            fActivities.put(key, nfdVal[key].toString())
        }

        Collections.sort(dateList, CustomerDateComparator())
        //Collections.reverse(dateList)

        user.notifyDataSetChanged()

        lvActivities.smoothScrollToPosition(dateList.size-1)
    }

    inner class CustomerDateComparator : Comparator<String> {

        override fun compare(emp1: String, emp2: String): Int {
            return emp1.compareTo(emp2)
        }
    }

    private fun smsOrVideo(){
        dialog.setContentView(R.layout.conformation_dialog)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)
        val tvCSMS = dialog.findViewById<TextView>(R.id.tvCSMS)
        tvCSMS.text = "What type of message you want to send?."
        val tvYes = dialog.findViewById<TextView>(R.id.tvYes)
        tvYes.text = "SMS"
        tvYes.setOnClickListener {
            smsPermissionCheck(customerDetails.phone.toString())
        }
        val tvNo = dialog.findViewById<TextView>(R.id.tvNo)
        tvNo.text = "Video"
        tvNo.visibility = View.GONE
        tvNo.setOnClickListener {
            videoTemplates(customerDetails.phone.toString())
        }
        dialog.show()
    }

    private fun showTemplates(phoneNumber: String){
        dialog.setContentView(R.layout.sms_template)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        val etCustomSMS = dialog.findViewById<EditText>(R.id.etCustomSMS)
        val ibSendSMS = dialog.findViewById<ImageButton>(R.id.ibSendSMS)
        ibSendSMS.setOnClickListener {
            if (TextUtils.isEmpty(etCustomSMS.text.toString())){
                Toast.makeText(baseContext, "Please enter your message", Toast.LENGTH_SHORT).show()
            } else {
                sendSMS(phoneNumber, etCustomSMS.text.toString())
                dialog.dismiss()
            }
        }

        val cvDirection = dialog.findViewById<android.support.v7.widget.CardView>(R.id.cvDirection)
        cvDirection.setOnClickListener {
            sendSMS(phoneNumber, "Hi "+customerDetails.parentName+", here is "+center.name+"'s Location: "+center.directions+". See you soon!")
            dialog.dismiss()
        }

        val lvSMSTemplate = dialog.findViewById<ListView>(R.id.lvSMSTemplate)

        val tvCancel = dialog.findViewById<TextView>(R.id.tvCancel)
        tvCancel.setOnClickListener { dialog.dismiss() }

        val templateNames = ArrayList<String>()
        templateNames.add("Thanks For Your Interest")
        templateNames.add("Montessori Demo Class Invitation")
        templateNames.add("Called & No Response SMS")
        templateNames.add("Book Demo Activity Class - 1")
        templateNames.add("Book Demo Activity Class - 2")
        templateNames.add("Book Demo Activity Class - 3")
        templateNames.add("Benefits Of Montessori Language Activity")
        templateNames.add("Thank you for visiting - 1")
        templateNames.add("Thank you & Admission Offers")
        templateNames.add("Marketing 1: Enrollment & Admission Offers")
        templateNames.add("Marketing 2: Demo Invite")
        templateNames.add("Admission Done")
        templateNames.add("Marketing 3: Ask for visit")
        templateNames.add("Thank you for visiting - 1")
        templateNames.add("Admission Offer Reminder & Expiry")
        templateNames.add("Admission Offer Clarification")
        templateNames.add("Marketing Admission Assistance")
        templateNames.add("Marketing 4: Push For Admission")
        templateNames.add("Marketing 5: Visited & Not Admitted Yet")
        templateNames.add("BMTT - 1")
        templateNames.add("BMTT - 2")
        templateNames.add("BMTT Admission Done")
        templateNames.add("BMTT Enquiry Response")

        val templateList = ArrayList<String>()
        templateList.add("Hi "+customerDetails.parentName+ ", thank you for your interest in "+center.name+" for "+customerDetails.childName+". Now experience the joy of learning.")
        templateList.add("Hi "+customerDetails.parentName+ ", introduce your child, "+customerDetails.childName+" to world of Montessori. Take a Montessori demo class at "+center.name+". See the difference, discover your child's potential.")
        templateList.add("Hi "+customerDetails.parentName+ ", called you for discussing "+customerDetails.childName+"'s admission at "+center.name+". Please feel free to contact me at your convenience. Thanks.")
        templateList.add("Hi "+customerDetails.parentName+ ", seeing is believing. See "+customerDetails.childName+" doing Montessori activity in the demo class at "+center.name+". Call to book the demo activity class for your child")
        templateList.add("Hi "+customerDetails.parentName+ ", let "+customerDetails.childName+" touch, feel, explore Montessori activities in our demo activity class at "+center.name+". Call to book demo activity date. Thanks.")
        templateList.add("Hi "+customerDetails.parentName+ ", Montessori activities nurture the brain development of children. Call to book the Montessori demo activity date for "+customerDetails.childName+" at "+center.name+". Thanks.")
        templateList.add("Hi "+customerDetails.parentName+ ", develop "+customerDetails.childName+"'s communication skills through Montessori Language & Phonics activities. Let "+customerDetails.childName+" join the demo activity class at "+center.name+". Call to know more.")
        templateList.add("Hi "+customerDetails.parentName+ ", thank you for visiting our "+center.name+". We look forward to seeing "+customerDetails.childName+" in our "+center.name+" soon. Have a nice day!")
        templateList.add("Hi "+customerDetails.parentName+ ", thank you for your enquiry once again. Let "+customerDetails.childName+" be part of "+center.name+" family. Contact us to know our admission offers.")
        templateList.add("Hi "+customerDetails.parentName+ ", see the No.1! The brightest kids are at "+center.name+". For enrollment & admission offers, please feel free to call us.")
        templateList.add("Hi "+customerDetails.parentName+ ", it would be our pleasure to organise a demo class for "+customerDetails.childName+" at "+center.name+". Kindly call to schedule the demo. Thanks.")
        templateList.add("Hi "+customerDetails.parentName+ ", congratulations!! "+center.name+" family welcomes "+customerDetails.childName+" to "+center.name+". We are sure "+customerDetails.childName+" will enjoy spending time at "+center.name+".")
        templateList.add("Hi "+customerDetails.parentName+ ", have a Bright Start, Right Start for "+customerDetails.childName+" at "+center.name+". Visit our centre and discover our world class facility & curriculum. Thanks.")
        templateList.add("Hi "+customerDetails.parentName+ ", we thank you for visiting "+center.name+" - sunshine of learning, fun & care. For any further clarification on "+customerDetails.childName+"'s admission, please call us.")
        templateList.add("Hi "+customerDetails.parentName+ ", our best admission offer to you will expire soon. We request you avail the offer made to you at the earliest. Hope you will do the admission for "+customerDetails.childName+" soon at "+center.name+". Thanks.")
        templateList.add("Hi "+customerDetails.parentName+ ", pls feel free to call to check with us if you need any clarification in our offer given to you for "+customerDetails.childName+" admission. We are here to help you.")
        templateList.add("Hi "+customerDetails.parentName+ ", hope you have found our early childhood learning approach well suited to best prepare "+customerDetails.childName+" for the future. Please call this number "+center.phone+" for admission assistance.")
        templateList.add("Hi "+customerDetails.parentName+ ", please take admission for "+customerDetails.childName+" at the earliest before the batches get filled at "+center.name+". For admission related query, call "+center.phone+". Thanks.")
        templateList.add("Hi "+customerDetails.parentName+ ", hope you liked your visit to "+center.name+", having the best preschool methodology in your area. For admission for "+customerDetails.childName+", Kindly call us at "+center.phone)
        templateList.add("Hi "+customerDetails.parentName+ ", thank you for the telecon we had with you. Your enquiry has been assigned to our "+center.name+" center, "+center.phone)
        templateList.add("Hi "+customerDetails.parentName+ ", there is increased demand for Montessori trained professionals. Our BMTT program will make you a well trained Montessori Professional.")
        templateList.add("Hi "+customerDetails.parentName+ ", congratulations on enrolling for Bright Montessori Teachers Training program. Please login at  http://brightcourse.in. Thanks.")
        templateList.add("Hi "+customerDetails.parentName+ ", thank you for your interest in BMTT program. Our BMTT coordinator, will contact you soon to explain more. ")

        val smsTemplatesAdapter = SMSTemplateAdapter(this@CustomerActivitiesActivity,templateNames, templateList)
        lvSMSTemplate.adapter = smsTemplatesAdapter

        lvSMSTemplate.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            dialog.setContentView(R.layout.sms_confirmation_dialog)
            dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.setCancelable(false)
            val tvTitle = dialog.findViewById<TextView>(R.id.tvMessage)
            tvTitle.text = "Are you sure, you want to send this message?"
            val tvCSMS = dialog.findViewById<TextView>(R.id.tvCSMS)
            tvCSMS.text = templateList[position]
            val tvYes = dialog.findViewById<TextView>(R.id.tvYes)
            tvYes.setOnClickListener {
                sendSMS(phoneNumber, templateList[position])
                dialog.dismiss()
            }
            val tvNo = dialog.findViewById<TextView>(R.id.tvNo)
            tvNo.setOnClickListener { dialog.dismiss() }
        }
        dialog.show()
    }

    private fun smsPermissionCheck(phoneNumber: String){

        val permissionCheck = ContextCompat.checkSelfPermission(this@CustomerActivitiesActivity, android.Manifest.permission.SEND_SMS)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            showTemplates(phoneNumber)
        } else {

            val dialogBuilder = AlertDialog.Builder(this@CustomerActivitiesActivity)
            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.permission_dialog, null)
            dialogBuilder.setView(dialogView)

            val b = dialogBuilder.create()

            val dialog_message = dialogView.findViewById<View>(R.id.dialog_message) as TextView
            dialog_message.text = "This app needs SMS permission for sending sms to customers."
            val pCancel = dialogView.findViewById<View>(R.id.pCancel) as TextView
            val pSettings = dialogView.findViewById<View>(R.id.pSettings) as TextView
            val pOk = dialogView.findViewById<View>(R.id.pOk) as TextView
            pCancel.setOnClickListener {
                b.dismiss()
            }
            pSettings.setOnClickListener {
                b.dismiss()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            pOk.setOnClickListener {
                b.dismiss()
                ActivityCompat.requestPermissions(this@CustomerActivitiesActivity, arrayOf(android.Manifest.permission.SEND_SMS), 3)
            }

            b.show()

        }
    }

    private fun sendSMS(phoneNumber: String, message: String){
        SmsManager.getDefault().sendTextMessage(phoneNumber, null, message, null, null)
        basicSnackBar("SMS sent successfully to "+customerDetails.parentName)
    }

    private fun videoTemplates(phoneNumber: String){

        dialog.setContentView(R.layout.videos_template)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        val spLanguage = dialog.findViewById<Spinner>(R.id.spLanguage)
        val languageItem = ArrayList<String>()
        languageItem.add("English")
        languageItem.add("Spanish")
        languageItem.add("Kannada")
        languageItem.add("Japanese")
        val statusAdapter = SpinnerAdapter(this, R.layout.status_item, languageItem)
        spLanguage.adapter = statusAdapter

        val videoList = ArrayList<String>()

        val videoMap = HashMap<String, ArrayList<String>>()
        videoMap.put("English", videoList)

        spLanguage.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView:AdapterView<*>, selectedItemView:View, position:Int, id:Long) {
                videoList.clear()
                videoList.addAll(videoMap["English"] as ArrayList<String>)
            }
            override fun onNothingSelected(parentView:AdapterView<*>) { }
        }

        val lvVideoTemplate = dialog.findViewById<ListView>(R.id.lvVideoTemplate)

        val tvCancel = dialog.findViewById<TextView>(R.id.tvCancel)
        tvCancel.setOnClickListener { dialog.dismiss() }

        val videoNames = ArrayList<String>()
        videoNames.add("Thanks For Your Interest")
        videoNames.add("Montessori Demo Class Invitation")
        videoNames.add("Called & No Response SMS")
        videoNames.add("Book Demo Activity Class - 1")
        videoNames.add("Book Demo Activity Class - 2")
        videoNames.add("Called & No Response SMS")
        videoNames.add("Book Demo Activity Class - 1")
        videoNames.add("Book Demo Activity Class - 2")

        val videoTemplatesAdapter = VideoTemplatesAdapter(this@CustomerActivitiesActivity, videoNames)
        lvVideoTemplate.adapter = videoTemplatesAdapter

        lvVideoTemplate.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            sendWhatsApp(phoneNumber, videoNames[position])
        }
    }

    private fun sendWhatsApp(phNumber: String, message: String) {

        val ph = "+91"+phNumber
        val packageManager = packageManager
        val i = Intent(Intent.ACTION_VIEW)
        try {
            val url = "https://api.whatsapp.com/send?phone="+ ph +"&text=" + URLEncoder.encode(message, "UTF-8")
            i.`package` = "com.whatsapp"
            i.data = Uri.parse(url)
            if (i.resolveActivity(packageManager) != null) {
                startActivity(i)
            }
        } catch (e: Exception){
            e.printStackTrace()
        }

    }

    private fun telephonePermissionCheck(phoneNumber: String){

        val permissionCheck = ContextCompat.checkSelfPermission(this@CustomerActivitiesActivity, android.Manifest.permission.CALL_PHONE)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:"+phoneNumber)
            startActivity(intent)
        } else {

            val dialogBuilder = AlertDialog.Builder(this@CustomerActivitiesActivity)
            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.permission_dialog, null)
            dialogBuilder.setView(dialogView)

            val b = dialogBuilder.create()

            val dialog_message = dialogView.findViewById<View>(R.id.dialog_message) as TextView
            dialog_message.text = "This app needs Telephone permission for making calls to customers."
            val pCancel = dialogView.findViewById<View>(R.id.pCancel) as TextView
            val pSettings = dialogView.findViewById<View>(R.id.pSettings) as TextView
            val pOk = dialogView.findViewById<View>(R.id.pOk) as TextView
            pCancel.setOnClickListener {
                b.dismiss()
            }
            pSettings.setOnClickListener {
                b.dismiss()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            pOk.setOnClickListener {
                b.dismiss()
                ActivityCompat.requestPermissions(this@CustomerActivitiesActivity, arrayOf(android.Manifest.permission.CALL_PHONE), 3)
            }

            b.show()

        }
    }

    @SuppressLint("SimpleDateFormat")
    private fun updateNFD(){
        dialog.setContentView(R.layout.update_nfd_dialog)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        val etTodayUpdate = dialog.findViewById<EditText>(R.id.etTodayUpdate)
        //val etFollowUpdate = dialog.findViewById<EditText>(R.id.etFollowUpdate)
        val tvNFD = dialog.findViewById<TextView>(R.id.tvNFD)
        tvNFD.setOnClickListener {
            val myCalendar = Calendar.getInstance()
            val date = DatePickerDialog.OnDateSetListener { view, year, monthOfYear, dayOfMonth ->
                myCalendar.set(Calendar.YEAR, year)
                myCalendar.set(Calendar.MONTH, monthOfYear)
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val myFormat = "dd MMM, yyyy"
                val sdf = SimpleDateFormat(myFormat, Locale.US)

                tvNFD.text = sdf.format(myCalendar.time)
            }
            DatePickerDialog(this, date, myCalendar.get(Calendar.YEAR), myCalendar.get(Calendar.MONTH), myCalendar.get(Calendar.DAY_OF_MONTH)).show()
        }
        val spStatus = dialog.findViewById<Spinner>(R.id.spStatus)
        val statusItem = ArrayList<String>()
        statusItem.add("open")
        statusItem.add("close")
        //spStatus.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        val statusAdapter = SpinnerAdapter(this, R.layout.status_item, statusItem)
        spStatus.adapter = statusAdapter
        spStatus.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView:AdapterView<*>, selectedItemView:View, position:Int, id:Long) {
                if (statusItem[position] == "close") {
                    tvNFD.visibility = View.GONE
                } else {
                    tvNFD.visibility = View.VISIBLE
                }
            }
            override fun onNothingSelected(parentView:AdapterView<*>) { }
        }

        val spLead = dialog.findViewById<Spinner>(R.id.spLead)
        val leadItem = ArrayList<String>()
        leadItem.add("visited")
        leadItem.add("not visited")
        leadItem.add("converted")
        leadItem.add("lost")
        val leadAdapter = SpinnerAdapter(this, R.layout.status_item, leadItem)
        spLead.adapter = leadAdapter
        spLead.setSelection(leadItem.indexOf(customerDetails.leadStage))

        val ratingBar = dialog.findViewById<RatingBar>(R.id.ratingBar)
        ratingBar.rating = customerDetails.rating!!.toFloat()

        val btnUpdate = dialog.findViewById<Button>(R.id.btnUpdateNFD)
        btnUpdate.setOnClickListener {
            llRating.removeAllViews()
            val ratingPoint = ratingBar.rating.toInt()
            for (i in 0 until ratingPoint)
            {
                val imageView = ImageView(baseContext)
                imageView.setImageResource(R.drawable.icon_rating)
                imageView.layoutParams = LinearLayout.LayoutParams(40, 40)
                llRating.addView(imageView)
            }
            if (spStatus.selectedItem == "open") {
                if (TextUtils.isEmpty(etTodayUpdate.text.toString()) ||
                        TextUtils.isEmpty(tvNFD.text.toString())) {
                    Toast.makeText(baseContext, "Please enter all details", Toast.LENGTH_SHORT).show()
                } else {
                    val myCalendar = Calendar.getInstance()
                    val tDate = myCalendar.time

                    val originalFormat = SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH)
                    val targetFormat = SimpleDateFormat("yyyy/MM/dd")
                    val date = originalFormat.parse(tvNFD.text.toString())
                    val dd = targetFormat.format(date)

                    val year = dd.substring(0,4)
                    val month = dd.substring(5,7)
                    val day = dd.substring(8,10)

                    myCalendar.set(Calendar.YEAR, year.toInt())
                    myCalendar.set(Calendar.MONTH, month.toInt()-1)
                    myCalendar.set(Calendar.DAY_OF_MONTH, day.toInt())

                    val nfdDate = myCalendar.time

                    if (nfdDate.before(tDate) || nfdDate == tDate){
                        Toast.makeText(baseContext, "Please check your nfd", Toast.LENGTH_SHORT).show()
                    } else {
                        UpdateNFD().execute(spStatus.selectedItem.toString(), etTodayUpdate.text.toString(), ratingPoint.toString(), spLead.selectedItem.toString(), tvNFD.text.toString())
                    }
                }
            } else {
                if (TextUtils.isEmpty(etTodayUpdate.text.toString())) {
                    Toast.makeText(baseContext, "Please enter closing reason for enquiry", Toast.LENGTH_SHORT).show()
                } else {
                    dialog.setContentView(R.layout.conformation_dialog)
                    dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
                    dialog.setCancelable(false)
                    val tvCSMS = dialog.findViewById<TextView>(R.id.tvCSMS)
                    tvCSMS.text = "Are you sure, you want to close this enquiry."
                    val tvYes = dialog.findViewById<TextView>(R.id.tvYes)
                    tvYes.setOnClickListener {
                        llOptions.visibility = View.GONE
                        UpdateNFD().execute(spStatus.selectedItem.toString(), etTodayUpdate.text.toString(), spLead.selectedItem.toString(), ratingPoint.toString())
                    }
                    val tvNo = dialog.findViewById<TextView>(R.id.tvNo)
                    tvNo.setOnClickListener { dialog.dismiss() }
                }
            }
        }
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)
        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }


    @SuppressLint("StaticFieldLeak")
    inner class UpdateNFD : AsyncTask<String, Void, Boolean>() {

        private var customerDetailsDo = CustomerDetailsDO()
        private val progressDialog = ProgressDialog(this@CustomerActivitiesActivity, R.style.MyAlertDialogStyle)

        @SuppressLint("SimpleDateFormat")
        override fun onPreExecute() {

            customerDetailsDo = customerDetails

            progressDialog.setMessage("Updating NFD, please wait...")
            progressDialog.setCancelable(false)
            progressDialog.show()

        }

        @SuppressLint("SimpleDateFormat")
        override fun doInBackground(vararg string: String?): Boolean {

            val myCalendar = Calendar.getInstance()
            val myFormat = "yyyy/MM/dd"
            val sdf = SimpleDateFormat(myFormat, Locale.US)
            val todayDate = sdf.format(myCalendar.time)

            val nfdVal = customerDetailsDo.nfd
            val nnfd = HashMap<String, String>()
            val nfdKeys = nfdVal!!.keys
            for (key1 in nfdKeys) {
                nnfd.put(key1, nfdVal[key1].toString())
            }

            var tempNFD = "null"

            var newStatus = string[0]

            if (newStatus == "open"){
                val originalFormat = SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH)
                val targetFormat = SimpleDateFormat("yyyy/MM/dd")
                val date = originalFormat.parse(string[4].toString())
                val nfdDate = targetFormat.format(date)

                tempNFD = nfdDate

                nnfd.put(nfdDate, " ")
                if (!dateList.contains(nfdDate)) {
                    dateList.add(nfdDate)
                }
                fActivities.put(nfdDate, " ")
            } else {
                newStatus = newStatus + ", " + string[1]
            }

            nnfd.put(todayDate, string[1].toString())
            if (!dateList.contains(todayDate)){
                dateList.add(todayDate)
            }
            fActivities.put(todayDate, string[1].toString())

            val newDateList = ArrayList<String>()

            for (i in 0 until dateList.size){
                if (fActivities[dateList[i]] == " " && dateList[i] != tempNFD){
                    fActivities.remove(dateList[i])
                } else {
                    newDateList.add(dateList[i])
                }
            }

            dateList.clear()
            dateList.addAll(newDateList)

            val newNfd = nnfd
            val nNfd = HashMap<String, String>()
            val newKeys = newNfd.keys
            for (key1 in newKeys) {
                val key = key1
                if (key == tempNFD){
                    nNfd.put(key, newNfd[key].toString())
                } else {
                    if (newNfd[key].toString() != " ") {
                        nNfd.put(key, newNfd[key].toString())
                    }
                }
            }

            /*if (string[0] == "open"){
                val originalFormat = SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH)
                val targetFormat = SimpleDateFormat("yyyy/MM/dd")
                val date = originalFormat.parse(string[3].toString())
                val nfdDate = targetFormat.format(date)
                nnfd.put(nfdDate, string[2].toString())
                if (!dateList.contains(nfdDate)) {
                    dateList.add(nfdDate)
                }
                fActivities.put(nfdDate, string[2].toString())
            }

            if (nnfd[todayDate] != null) {
                nnfd.put(todayDate, nnfd[todayDate] +"\n"+ string[1])
                fActivities.put(todayDate, nnfd[todayDate].toString())
            } else {
                nnfd.put(todayDate, string[1].toString())
                dateList.add(todayDate)
                fActivities.put(todayDate, string[1].toString())
            }*/

            customerDetailsDo.status(newStatus.toString())
            customerDetailsDo.rating(string[2].toString())
            customerDetailsDo.leadStage(string[3].toString())
            customerDetailsDo.nfd(nNfd)

            val awsProvider = AWSProvider()
            val dynamoDBClient = AmazonDynamoDBClient(awsProvider.getCredentialsProvider(baseContext))
            dynamoDBClient.setRegion(Region.getRegion(Regions.US_EAST_1))
            val dynamoDBMapper = DynamoDBMapper.builder()
                    .dynamoDBClient(dynamoDBClient)
                    .awsConfiguration(AWSMobileClient.getInstance().configuration)
                    .build()
            return try {
                dynamoDBMapper.save(customerDetailsDo)
                true
            } catch (e: AmazonClientException) {
                basicSnackBar("Network connection error!!")
                false
            }

        }

        override fun onPostExecute(result: Boolean?) {
            progressDialog.dismiss()
            if (result!!) {
                Collections.sort(dateList, CustomerDateComparator())
                //Collections.reverse(dateList)
                user.notifyDataSetChanged()
                lvActivities.smoothScrollToPosition(dateList.size-1)
                if (dialog.isShowing){
                    dialog.dismiss()
                }
                updateStatus = "update"
            }
        }

    }



    class SpinnerAdapter(context: Context, resourceId: Int, private val objects: List<String>) : ArrayAdapter<String>(context, resourceId, objects) {

        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getCustomView(position, convertView, parent)
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return getCustomView(position, convertView, parent)
        }

        private fun getCustomView(position: Int, convertView: View?, parent: ViewGroup): View {

            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val row = inflater.inflate(R.layout.status_item, parent, false)
            val label = row!!.findViewById<TextView>(R.id.tvSpinner)
            label.text = objects[position]

            return row
        }

    }

    inner class ActivitiesAdapter : BaseAdapter {

        private var dateList = ArrayList<String>()
        private var fActivities = HashMap<String, String>()
        private var context: Context? = null
        private var inflater: LayoutInflater? = null

        constructor(context: Context, dateList: ArrayList<String>, fActivities: HashMap<String, String>) : super() {
            this.dateList = dateList
            this.fActivities = fActivities
            this.context = context
            inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }

        @SuppressLint("ViewHolder", "InflateParams", "SimpleDateFormat")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {

            val holder = Holder()
            val rowView = inflater!!.inflate(R.layout.activities_item, null)
            holder.tvDate = rowView.findViewById(R.id.tvDate)
            val originalFormat = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
            val targetFormat = SimpleDateFormat("dd MMM, yyyy")
            val date = originalFormat.parse(dateList[position])
            holder.tvDate!!.text = targetFormat.format(date)
            holder.tvActivity = rowView.findViewById(R.id.tvActivity)
            holder.tvActivity!!.text = fActivities[dateList[position]]
            holder.lineView = rowView.findViewById(R.id.lineView)
            if (position == (fActivities.size)-1){
                val lv = holder.lineView as View
                lv.visibility = View.GONE
            }

            return rowView
        }

        override fun getItem(position: Int): Any {
            return dateList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return dateList.size
        }

        private inner class Holder {
            internal var tvDate: TextView? = null
            internal var tvActivity: TextView? = null
            internal var lineView: View? = null
        }

    }

    class SMSTemplateAdapter : BaseAdapter {

        private var templateList = ArrayList<String>()
        private var templateNames = ArrayList<String>()
        private var inflater: LayoutInflater? = null

        constructor(context: Context, templateNames: ArrayList<String>, templateList: ArrayList<String>) : super() {
            this.templateList = templateList
            this.templateNames = templateNames
            //this.context = context
            inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }

        @SuppressLint("ViewHolder", "InflateParams")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {

            val holder = Holder()
            val rowView = inflater!!.inflate(R.layout.sms_template_item, null)
            holder.tvTemplateName = rowView.findViewById(R.id.tvTemplateName)
            holder.tvTemplateName!!.text = templateNames[position]
            holder.tvTemplateMessage = rowView.findViewById(R.id.tvTemplateMessage)
            holder.tvTemplateMessage!!.text = templateList[position]

            return rowView
        }

        override fun getItem(position: Int): Any {
            return templateList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return templateList.size
        }

        private inner class Holder {
            internal var tvTemplateName: TextView? = null
            internal var tvTemplateMessage: TextView? = null
        }
    }

    class VideoTemplatesAdapter : BaseAdapter {

        private var videoNames = ArrayList<String>()
        private var inflater: LayoutInflater? = null

        constructor(context: Context, videoNames: ArrayList<String>) : super() {
            this.videoNames = videoNames
            //this.context = context
            inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }

        @SuppressLint("ViewHolder", "InflateParams")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {

            val holder = Holder()
            val rowView = inflater!!.inflate(R.layout.videos_template_item, null)
            holder.tvVideoName = rowView.findViewById(R.id.tvVideoName)
            holder.tvVideoName!!.text = videoNames[position]

            return rowView
        }

        override fun getItem(position: Int): Any {
            return videoNames[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return videoNames.size
        }

        private inner class Holder {
            internal var tvVideoName: TextView? = null
        }
    }

    private fun refreshMain(){
        if (updateStatus == "update"){
            mainActivity!!.finish()
            startActivity(Intent(this@CustomerActivitiesActivity, MainActivity::class.java))
            finish()
        } else {
            super.onBackPressed()
        }
    }

    override fun onBackPressed() {
        refreshMain()
    }

    override fun onSupportNavigateUp(): Boolean {
        refreshMain()
        return false
    }

}
