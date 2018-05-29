package sagsaguz.enquirytracking.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import sagsaguz.enquirytracking.R
import sagsaguz.enquirytracking.utils.CustomerDetailsDO
import java.util.ArrayList

class UserListAdapter : BaseAdapter {

    private var customerList = ArrayList<CustomerDetailsDO>()
    private var context: Context? = null
    private var inflater: LayoutInflater? = null

    constructor(context: Context, customerList: ArrayList<CustomerDetailsDO>) : super() {
        this.customerList = customerList
        this.context = context
        inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    @SuppressLint("ViewHolder", "InflateParams")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {

        val holder = Holder()
        val rowView = inflater!!.inflate(R.layout.users_list_item, null)
        holder.userName = rowView.findViewById(R.id.user_name)
        holder.userName!!.text = customerList[position].childName
        holder.userPhone = rowView.findViewById(R.id.user_phone)
        holder.userPhone!!.text = customerList[position].phone
        holder.llRating = rowView.findViewById(R.id.llRating)

        val rating = holder.llRating as LinearLayout
        for (i in 0 until customerList[position].rating!!.toInt())
        {
            val imageView = ImageView(context)
            imageView.setImageResource(R.drawable.icon_rating)
            imageView.layoutParams = LinearLayout.LayoutParams(40, 40)
            rating.addView(imageView)
        }

        holder.tvEnquiryStatus = rowView.findViewById(R.id.tvEnquiryStatus)
        holder.tvEnquiryStatus!!.text = customerList[position].status
        holder.tvFollowUp = rowView.findViewById(R.id.tvFollowUp)
        holder.tvFollowUp!!.text = customerList[position].nfd!!.size.toString()
        holder.tvAssignedTo = rowView.findViewById(R.id.tvAssignedTo)
        holder.tvAssignedTo!!.text = customerList[position].center

        return rowView
    }

    override fun getItem(position: Int): Any {
        return customerList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return customerList.size
    }

    private inner class Holder {
        internal var userName: TextView? = null
        internal var userPhone: TextView? = null
        internal var llRating: LinearLayout? = null
        internal var tvEnquiryStatus: TextView? = null
        internal var tvFollowUp: TextView? = null
        internal var tvAssignedTo: TextView? = null
    }

}