/*
 * DirectInputCheck.kt
 * Implements the DirectInputCheck class
 * A DirectInputCheck checks if a station url is valid and returns station via a listener
 *
 * This file is part of
 * TRANSISTOR - Radio App for Android
 *
 * Copyright (c) 2015-23 - Y20K.org
 * Licensed under the MIT-License
 * http://opensource.org/licenses/MIT
 */


package org.y20k.transistor.search

import android.content.Context
import android.util.Log
import android.webkit.URLUtil
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.y20k.transistor.Keys
import org.y20k.transistor.R
import org.y20k.transistor.core.Station
import org.y20k.transistor.helpers.NetworkHelper
import java.net.URL
import java.util.GregorianCalendar
import java.util.Locale


/*
 * DirectInputCheck class
 */
class DirectInputCheck(private var directInputCheckListener: DirectInputCheckListener) {

    /* Interface used to send back station list for checked */
    interface DirectInputCheckListener {
        fun onDirectInputCheck(stationList: MutableList<Station>) {
        }
    }


    /* Define log tag */
    private val TAG: String = DirectInputCheck::class.java.simpleName


    /* Main class variables */
    private var lastCheckedAddress: String = String()


/*
TEST CASES - TODO REMOVE WHEN FINISHED
https://www.radioeins.de/live.m3u
https://www.radioeins.de/live.pls
http://radio.xaq.nl/m3u/speech.m3u
 */

    /* Searches station(s) on radio-browser.info */
    fun checkStationAddress(context: Context, query: String) {
        // check if valid URL
        if (URLUtil.isValidUrl(query)) {
            val stationList: MutableList<Station> = mutableListOf()
            CoroutineScope(IO).launch {
                val contentType: String = NetworkHelper.detectContentType(query).type.lowercase(Locale.getDefault())
                Log.e(TAG, "contentType => $contentType") // todo remove when finished

                // CASE: M3U playlist detected
                if (Keys.MIME_TYPES_M3U.contains(contentType)) {
                    val lines: List<String> = downloadPlaylist(query)
                    stationList.addAll(readM3uPlaylistContent(lines))
                    Log.e(TAG, "Downloaded M3U =>\n$stationList") // todo remove when finished
                }
                // CASE: PLS playlist detected
                else if (Keys.MIME_TYPES_PLS.contains(contentType)) {
                    val lines: List<String> = downloadPlaylist(query)
                    stationList.addAll(readPlsPlaylistContent(lines))
                    Log.e(TAG, "Downloaded PLS =>\n$stationList") // todo remove when finished
                }
                // CASE: stream address detected
                else if (Keys.MIME_TYPES_MPEG.contains(contentType) or
                    Keys.MIME_TYPES_OGG.contains(contentType) or
                    Keys.MIME_TYPES_AAC.contains(contentType) or
                    Keys.MIME_TYPES_HLS.contains(contentType)) {
                    // create station and add to collection
                    val station: Station = Station(name = query, streamUris = mutableListOf(query), streamContent = contentType, modificationDate = GregorianCalendar.getInstance().time)
                    if (lastCheckedAddress != query) {
                        stationList.add(station)
                    }
                    lastCheckedAddress = query
                }
                // CASE: invalid address
                else {
                    withContext(Main) {
                        Toast.makeText(context, R.string.toastmessage_station_not_valid, Toast.LENGTH_LONG).show()
                    }
                }
                // hand over station is to listener
                if (stationList.isNotEmpty()) {
                    withContext(Main) {
                        directInputCheckListener.onDirectInputCheck(stationList)
                    }
                }
            }
        }
    }


    /* Download playlist - up to 100 lines, with max. 200 characters */
    private fun downloadPlaylist(playlistUrlString: String): List<String> {
        val lines = mutableListOf<String>()
        val connection = URL(playlistUrlString).openConnection()
        val reader = connection.getInputStream().bufferedReader()
        reader.useLines { sequence ->
            sequence.take(100).forEach { line ->
                val trimmedLine = line.take(2000)
                lines.add(trimmedLine)
            }
        }
        return lines
    }


    /* Reads a m3u playlist and returns a list of stations */
    private fun readM3uPlaylistContent(playlist: List<String>): List<Station> {
        val stations: MutableList<Station> = mutableListOf()
        var name: String = String()
        var streamUri: String
        var contentType: String

        for (line in playlist) {
            // get name of station
            if (line.startsWith("#EXTINF:")) {
                val titleStartIndex = line.indexOf(',') + 1
                name = line.substring(titleStartIndex).trim()
            }
            // get stream uri and check mime type
            else if (line.isNotBlank() && !line.startsWith("#")) {
                streamUri = line.trim()
                // use the stream address as the name if no name is specified
                if (name.isEmpty()) {
                    name = streamUri
                }
                contentType = NetworkHelper.detectContentType(streamUri).type.lowercase(Locale.getDefault())
                // store station in list if mime type is supported
                if (contentType != Keys.MIME_TYPE_UNSUPPORTED) {
                    val station = Station(name = name, streamUris = mutableListOf(streamUri), streamContent = contentType, modificationDate = GregorianCalendar.getInstance().time)
                    stations.add(station)
                }
                // reset name for the next station - useful if playlist does not provide name(s)
                name = String()
            }
        }
        return stations
    }


    /* Reads a pls playlist and returns a list of stations */
    private fun readPlsPlaylistContent(playlist: List<String>): List<Station> {
        val stations: MutableList<Station> = mutableListOf()
        // todo implement
        return stations
    }

}