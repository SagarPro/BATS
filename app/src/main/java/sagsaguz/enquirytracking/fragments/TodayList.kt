package sagsaguz.enquirytracking.fragments

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import sagsaguz.enquirytracking.CustomerActivitiesActivity
import sagsaguz.enquirytracking.R
import sagsaguz.enquirytracking.adapter.UserListAdapter
import sagsaguz.enquirytracking.utils.CustomerDetailsDO
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class TodayList : Fragment() {

    lateinit var tvMessage: TextView
    lateinit var lvToday: ListView

    lateinit var user : UserListAdapter

    val fCustomerList = ArrayList<CustomerDetailsDO>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.today_list, container, false)

        tvMessage = view.findViewById(R.id.tvMessage)
        lvToday = view.findViewById(R.id.today_list_fragment)

        user = UserListAdapter(this.context!!, fCustomerList)
        lvToday.adapter = user

        filterByRating()

        lvToday.setOnItemClickListener { parent, view1, position, id ->
            val intent = Intent(this.context!!, CustomerActivitiesActivity::class.java)
            intent.putExtra("customer", fCustomerList[position])
            startActivity(intent)
        }

        return view
    }

    companion object {

        var tCustomersList = ArrayList<CustomerDetailsDO>()

        fun newInstance(customersList: ArrayList<CustomerDetailsDO>): TodayList {
            val args = Bundle()
            val fragment = TodayList()
            fragment.arguments = args
            this.tCustomersList = customersList
            return fragment
        }
    }

    private fun filterByRating(){
        fCustomerList.clear()

        Collections.sort(tCustomersList, CustomerRatingComparator())
        Collections.reverse(tCustomersList)

        val myCalendar = Calendar.getInstance()
        val myFormat = "yyyy/MM/dd"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        val todayDate = sdf.format(myCalendar.time)

        for (i in 0 until tCustomersList.size) {

            val nfdVal = tCustomersList[i].nfd
            val nfdKeys = nfdVal!!.keys
            for (key1 in nfdKeys) {
                if (key1 == todayDate && tCustomersList[i].status == "open") {
                    fCustomerList.add(tCustomersList[i])
                }
            }

        }

        if (fCustomerList.isEmpty()) {
            lvToday.visibility = View.GONE
            tvMessage.visibility = View.VISIBLE
        } else {
            tvMessage.visibility = View.GONE
            lvToday.visibility = View.VISIBLE
        }

        user.notifyDataSetChanged()
    }

    inner class CustomerRatingComparator : Comparator<CustomerDetailsDO> {

        override fun compare(obj1: CustomerDetailsDO, obj2: CustomerDetailsDO): Int {
            return obj1.rating!!.compareTo(obj2.rating!!)
        }
    }

}