package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.LocationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Interactive OpenStreetMap (OSM) Location Picker dialog.
 * 100% Free & Open Source - Requires ZERO Google Maps API Key or billing.
 * Allows panning, zooming, and dropping a pinpoint pin on the customer's exact house/shop.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LocationPickerMapDialog(
    initialLatitude: Double? = null,
    initialLongitude: Double? = null,
    initialAddress: String = "",
    onDismiss: () -> Unit,
    onLocationSelected: (latitude: Double, longitude: Double, address: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Default to central India coordinates if none provided
    val defaultLat = 23.1815
    val defaultLng = 79.9864

    val startLat = if (initialLatitude != null && initialLatitude != 0.0) initialLatitude else defaultLat
    val startLng = if (initialLongitude != null && initialLongitude != 0.0) initialLongitude else defaultLng

    var selectedLat by remember { mutableStateOf(startLat) }
    var selectedLng by remember { mutableStateOf(startLng) }
    var resolvedAddress by remember { mutableStateOf(initialAddress) }
    var searchQuery by remember { mutableStateOf("") }

    var isFetchingGps by remember { mutableStateOf(false) }
    var isResolvingAddress by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var isMapLoading by remember { mutableStateOf(true) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var geocodeJob by remember { mutableStateOf<Job?>(null) }

    // Reverse geocode with a debounce when map pin stops moving
    fun debounceReverseGeocode(lat: Double, lng: Double) {
        selectedLat = lat
        selectedLng = lng
        geocodeJob?.cancel()
        geocodeJob = scope.launch {
            delay(400) // Debounce while dragging
            isResolvingAddress = true
            val address = LocationUtils.reverseGeocode(context, lat, lng)
            resolvedAddress = address
            isResolvingAddress = false
        }
    }

    // Fly map to coordinates
    fun flyMapTo(lat: Double, lng: Double, zoom: Int = 17) {
        selectedLat = lat
        selectedLng = lng
        webViewRef?.post {
            webViewRef?.evaluateJavascript("flyTo($lat, $lng, $zoom);", null)
        }
        debounceReverseGeocode(lat, lng)
    }

    // Fetch live hardware GPS
    fun fetchCurrentGps() {
        isFetchingGps = true
        LocationUtils.fetchHighAccuracyLocation(context) { lat, lng, addr, err ->
            isFetchingGps = false
            if (lat != null && lng != null) {
                flyMapTo(lat, lng, 18)
                if (!addr.isNullOrBlank()) {
                    resolvedAddress = addr
                }
                Toast.makeText(context, "सटीक GPS लोकेशन मिल गई! 🎯", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, err ?: "GPS लोकेशन नहीं मिल सकी। कृपया फोन का GPS ऑन करें।", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Search locality / place name
    fun searchPlace(query: String) {
        if (query.isBlank()) return
        keyboardController?.hide()
        isSearching = true
        scope.launch {
            val result = LocationUtils.searchLocationName(context, query.trim())
            isSearching = false
            if (result != null) {
                flyMapTo(result.first, result.second, 17)
                Toast.makeText(context, "स्थान मिल गया!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "स्थान नहीं मिला। कृपया दूसरा नाम या शहर लिखकर देखें।", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // JavaScript interface to receive live coordinates when user pans the map
    val jsBridge = remember {
        object {
            @JavascriptInterface
            fun onLocationMoved(lat: Double, lng: Double) {
                scope.launch(Dispatchers.Main) {
                    debounceReverseGeocode(lat, lng)
                }
            }
        }
    }

    // Prepare Leaflet OpenStreetMap HTML with ultra-reliable multi-CDN and multi-tile fallbacks
    val mapHtml = remember(startLat, startLng) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <style>
                html, body, #map {
                    width: 100%;
                    height: 100%;
                    margin: 0;
                    padding: 0;
                    background: #f0ede5;
                    overflow: hidden;
                }
                .leaflet-control-attribution {
                    font-size: 9px !important;
                    background: rgba(255,255,255,0.7) !important;
                }
            </style>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <script>
                if (typeof L === 'undefined') {
                    document.write('<script src="https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js"><\\/script>');
                }
            </script>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = null;
                function initMap() {
                    if (typeof L === 'undefined') {
                        setTimeout(initMap, 200);
                        return;
                    }
                    if (map !== null) {
                        map.invalidateSize();
                        return;
                    }

                    try {
                        map = L.map('map', {
                            zoomControl: false,
                            attributionControl: true,
                            fadeAnimation: false
                        }).setView([$startLat, $startLng], 16);

                        // High-speed OSM Tile provider with CartoDB fallback
                        var osmLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                            maxZoom: 19,
                            subdomains: ['a', 'b', 'c'],
                            attribution: '&copy; OpenStreetMap'
                        });

                        var cartoLayer = L.tileLayer('https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png', {
                            maxZoom: 19,
                            subdomains: ['a', 'b', 'c', 'd'],
                            attribution: '&copy; CartoDB'
                        });

                        // Add primary layer, switch to fallback if tiles fail
                        osmLayer.addTo(map);

                        osmLayer.on('tileerror', function() {
                            if (!map.hasLayer(cartoLayer)) {
                                cartoLayer.addTo(map);
                            }
                        });

                        // Recalculate dimensions so map fills the screen properly
                        setTimeout(function() { if (map) map.invalidateSize(); }, 150);
                        setTimeout(function() { if (map) map.invalidateSize(); }, 500);
                        setTimeout(function() { if (map) map.invalidateSize(); }, 1200);

                        map.on('moveend', function() {
                            var c = map.getCenter();
                            if (window.AndroidBridge && window.AndroidBridge.onLocationMoved) {
                                window.AndroidBridge.onLocationMoved(c.lat, c.lng);
                            }
                        });

                        window.flyTo = function(lat, lng, zoom) {
                            if (map) {
                                map.flyTo([lat, lng], zoom || 17, { animate: true, duration: 0.8 });
                                setTimeout(function() { map.invalidateSize(); }, 300);
                            }
                        };

                        window.zoomIn = function() { if (map) map.zoomIn(); };
                        window.zoomOut = function() { if (map) map.zoomOut(); };
                    } catch (e) {
                        console.error("Map init error:", e);
                    }
                }

                if (document.readyState === 'complete' || document.readyState === 'interactive') {
                    initMap();
                } else {
                    window.addEventListener('DOMContentLoaded', initMap);
                }
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    // Auto-detect GPS on first launch if no initial coordinates
    LaunchedEffect(Unit) {
        if (initialLatitude == null || initialLatitude == 0.0) {
            fetchCurrentGps()
        } else if (initialAddress.isBlank()) {
            debounceReverseGeocode(startLat, startLng)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "सटीक लोकेशन चुनें (Map)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = Color(0xFFE3F2FD),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "OpenStreetMap • 0 API Key",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF1565C0),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar for City / Colony / Landmark
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("शहर, इलाका या दुकान का नाम खोजें...", fontSize = 13.sp) },
                    leadingIcon = {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { searchPlace(searchQuery) }),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Interactive Map View with Overlaid Center Pin
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                ) {
                    // OpenStreetMap WebView
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewRef = this
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                settings.cacheMode = WebSettings.LOAD_DEFAULT
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                settings.userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                                addJavascriptInterface(jsBridge, "AndroidBridge")

                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        isMapLoading = false
                                        view?.postDelayed({
                                            view.evaluateJavascript("if (window.initMap) { window.initMap(); }", null)
                                        }, 200)
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        isMapLoading = false
                                    }
                                }

                                loadDataWithBaseURL("https://www.openstreetmap.org/", mapHtml, "text/html", "UTF-8", null)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Map Loading Spinner
                    if (isMapLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("नक्शा लोड हो रहा है...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Centered Floating Pin (Anchor at bottom center of pin)
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-18).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // High Visibility Red Location Pin
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Map Center Pin",
                                tint = Color(0xFFD32F2F),
                                modifier = Modifier
                                    .size(42.dp)
                                    .shadow(6.dp, CircleShape)
                            )
                            // Anchor Shadow Dot on the exact street spot
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            )
                        }
                    }

                    // Floating Controls on Map
                    // 1. My Current GPS Location Button (Top Right)
                    FilledTonalIconButton(
                        onClick = { fetchCurrentGps() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                            .shadow(4.dp, CircleShape),
                        colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isFetchingGps) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = "Current Location")
                        }
                    }

                    // 2. Zoom In & Zoom Out Buttons (Right Center)
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = { webViewRef?.evaluateJavascript("zoomIn();", null) },
                            modifier = Modifier
                                .size(36.dp)
                                .shadow(2.dp, CircleShape),
                            colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", modifier = Modifier.size(18.dp))
                        }

                        FilledTonalIconButton(
                            onClick = { webViewRef?.evaluateJavascript("zoomOut();", null) },
                            modifier = Modifier
                                .size(36.dp)
                                .shadow(2.dp, CircleShape),
                            colors = androidx.compose.material3.IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(18.dp))
                        }
                    }

                    // 3. Live Coordinates Pill (Bottom Left)
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.5f", selectedLat)}, ${String.format(Locale.US, "%.5f", selectedLng)}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // 4. Instructions Pill (Top Center)
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = "नक्शे को हिलाकर लाल पिन सही जगह रखें",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Address & Google Maps Verification Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📍 चुनी गई जगह का पता:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Open in Google Maps Native App for instant test
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable {
                                        LocationUtils.openGoogleMaps(
                                            context,
                                            selectedLat,
                                            selectedLng,
                                            resolvedAddress
                                        )
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Google Maps में चेक करें",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (isResolvingAddress) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp)
                                Text("पता लोड हो रहा है...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            Text(
                                text = resolvedAddress.ifBlank { "स्थान का पता प्राप्त हो रहा है..." },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("रद्द करें (Cancel)")
                    }

                    Button(
                        onClick = {
                            val finalAddr = resolvedAddress.ifBlank {
                                "Location (${String.format(Locale.US, "%.5f", selectedLat)}, ${String.format(Locale.US, "%.5f", selectedLng)})"
                            }
                            onLocationSelected(selectedLat, selectedLng, finalAddr)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("confirm_map_location_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("✓ यही लोकेशन सेट करें", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
