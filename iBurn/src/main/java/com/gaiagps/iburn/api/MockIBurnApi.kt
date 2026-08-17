package com.gaiagps.iburn.api

import android.content.Context
import com.gaiagps.iburn.api.response.Art
import com.gaiagps.iburn.api.response.Camp
import com.gaiagps.iburn.api.response.DataManifest
import com.gaiagps.iburn.api.response.Event
import com.gaiagps.iburn.api.response.MutantVehicle
import com.gaiagps.iburn.api.response.ResourceManifest
import com.gaiagps.iburn.api.typeadapter.PlayaDateTypeAdapter
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import okio.Buffer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

open class MockIBurnApi(private val context: Context) : IBurnApi {
    private val gson: Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .registerTypeAdapter(Date::class.java, PlayaDateTypeAdapter())
        .create()

    private val manifest: DataManifest = DataManifest(
        ResourceManifest("art.json", Date()),
        ResourceManifest("camp.json", Date()),
        ResourceManifest("event.json", Date())
    )

    override suspend fun getDataManifest(): DataManifest = manifest

    override suspend fun getCamps(): List<Camp> = withContext(Dispatchers.IO) {
        context.assets.open("json/camp.json").use { input ->
            BufferedReader(InputStreamReader(input)).use { reader ->
                gson.fromJson(reader, Array<Camp>::class.java).toList()
            }
        }
    }

    override suspend fun getArt(): List<Art> = withContext(Dispatchers.IO) {
        context.assets.open("json/art.json").use { input ->
            BufferedReader(InputStreamReader(input)).use { reader ->
                gson.fromJson(reader, Array<Art>::class.java).toList()
            }
        }
    }

    override suspend fun getEvents(): List<Event> = withContext(Dispatchers.IO) {
        context.assets.open("json/event.json").use { input ->
            BufferedReader(InputStreamReader(input)).use { reader ->
                gson.fromJson(reader, Array<Event>::class.java).toList()
            }
        }
    }

    override suspend fun getMutantVehicles(): List<MutantVehicle> = withContext(Dispatchers.IO) {
        context.assets.open("json/mv.json").use { input ->
            BufferedReader(InputStreamReader(input)).use { reader ->
                gson.fromJson(reader, Array<MutantVehicle>::class.java).toList()
            }
        }
    }

    override suspend fun getTiles(): ResponseBody {
        // Unused in tests; return empty body
        val buffer = Buffer()
        return ResponseBody.create(null, buffer.size, buffer)
    }
}
