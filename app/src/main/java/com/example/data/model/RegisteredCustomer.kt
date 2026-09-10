package com.example.data.model

data class RegisteredCustomer(
    val id: String = "",
    val name: String = "ग्राहक",
    val phone: String = "",
    val address: String = "",
    val city: String = "जबलपुर",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val registeredAt: Long = System.currentTimeMillis(),
    val accountType: String = "नया खाता",
    val lastOrderNote: String = ""
) {
    val googleMapsUrl: String
        get() = if (latitude != 0.0 && longitude != 0.0) {
            "https://www.google.com/maps/search/?api=1&query=$latitude,$longitude"
        } else {
            ""
        }
}
