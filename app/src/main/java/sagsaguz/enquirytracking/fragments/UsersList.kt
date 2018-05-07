package sagsaguz.enquirytracking.fragments

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.SwitchCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import sagsaguz.enquirytracking.CustomerActivitiesActivity
import sagsaguz.enquirytracking.R
import sagsaguz.enquirytracking.utils.CustomerDetailsDO
import android.widget.CompoundButton
import sagsaguz.enquirytracking.adapter.UserListAdapter
import java.util.*
import kotlin.collections.ArrayList


class UsersList : Fragment() {

    lateinit var swFilter : SwitchCompat

    val fCustomerList = ArrayList<CustomerDetailsDO>()

    lateinit var user : UserListAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.user_list, container, false)

        val tvMessage = view.findViewById<TextView>(R.id.tvMessage)
        val listView = view.findViewById<ListView>(R.id.user_list_fragment)

        if (uCustomersList.isEmpty()) {
            listView.visibility = View.GONE
            tvMessage.visibility = View.VISIBLE
        } else {
            tvMessage.visibility = View.GONE
            listView.visibility = View.VISIBLE
        }

        user = UserListAdapter(this.context!!, fCustomerList)
        listView.adapter = user

        filterByRating()

        listView.setOnItemClickListener { parent, view, position, id ->
            //Toast.makeText(context, "Position Clicked:"+" "+position,Toast.LENGTH_SHORT).show()
            val intent = Intent(context, CustomerActivitiesActivity::class.java)
            intent.putExtra("customer", fCustomerList[position])
            startActivity(intent)
        }

        swFilter = view.findViewById<SwitchCompat>(R.id.swFilter)
        swFilter.setOnCheckedChangeListener { compoundButton, checked ->
            if (checked) {
                filterByDate()
            } else {
                filterByRating()
            }
        }

        return view
    }

    companion object {

        var uCustomersList = ArrayList<CustomerDetailsDO>()

        fun newInstance(customersList: ArrayList<CustomerDetailsDO>): UsersList {
            val args = Bundle()
            val fragment = UsersList()
            fragment.arguments = args
            this.uCustomersList = customersList
            return fragment
        }

    }

    private fun filterByRating(){
        fCustomerList.clear()

        Collections.sort(uCustomersList, CustomerRatingComparator())
        Collections.reverse(uCustomersList)

        for (i in 0 until uCustomersList.size){
            fCustomerList.add(uCustomersList[i])
        }

        user.notifyDataSetChanged()
    }

    private fun filterByDate(){
        fCustomerList.clear()

        Collections.sort(uCustomersList, CustomerDateComparator())
        Collections.reverse(uCustomersList)

        for (i in 0 until uCustomersList.size){
            fCustomerList.add(uCustomersList[i])
        }

        user.notifyDataSetChanged()
    }

    inner class CustomerDateComparator : Comparator<CustomerDetailsDO> {

        override fun compare(obj1: CustomerDetailsDO, obj2: CustomerDetailsDO): Int {
            return obj1.createdDate!!.compareTo(obj2.createdDate!!)
        }
    }

    inner class CustomerRatingComparator : Comparator<CustomerDetailsDO> {

        override fun compare(obj1: CustomerDetailsDO, obj2: CustomerDetailsDO): Int {
            return obj1.rating!!.compareTo(obj2.rating!!)
        }
    }

}