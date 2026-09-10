package com.example.util

object PhoneNumberUtils {
    const val INDIA_COUNTRY_CODE = "+91"
    const val DEFAULT_PHONE_PREFIX = "+91 "

    /**
     * Formats user input while typing to ensure India country code (+91) is automatically included
     * and readily available for direct mobile number entry.
     */
    fun formatInputPhone(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return DEFAULT_PHONE_PREFIX
        }

        // If input starts with +91 (with or without space)
        if (trimmed.startsWith("+91")) {
            val rest = trimmed.removePrefix("+91").trimStart()
            val filtered = rest.filter { it.isDigit() || it == ' ' || it == '-' }
            return if (filtered.isEmpty()) {
                DEFAULT_PHONE_PREFIX
            } else {
                "$DEFAULT_PHONE_PREFIX$filtered"
            }
        }

        // If user typed another international code starting with '+'
        if (trimmed.startsWith("+")) {
            return trimmed
        }

        // Strip leading trunk zero if user enters 09876543210
        var clean = trimmed
        if (clean.startsWith("0") && clean.length > 1) {
            clean = clean.substring(1).trimStart()
        }

        // If user typed 91 without '+' (e.g. 919876543210)
        if (clean.startsWith("91") && clean.length > 10) {
            clean = clean.substring(2).trimStart()
        }

        val filtered = clean.filter { it.isDigit() || it == ' ' || it == '-' }
        return "$DEFAULT_PHONE_PREFIX$filtered"
    }

    /**
     * Normalizes the phone number before saving to database.
     * If user didn't enter any actual subscriber digits (just "+91" or blank), returns empty string "".
     * Otherwise formats nicely with "+91 XXXXXXXXXX".
     */
    fun normalizeForStorage(phone: String): String {
        val trimmed = phone.trim()
        if (trimmed.isEmpty() || trimmed == "+91" || trimmed == "+91 " || trimmed == "+") {
            return ""
        }

        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.isEmpty() || digitsOnly == "91") {
            return ""
        }

        // If starts with +91
        if (trimmed.startsWith("+91")) {
            val subscriber = trimmed.removePrefix("+91").trim()
            val subDigits = subscriber.filter { it.isDigit() }
            return when {
                subDigits.isEmpty() -> ""
                subDigits.length == 10 -> "+91 ${subDigits.substring(0, 5)} ${subDigits.substring(5)}"
                else -> "+91 $subscriber"
            }
        }

        // Other international country codes
        if (trimmed.startsWith("+")) {
            return trimmed
        }

        var subDigits = digitsOnly
        if (subDigits.startsWith("0") && subDigits.length > 10) {
            subDigits = subDigits.substring(1)
        }
        if (subDigits.startsWith("91") && subDigits.length > 10) {
            subDigits = subDigits.substring(2)
        }

        return when {
            subDigits.isEmpty() -> ""
            subDigits.length == 10 -> "+91 ${subDigits.substring(0, 5)} ${subDigits.substring(5)}"
            else -> "+91 $subDigits"
        }
    }
}
