package sagsaguz.enquirytracking

import android.annotation.SuppressLint
import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.regex.Pattern
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.provider.Settings
import android.support.design.widget.BaseTransientBottomBar
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.telephony.SmsManager
import android.widget.DatePicker
import com.amazonaws.AmazonClientException
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import com.amazonaws.services.dynamodbv2.model.ScanRequest
import com.amazonaws.services.dynamodbv2.model.ScanResult
import sagsaguz.enquirytracking.MainActivity.Companion.mainActivity
import sagsaguz.enquirytracking.utils.AWSProvider
import sagsaguz.enquirytracking.utils.CenterDO
import sagsaguz.enquirytracking.utils.Config
import sagsaguz.enquirytracking.utils.CustomerDetailsDO
import java.util.*
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList


class NewEnquiryActivity : AppCompatActivity() {

    lateinit var rlNewEnquiry : RelativeLayout

    lateinit var ivBack : ImageView

    lateinit var etChildName : EditText
    lateinit var etParentName : EditText
    lateinit var etChildAge : EditText
    lateinit var etHouseLocality : EditText
    lateinit var etMobileNumber : EditText
    lateinit var etParentEmail : EditText

    lateinit var AssignTo : TextView
    lateinit var spAssignTo : Spinner
    lateinit var spLeadStage : Spinner
    lateinit var spAdmissionFor : Spinner
    lateinit var spEnquiryType : Spinner
    lateinit var spHYK : Spinner

    lateinit var etFollowUpNotes : EditText
    lateinit var tvNFD : TextView
    lateinit var spRating : Spinner

    lateinit var btnAddEnquiry : Button

    lateinit var assignAdapter : SpinnerAdapter
    val assignList = ArrayList<String>()
    val centersPhoneList = ArrayList<String>()

