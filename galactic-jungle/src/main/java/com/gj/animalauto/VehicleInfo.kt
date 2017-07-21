package com.gj.animalauto

/**
 * Created by dbro on 7/19/17.
 */


fun getVehicleIconResId(vehicleId: Int, greyedIcon: Boolean = false): Int {
    return when (vehicleId) {
        1 -> if (greyedIcon) R.drawable.map_icon_lion_grey else R.drawable.map_icon_lion
        3 -> if (greyedIcon) R.drawable.map_icon_tiger_grey else R.drawable.map_icon_tiger
        4 -> if (greyedIcon) R.drawable.map_icon_zebra_grey else R.drawable.map_icon_zebra
        5 -> if (greyedIcon) R.drawable.map_icon_rhino_grey else R.drawable.map_icon_rhino
        else -> if (greyedIcon) R.drawable.map_icon_elephant_grey else R.drawable.map_icon_elephant
    }
}

fun getVehicleName(vehicleId: Int): String {
    when (vehicleId) {
        1 -> return "Lion"
        3 -> return "Tiger"
        4 -> return "Zebra"
        5 -> return "Rhino"
        else -> return "Elephant"
    }
}