package com.gaiagps.iburn.util

import android.net.Uri
import com.gaiagps.iburn.database.*
import java.text.SimpleDateFormat
import java.util.*

object ShareUrlBuilder {
    
    private const val BASE_URL = "https://iburnapp.com"
    private val ISO_8601 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
    
    fun buildShareUrl(item: PlayaItem): Uri {
        val builder = Uri.Builder()
            .scheme("https")
            .authority("iburnapp.com")
        
        // Add path and uid based on type
        when (item) {
            is Art -> {
                builder.appendPath("art")
                    .appendPath("") // Creates /art/
                    .appendQueryParameter("uid", item.playaId)
            }
            is Camp -> {
                builder.appendPath("camp")
                    .appendPath("") // Creates /camp/
                    .appendQueryParameter("uid", item.playaId)
            }
            is Event -> {
                builder.appendPath("event")
                    .appendPath("") // Creates /event/
                    .appendQueryParameter("uid", item.playaId)
            }
            is UserPoi -> {
                // User POIs could be handled as pins
                return buildPinShareUrl(
                    latitude = item.latitude.toDouble(),
                    longitude = item.longitude.toDouble(),
                    title = item.name ?: "Custom Pin",
                    description = item.description,
                    address = item.playaAddress
                )
            }
        }
        
        // Add metadata as query parameters
        item.name?.let { builder.appendQueryParameter("title", it) }
        
        item.description?.take(100)?.let { desc ->
            builder.appendQueryParameter("desc", desc)
        }
        
        if (item.latitude != 0f) {
            builder.appendQueryParameter("lat", String.format(Locale.US, "%.6f", item.latitude))
        }
        
        if (item.longitude != 0f) {
            builder.appendQueryParameter("lng", String.format(Locale.US, "%.6f", item.longitude))
        }
        
        item.playaAddress?.let { addr ->
            builder.appendQueryParameter("addr", addr)
        }
        
        // Event-specific parameters
        if (item is Event) {

            item.startTime?.let { start ->
                builder.appendQueryParameter("start", ISO_8601.format(Date(start)))
            }
            item.endTime?.let { end ->
                builder.appendQueryParameter("end", ISO_8601.format(Date(end)))
            }

            item.campPlayaId?.let { campId ->
                builder.appendQueryParameter("host_id", campId)
                builder.appendQueryParameter("host_type", "camp")
            }

            item.artPlayaId?.let { artId ->
                builder.appendQueryParameter("host_id", artId)
                builder.appendQueryParameter("host_type", "art")
            }

            item.type?.let { type ->
                builder.appendQueryParameter("type", type)
            }
            
            if (item.allDay) {
                builder.appendQueryParameter("all_day", "true")
            }
        }
        
        // Add current year
        builder.appendQueryParameter("year", Calendar.getInstance().get(Calendar.YEAR).toString())
        
        return builder.build()
    }
    
    fun buildPinShareUrl(pin: MapPin): Uri {
        return buildPinShareUrl(
            latitude = pin.latitude.toDouble(),
            longitude = pin.longitude.toDouble(),
            title = pin.title,
            description = pin.description,
            address = pin.address,
            color = pin.color
        )
    }
    
    fun buildPinShareUrl(
        latitude: Double,
        longitude: Double,
        title: String,
        description: String? = null,
        address: String? = null,
        color: String? = null
    ): Uri {
        return Uri.Builder()
            .scheme("https")
            .authority("iburnapp.com")
            .appendPath("pin")
            .appendQueryParameter("lat", String.format(Locale.US, "%.6f", latitude))
            .appendQueryParameter("lng", String.format(Locale.US, "%.6f", longitude))
            .appendQueryParameter("title", title)
            .apply {
                description?.let { appendQueryParameter("desc", it) }
                address?.let { appendQueryParameter("addr", it) }
                color?.let { appendQueryParameter("color", it) }
            }
            .build()
    }
}