    var adminLocation = "ALL Users"
    lateinit var preschoolName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_enquiry_layout)

        rlNewEnquiry = findViewById(R.id.rlNewEnquiry)

        ivBack = findViewById(R.id.ivBack)
        ivBack.setOnClickListener { finish() }

        etChildName = findViewById(R.id.etChildName)
        etParentName = findViewById(R.id.etParentName)
        etChildAge = findViewById(R.id.etChildAge)
        etHouseLocality = findViewById(R.id.etHouseLocality)
        etMobileNumber = findViewById(R.id.etMobileNumber)
        etParentEmail = findViewById(R.id.etParentEmail)

        AssignTo = findViewById(R.id.AssignTo)
        spAssignTo = findViewById(R.id.spAssignTo)
        spLeadStage = findViewById(R.id.spLeadStage)
        spAdmissionFor = findViewById(R.id.spAdmissionFor)
        spEnquiryType = findViewById(R.id.spEnquiryType)
        spHYK = findViewById(R.id.spHYK)

        etFollowUpNotes = findViewById(R.id.etFollowUpNotes)
        tvNFD = findViewById(R.id.tvNFD)
        spRating = findViewById(R.id.spRating)

        val intent = intent
        adminLocation = intent.getStringExtra("centerLocation")
        preschoolName = intent.getStringExtra("centerName")
        if (adminLocation == "All Users"){
            AssignTo.visibility = View.VISIBLE
            spAssignTo.visibility = View.VISIBLE
        } else {
            AssignTo.visibility = View.GONE
            spAssignTo.visibility = View.GONE
        }

        Centers().execute()

        val leadStageList = ArrayList<String>()
        leadStageList.add("visited")
        leadStageList.add("not visited")
        leadStageList.add("converted")
        leadStageList.add("lost")

        val admissionList = ArrayList<String>()
        admissionList.add("Playgroup")
        admissionList.add("Nursery")
        admissionList.add("LKG")
        admissionList.add("UKG")
        admissionList.add("Day Care")
        admissionList.add("Others")

        val enquiryList = ArrayList<String>()
        enquiryList.add("Call")
        enquiryList.add("Walk-in")
        enquiryList.add("Web")
        enquiryList.add("Others")

        val hykList = ArrayList<String>()
        hykList.add("Reference")
        hykList.add("Pamphlet")
        hykList.add("Board/Banner")
        hykList.add("Web")
        hykList.add("Centre Location")
        hykList.add("Others")

        val ratingList = ArrayList<String>()
        ratingList.add("1")
        ratingList.add("2")
        ratingList.add("3")
        ratingList.add("4")
        ratingList.add("5")

        val leadAdapter = SpinnerAdapter(this, R.layout.spinner_item, leadStageList)
        spLeadStage.adapter = leadAdapter

        //spAssignTo.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        assignAdapter = SpinnerAdapter(this, R.layout.spinner_item, assignList)
        spAssignTo.adapter = assignAdapter

        //spAdmissionFor.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        val admissionAdapter = SpinnerAdapter(this, R.layout.spinner_item, admissionList)
        spAdmissionFor.adapter = admissionAdapter

        //spEnquiryType.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        val enquiryAdapter = SpinnerAdapter(this, R.layout.spinner_item, enquiryList)
        spEnquiryType.adapter = enquiryAdapter

        //spHYK.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        val hykAdapter = SpinnerAdapter(this, R.layout.spinner_item, hykList)
        spHYK.adapter = hykAdapter

        //spRating.setBackgroundColor(resources.getColor(R.color.colorPrimary))
        val ratingAdapter = SpinnerAdapter(this, R.layout.spinner_item, ratingList)
        spRating.adapter = ratingAdapter

        tvNFD.setOnClickListener { dateViewDialog() }

        btnAddEnquiry = findViewById(R.id.btnAddEnquiry)
        btnAddEnquiry.setOnClickListener { validation() }

    }

    private fun dateViewDialog(){
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

    private fun validation(){
        val validatedResult = validateUserDetails()
        if (validatedResult){
            AddEnquiry().execute()
        }
    }

    private fun validateUserDetails(): Boolean {
        if (TextUtils.isEmpty(etChildName.text.toString()) ||
                TextUtils.isEmpty(etParentName.text.toString()) ||
                TextUtils.isEmpty(etChildAge.text.toString()) ||
                TextUtils.isEmpty(etHouseLocality.text.toString()) ||
                TextUtils.isEmpty(etMobileNumber.text.toString()) ||
                TextUtils.isEmpty(etParentEmail.text.toString()) ||
                TextUtils.isEmpty(etFollowUpNotes.text.toString()) ||
                TextUtils.isEmpty(tvNFD.text.toString())) {
            basicSnackBar("Please enter your details")
            return false
        }

        return if (isValidEmail()){
            if (etMobileNumber.length()==10){
                true
            } else {
                basicSnackBar("Enter valid 10 digit phone number")
                false
            }
        } else {
            basicSnackBar("Enter your valid email address")
            false
        }
    }

    private fun isValidEmail(): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        val patternEmail = Pattern.compile(emailPattern, Pattern.CASE_INSENSITIVE)
        val matcherEmail = patternEmail.matcher(etParentEmail.text.toString())
        return matcherEmail.find()
    }

    fun basicSnackBar(message: String) {
        val snackbar = Snackbar.make(rlNewEnquiry, message, Snackbar.LENGTH_SHORT)
        val sbView = snackbar.view
        sbView.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.colorPrimary))
        val textView = sbView.findViewById<TextView>(android.support.design.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(baseContext, R.color.colorWhite))
        snackbar.show()
    }

    fun showSnackBar(message: String) {
        val snackbar = Snackbar.make(rlNewEnquiry, message, Snackbar.LENGTH_SHORT)
                .setAction("Try Again") { validation() }
        snackbar.setActionTextColor(ContextCompat.getColor(baseContext, R.color.colorAccent))

        val sbView = snackbar.view
        sbView.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.colorPrimary))
        val textView = sbView.findViewById<TextView>(android.support.design.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(baseContext, R.color.colorWhite))
        snackbar.duration = BaseTransientBottomBar.LENGTH_INDEFINITE
        snackbar.show()
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
            val row = inflater.inflate(R.layout.spinner_item, parent, false)
            val label = row!!.findViewById<TextView>(R.id.tvSpinner)
            label.text = objects[position]

            return row
        }

    }

    private fun smsPermissionCheck(customerNo: String, centerNo: String){

        val permissionCheck = ContextCompat.checkSelfPermission(this@NewEnquiryActivity, android.Manifest.permission.SEND_SMS)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            sendSMS(customerNo, centerNo)
        } else {

            val dialogBuilder = AlertDialog.Builder(this@NewEnquiryActivity)
            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.permission_dialog, null)
            dialogBuilder.setView(dialogView)

            val b = dialogBuilder.create()

            val dialog_message = dialogView.findViewById<View>(R.id.dialog_message) as TextView
            dialog_message.text = "This app needs SMS permission for sending sms."
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
                ActivityCompat.requestPermissions(this@NewEnquiryActivity, arrayOf(android.Manifest.permission.SEND_SMS), 3)
            }

            b.show()

        }
    }

    private fun sendSMS(customerNo: String, centerNo: String){
        var customerMessage = "Hi "+etParentName.text+", thank you for your interest in "+preschoolName+" for "+etChildName.text+". Now experience the joy of learning."
        val centerMessage = "Hi, "+etParentName.text+" has been assigned to your center for follow up admission."
        if (adminLocation == "All Users"){
            customerMessage = "Hi "+etParentName.text+", thank you for your interest in "+spAssignTo.selectedItem.toString()+" for "+etChildName.text+", full of sunshine, learning, fun & care. Thank you"
            SmsManager.getDefault().sendTextMessage(centerNo, null, centerMessage, null, null)
        }
        SmsManager.getDefault().sendTextMessage(customerNo, null, customerMessage, null, null)
        mainActivity!!.finish()
        startActivity(Intent(this@NewEnquiryActivity, MainActivity::class.java))
        finish()
    }

    /*private fun sendToCustomer(customerNo: String){
        val customerMessage = "Hi "+etParentName.text+", thank you for your interest in "+spAssignTo.selectedItem.toString()+" for "+etChildName.text+", full of sunshine, learning, fun & care. Thank you"
        SmsManager.getDefault().sendTextMessage(customerNo, null, customerMessage, null, null)
        mainActivity!!.finish()
        startActivity(Intent(this@NewEnquiryActivity, MainActivity::class.java))
        finish()
    }*/

    @SuppressLint("StaticFieldLeak")
    inner class AddEnquiry : AsyncTask<Void, Void, Boolean>() {

        private var customerDetailsDo = CustomerDetailsDO()
        val progressDialog = ProgressDialog(this@NewEnquiryActivity, R.style.MyAlertDialogStyle)

        @SuppressLint("SimpleDateFormat")
        override fun onPreExecute() {

            progressDialog.setMessage("Adding enquiry, please wait...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            customerDetailsDo.phone(etMobileNumber.text.toString())
            customerDetailsDo.emailId(etParentEmail.text.toString())
            customerDetailsDo.childName(etChildName.text.toString())
            customerDetailsDo.parentName(etParentName.text.toString())
            customerDetailsDo.childAge(etChildAge.text.toString())
            customerDetailsDo.locality(etHouseLocality.text.toString())
            customerDetailsDo.admissionFor(spAdmissionFor.selectedItem.toString())
            customerDetailsDo.enquiryType(spEnquiryType.selectedItem.toString())
            customerDetailsDo.hyk(spHYK.selectedItem.toString())
            customerDetailsDo.rating(spRating.selectedItem.toString())
            customerDetailsDo.status("open")
            customerDetailsDo.leadStage(spLeadStage.selectedItem.toString())

            if (adminLocation == "All Users"){
                customerDetailsDo.center(spAssignTo.selectedItem.toString())
            } else {
                customerDetailsDo.center(preschoolName)
            }

            val myCalendar = Calendar.getInstance()
            val myFormat = "yyyy/MM/dd"
            val sdf = SimpleDateFormat(myFormat, Locale.US)

            customerDetailsDo.createdDate(sdf.format(myCalendar.time))

            val nfd = HashMap<String, String>()

            val originalFormat = SimpleDateFormat("dd MMM, yyyy", Locale.ENGLISH)
            val targetFormat = SimpleDateFormat("yyyy/MM/dd")
            val date = originalFormat.parse(tvNFD.text.toString())
            val dd = targetFormat.format(date)

            nfd.put(dd, etFollowUpNotes.text.toString())

            customerDetailsDo.nfd(nfd)
        }

        override fun doInBackground(vararg p0: Void?): Boolean {

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
                showSnackBar("Network connection error!!")
                false
            }

        }

        override fun onPostExecute(result: Boolean?) {
            progressDialog.dismiss()
            if (result!!) {
                if (adminLocation == "All Users"){
                    smsPermissionCheck(customerDetailsDo.phone.toString(), centersPhoneList[spAssignTo.selectedItemPosition])
                } else {
                    smsPermissionCheck(customerDetailsDo.phone.toString(),"center")
                }
            }
        }

    }

    @SuppressLint("StaticFieldLeak")
    inner class Centers : AsyncTask<Void, Void, Boolean>() {

        override fun onPreExecute() {
            assignList.clear()
            centersPhoneList.clear()
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
                        if (map["name"]!!.s != "Head Office") {
                            assignList.add(map["name"]!!.s)
                            centersPhoneList.add(map["phone"]!!.s)
                        }
                    }
                } while (result!!.lastEvaluatedKey != null)

                return true
            } catch (e : AmazonClientException){
                showSnackBar("Network connection error!!")
                return false
            }
        }

        override fun onPostExecute(result: Boolean?) {
            if (result!!){
                assignAdapter.notifyDataSetChanged()
            }
        }

    }

}
