package sagsaguz.enquirytracking

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.*
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.support.design.widget.BaseTransientBottomBar
import android.support.design.widget.Snackbar
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v4.view.ViewPager
import android.telephony.SmsManager
import android.text.TextUtils
import android.text.method.KeyListener
import android.view.*
import android.widget.*
import com.amazonaws.AmazonClientException
import com.amazonaws.mobile.client.AWSMobileClient
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBQueryExpression
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.model.ScanRequest
import com.amazonaws.services.dynamodbv2.model.ScanResult
import com.google.gson.Gson
import kotlinx.android.synthetic.main.add_center_dialog.*
import sagsaguz.enquirytracking.alarm.AlarmReceiver
import sagsaguz.enquirytracking.fragments.PendingList
import sagsaguz.enquirytracking.fragments.TodayList
import sagsaguz.enquirytracking.fragments.UsersList
import sagsaguz.enquirytracking.utils.AWSProvider
import sagsaguz.enquirytracking.utils.CenterDO
import sagsaguz.enquirytracking.utils.Config
import sagsaguz.enquirytracking.utils.CustomerDetailsDO
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity() {

    lateinit var rlMainActivity : RelativeLayout

    lateinit var pbMain : ProgressBar

    lateinit var viewPager : ViewPager
    lateinit var tabParts : TabLayout
    var customersList = ArrayList<CustomerDetailsDO>()
    var tCustomerList = ArrayList<CustomerDetailsDO>()
    var pCustomerList = ArrayList<CustomerDetailsDO>()

    lateinit var dialog : Dialog

    var centerDo = CenterDO()

    var centerDet = CenterDO()

    lateinit var adminLocation : String
    lateinit var preschoolName : String

    lateinit var adminPreferences: SharedPreferences

    companion object {
        @SuppressLint("StaticFieldLeak")
        var mainActivity: MainActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        title = "Admission Tracking"

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        rlMainActivity = findViewById(R.id.rlMainActivity)

        mainActivity = this

        pbMain = findViewById(R.id.pbMain)
        pbMain.indeterminateDrawable.setColorFilter(resources.getColor(R.color.colorEmerald), android.graphics.PorterDuff.Mode.MULTIPLY)
        pbMain.visibility = View.GONE

        dialog = Dialog(this)

        adminPreferences = getSharedPreferences("AdminDetails", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = adminPreferences.getString("Center", "")
        centerDet = gson.fromJson<CenterDO>(json, CenterDO::class.java)
        adminLocation = centerDet.location.toString()
        preschoolName = centerDet.name.toString()

        if (adminLocation == "All Users") {
            CustomerDetails().execute()
        } else {
            CustomerByCenterDetails().execute(preschoolName)
        }

        //scheduleAlarm()

    }

    private fun scheduleAlarm(){
        val time = GregorianCalendar.MILLISECOND + 1*60*1000
        val intentAlarm = Intent(this, AlarmReceiver::class.java)
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(AlarmManager.RTC_WAKEUP, time.toLong(), PendingIntent.getBroadcast(this, 1, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT))
        val pendingIntent = PendingIntent.getBroadcast(this@MainActivity, 0, intentAlarm, 0)
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), time.toLong(), pendingIntent)
        Toast.makeText(baseContext, "Schedule", Toast.LENGTH_SHORT).show()
    }

    private fun setViewPager(){

        viewPager = findViewById(R.id.viewPager)
        viewPager.offscreenPageLimit = 1

        tabParts = findViewById(R.id.tabParts)
        tabParts.setSelectedTabIndicatorColor(resources.getColor(R.color.colorPrimary))

        setTabDetails()

    }

    private fun setTabDetails(){
        setupViewPager(viewPager)
        tabParts.setupWithViewPager(viewPager)
    }

    private fun setupViewPager(viewPager: ViewPager) {

        val adapter = ViewPagerAdapter(supportFragmentManager)
        adapter.addFrag(UsersList.newInstance(customersList), "CUSTOMERS")
        adapter.addFrag(TodayList.newInstance(tCustomerList), "TODAY")
        adapter.addFrag(PendingList.newInstance(pCustomerList), "PENDING")
        viewPager.adapter = adapter

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        val filter = menu.findItem(R.id.filter)
        filter.isVisible = adminLocation == "All Users"
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        when (id) {
            R.id.new_enquiry -> {
                if (adminLocation == "All Users"){
                    showAddDialog()
                } else {
                    val intent = Intent(this@MainActivity, NewEnquiryActivity::class.java)
                    intent.putExtra("centerLocation", adminLocation)
                    intent.putExtra("centerName", preschoolName)
                    startActivity(intent)
                }
                return true
            }
            R.id.filter -> {
                CenterDetails().execute()
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    fun basicSnackBar(message: String) {
        val snackbar = Snackbar.make(rlMainActivity, message, Snackbar.LENGTH_SHORT)
        val sbView = snackbar.view
        sbView.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.colorPrimary))
        val textView = sbView.findViewById<TextView>(android.support.design.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(baseContext, R.color.colorWhite))
        snackbar.show()
    }

    fun showSnackBar(message: String) {
        val snackbar = Snackbar.make(rlMainActivity, message, Snackbar.LENGTH_SHORT)
                .setAction("Try Again") { CustomerDetails().execute() }
        snackbar.setActionTextColor(ContextCompat.getColor(baseContext, R.color.colorAccent))

        val sbView = snackbar.view
        sbView.setBackgroundColor(ContextCompat.getColor(baseContext, R.color.colorPrimary))
        val textView = sbView.findViewById<TextView>(android.support.design.R.id.snackbar_text)
        textView.setTextColor(ContextCompat.getColor(baseContext, R.color.colorWhite))
        snackbar.duration = BaseTransientBottomBar.LENGTH_INDEFINITE
        snackbar.show()
    }

    private fun showAddDialog() {
        dialog.setContentView(R.layout.list_view_dialog)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)

        val userType = ArrayList<String>()
        userType.add("Center")
        userType.add("Customer")

        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
        tvTitle.text = "Select User Type"
        val lvAddUser = dialog.findViewById<ListView>(R.id.lvItems)

        val centresListAdapter = CentresListAdapter(this@MainActivity, userType)
        lvAddUser.adapter = centresListAdapter

        lvAddUser.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            dialog.dismiss()
            if (userType[position] == userType[0]) {
                showAddAdminDialog()
            } else {
                val intent = Intent(this@MainActivity, NewEnquiryActivity::class.java)
                intent.putExtra("centerLocation", adminLocation)
                intent.putExtra("centerName", preschoolName)
                startActivity(intent)
            }
        }

        dialog.show()
    }

    private fun showAddAdminDialog(){
        dialog.setContentView(R.layout.add_center_dialog)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)

        val etCenterEmail = dialog.findViewById<EditText>(R.id.etCenterEmail)
        val etCenterPhone = dialog.findViewById<EditText>(R.id.etCenterPhone)
        val etCenterName = dialog.findViewById<EditText>(R.id.etCentreName)
        val etCenterPassword = dialog.findViewById<EditText>(R.id.etCenterPassword)
        val etCenterLocation = dialog.findViewById<EditText>(R.id.etCenterLocation)
        val etCenterDirections = dialog.findViewById<EditText>(R.id.etCenterDirections)

        val btnCreate = dialog.findViewById<Button>(R.id.btnCreate)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        btnCreate.setOnClickListener {

            if (TextUtils.isEmpty(etCenterEmail.text.toString()) ||
                    TextUtils.isEmpty(etCenterPhone.text.toString()) ||
                    TextUtils.isEmpty(etCenterName.text.toString()) ||
                    TextUtils.isEmpty(etCenterPassword.text.toString()) ||
                    TextUtils.isEmpty(etCenterLocation.text.toString()) ||
                    TextUtils.isEmpty(etCenterDirections.text.toString())) {
                basicSnackBar("Please enter valid details for all fields")
            } else {
                centerDo.name = etCenterName.text.toString()
                centerDo.phone = etCenterPhone.text.toString()
                centerDo.emailId = etCenterEmail.text.toString()
                centerDo.location = etCenterLocation.text.toString()
                centerDo.password = etCenterPassword.text.toString()
                centerDo.directions = etCenterDirections.text.toString()
                centerDo.accessType = "Access"
                AddAdmin().execute()
            }

        }

        btnCancel.setOnClickListener{ dialog.dismiss() }

        dialog.show()
    }

    private fun centersDialog(centerList: ArrayList<CenterDO>){
        dialog.setContentView(R.layout.list_view_dialog)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)

        val tvTitle = dialog.findViewById<TextView>(R.id.tvTitle)
        tvTitle.text = "Select Center"
        val tvNote = dialog.findViewById<TextView>(R.id.tvNote)
        tvNote.visibility = View.VISIBLE
        val lvCentres = dialog.findViewById<ListView>(R.id.lvItems)
        val centerName = ArrayList<String>()
        centerName.clear()
        Collections.sort(centerList, CenterLocationComparator())
        for (i in 0 until centerList.size){
            centerName.add(centerList[i].name.toString())
        }

        val centresListAdapter = CentresListAdapter(this@MainActivity, centerName)
        lvCentres.adapter = centresListAdapter

        lvCentres.onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
            if (centerName[position] == "Head Office") {
                CustomerDetails().execute()
            } else {
                CustomerByCenterDetails().execute(centerName[position])
            }
        }

        lvCentres.onItemLongClickListener = AdapterView.OnItemLongClickListener { adapterView, view, pos, l ->
            showCentreDetailsDialog(centerList[pos])
        }
        dialog.show()
    }

    inner class CenterLocationComparator : Comparator<CenterDO> {
        override fun compare(obj1: CenterDO, obj2: CenterDO): Int {
            return obj1.location.toString().toLowerCase().compareTo(obj2.location.toString().toLowerCase())
        }
    }

    private fun showCentreDetailsDialog(centerDetails: CenterDO): Boolean {
        dialog.setContentView(R.layout.center_details_dialog)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)

        val tvCName = dialog.findViewById<TextView>(R.id.tvCName)
        tvCName.text = centerDetails.name
        val tvCEmail = dialog.findViewById<TextView>(R.id.tvCEmail)
        tvCEmail.text = centerDetails.emailId
        val tvCPhone = dialog.findViewById<TextView>(R.id.tvCPhone)
        tvCPhone.text = centerDetails.phone
        val etCDirection = dialog.findViewById<EditText>(R.id.etCDirection)
        etCDirection.setText(centerDetails.directions)
        etCDirection.tag = etCDirection.keyListener
        etCDirection.keyListener = null
        val etCPassword = dialog.findViewById<EditText>(R.id.etCPassword)
        etCPassword.setText(centerDetails.password)
        etCPassword.tag = etCPassword.keyListener
        etCPassword.keyListener = null
        val spAccessType = dialog.findViewById<Spinner>(R.id.spAccessType)
        val accessTypeItem = ArrayList<String>()
        accessTypeItem.add("Access")
        accessTypeItem.add("Restrict")
        val accessTypeAdapter = SpinnerAdapter(this, R.layout.status_item, accessTypeItem)
        spAccessType.adapter = accessTypeAdapter
        spAccessType.setSelection(accessTypeItem.indexOf(centerDetails.accessType))
        spAccessType.isEnabled = false

        val btnCancel = dialog.findViewById<Button>(R.id.btnCCancel)
        val btnEdit = dialog.findViewById<Button>(R.id.btnCEdit)
        val btnSave = dialog.findViewById<Button>(R.id.btnCSave)
        btnSave.visibility = View.GONE

        btnCancel.setOnClickListener{ dialog.dismiss() }

        btnEdit.setOnClickListener{
            btnEdit.visibility = View.GONE
            etCPassword.keyListener = etCPassword.tag as KeyListener
            etCDirection.keyListener = etCDirection.tag as KeyListener
            btnSave.visibility = View.VISIBLE
            spAccessType.isEnabled = true
        }

        btnSave.setOnClickListener{
            val progressDialog = ProgressDialog(this@MainActivity, R.style.MyAlertDialogStyle)
            progressDialog.setMessage("Updating center details...")
            progressDialog.setCancelable(false)
            progressDialog.show()
            btnSave.visibility = View.GONE
            btnEdit.visibility = View.VISIBLE
            etCDirection.keyListener = null
            etCPassword.keyListener = null
            spAccessType.isEnabled = false
            Thread(Runnable {
                val awsProvider = AWSProvider()
                val dynamoDBClient = AmazonDynamoDBClient(awsProvider.getCredentialsProvider(baseContext))
                dynamoDBClient.setRegion(Region.getRegion(Regions.US_EAST_1))
                val dynamoDBMapper = DynamoDBMapper.builder()
                        .dynamoDBClient(dynamoDBClient)
                        .awsConfiguration(AWSMobileClient.getInstance().configuration)
                        .build()
                val centerDO = CenterDO()
                centerDO.name = tvCName.text.toString()
                centerDO.emailId = tvCEmail.text.toString()
                centerDO.phone = tvCPhone.text.toString()
                centerDO.location = centerDetails.location
                centerDO.directions = etCDirection.text.toString()
                centerDO.password = etCPassword.text.toString()
                centerDO.accessType = spAccessType.selectedItem.toString()
                dynamoDBMapper.save<CenterDO>(centerDO)
                progressDialog.dismiss()
                basicSnackBar("Successfully updated "+centerDetails.name+" details.")
            }).start()
        }

        dialog.show()
        return true
    }


    internal inner class ViewPagerAdapter (manager: FragmentManager) : FragmentPagerAdapter(manager) {
        private val mFragmentList = java.util.ArrayList<Fragment>()
        private val mFragmentTitleList = java.util.ArrayList<String>()

        override fun getItem(position: Int): Fragment {
            return mFragmentList[position]
        }

        override fun getCount(): Int {
            return mFragmentList.size
        }

        fun addFrag(fragment: Fragment, title: String) {
            mFragmentList.add(fragment)
            mFragmentTitleList.add(title)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return mFragmentTitleList[position]
        }
    }

    @SuppressLint("StaticFieldLeak")
    inner class CustomerDetails : AsyncTask<Void, Void, Boolean>() {

        override fun onPreExecute() {
            pbMain.visibility = View.VISIBLE
            customersList.clear()
            tCustomerList.clear()
            pCustomerList.clear()
        }

        override fun doInBackground(vararg p0: Void?): Boolean {

            val awsProvider = AWSProvider()
            val dynamoDBClient = AmazonDynamoDBClient(awsProvider.getCredentialsProvider(baseContext))
            dynamoDBClient.setRegion(Region.getRegion(Regions.US_EAST_1))

            try {
                var result: ScanResult? = null
                do {
                    val req = ScanRequest()
                    req.tableName = Config.CUSTOMERTABLE
                    if (result != null) {
                        req.exclusiveStartKey = result.lastEvaluatedKey
                    }
                    result = dynamoDBClient.scan(req)
                    val rows = result.items
                    for (map in rows) {

                        val customerDetailsDo = CustomerDetailsDO()

                        customerDetailsDo.phone = map["phone"]!!.s
                        customerDetailsDo.emailId = map["emailId"]!!.s
                        customerDetailsDo.admissionFor = map["admissionFor"]!!.s
                        customerDetailsDo.childAge = map["childAge"]!!.s
                        customerDetailsDo.childName = map["childName"]!!.s
                        customerDetailsDo.createdDate = map["createdDate"]!!.s
                        customerDetailsDo.enquiryType = map["enquiryType"]!!.s
                        customerDetailsDo.hyk = map["hyk"]!!.s
                        customerDetailsDo.locality = map["locality"]!!.s
                        customerDetailsDo.parentName = map["parentName"]!!.s
                        customerDetailsDo.rating = map["rating"]!!.s
                        customerDetailsDo.center = map["center"]!!.s
                        customerDetailsDo.status = map["status"]!!.s
                        customerDetailsDo.leadStage = map["leadStage"]!!.s

                        val nfdVal = map["NFD"]!!.m
                        val nfd = HashMap<String, String>()
                        val nfdKeys = nfdVal.keys
                        for (key1 in nfdKeys) {
                            val key = key1 as String
                            nfd.put(key, nfdVal[key]!!.s)
                        }

                        customerDetailsDo.nfd = nfd

                        customersList.add(customerDetailsDo)
                        tCustomerList.add(customerDetailsDo)
                        pCustomerList.add(customerDetailsDo)

                    }
                } while (result!!.lastEvaluatedKey != null)

                return true
            } catch (e : AmazonClientException){
                showSnackBar("Network connection error!!")
                return false
            }
        }

        override fun onPostExecute(result: Boolean?) {
            pbMain.visibility = View.GONE
            if (dialog.isShowing) {
                dialog.dismiss()
            }
            if (result!!){
                setViewPager()
            }
        }

    }

    @SuppressLint("StaticFieldLeak")
    inner class AddAdmin : AsyncTask<Void, Void, Boolean>() {

        val progressDialog = ProgressDialog(this@MainActivity, R.style.MyAlertDialogStyle)

        override fun onPreExecute() {
            progressDialog.setMessage("Adding center, please wait...")
            progressDialog.setCancelable(false)
            progressDialog.show()
        }

        override fun doInBackground(vararg p0: Void?): Boolean {

            val awsProvider = AWSProvider()
            val dynamoDBClient = AmazonDynamoDBClient(awsProvider.getCredentialsProvider(baseContext))
            dynamoDBClient.setRegion(Region.getRegion(Regions.US_EAST_1))
            val dynamoDBMapper = DynamoDBMapper.builder()
                    .dynamoDBClient(dynamoDBClient)
                    .awsConfiguration(AWSMobileClient.getInstance().configuration)
                    .build()

            try {
                var result: ScanResult? = null
                do {
                    val req = ScanRequest()
                    req.tableName = Config.CENTERTABLE
                    if (result != null) {
                        req.exclusiveStartKey = result.lastEvaluatedKey
                    }
                    result = dynamoDBClient.scan(req)
                    val rows = result!!.items
                    for (map in rows) {
                        try {
                            if (map["phone"]!!.s == centerDo.phone) {
                                return false
                            }
                        } catch (e: NumberFormatException) {
                            println(e.message)
                        }
                    }
                } while (result!!.lastEvaluatedKey != null)

                dynamoDBMapper.save<CenterDO>(centerDo)

                return true
            } catch (e: AmazonClientException) {
                showSnackBar("Network connection error!!")
                return false
            }

        }

        override fun onPostExecute(result: Boolean) {
            progressDialog.dismiss()
            if (result){
                basicSnackBar("Successfully created center")
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            } else {
                basicSnackBar("Center with this details already exists")
            }
        }
    }


    @SuppressLint("StaticFieldLeak")
    inner class CenterDetails : AsyncTask<Void, Void, Boolean>() {

        val centerList = ArrayList<CenterDO>()

        override fun onPreExecute() {
            pbMain.visibility = View.VISIBLE
            centerList.clear()
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

                        val centerDo = CenterDO()

                        centerDo.phone = map["phone"]!!.s
                        centerDo.emailId = map["emailId"]!!.s
                        centerDo.name = map["name"]!!.s
                        centerDo.location = map["location"]!!.s
                        centerDo.password = map["password"]!!.s
                        centerDo.directions = map["directions"]!!.s
                        centerDo.accessType = map["accessType"]!!.s

                        centerList.add(centerDo)

                    }
                } while (result!!.lastEvaluatedKey != null)

                return true
            } catch (e : AmazonClientException){
                showSnackBar("Network connection error!!")
                return false
            }
        }

        override fun onPostExecute(result: Boolean?) {
            pbMain.visibility = View.GONE
            if (dialog.isShowing) {
                dialog.dismiss()
            }
            if (result!!){
                centersDialog(centerList)
            }
        }

    }


    @SuppressLint("StaticFieldLeak")
    inner class CustomerByCenterDetails : AsyncTask<String, Void, Boolean>() {

        override fun onPreExecute() {
            pbMain.visibility = View.VISIBLE
            customersList.clear()
            tCustomerList.clear()
            pCustomerList.clear()
        }

        override fun doInBackground(vararg p0: String?): Boolean {

            val awsProvider = AWSProvider()
            val dynamoDBClient = AmazonDynamoDBClient(awsProvider.getCredentialsProvider(baseContext))
            dynamoDBClient.setRegion(Region.getRegion(Regions.US_EAST_1))

            try {

                if (adminLocation != "All Users") {

                    var center = CenterDO()
                    center.phone = centerDet.phone
                    val dynamoDBMapper = DynamoDBMapper.builder()
                            .dynamoDBClient(dynamoDBClient)
                            .awsConfiguration(AWSMobileClient.getInstance().configuration)
                            .build()
                    val queryExpression = DynamoDBQueryExpression<CenterDO>()
                            .withHashKeyValues(center)
                            .withConsistentRead(false)
                    val cResult = dynamoDBMapper.query(CenterDO::class.java, queryExpression)

                    val gson = Gson()
                    for (i in 0 until cResult.size) {
                        val jsonFormOfItem = gson.toJson(cResult[i])
                        center = gson.fromJson(jsonFormOfItem, CenterDO::class.java)
                    }

                    if (center.accessType != "Access"){
                        val prefsEditor = adminPreferences.edit()
                        prefsEditor.putString("Login", "logout")
                        prefsEditor.apply()
                        startActivity(Intent(baseContext, LoginActivity::class.java))
                        finish()
                    }

                }

                var result: ScanResult? = null
                do {
                    val req = ScanRequest()
                    req.tableName = Config.CUSTOMERTABLE
                    if (result != null) {
                        req.exclusiveStartKey = result.lastEvaluatedKey
                    }
                    result = dynamoDBClient.scan(req)
                    val rows = result.items
                    for (map in rows) {

                        if (map["center"]!!.s == p0[0]) {

                            val customerDetailsDo = CustomerDetailsDO()

                            customerDetailsDo.phone = map["phone"]!!.s
                            customerDetailsDo.emailId = map["emailId"]!!.s
                            customerDetailsDo.admissionFor = map["admissionFor"]!!.s
                            customerDetailsDo.childAge = map["childAge"]!!.s
                            customerDetailsDo.childName = map["childName"]!!.s
                            customerDetailsDo.createdDate = map["createdDate"]!!.s
                            customerDetailsDo.enquiryType = map["enquiryType"]!!.s
                            customerDetailsDo.hyk = map["hyk"]!!.s
                            customerDetailsDo.locality = map["locality"]!!.s
                            customerDetailsDo.parentName = map["parentName"]!!.s
                            customerDetailsDo.rating = map["rating"]!!.s
                            customerDetailsDo.center = map["center"]!!.s
                            customerDetailsDo.status = map["status"]!!.s

                            val nfdVal = map["NFD"]!!.m
                            val nfd = HashMap<String, String>()
                            val nfdKeys = nfdVal.keys
                            for (key1 in nfdKeys) {
                                val key = key1 as String
                                nfd.put(key, nfdVal[key]!!.s)
                            }

                            customerDetailsDo.nfd = nfd

                            customersList.add(customerDetailsDo)
                            tCustomerList.add(customerDetailsDo)
                            pCustomerList.add(customerDetailsDo)

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
            pbMain.visibility = View.GONE
            if (dialog.isShowing) {
                dialog.dismiss()
            }
            if (result!!){
                setViewPager()
            }
        }

    }



    class CentresListAdapter : BaseAdapter {

        private var centreList = ArrayList<String>()
        private var inflater: LayoutInflater? = null

        constructor(context: Context, centerList: ArrayList<String>) : super() {
            this.centreList = centerList
            //this.context = context
            inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        }

        @SuppressLint("ViewHolder", "InflateParams")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {

            val holder = Holder()
            val rowView = inflater!!.inflate(R.layout.list_view_item, null)
            holder.centreName = rowView.findViewById(R.id.centreName)
            holder.centreName!!.text = centreList[position]

            return rowView
        }

        override fun getItem(position: Int): Any {
            return centreList[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getCount(): Int {
            return centreList.size
        }

        private inner class Holder {
            internal var centreName: TextView? = null
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

    override fun onSupportNavigateUp(): Boolean {
        dialog.setContentView(R.layout.conformation_dialog)
        dialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setCancelable(false)
        val tvCSMS = dialog.findViewById<TextView>(R.id.tvCSMS)
        tvCSMS.text = "Are you sure, you want to logout."
        val tvYes = dialog.findViewById<TextView>(R.id.tvYes)
        tvYes.setOnClickListener {
            val prefsEditor = adminPreferences.edit()
            prefsEditor.putString("Login", "logout")
            prefsEditor.apply()
            startActivity(Intent(baseContext, LoginActivity::class.java))
            finish()
        }
        val tvNo = dialog.findViewById<TextView>(R.id.tvNo)
        tvNo.setOnClickListener { dialog.dismiss() }
        dialog.show()
        return false
    }

}
