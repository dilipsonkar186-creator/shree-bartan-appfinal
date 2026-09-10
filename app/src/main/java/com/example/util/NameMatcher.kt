package com.example.util

import com.example.data.entity.CustomerEntity
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object NameMatcher {

    private val STOP_WORDS = setOf(
        "search", "search for", "find", "find customer", "look for", "look up", "show", "show me",
        "open", "open account", "open customer", "open khata", "check", "check customer", "get", "get details of",
        "tell me about", "who is", "where is", "details of", "detail of", "detail", "details",
        "info", "information", "report", "bill", "bills", "dues", "due", "balance", "pending",
        "unpaid", "paid", "payment", "khata", "ledger", "account", "profile", "contact", "phone",
        "number", "customer", "customers", "client", "person", "mr", "mrs", "shri", "shree",
        "ji", "the", "a", "an", "of", "for", "in", "to", "from", "with", "about", "please", "can",
        "you", "give", "me", "view", "display", "fetch",
        // Hindi filler and command words
        "खोलना", "खोलो", "खोल", "खोलिए", "खोलिये", "खोलकर", "खोल दो", "खोल दीजिए", "दिखाना", "दिखाओ", "दिखाइए",
        "दिखा", "दिखा दो", "दिखा दीजिए", "बताओ", "बताइए", "बता", "बताना", "बता दो", "निकालो", "निकालना", "निकाल",
        "ढूंढो", "ढूंढना", "ढूंढ", "खोजो", "खोजना", "खोज", "लाओ", "लाना", "देखना", "देना", "दो", "दीजिए", "करो", "करना",
        "का", "की", "के", "को", "में", "से", "पर", "ने", "भी", "वाला", "वाली", "वाले",
        "कस्टमर", "ग्राहक", "खाता", "लेजर", "हिसाब", "बिल", "पेमेंट", "बकाया", "डिटेल", "डिटेल्स",
        "रिपोर्ट", "इतिहास", "खरीद", "श्री", "जी", "का हिसाब", "का बिल", "की डिटेल", "का खाता",
        "कौन", "कौनसा", "कहा", "कहाँ", "कितना", "कितने", "बाकी", "नंबर",
        "kholo", "kholna", "khol", "dikhao", "dikhana", "dikha", "batao", "batana", "nikalo", "nikalna",
        "dhundo", "khojo", "dekho", "dekhna", "ka", "ki", "ke", "ko", "se", "par", "mein", "ne"
    )

    /**
     * Cleans an input query by removing common filler / command phrases and words (e.g. "उमा सेन का खाता खोलना" -> "उमा सेन")
     */
    fun cleanQuery(query: String): String {
        var raw = query.trim().lowercase(Locale.ROOT)
        if (raw.isBlank()) return ""

        val phrasesToRemove = listOf(
            "का खाता खोलना", "का खाता खोलो", "का खाता खोल दो", "का खाता खोलिए", "का खाता दिखाना", "का खाता दिखाओ",
            "का खाता बताओ", "का खाता निकालो", "का खाता देखना", "का खाता", "की खाता", "के खाते", "का बहीखाता",
            "का लेजर खोलना", "का लेजर खोलो", "का लेजर", "का हिसाब दिखाओ", "का हिसाब बताओ", "का हिसाब",
            "की उधारी", "का बकाया", "का बैलेंस", "का बिल", "की डिटेल", "का प्रोफाइल", "का रिकॉर्ड",
            "खाता खोलना", "खाता खोलो", "खाता खोल", "खाता दिखाओ", "खाता बताओ", "खाता निकालो", "खाता देखना",
            "का खाता खोलें", "खाता खोलें", "का लेजर दिखाएं", "का हिसाब दिखाएं",
            "open khata", "open account", "open ledger", "open customer", "show khata", "show account", "view khata",
            "ka khata kholna", "ka khata kholo", "ka khata", "ki detail", "ka hisab"
        )

        for (phrase in phrasesToRemove) {
            raw = raw.replace(phrase, " ")
        }

        val tokens = raw.split(Regex("[\\s,?.!/\\-_]+")).filter { token ->
            token.isNotBlank() && token !in STOP_WORDS
        }

        return if (tokens.isNotEmpty()) {
            tokens.joinToString(" ")
        } else {
            raw.trim()
        }
    }

    data class MatchResult(
        val customer: CustomerEntity,
        val score: Int,
        val matchType: String
    )

    fun findBestMatch(query: String, allCustomers: List<CustomerEntity>): MatchResult? {
        val matches = findMatchingCustomers(query, allCustomers)
        return matches.firstOrNull()
    }

    fun findAllMatches(query: String, allCustomers: List<CustomerEntity>): List<MatchResult> {
        return findMatchingCustomers(query, allCustomers)
    }

    /**
     * Filters and orders customers matching a query string (supporting Hindi speech, English names,
     * phone, book number, page number, phonetic variations, and typo tolerance).
     */
    fun filterCustomers(query: String, allCustomers: List<CustomerEntity>): List<CustomerEntity> {
        if (query.isBlank()) return allCustomers
        val matches = findMatchingCustomers(query, allCustomers)
        return matches.map { it.customer }
    }

    /**
     * Matches a query string against a list of customers and returns matches ordered by relevance score.
     */
    fun findMatchingCustomers(
        query: String,
        allCustomers: List<CustomerEntity>
    ): List<MatchResult> {
        val rawQuery = query.trim()
        if (rawQuery.isBlank()) return emptyList()

        val rawQueryLower = rawQuery.lowercase(Locale.ROOT)
        val cleanedQuery = cleanQuery(rawQueryLower)

        // Determine target clean query
        val targetQuery = if (cleanedQuery.isNotBlank() && cleanedQuery != rawQueryLower) cleanedQuery else rawQueryLower
        val queryTransliterations = getAllTransliterationVariants(targetQuery)
        val cleanedTransliterations = if (cleanedQuery.isNotBlank()) getAllTransliterationVariants(cleanedQuery) else emptyList()
        
        val allQueryForms = (listOf(targetQuery, cleanedQuery) + queryTransliterations + cleanedTransliterations)
            .map { it.trim() }
            .filter { it.isNotBlank() && it !in STOP_WORDS && it.length >= 2 }
            .distinct()

        val queryPhonetics = allQueryForms.map { toPhoneticKey(it) }.filter { it.isNotBlank() }.distinct()

        val queryTokens = (allQueryForms.flatMap { it.split(Regex("[\\s,?.!/\\-_]+")) })
            .map { it.trim() }
            .filter { it.length >= 2 && it !in STOP_WORDS }
            .distinct()

        val queryTokenPhonetics = queryTokens.map { toPhoneticKey(it) }.filter { it.isNotBlank() }.distinct()

        val results = mutableListOf<MatchResult>()

        val primaryQueryToken = queryTokens.firstOrNull() ?: cleanedQuery
        val queryFirstLetter = primaryQueryToken.firstOrNull()?.lowercaseChar()

        for (customer in allCustomers) {
            val cName = customer.name.trim()
            if (cName.isBlank()) continue

            val cNameLower = cName.lowercase(Locale.ROOT)
            val cNameTransliterations = getAllTransliterationVariants(cNameLower)
            val allCustomerNameForms = (listOf(cNameLower) + cNameTransliterations).distinct()
            val customerPhonetics = allCustomerNameForms.map { toPhoneticKey(it) }.filter { it.isNotBlank() }.distinct()

            val customerTokens = allCustomerNameForms.flatMap { it.split(Regex("[\\s,?.!/\\-_]+")) }
                .map { it.trim() }
                .filter { it.length >= 2 }
                .distinct()
            val customerTokenPhonetics = customerTokens.map { toPhoneticKey(it) }.filter { it.isNotBlank() }.distinct()

            val cPhone = customer.phone.trim().filter { it.isDigit() }
            val cBook = customer.bookNumber.trim().lowercase(Locale.ROOT)
            val cPage = customer.pageNumber.trim().lowercase(Locale.ROOT)
            val cAddress = customer.address.trim().lowercase(Locale.ROOT)

            val queryDigits = rawQuery.filter { it.isDigit() }

            var score = 0
            var matchType = "None"

            // 1. Phone number match
            if (queryDigits.length >= 3 && cPhone.isNotBlank() && (cPhone.contains(queryDigits) || queryDigits.contains(cPhone))) {
                score = 100
                matchType = "Phone Match"
            }
            // 2. Book / Page match
            else if (cBook.isNotBlank() && (cBook == rawQueryLower || cBook == cleanedQuery || rawQueryLower.contains("bk $cBook") || rawQueryLower.contains("book $cBook"))) {
                score = 98
                matchType = "Book Number Match"
            }
            else if (cPage.isNotBlank() && (cPage == rawQueryLower || cPage == cleanedQuery || rawQueryLower.contains("pg $cPage") || rawQueryLower.contains("page $cPage"))) {
                score = 98
                matchType = "Page Number Match"
            }
            // 3. Exact Full Name Match (raw or transliterated, e.g. "UMA SEN" or "उमा सेन")
            else if (allQueryForms.any { qForm -> allCustomerNameForms.any { cForm -> qForm == cForm } }) {
                score = 100
                matchType = "Exact Name Match"
            }
            // 4. Exact Phonetic Name Match
            else if (queryPhonetics.any { qp -> customerPhonetics.any { cp -> qp == cp } }) {
                score = 97
                matchType = "Phonetic Exact Match"
            }
            // 5. Customer name starts with full query
            else if (allQueryForms.any { qForm -> qForm.length >= 3 && allCustomerNameForms.any { cForm -> cForm.startsWith(qForm) } }) {
                score = 95
                matchType = "Prefix Match"
            }
            // 6. All query tokens matched in customer name (e.g. query "UMA SEN", customer is "UMA SEN" or "SEN UMA")
            else if (queryTokens.size >= 2 && queryTokens.all { qToken ->
                    val qp = toPhoneticKey(qToken)
                    customerTokens.any { cToken ->
                        cToken == qToken || (qp.isNotBlank() && toPhoneticKey(cToken) == qp)
                    }
                }) {
                score = 92
                matchType = "All Tokens Match"
            }
            // 7. Primary token exact match in customer tokens (e.g. query "UMA SEN", customer has "UMA", or query "UMA" customer is "UMA DEVI")
            else if (primaryQueryToken.length >= 3 && (customerTokens.any { it == primaryQueryToken } || 
                     queryTransliterations.any { qTrans -> customerTokens.any { it == qTrans } })) {
                score = 85
                matchType = "Primary Token Match"
            }
            // 8. Customer starts with primary token
            else if (primaryQueryToken.length >= 3 && allCustomerNameForms.any { cForm -> cForm.startsWith(primaryQueryToken) }) {
                score = 82
                matchType = "Primary Token Prefix Match"
            }
            // 9. Single token match if query has multiple tokens (e.g. query "UMA SEN", matches "SEN" token exactly)
            else if (queryTokens.size > 1 && queryTokens.any { qToken ->
                    qToken.length >= 3 && customerTokens.any { cToken -> cToken == qToken }
                }) {
                score = 75
                matchType = "Single Token Match"
            }
            // 10. Same letter / initial prefix match (only if query token length >= 3)
            else if (queryFirstLetter != null && primaryQueryToken.length >= 3 && cNameLower.startsWith(queryFirstLetter)) {
                if (cNameLower.startsWith(primaryQueryToken.take(2))) {
                    score = 65
                    matchType = "Initial 2-Letter Match"
                } else {
                    score = 55
                    matchType = "First Letter Match"
                }
            }
            // 11. Strict Typo Tolerance (only for words of length >= 4 with distance == 1)
            else if (primaryQueryToken.length >= 4) {
                val hasCloseTypo = customerTokens.any { cToken ->
                    cToken.length >= 4 && levenshteinDistance(cToken, primaryQueryToken) == 1
                } || customerTokenPhonetics.any { cp ->
                    val qp = toPhoneticKey(primaryQueryToken)
                    qp.length >= 4 && cp.length >= 4 && levenshteinDistance(cp, qp) == 1
                }
                if (hasCloseTypo) {
                    score = 60
                    matchType = "Typo Match"
                } else if (cAddress.isNotBlank() && (cAddress.contains(rawQueryLower) || cAddress.contains(cleanedQuery))) {
                    score = 52
                    matchType = "Address Match"
                }
            }

            if (score >= 50) {
                results.add(MatchResult(customer, score, matchType))
            }
        }

        // Sort primarily by score descending, then alphabetically by name for equal scores
        return results.sortedWith(
            compareByDescending<MatchResult> { it.score }
                .thenBy { it.customer.name.trim().lowercase(Locale.ROOT) }
        )
    }

    /**
     * Checks if text contains Devanagari script characters.
     */
    fun containsDevanagari(text: String): Boolean {
        return text.any { it.code in 0x0900..0x097F }
    }

    /**
     * Returns multiple transliteration variants of a string to account for Hindi-to-English
     * spelling variations (e.g. "सोनकर" -> ["sonkar", "sonkr", "shonkar"], "रितेश" -> ["ritesh", "reetesh"]).
     */
    fun getAllTransliterationVariants(input: String): List<String> {
        val trimmed = input.trim().lowercase(Locale.ROOT)
        if (trimmed.isBlank()) return emptyList()

        if (!containsDevanagari(trimmed)) {
            val variants = mutableListOf(trimmed)
            // Add common variations
            variants.add(trimmed.replace("sh", "s"))
            variants.add(trimmed.replace("s", "sh"))
            variants.add(trimmed.replace("ee", "i"))
            variants.add(trimmed.replace("i", "ee"))
            variants.add(trimmed.replace("oo", "u"))
            variants.add(trimmed.replace("u", "oo"))
            variants.add(trimmed.replace("v", "w"))
            variants.add(trimmed.replace("w", "v"))
            return variants.filter { it.isNotBlank() }.distinct()
        }

        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val wordVariantsList = words.map { getSingleWordVariants(it) }

        val combinedVariants = mutableListOf<String>()
        if (wordVariantsList.isNotEmpty()) {
            var currentCombos = wordVariantsList[0]
            for (wIndex in 1 until wordVariantsList.size) {
                val nextWordVariants = wordVariantsList[wIndex]
                val nextCombos = mutableListOf<String>()
                for (c in currentCombos) {
                    for (nw in nextWordVariants) {
                        nextCombos.add("$c $nw")
                    }
                }
                currentCombos = nextCombos.take(12)
            }
            combinedVariants.addAll(currentCombos)
        }

        for (wVars in wordVariantsList) {
            combinedVariants.addAll(wVars)
        }

        return combinedVariants.filter { it.isNotBlank() }.distinct()
    }

    private fun getSingleWordVariants(word: String): List<String> {
        if (!containsDevanagari(word)) return listOf(word)

        val v1 = transliterateDevanagariToLatin(word, mode = 1) // Natural Hindi
        val v2 = transliterateDevanagariToLatin(word, mode = 2) // Full schwa
        val v3 = transliterateDevanagariToLatin(word, mode = 0) // No schwa

        val baseVariants = listOf(v1, v2, v3).filter { it.isNotBlank() }.distinct()
        val extra = mutableListOf<String>()

        for (v in baseVariants) {
            extra.add(v.replace("sh", "s"))
            extra.add(v.replace("s", "sh"))
            extra.add(v.replace("ee", "i"))
            extra.add(v.replace("i", "ee"))
            extra.add(v.replace("oo", "u"))
            extra.add(v.replace("u", "oo"))
            extra.add(v.replace("v", "w"))
            extra.add(v.replace("w", "v"))
            extra.add(v.replace("j", "z"))
            extra.add(v.replace("z", "j"))
            extra.add(v.replace("e", "ee"))
            extra.add(v.replace("e", "i"))
        }

        return (baseVariants + extra).filter { it.isNotBlank() }.distinct()
    }

    /**
     * Converts Devanagari script strings to Latin / English representation.
     * mode 0 = no inherent schwa vowels
     * mode 1 = smart natural Hindi schwa rules (e.g. "सोनकर" -> "sonkar", "कमल" -> "kamal", "रितेश" -> "ritesh")
     * mode 2 = full Sanskrit schwa (e.g. "सोनकर" -> "sonakar")
     */
    fun transliterateDevanagariToLatin(input: String, mode: Int = 1): String {
        val sb = StringBuilder()
        var i = 0
        val len = input.length

        fun isConsonant(c: Char): Boolean {
            return c in '\u0915'..'\u0939' || c in '\u0958'..'\u095F'
        }

        fun isMatra(c: Char): Boolean {
            return c in '\u093E'..'\u094C' || c == '\u0962' || c == '\u0963' || c == '\u0943' || c == '\u0944'
        }

        fun isHalant(c: Char): Boolean {
            return c == '\u094D'
        }

        var prevHadMatraOrHalant = false

        while (i < len) {
            val ch = input[i]
            var transliterated = ""
            var wasConsonant = false

            when (ch) {
                'अ' -> transliterated = "a"
                'आ' -> transliterated = "aa"
                'इ' -> transliterated = "i"
                'ई' -> transliterated = "ee"
                'उ' -> transliterated = "u"
                'ऊ' -> transliterated = "oo"
                'ऋ' -> transliterated = "ri"
                'ए' -> transliterated = "e"
                'ऐ' -> transliterated = "ai"
                'ओ' -> transliterated = "o"
                'औ' -> transliterated = "au"
                'क', '\u0958' -> { transliterated = "k"; wasConsonant = true }
                'ख', '\u0959' -> { transliterated = "kh"; wasConsonant = true }
                'ग', '\u095A' -> { transliterated = "g"; wasConsonant = true }
                'घ' -> { transliterated = "gh"; wasConsonant = true }
                'ङ' -> { transliterated = "ng"; wasConsonant = true }
                'च' -> { transliterated = "ch"; wasConsonant = true }
                'छ' -> { transliterated = "chh"; wasConsonant = true }
                'ज', '\u095B' -> { transliterated = "j"; wasConsonant = true }
                'झ' -> { transliterated = "jh"; wasConsonant = true }
                'ञ' -> { transliterated = "ny"; wasConsonant = true }
                'ट' -> { transliterated = "t"; wasConsonant = true }
                'ठ' -> { transliterated = "th"; wasConsonant = true }
                'ड', '\u095C' -> { transliterated = "d"; wasConsonant = true }
                'ढ', '\u095D' -> { transliterated = "dh"; wasConsonant = true }
                'ण' -> { transliterated = "n"; wasConsonant = true }
                'त' -> { transliterated = "t"; wasConsonant = true }
                'थ' -> { transliterated = "th"; wasConsonant = true }
                'द' -> { transliterated = "d"; wasConsonant = true }
                'ध' -> { transliterated = "dh"; wasConsonant = true }
                'न' -> { transliterated = "n"; wasConsonant = true }
                'प' -> { transliterated = "p"; wasConsonant = true }
                'फ', '\u095E' -> { transliterated = "ph"; wasConsonant = true }
                'ब' -> { transliterated = "b"; wasConsonant = true }
                'भ' -> { transliterated = "bh"; wasConsonant = true }
                'म' -> { transliterated = "m"; wasConsonant = true }
                'य' -> { transliterated = "y"; wasConsonant = true }
                'र' -> { transliterated = "r"; wasConsonant = true }
                'ल' -> { transliterated = "l"; wasConsonant = true }
                'व' -> { transliterated = "v"; wasConsonant = true }
                'श', 'ष' -> { transliterated = "sh"; wasConsonant = true }
                'स' -> { transliterated = "s"; wasConsonant = true }
                'ह' -> { transliterated = "h"; wasConsonant = true }
                'ा' -> { transliterated = "a"; prevHadMatraOrHalant = true }
                'ि' -> { transliterated = "i"; prevHadMatraOrHalant = true }
                'ी' -> { transliterated = "i"; prevHadMatraOrHalant = true }
                'ु' -> { transliterated = "u"; prevHadMatraOrHalant = true }
                'ू' -> { transliterated = "u"; prevHadMatraOrHalant = true }
                'ृ' -> { transliterated = "ri"; prevHadMatraOrHalant = true }
                'े' -> { transliterated = "e"; prevHadMatraOrHalant = true }
                'ै' -> { transliterated = "ai"; prevHadMatraOrHalant = true }
                'ो' -> { transliterated = "o"; prevHadMatraOrHalant = true }
                'ौ' -> { transliterated = "au"; prevHadMatraOrHalant = true }
                'ं', 'ँ' -> transliterated = "n"
                'ः' -> transliterated = "h"
                '्' -> { prevHadMatraOrHalant = true /* Halant */ }
                else -> {
                    if (ch.code !in 0x0900..0x097F) {
                        transliterated = ch.toString()
                    }
                    prevHadMatraOrHalant = false
                }
            }

            sb.append(transliterated)

            if (wasConsonant && mode > 0) {
                val nextChar = if (i + 1 < len) input[i + 1] else null
                val nextNextChar = if (i + 2 < len) input[i + 2] else null

                if (nextChar != null && isConsonant(nextChar)) {
                    if (mode == 2) {
                        sb.append("a")
                    } else {
                        // Natural Hindi: if previous consonant had a matra (like 'सो' -> 'न' -> 'कर'), 'न' doesn't get 'a'
                        if (prevHadMatraOrHalant) {
                            // Suppress 'a' after 'न' in "सोनकर"
                        } else if (nextNextChar != null && isConsonant(nextNextChar)) {
                            // In "कमल" -> 'क' followed by 'म' followed by 'ल' -> 'क' gets 'a'
                            sb.append("a")
                        } else {
                            // Followed by last consonant in word (like 'क' before 'र' in "सोनकर" or 'म' before 'ल' in "कमल")
                            sb.append("a")
                        }
                    }
                }
                prevHadMatraOrHalant = false
            }

            i++
        }
        return sb.toString().trim()
    }

    /**
     * Converts an English or romanized Indian name to a normalized phonetic key.
     * Helps match "Ritesh" <-> "Retesh", "Sonkar" <-> "Shonkar" <-> "Sonker", "Dilip" <-> "Dileep".
     */
    fun toPhoneticKey(input: String): String {
        var s = input.trim().lowercase(Locale.ROOT)
        if (s.isBlank()) return ""

        // If in Devanagari, convert to latin first
        if (containsDevanagari(s)) {
            s = transliterateDevanagariToLatin(s, mode = 1)
        }

        // 1. Common vowel normalizations
        s = s.replace("ee", "i")
            .replace("ea", "i")
            .replace("oo", "u")
            .replace("ou", "u")
            .replace("ai", "i")
            .replace("ay", "i")
            .replace("ey", "i")
            .replace("ie", "i")
            .replace("aa", "a")
            .replace("e", "i")
            .replace("y", "i")
            .replace("o", "u")

        // 2. Consonant normalizations
        s = s.replace("ph", "f")
            .replace("bh", "b")
            .replace("dh", "d")
            .replace("th", "t")
            .replace("gh", "g")
            .replace("kh", "k")
            .replace("jh", "j")
            .replace("chh", "ch")
            .replace("sh", "s")
            .replace("w", "v")
            .replace("z", "j")
            .replace("c", "k")
            .replace("q", "k")
            .replace("x", "ks")

        // 3. Remove double letters (e.g. "sonkkar" -> "sonkar")
        val deduplicated = StringBuilder()
        var lastChar = ' '
        for (ch in s) {
            if (ch != lastChar || ch.isDigit()) {
                deduplicated.append(ch)
                lastChar = ch
            }
        }

        return deduplicated.toString().trim()
    }

    fun calculateSimilarity(s1: String, s2: String): Int {
        val t1 = s1.trim().lowercase(Locale.ROOT)
        val t2 = s2.trim().lowercase(Locale.ROOT)
        if (t1.isEmpty() || t2.isEmpty()) return 0
        if (t1 == t2) return 100

        val p1 = toPhoneticKey(t1)
        val p2 = toPhoneticKey(t2)
        if (p1.isNotEmpty() && p1 == p2) return 98
        if (p1.contains(p2) || p2.contains(p1)) return 90

        val dist = levenshteinDistance(p1, p2)
        val maxLen = max(p1.length, p2.length)
        if (maxLen == 0) return 0
        val sim = ((1.0 - (dist.toDouble() / maxLen)) * 100).toInt()
        return sim.coerceIn(0, 100)
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    dp[i - 1][j] + 1,
                    min(dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
                )
            }
        }
        return dp[s1.length][s2.length]
    }
}

