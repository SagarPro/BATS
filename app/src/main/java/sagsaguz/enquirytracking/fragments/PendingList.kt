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

class PendingList : Fragment() {

    lateinit var tvMessage: TextView
    lateinit var lvPending: ListView

    lateinit var user : UserListAdapter

    private val fCustomerList = ArrayList<CustomerDetailsDO>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.pending_list, container, false)

        tvMessage = view.findViewById(R.id.tvMessage)
        lvPending = view.findViewById(R.id.pending_list_fragment)

        user = UserListAdapter(this.context!!, fCustomerList)
        lvPending.adapter = user

        filterByRating()

        lvPending.setOnItemClickListener { parent, view1, position, id ->
            val intent = Intent(this.context!!, CustomerActivitiesActivity::class.java)
            intent.putExtra("customer", fCustomerList[position])
            startActivity(intent)
        }

        return view
    }

    companion object {

        var pCustomersList = ArrayList<CustomerDetailsDO>()

        fun newInstance(customersList: ArrayList<CustomerDetailsDO>): PendingList {
            val args = Bundle()
            val fragment = PendingList()
            fragment.arguments = args
            this.pCustomersList = customersList
            return fragment
        }
    }

    private fun filterByRating(){
        fCustomerList.clear()

        Collections.sort(pCustomersList, CustomerRatingComparator())
        Collections.reverse(pCustomersList)

        val myCalendar = Calendar.getInstance()
        val tDate = myCalendar.time
        val myFormat = "yyyy/MM/dd"
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        val todayDate = sdf.format(myCalendar.time)

        for (i in 0 until pCustomersList.size) {
            fCustomerList.add(pCustomersList[i])

            val nfdVal = pCustomersList[i].nfd
            val nfdKeys = nfdVal!!.keys
            for (key1 in nfdKeys) {

                val year = key1.substring(0,4)
                val month = key1.substring(5,7)
                val day = key1.substring(8,10)

                myCalendar.set(Calendar.YEAR, year.toInt())
                myCalendar.set(Calendar.MONTH, month.toInt()-1)
                myCalendar.set(Calendar.DAY_OF_MONTH, day.toInt())

                val date = myCalendar.time

                if (date.after(tDate) || pCustomersList[i].status != "open" || date == tDate){
                    fCustomerList.remove(pCustomersList[i])
                    break
                }

            }

        }

        if (fCustomerList.isEmpty()) {
            lvPending.visibility = View.GONE
            tvMessage.visibility = View.VISIBLE
        } else {
            tvMessage.visibility = View.GONE
            lvPending.visibility = View.VISIBLE
        }

        user.notifyDataSetChanged()
    }

    inner class CustomerRatingComparator : Comparator<CustomerDetailsDO> {

        override fun compare(obj1: CustomerDetailsDO, obj2: CustomerDetailsDO): Int {
            return obj1.rating!!.compareTo(obj2.rating!!)
        }
    }

}