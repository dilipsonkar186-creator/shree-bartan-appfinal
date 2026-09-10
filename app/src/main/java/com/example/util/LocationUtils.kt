package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object LocationUtils {

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Fetches current GPS location with high accuracy (enableHighAccuracy: true)
     * and fresh coordinates (maximumAge: 0) using Priority.PRIORITY_HIGH_ACCURACY.
     */
    @SuppressLint("MissingPermission")
    fun fetchHighAccuracyLocation(
        context: Context,
        onResult: (latitude: Double?, longitude: Double?, address: String?, errorMessage: String?) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onResult(null, null, null, "Location permission not granted")
            return
        }

        try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val cancellationTokenSource = CancellationTokenSource()

            // Priority.PRIORITY_HIGH_ACCURACY forces fresh GPS fix with maximum accuracy (maximumAge: 0)
            fusedClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude
                    reverseGeocodeAsync(context, lat, lng) { addr ->
                        onResult(lat, lng, addr, null)
                    }
                } else {
                    // Fallback to lastLocation if current location returned null
                    fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            val lat = lastLoc.latitude
                            val lng = lastLoc.longitude
                            reverseGeocodeAsync(context, lat, lng) { addr ->
                                onResult(lat, lng, addr, null)
                            }
                        } else {
                            onResult(null, null, null, "Location unavailable. Please ensure GPS is enabled.")
                        }
                    }.addOnFailureListener { e ->
                        onResult(null, null, null, e.message ?: "Failed to retrieve location")
                    }
                }
            }.addOnFailureListener { e ->
                onResult(null, null, null, e.message ?: "GPS detection failed")
            }
        } catch (e: Exception) {
            onResult(null, null, null, e.message ?: "Error requesting location")
        }
    }

    /**
     * Reverse geocodes coordinates to a human-readable address.
     */
    fun reverseGeocodeAsync(
        context: Context,
        latitude: Double,
        longitude: Double,
        onComplete: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val resolvedAddress = reverseGeocode(context, latitude, longitude)
            withContext(Dispatchers.Main) {
                onComplete(resolvedAddress)
            }
        }
    }

    suspend fun reverseGeocode(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String = withContext(Dispatchers.IO) {
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val formatted = formatAddress(addr)
                    if (formatted.isNotBlank()) {
                        return@withContext formatted
                    }
                }
            }
        } catch (e: Exception) {
            // Log or ignore, fallback to HTTP Nominatim or coordinates
        }

        // Fallback: OpenStreetMap Nominatim reverse geocoding
        try {
            val urlString = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude&zoom=18&addressdetails=1"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "CustomerFoldersApp/1.0")
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                val json = JSONObject(response)
                val displayName = json.optString("display_name", "")
                if (displayName.isNotBlank()) {
                    return@withContext displayName
                }
            }
        } catch (e: Exception) {
            // Ignore network errors
        }

        return@withContext "Location (${String.format(Locale.US, "%.5f", latitude)}, ${String.format(Locale.US, "%.5f", longitude)})"
    }

    private fun formatAddress(addr: Address): String {
        val lines = (0..addr.maxAddressLineIndex).mapNotNull { addr.getAddressLine(it) }
        if (lines.isNotEmpty()) {
            return lines.joinToString(", ")
        }
        val parts = mutableListOf<String>()
        addr.featureName?.let { if (it != addr.thoroughfare) parts.add(it) }
        addr.subLocality?.let { parts.add(it) }
        addr.locality?.let { parts.add(it) }
        addr.subAdminArea?.let { parts.add(it) }
        addr.adminArea?.let { parts.add(it) }
        addr.postalCode?.let { parts.add(it) }
        return parts.distinct().joinToString(", ")
    }

    suspend fun searchLocationName(context: Context, query: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext null
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(query, 1)
                if (!results.isNullOrEmpty()) {
                    return@withContext Pair(results[0].latitude, results[0].longitude)
                }
            }
        } catch (_: Exception) {}

        // Fallback: OpenStreetMap Nominatim search
        try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val urlString = "https://nominatim.openstreetmap.org/search?format=json&q=$encodedQuery&limit=1"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "CustomerFoldersApp/1.0")
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            if (connection.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                val jsonArray = org.json.JSONArray(response)
                if (jsonArray.length() > 0) {
                    val obj = jsonArray.getJSONObject(0)
                    val lat = obj.optDouble("lat", 0.0)
                    val lon = obj.optDouble("lon", 0.0)
                    if (lat != 0.0 && lon != 0.0) {
                        return@withContext Pair(lat, lon)
                    }
                }
            }
        } catch (_: Exception) {}

        return@withContext null
    }

    /**
     * Opens coordinates in Google Maps app or browser with high precision pin-drop and navigation support.
     */
    fun openGoogleMaps(context: Context, latitude: Double, longitude: Double, label: String = "") {
        val encodedLabel = android.net.Uri.encode(label.ifBlank { "Customer Location" })
        
        // 1. Try Google Maps Native App (Direct Package Intent with high-accuracy query pin)
        try {
            val appUri = android.net.Uri.parse("google.navigation:q=$latitude,$longitude&mode=d")
            val navIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, appUri).apply {
                setPackage("com.google.android.apps.maps")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (navIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(navIntent)
                return
            }
        } catch (_: Exception) { }

        // 2. Try geo: URI with pin-drop coordinates and label
        try {
            val geoUri = android.net.Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)&z=18")
            val mapIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, geoUri).apply {
                setPackage("com.google.android.apps.maps")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
                return
            }
        } catch (_: Exception) { }

        // 3. Try generic geo URI (open in any installed map app like Petal/Apple/Here)
        try {
            val geoUri = android.net.Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude($encodedLabel)&z=18")
            val genericIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, geoUri).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            if (genericIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(genericIntent)
                return
            }
        } catch (_: Exception) { }

        // 4. Universal 100% Reliable Web & Browser Fallback (Google Maps Web with exact pin)
        try {
            val webUri = android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
            val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, webUri).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(webIntent)
        } catch (e: Exception) {
            android.widget.Toast.makeText(context, "Could not open map: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
