package com.gj.animalauto.message

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.text.TextUtils
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.gj.animalauto.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by dbro on 7/23/17.
 */

val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

data class MessageLog(val message: GjMessage, val time: Long)

class MessageAdapter(ctx: Context, cars: List<MessageLog>) : ArrayAdapter<MessageLog>(ctx, android.R.layout.simple_list_item_1, cars) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val messageLog = getItem(position)
        // Check if an existing view is being reused, otherwise inflate the view
        var view = convertView
        if (view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.simple_list_item, parent, false)
        }
        val date = dateFormat.format(Date(messageLog.time))

        val label = "$date : ${messageLog.message}"
        (view as TextView).text = label
        return view
    }
}