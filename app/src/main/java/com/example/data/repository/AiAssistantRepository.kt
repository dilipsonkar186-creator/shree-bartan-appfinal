package com.example.data.repository

import com.example.data.entity.CustomerEntity
import com.example.data.entity.TransactionEntity
import com.example.data.model.*
import com.example.data.remote.GeminiAssistantService
import com.example.data.remote.GeminiParsedResponse
import com.example.util.NameMatcher
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AiAssistantRepository(
    private val folderRepository: FolderRepository,
    private val geminiService: GeminiAssistantService = GeminiAssistantService()
) {

    suspend fun processQuery(query: String): AiAssistantResult {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            return AiAssistantResult(
                queryText = query,
                responseText = "कृपया कोई नाम, आज की पेमेंट या फोल्डर/एरिया का हिसाब पूछें (जैसे: 'आज कितनी पेमेंट आई?', 'घाना का कलेक्शन', 'अंजू खोजो')",
                resultType = AiResultType.GENERAL_TEXT
            )
        }

        val allCustomers = folderRepository.allCustomers.first().filter { !it.isDeleted }
        val customerNames = allCustomers.map { it.name }
        val lowerQuery = trimmedQuery.lowercase(Locale.ROOT)

        // -----------------------------------------------------------------------------------------
        // FAST-PATH 1: Instant Local Intent Detection (Today's New Customers, Today's Payments, Week Collection, Month Summary, Area/Folder Collection, Shop Summary, High Dues, Unpaid Customers)
        // Resolves in < 5 milliseconds locally on-device without network latency!
        // -----------------------------------------------------------------------------------------

        // 0. Today's New Customers Intent (आज कितने नए कस्टमर/ग्राहक जुड़े, new customers today, etc.)
        val isTodayNewCustomerQuery = (lowerQuery.contains("नए") || lowerQuery.contains("नया") || lowerQuery.contains("new") || lowerQuery.contains("naye")) &&
                (lowerQuery.contains("कस्टमर") || lowerQuery.contains("ग्राहक") || lowerQuery.contains("customer") || lowerQuery.contains("खाते") || lowerQuery.contains("अकाउंट")) &&
                (lowerQuery.contains("आज") || lowerQuery.contains("today") || lowerQuery.contains("aaj") || lowerQuery.contains("जुड़े") || lowerQuery.contains("बने") || lowerQuery.contains("ऐड") || lowerQuery.contains("add"))
        if (isTodayNewCustomerQuery) {
            return fetchTodaysNewCustomersResult(trimmedQuery)
        }

        // 1. Today's Payments & Daily Recovery Intent (वसूली / कितने पैसे आए / आज का कलेक्शन)
        val isTodayPaymentQuery = (lowerQuery.contains("आज") || lowerQuery.contains("today") || lowerQuery.contains("aaj") ||
                lowerQuery.contains("डेली") || lowerQuery.contains("daily") || lowerQuery.contains("दिन का")) &&
                (lowerQuery.contains("वसूली") || lowerQuery.contains("वसूल") || lowerQuery.contains("रिकवरी") ||
                 lowerQuery.contains("recovery") || lowerQuery.contains("पेमेंट") || lowerQuery.contains("payment") ||
                 lowerQuery.contains("कलेक्शन") || lowerQuery.contains("collection") || lowerQuery.contains("पैसे") ||
                 lowerQuery.contains("कितनी") || lowerQuery.contains("कितना") || lowerQuery.contains("किसने") ||
                 lowerQuery.contains("किस-किस") || lowerQuery.contains("who paid") || lowerQuery.contains("received") ||
                 lowerQuery.contains("आया") || lowerQuery.contains("आई") || lowerQuery.contains("आए") ||
                 lowerQuery.contains("जमा") || lowerQuery.contains("मिले"))
        if (isTodayPaymentQuery) {
            return fetchTodayPaymentsResult(trimmedQuery)
        }

        // 2. Weekly Recovery Intent (हफ्ते का / एक-आधा हफ्ते का / साप्ताहिक कलेक्शन)
        val isWeeklyQuery = lowerQuery.contains("हफ्ते") || lowerQuery.contains("हफ्ता") || lowerQuery.contains("week") ||
                lowerQuery.contains("weekly") || lowerQuery.contains("7 दिन") || lowerQuery.contains("7 days") ||
                lowerQuery.contains("साप्ताहिक") || lowerQuery.contains("सात दिन")
        if (isWeeklyQuery) {
            return fetchWeeklyCollectionResult(trimmedQuery)
        }

        // 3. This Month / Custom Month Full Summary Intent (महीने का / मंथली वाइज / महीने की वसूली)
        val monthNames = listOf("जनवरी", "फ़रवरी", "फरवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर")
        val isSpecificMonthMentioned = monthNames.any { lowerQuery.contains(it) }
        val isMonthSummaryQuery = isSpecificMonthMentioned || (lowerQuery.contains("इस महीने") || lowerQuery.contains("इस माह") ||
                lowerQuery.contains("this month") || lowerQuery.contains("महीने का") || lowerQuery.contains("मंथली") ||
                lowerQuery.contains("monthly") || lowerQuery.contains("महीने की") || lowerQuery.contains("माह का")) &&
                (lowerQuery.contains("हिसाब") || lowerQuery.contains("summary") || lowerQuery.contains("कलेक्शन") ||
                 lowerQuery.contains("वसूली") || lowerQuery.contains("report") || lowerQuery.contains("पूरा") ||
                 lowerQuery.contains("overview") || lowerQuery.contains("total") || lowerQuery.contains("बताओ"))
        if (isMonthSummaryQuery) {
            val detectedMonth = if (isSpecificMonthMentioned) {
                val foundIndex = monthNames.indexOfFirst { lowerQuery.contains(it) }
                if (foundIndex >= 2) foundIndex else if (foundIndex == 0) 1 else 2 // January=1, February=2...
            } else {
                Calendar.getInstance().get(Calendar.MONTH) + 1
            }
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            return fetchMonthlySummaryResult(trimmedQuery, detectedMonth, currentYear)
        }

        // 4. Area / Folder-Wise Collection & Report Intent (घाना, आधारताल, आधार, ताल, रांझी, सदर आदि)
        val allFolders = folderRepository.foldersWithCount.first().filter { !it.folder.isDeleted }
        val matchedFolder = findMatchingFolder(trimmedQuery, allFolders)
        val isAreaOrCollectionKeyword = lowerQuery.contains("कलेक्शन") || lowerQuery.contains("collection") ||
                lowerQuery.contains("वसूली") || lowerQuery.contains("एरिया") || lowerQuery.contains("area") ||
                lowerQuery.contains("फोल्डर") || lowerQuery.contains("folder") || lowerQuery.contains("हिसाब") ||
                lowerQuery.contains("total") || lowerQuery.contains("कितना आया") || lowerQuery.contains("का बताओ") ||
                lowerQuery.contains("के ग्राहक") || lowerQuery.contains("की लिस्ट")
        if (matchedFolder != null && (isAreaOrCollectionKeyword || lowerQuery.length <= 15)) {
            return fetchAreaFolderCollectionResult(trimmedQuery, matchedFolder)
        }

        // 5. Unpaid / Pending Payment Intent (जिनने पेमेंट नहीं करी / बकाया वाले / उधारी वाले)
        val isUnpaidQuery = lowerQuery.contains("पेमेंट नहीं") || lowerQuery.contains("पैसे नहीं") ||
                lowerQuery.contains("रुपए नहीं") || lowerQuery.contains("पैसा बाकी") || lowerQuery.contains("बाकी है") ||
                lowerQuery.contains("बकाया वाले") || lowerQuery.contains("उधारी वाले") || lowerQuery.contains("unpaid") ||
                lowerQuery.contains("pending") || lowerQuery.contains("पेंडिंग") || lowerQuery.contains("not paid") ||
                lowerQuery.contains("payment due") || lowerQuery.contains("बकायादार") || lowerQuery.contains("बाकीदार") ||
                lowerQuery.contains("किसने पैसे नहीं दिए")
        if (isUnpaidQuery) {
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            return fetchUnpaidCustomersResult(trimmedQuery, currentMonth, null)
        }

        // 6. High Dues Intent
        if (lowerQuery.contains("highest") || lowerQuery.contains("हाई") || lowerQuery.contains("सबसे ज्यादा") ||
            lowerQuery.contains("ज्यादा बकाया") || lowerQuery.contains("top dues") || lowerQuery.contains("most due") ||
            lowerQuery.contains("अधिकतम बकाया")) {
            return fetchHighDuesCustomersResult(trimmedQuery)
        }

        // 7. Shop Summary Intent
        if (lowerQuery.contains("summary") || lowerQuery.contains("दुकान") || lowerQuery.contains("कुल हिसाब") ||
            lowerQuery.contains("total dues") || lowerQuery.contains("कुल बकाया") || lowerQuery.contains("overview") ||
            lowerQuery.contains("सबका हिसाब") || lowerQuery.contains("shop report")) {
            return fetchShopSummaryResult(trimmedQuery)
        }

        // -----------------------------------------------------------------------------------------
        // FAST-PATH 2: Instant Local Customer Name & Phone Matching (< 5ms)
        // Matches exact names, prefixes, Devanagari Hindi transliterations, typos, and phone numbers
        // -----------------------------------------------------------------------------------------
        val directNameMatches = NameMatcher.findMatchingCustomers(trimmedQuery, allCustomers)
        if (directNameMatches.isNotEmpty()) {
            val topMatch = directNameMatches.first()
            if (topMatch.score >= 65 || directNameMatches.size == 1) {
                return fetchCustomerDetailsResult(trimmedQuery, topMatch.customer.name)
            }
        }

        // -----------------------------------------------------------------------------------------
        // FALLBACK PATH: Gemini AI Assistant for free-form & advanced conversational queries
        // -----------------------------------------------------------------------------------------
        val geminiResponse = geminiService.processQueryWithGemini(trimmedQuery, customerNames)

        return when (geminiResponse) {
            is GeminiParsedResponse.FunctionCallRequest -> {
                executeFunctionCall(trimmedQuery, geminiResponse.functionName, geminiResponse.args)
            }
            is GeminiParsedResponse.TextResponse -> {
                if (directNameMatches.isNotEmpty()) {
                    fetchCustomerDetailsResult(trimmedQuery, directNameMatches.first().customer.name)
                } else {
                    AiAssistantResult(
                        queryText = trimmedQuery,
                        responseText = geminiResponse.text,
                        resultType = AiResultType.GENERAL_TEXT
                    )
                }
            }
            is GeminiParsedResponse.ErrorResponse -> {
                // Intelligent rule-based fallback
                executeFallbackNlpQuery(trimmedQuery)
            }
        }
    }

    private suspend fun executeFunctionCall(
        userQuery: String,
        functionName: String,
        args: Map<String, Any?>
    ): AiAssistantResult {
        return when (functionName) {
            "getTodaysNewCustomers" -> {
                fetchTodaysNewCustomersResult(userQuery)
            }
            "getTodayPayments" -> {
                fetchTodayPaymentsResult(userQuery)
            }
            "getWeeklyCollection" -> {
                fetchWeeklyCollectionResult(userQuery)
            }
            "getMonthlySummary" -> {
                val month = (args["month"] as? Number)?.toInt() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)
                val year = (args["year"] as? Number)?.toInt() ?: Calendar.getInstance().get(Calendar.YEAR)
                fetchMonthlySummaryResult(userQuery, month, year)
            }
            "getAreaFolderCollection" -> {
                val areaOrFolderName = args["areaOrFolderName"]?.toString() ?: userQuery
                val allFolders = folderRepository.foldersWithCount.first().filter { !it.folder.isDeleted }
                val folder = findMatchingFolder(areaOrFolderName, allFolders)
                if (folder != null) {
                    fetchAreaFolderCollectionResult(userQuery, folder)
                } else {
                    fetchShopSummaryResult(userQuery)
                }
            }
            "getUnpaidCustomers" -> {
                val month = (args["month"] as? Number)?.toInt()
                val year = (args["year"] as? Number)?.toInt()
                fetchUnpaidCustomersResult(userQuery, month, year)
            }
            "getCustomerDetails" -> {
                val customerName = args["customerName"]?.toString()?.ifBlank { userQuery } ?: userQuery
                fetchCustomerDetailsResult(userQuery, customerName)
            }
            "getHighDuesCustomers" -> {
                fetchHighDuesCustomersResult(userQuery)
            }
            "getShopSummary" -> {
                fetchShopSummaryResult(userQuery)
            }
            else -> {
                executeFallbackNlpQuery(userQuery)
            }
        }
    }

    private suspend fun executeFallbackNlpQuery(userQuery: String): AiAssistantResult {
        val lower = userQuery.lowercase()
        val allCustomers = folderRepository.allCustomers.first().filter { !it.isDeleted }
        val allFolders = folderRepository.foldersWithCount.first().filter { !it.folder.isDeleted }

        if ((lower.contains("नए") || lower.contains("new") || lower.contains("naye")) &&
            (lower.contains("कस्टमर") || lower.contains("ग्राहक") || lower.contains("customer") || lower.contains("खाता"))) {
            return fetchTodaysNewCustomersResult(userQuery)
        }

        if (lower.contains("आज") || lower.contains("today") || lower.contains("aaj") || lower.contains("वसूली")) {
            return fetchTodayPaymentsResult(userQuery)
        }

        if (lower.contains("हफ्ते") || lower.contains("week")) {
            return fetchWeeklyCollectionResult(userQuery)
        }

        if (lower.contains("महीने") || lower.contains("month") || lower.contains("मंथली")) {
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            return fetchMonthlySummaryResult(userQuery, currentMonth, currentYear)
        }

        val matchedFolder = findMatchingFolder(userQuery, allFolders)
        if (matchedFolder != null) {
            return fetchAreaFolderCollectionResult(userQuery, matchedFolder)
        }

        if (lower.contains("summary") || lower.contains("दुकान") || lower.contains("कुल हिसाब") ||
            lower.contains("total dues") || lower.contains("कुल बकाया") || lower.contains("overview")) {
            return fetchShopSummaryResult(userQuery)
        }

        if (lower.contains("highest") || lower.contains("हाई") || lower.contains("सबसे ज्यादा") || lower.contains("ज्यादा बकाया") || lower.contains("top dues")) {
            return fetchHighDuesCustomersResult(userQuery)
        }

        if (lower.contains("unpaid") || lower.contains("किसने पेमेंट नहीं") || lower.contains("pending") || lower.contains("पेंडिंग") || lower.contains("पैसे नहीं")) {
            val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
            return fetchUnpaidCustomersResult(userQuery, currentMonth, null)
        }

        val nameMatches = NameMatcher.findMatchingCustomers(userQuery, allCustomers)
        if (nameMatches.isNotEmpty()) {
            return fetchCustomerDetailsResult(userQuery, nameMatches.first().customer.name)
        }

        val cleanedName = NameMatcher.cleanQuery(userQuery)
        return fetchCustomerDetailsResult(userQuery, cleanedName)
    }

    // -----------------------------------------------------------------------------------------
    // 0. TODAY'S NEW CUSTOMERS (आज के नए जुड़े ग्राहक)
    // -----------------------------------------------------------------------------------------
    private suspend fun fetchTodaysNewCustomersResult(userQuery: String): AiAssistantResult {
        val allCustomers = folderRepository.allCustomers.first().filter { !it.isDeleted }
        val allTransactions = folderRepository.allTransactions.first()
        val allFolders = folderRepository.foldersWithCount.first().associate { it.folder.id to it.folder.name }

        val cal = Calendar.getInstance()
        val todayYear = cal.get(Calendar.YEAR)
        val todayDayOfYear = cal.get(Calendar.DAY_OF_YEAR)

        val todayDateStr = SimpleDateFormat("dd MMMM yyyy", Locale("hi", "IN")).format(Date())

        val custCal = Calendar.getInstance()
        val todaysCustomers = allCustomers.filter { customer ->
            custCal.timeInMillis = customer.createdAt
            custCal.get(Calendar.YEAR) == todayYear && custCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear
        }.sortedByDescending { it.createdAt }

        val customerDuesList = todaysCustomers.map { customer ->
            val custTx = allTransactions.filter { it.customerId == customer.id }
            val goodsProvided = custTx.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
            val paid = custTx.filter { it.type == "PAYMENT_DEPOSIT" || it.type == "PAYMENT_RECEIVED" }.sumOf { it.totalAmount }
            val dues = goodsProvided - paid
            val lastTxDate = custTx.maxOfOrNull { it.dateMillis } ?: customer.createdAt

            CustomerWithDuesInfo(
                customer = customer,
                folderName = allFolders[customer.folderId] ?: "General",
                totalGoodsPurchased = goodsProvided,
                totalPaid = paid,
                remainingDues = dues,
                lastTransactionDate = lastTxDate
            )
        }

        val summaryInfo = TodaysNewCustomersSummaryInfo(
            title = "आज के नए ग्राहक (Today's New Customers)",
            dateFormatted = todayDateStr,
            totalCount = customerDuesList.size,
            customers = customerDuesList
        )

        val responseText = if (customerDuesList.isEmpty()) {
            "⚡ आज ($todayDateStr) को अभी तक कोई नया कस्टमर नहीं जुड़ा है।"
        } else {
            val namesStr = customerDuesList.take(5).joinToString(", ") { it.customer.name }
            "⚡ आज ($todayDateStr) कुल ${customerDuesList.size} नए ग्राहक जुड़े हैं: $namesStr${if (customerDuesList.size > 5) " आदि..." else ""}। नीचे विवरण देखें:"
        }

        return AiAssistantResult(
            queryText = userQuery,
            responseText = responseText,
            resultType = AiResultType.TODAYS_NEW_CUSTOMERS,
            todaysNewCustomersSummary = summaryInfo
        )
    }

    // -----------------------------------------------------------------------------------------
    // 1. TODAY'S PAYMENTS & COLLECTION
    // -----------------------------------------------------------------------------------------
    private suspend fun fetchTodayPaymentsResult(userQuery: String): AiAssistantResult {
        val allTransactions = folderRepository.allTransactions.first()
        val allCustomers = folderRepository.allCustomers.first().associateBy { it.id }
        val allFolders = folderRepository.foldersWithCount.first().associate { it.folder.id to it.folder.name }

        val cal = Calendar.getInstance()
        val todayYear = cal.get(Calendar.YEAR)
        val todayDayOfYear = cal.get(Calendar.DAY_OF_YEAR)

        val todayDateStr = SimpleDateFormat("dd MMMM yyyy", Locale("hi", "IN")).format(Date())

        val txCal = Calendar.getInstance()
        val todayPayments = allTransactions.filter { tx ->
            val isPayment = tx.type == "PAYMENT_DEPOSIT" || tx.type == "PAYMENT_RECEIVED"
            if (!isPayment) return@filter false
            txCal.timeInMillis = tx.dateMillis
            txCal.get(Calendar.YEAR) == todayYear && txCal.get(Calendar.DAY_OF_YEAR) == todayDayOfYear
        }.sortedByDescending { it.dateMillis }

        val paymentItems = todayPayments.mapNotNull { tx ->
            val customer = allCustomers[tx.customerId] ?: return@mapNotNull null
            PaymentTransactionWithCustomer(
                transaction = tx,
                customer = customer,
                folderName = allFolders[customer.folderId] ?: "General"
            )
        }

        val totalAmount = paymentItems.sumOf { it.transaction.totalAmount }
        val uniqueCustomersCount = paymentItems.map { it.customer.id }.distinct().size

        val summaryInfo = TodayPaymentsSummaryInfo(
            title = "आज की कुल वसूली (Today's Recovery)",
            dateFormatted = todayDateStr,
            totalReceivedToday = totalAmount,
            todayPaymentsCount = paymentItems.size,
            uniqueCustomersCount = uniqueCustomersCount,
            payments = paymentItems
        )

        val responseText = if (paymentItems.isEmpty()) {
            "⚡ आज ($todayDateStr) को अभी तक कोई पेमेंट (वसूली) जमा नहीं हुई है।"
        } else {
            "⚡ आज ($todayDateStr) कुल ${uniqueCustomersCount} ग्राहकों से ₹${totalAmount.toInt()} की कुल वसूली आई है। नीचे ग्राहकों की लिस्ट देखें:"
        }

        return AiAssistantResult(
            queryText = userQuery,
            responseText = responseText,
            resultType = AiResultType.TODAYS_PAYMENTS,
            todayPaymentsSummary = summaryInfo
        )
    }

    // -----------------------------------------------------------------------------------------
    // 2. WEEKLY COLLECTION & RECOVERY
    // -----------------------------------------------------------------------------------------
    private suspend fun fetchWeeklyCollectionResult(userQuery: String): AiAssistantResult {
        val allTransactions = folderRepository.allTransactions.first()
        val allCustomers = folderRepository.allCustomers.first().associateBy { it.id }
        val allFolders = folderRepository.foldersWithCount.first().associate { it.folder.id to it.folder.name }

        val cal = Calendar.getInstance()
        val nowMillis = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysAgoMillis = cal.timeInMillis

        val dateFormat = SimpleDateFormat("dd MMM", Locale("hi", "IN"))
        val dateRangeStr = "${dateFormat.format(Date(sevenDaysAgoMillis))} से ${dateFormat.format(Date(nowMillis))}"

        val weekTransactions = allTransactions.filter { tx ->
            tx.dateMillis in sevenDaysAgoMillis..nowMillis
        }

        val weekPayments = weekTransactions.filter { tx ->
            tx.type == "PAYMENT_DEPOSIT" || tx.type == "PAYMENT_RECEIVED"
        }.sortedByDescending { it.dateMillis }

        val paymentItems = weekPayments.mapNotNull { tx ->
            val customer = allCustomers[tx.customerId] ?: return@mapNotNull null
            PaymentTransactionWithCustomer(
                transaction = tx,
                customer = customer,
                folderName = allFolders[customer.folderId] ?: "General"
            )
        }

        val totalCollectedInWeek = paymentItems.sumOf { it.transaction.totalAmount }
        val totalGoodsGivenInWeek = weekTransactions.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
        val activeCustomerIds = weekTransactions.map { it.customerId }.distinct()

        val weeklySummary = WeeklySummaryInfo(
            title = "इस हफ्ते की कुल वसूली (Weekly Collection)",
            dateRangeFormatted = dateRangeStr,
            totalCollectedInWeek = totalCollectedInWeek,
            totalGoodsGivenInWeek = totalGoodsGivenInWeek,
            activeCustomersInWeek = activeCustomerIds.size,
            paymentsCount = paymentItems.size,
            payments = paymentItems
        )

        val responseText = if (paymentItems.isEmpty()) {
            "⚡ इस हफ्ते ($dateRangeStr) कोई वसूली नहीं हुई है। कुल सामान दिया गया: ₹${totalGoodsGivenInWeek.toInt()}"
        } else {
            "⚡ इस हफ्ते ($dateRangeStr) कुल ${paymentItems.map { it.customer.id }.distinct().size} ग्राहकों से ₹${totalCollectedInWeek.toInt()} की वसूली आई है। कुल सामान दिया: ₹${totalGoodsGivenInWeek.toInt()}।"
        }

        return AiAssistantResult(
            queryText = userQuery,
            responseText = responseText,
            resultType = AiResultType.WEEKLY_COLLECTION,
            weeklySummary = weeklySummary
        )
    }

    // -----------------------------------------------------------------------------------------
    // 3. THIS MONTH FULL SUMMARY
    // -----------------------------------------------------------------------------------------
    private suspend fun fetchMonthlySummaryResult(userQuery: String, month: Int, year: Int): AiAssistantResult {
        val allTransactions = folderRepository.allTransactions.first()
        val allCustomers = folderRepository.allCustomers.first().filter { !it.isDeleted }
        val allCustomerMap = allCustomers.associateBy { it.id }
        val allFolders = folderRepository.foldersWithCount.first().associate { it.folder.id to it.folder.name }

        val months = listOf("जनवरी", "फ़रवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर")
        val monthNameStr = if (month in 1..12) months[month - 1] else "इस माह"

        val cal = Calendar.getInstance()
        var totalCollectedInMonth = 0.0
        var totalGoodsGivenInMonth = 0.0
        val activeCustomerIds = mutableSetOf<Long>()
        val monthPayments = mutableListOf<TransactionEntity>()

        for (tx in allTransactions) {
            cal.timeInMillis = tx.dateMillis
            val txMonth = cal.get(Calendar.MONTH) + 1
            val txYear = cal.get(Calendar.YEAR)

            if (txMonth == month && txYear == year) {
                activeCustomerIds.add(tx.customerId)
                when (tx.type) {
                    "PAYMENT_DEPOSIT", "PAYMENT_RECEIVED" -> {
                        totalCollectedInMonth += tx.totalAmount
                        monthPayments.add(tx)
                    }
                    "GOODS_PROVIDED" -> totalGoodsGivenInMonth += tx.totalAmount
                }
            }
        }

        val monthPaymentItems = monthPayments.sortedByDescending { it.dateMillis }.mapNotNull { tx ->
            val customer = allCustomerMap[tx.customerId] ?: return@mapNotNull null
            PaymentTransactionWithCustomer(
                transaction = tx,
                customer = customer,
                folderName = allFolders[customer.folderId] ?: "General"
            )
        }

        // Overall outstanding dues calculation across customers
        var totalOutstandingDues = 0.0
        var unpaidCustomersInMonth = 0
        for (customer in allCustomers) {
            val custTx = allTransactions.filter { it.customerId == customer.id }
            val goodsProvided = custTx.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
            val paid = custTx.filter { it.type == "PAYMENT_DEPOSIT" || it.type == "PAYMENT_RECEIVED" }.sumOf { it.totalAmount }
            val dues = goodsProvided - paid
            if (dues > 0.01) {
                totalOutstandingDues += dues
                unpaidCustomersInMonth++
            }
        }

        val monthlySummary = MonthlySummaryInfo(
            monthName = monthNameStr,
            year = year,
            totalCollectedInMonth = totalCollectedInMonth,
            totalGoodsGivenInMonth = totalGoodsGivenInMonth,
            activeCustomersInMonth = activeCustomerIds.size,
            unpaidCustomersInMonth = unpaidCustomersInMonth,
            totalOutstandingDues = totalOutstandingDues,
            payments = monthPaymentItems
        )

        val responseText = "⚡ $monthNameStr $year का पूरा हिसाब: कुल जमा वसूली: ₹${totalCollectedInMonth.toInt()} | दिया गया सामान: ₹${totalGoodsGivenInMonth.toInt()} | सक्रिय ग्राहक: ${activeCustomerIds.size} | कुल बकाया: ₹${totalOutstandingDues.toInt()}"

        return AiAssistantResult(
            queryText = userQuery,
            responseText = responseText,
            resultType = AiResultType.MONTHLY_SUMMARY,
            monthlySummary = monthlySummary
        )
    }

    // -----------------------------------------------------------------------------------------
    // 4. AREA / FOLDER-WISE COLLECTION & REPORT
    // -----------------------------------------------------------------------------------------
    private suspend fun fetchAreaFolderCollectionResult(userQuery: String, folderWithCount: FolderWithCount): AiAssistantResult {
        val folder = folderWithCount.folder
        val allCustomers = folderRepository.allCustomers.first().filter { !it.isDeleted && it.folderId == folder.id }
        val allTransactions = folderRepository.allTransactions.first().filter { it.folderId == folder.id }

        var totalCollection = 0.0
        var totalGoods = 0.0
        val customerDuesList = mutableListOf<CustomerWithDuesInfo>()

        for (customer in allCustomers) {
            val custTx = allTransactions.filter { it.customerId == customer.id }
            val goodsProvided = custTx.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
            val paid = custTx.filter { it.type == "PAYMENT_DEPOSIT" || it.type == "PAYMENT_RECEIVED" }.sumOf { it.totalAmount }
            val dues = goodsProvided - paid
            val lastTxDate = custTx.maxOfOrNull { it.dateMillis } ?: customer.createdAt

            totalCollection += paid
            totalGoods += goodsProvided

            customerDuesList.add(
                CustomerWithDuesInfo(
                    customer = customer,
                    folderName = folder.name,
                    totalGoodsPurchased = goodsProvided,
                    totalPaid = paid,
                    remainingDues = dues,
                    lastTransactionDate = lastTxDate
                )
            )
        }

        val totalRemainingDues = customerDuesList.filter { it.remainingDues > 0.01 }.sumOf { it.remainingDues }

        val areaSummary = AreaFolderSummaryInfo(
            folderName = folder.name,
            areaName = folder.areaTag,
            folderId = folder.id,
            totalCustomers = allCustomers.size,
            totalCollection = totalCollection,
            totalGoods = totalGoods,
            totalRemainingDues = totalRemainingDues,
            customers = customerDuesList.sortedByDescending { it.remainingDues }
        )

        val areaLabel = if (folder.areaTag.isNotBlank() && folder.areaTag != folder.name) " (एरिया: ${folder.areaTag})" else ""
        val responseText = "⚡ '${folder.name}'$areaLabel का कुल कलेक्शन: ₹${totalCollection.toInt()} | कुल दिया गया सामान: ₹${totalGoods.toInt()} | कुल बकाया: ₹${totalRemainingDues.toInt()} | कुल ग्राहक: ${allCustomers.size}"

        return AiAssistantResult(
            queryText = userQuery,
            responseText = responseText,
            resultType = AiResultType.AREA_FOLDER_COLLECTION,
            areaFolderSummary = areaSummary
        )
    }

    // -----------------------------------------------------------------------------------------
    // Helper: Matching Folder Name or Area from Hindi / English Query
    // -----------------------------------------------------------------------------------------
    private fun findMatchingFolder(query: String, allFolders: List<FolderWithCount>): FolderWithCount? {
        val cleanQuery = query.lowercase(Locale.ROOT)
            .replace("का", "")
            .replace("की", "")
            .replace("के", "")
            .replace("कलेक्शन", "")
            .replace("collection", "")
            .replace("फोल्डर", "")
            .replace("folder", "")
            .replace("एरिया", "")
            .replace("area", "")
            .replace("बताओ", "")
            .replace("हिसाब", "")
            .replace("टोटल", "")
            .replace("total", "")
            .trim()

        if (cleanQuery.isBlank()) return null

        // 1. Direct exact or contains in folder name or area
        for (f in allFolders) {
            val fName = f.folder.name.lowercase(Locale.ROOT).trim()
            val fArea = f.folder.areaTag.lowercase(Locale.ROOT).trim()

            if (cleanQuery.contains(fName) || fName.contains(cleanQuery) ||
                (fArea.isNotBlank() && (cleanQuery.contains(fArea) || fArea.contains(cleanQuery)))) {
                return f
            }
        }

        // 2. Common Hindi Transliterations (e.g. घाना -> ghana / ghanha / gana; अधारताल -> adhartal)
        val aliases = mapOf(
            "घाना" to listOf("ghana", "ghanha", "gana", "ghaana"),
            "ghana" to listOf("घाना", "ghanha", "gana"),
            "अधारताल" to listOf("adhartal", "adharttal", "adhartaal"),
            "adhartal" to listOf("अधारताल", "adharttal"),
            "रांझी" to listOf("ranjhi", "ranji", "raanjhi"),
            "ranjhi" to listOf("रांझी", "ranji"),
            "सदर" to listOf("sadar", "saadar"),
            "sadar" to listOf("सदर"),
            "गोरखपुर" to listOf("gorakhpur", "gorakh pur"),
            "gorakhpur" to listOf("गोरखपुर"),
            "गढ़ा" to listOf("garha", "gadha", "garaha"),
            "garha" to listOf("गढ़ा", "gadha"),
            "कैंट" to listOf("cantt", "cantonment", "kant"),
            "cantt" to listOf("कैंट")
        )

        for (f in allFolders) {
            val fName = f.folder.name.lowercase(Locale.ROOT).trim()
            val fArea = f.folder.areaTag.lowercase(Locale.ROOT).trim()

            for ((key, aliasList) in aliases) {
                if (cleanQuery.contains(key) || aliasList.any { cleanQuery.contains(it) }) {
                    if (fName.contains(key) || aliasList.any { fName.contains(it) } ||
                        fArea.contains(key) || aliasList.any { fArea.contains(it) }) {
                        return f
                    }
                }
            }
        }

        // 3. Token split matching (e.g. user asks "आधार", "ताल", "घाना", "सदर", etc.)
        val queryTokens = cleanQuery.split(" ", "_", "-", ",", "/").filter { it.length >= 2 }
        for (token in queryTokens) {
            // Check specific common sub-tokens like आधार, ताल -> अधारताल
            if (token == "आधार" || token == "ताल" || token == "adhar" || token == "tal") {
                val adharTalFolder = allFolders.firstOrNull {
                    it.folder.name.contains("अधारताल") || it.folder.name.contains("आधार") ||
                    it.folder.name.contains("ताल") || it.folder.name.contains("adhartal", ignoreCase = true) ||
                    it.folder.areaTag.contains("अधारताल") || it.folder.areaTag.contains("adhartal", ignoreCase = true)
                }
                if (adharTalFolder != null) return adharTalFolder
            }

            for (f in allFolders) {
                val fName = f.folder.name.lowercase(Locale.ROOT).trim()
                val fArea = f.folder.areaTag.lowercase(Locale.ROOT).trim()
                if (fName.contains(token) || (fArea.isNotBlank() && fArea.contains(token))) {
                    return f
                }
            }
        }

        // 3. NameMatcher fuzzy check
        var bestFolder: FolderWithCount? = null
        var bestScore = 0

        for (f in allFolders) {
            val score1 = NameMatcher.calculateSimilarity(cleanQuery, f.folder.name)
            val score2 = if (f.folder.areaTag.isNotBlank()) NameMatcher.calculateSimilarity(cleanQuery, f.folder.areaTag) else 0
            val maxS = maxOf(score1, score2)
            if (maxS > bestScore && maxS >= 60) {
                bestScore = maxS
                bestFolder = f
            }
        }

        return bestFolder
    }

    private suspend fun fetchUnpaidCustomersResult(
        userQuery: String,
        month: Int?,
        year: Int?
    ): AiAssistantResult {
        val allCustomers = folderRepository.allCustomers.first().filter { !it.isDeleted }
        val allTransactions = folderRepository.allTransactions.first()
        val allFolders = folderRepository.foldersWithCount.first()
        val folderMap = allFolders.associate { it.folder.id to it.folder.name }

        val unpaidList = mutableListOf<CustomerWithDuesInfo>()

        for (customer in allCustomers) {
            val custTx = allTransactions.filter { it.customerId == customer.id }
            val goodsProvided = custTx.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
            val totalPaid = custTx.filter { it.type == "PAYMENT_DEPOSIT" || it.type == "PAYMENT_RECEIVED" }.sumOf { it.totalAmount }
            val dues = goodsProvided - totalPaid

            val lastTxDate = custTx.maxOfOrNull { it.dateMillis } ?: customer.createdAt

            if (dues > 0.01) {
                var matchMonthFilter = true
                if (month != null && month in 1..12) {
                    val cal = Calendar.getInstance()
                    val paidInMonth = custTx.filter { tx ->
                        if (tx.type != "PAYMENT_DEPOSIT" && tx.type != "PAYMENT_RECEIVED") return@filter false
                        cal.timeInMillis = tx.dateMillis
                        val txMonth = cal.get(Calendar.MONTH) + 1
                        val txYear = cal.get(Calendar.YEAR)
                        txMonth == month && (year == null || txYear == year)
                    }.sumOf { it.totalAmount }

                    matchMonthFilter = paidInMonth == 0.0
                }

                if (matchMonthFilter) {
                    unpaidList.add(
                        CustomerWithDuesInfo(
                            customer = customer,
                            folderName = folderMap[customer.folderId] ?: "General",
                            totalGoodsPurchased = goodsProvided,
                            totalPaid = totalPaid,
                            remainingDues = dues,
                            lastTransactionDate = lastTxDate
                        )
                    )
                }
            }
        }

        unpaidList.sortByDescending { it.remainingDues }

        val monthNameStr = if (month != null && month in 1..12) {
            val months = listOf("जनवरी", "फ़रवरी", "मार्च", "अप्रैल", "मई", "जून", "जुलाई", "अगस्त", "सितंबर", "अक्टूबर", "नवंबर", "दिसंबर")
            months[month - 1]
        } else "इस अवधि"

        val totalUnpaidDues = unpaidList.sumOf { it.remainingDues }.toInt()
        val finalResponseText = if (unpaidList.isEmpty()) {
            "बधाई हो! $monthNameStr में कोई भी बकाया पेंडिंग नहीं है। सभी खाते चुकता हैं।"
        } else {
            "⚡ $monthNameStr में कुल ${unpaidList.size} ग्राहकों का ₹${totalUnpaidDues} बकाया पेंडिंग है। नीचे सूची देखें:"
        }

        return AiAssistantResult(
            queryText = userQuery,
            responseText = finalResponseText,
            resultType = AiResultType.UNPAID_CUSTOMERS,
            unpaidCustomers = unpaidList
        )
    }

    private suspend fun fetchCustomerDetailsResult(
        userQuery: String,
        customerName: String
    ): AiAssistantResult {
        val allCustomers = folderRepository.allCustomers.first().filter { !it.isDeleted }
        val allTransactions = folderRepository.allTransactions.first()
        val allFolders = folderRepository.foldersWithCount.first()
        val folderMap = allFolders.associate { it.folder.id to it.folder.name }

        fun buildCustomerDuesInfo(customer: CustomerEntity): CustomerWithDuesInfo {
            val custTx = allTransactions.filter { it.customerId == customer.id }
            val goodsProvided = custTx.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
            val totalPaid = custTx.filter { it.type == "PAYMENT_DEPOSIT" || it.type == "PAYMENT_RECEIVED" }.sumOf { it.totalAmount }
            val dues = goodsProvided - totalPaid
            val lastTxDate = custTx.maxOfOrNull { it.dateMillis } ?: customer.createdAt
            return CustomerWithDuesInfo(
                customer = customer,
                folderName = folderMap[customer.folderId] ?: "General",
                totalGoodsPurchased = goodsProvided,
                totalPaid = totalPaid,
                remainingDues = dues,
                lastTransactionDate = lastTxDate
            )
        }

        val queryMatches = NameMatcher.findMatchingCustomers(customerName, allCustomers)
        val fullQueryMatches = NameMatcher.findMatchingCustomers(userQuery, allCustomers)
        val combinedMatches = (queryMatches + fullQueryMatches).distinctBy { it.customer.id }.sortedByDescending { it.score }

        if (combinedMatches.isEmpty()) {
            val fallbackOptions = allCustomers.take(6).map { buildCustomerDuesInfo(it) }
            val displaySearchTerm = customerName.ifBlank { userQuery }
            return AiAssistantResult(
                queryText = userQuery,
                responseText = "'$displaySearchTerm' नाम से कोई सीधा खाता नहीं मिला। दुकान के उपलब्ध खाते नीचे दिए गए हैं:",
                resultType = AiResultType.SIMILAR_CUSTOMERS,
                similarCustomers = fallbackOptions
            )
        }

        val topScore = combinedMatches.first().score
        val exactMatches = combinedMatches.filter { it.score >= 95 }
        val highMatches = combinedMatches.filter { it.score >= 80 }

        if (exactMatches.isEmpty() && highMatches.size > 1 && topScore < 95) {
            val similarList = highMatches.map { buildCustomerDuesInfo(it.customer) }
            return AiAssistantResult(
                queryText = userQuery,
                responseText = "'$customerName' से मिलते-जुलते ${similarList.size} ग्राहक मिले हैं। विवरण देखने के लिए टैप करें:",
                resultType = AiResultType.SIMILAR_CUSTOMERS,
                similarCustomers = similarList
            )
        }

        val primaryCustomer = combinedMatches.first().customer
        val otherSimilarCustomers = combinedMatches
            .drop(1)
            .filter { it.score >= 60 }
            .take(6)
            .map { buildCustomerDuesInfo(it.customer) }

        val custTx = allTransactions.filter { it.customerId == primaryCustomer.id }.sortedByDescending { it.dateMillis }
        val goodsProvided = custTx.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
        val totalPaid = custTx.filter { it.type == "PAYMENT_DEPOSIT" || it.type == "PAYMENT_RECEIVED" }.sumOf { it.totalAmount }
        val dues = goodsProvided - totalPaid

        val report = CustomerDetailReport(
            customer = primaryCustomer,
            folderName = folderMap[primaryCustomer.folderId] ?: "General",
            totalGoodsPurchased = goodsProvided,
            totalPaid = totalPaid,
            remainingDues = dues,
            transactions = custTx,
            similarCustomers = otherSimilarCustomers
        )

        val duesStatus = if (report.remainingDues > 0.01) {
            "कुल बकाया: ₹${report.remainingDues.toInt()}"
        } else {
            "खाता चुकता है (0 बकाया)"
        }

        val phoneInfo = if (primaryCustomer.phone.isNotBlank()) " | फोन: ${primaryCustomer.phone}" else ""
        val folderInfo = "फोल्डर: ${report.folderName}"
        val relatedNote = if (otherSimilarCustomers.isNotEmpty()) " (${otherSimilarCustomers.size} अन्य मिलते-जुलते खाते उपलब्ध हैं)" else ""

        val finalResponseText = "⚡ ${primaryCustomer.name} ($folderInfo$phoneInfo) — कुल सामान: ₹${report.totalGoodsPurchased.toInt()}, जमा: ₹${report.totalPaid.toInt()}, $duesStatus।$relatedNote"

        return AiAssistantResult(
            queryText = userQuery,
            responseText = finalResponseText,
            resultType = AiResultType.CUSTOMER_DETAILS,
            customerReport = report,
            similarCustomers = otherSimilarCustomers
        )
    }

    private suspend fun fetchHighDuesCustomersResult(userQuery: String): AiAssistantResult {
        val result = fetchUnpaidCustomersResult(userQuery, null, null)
        val count = result.unpaidCustomers?.size ?: 0
        return result.copy(
            resultType = AiResultType.HIGH_DUES,
            responseText = "⚡ सबसे ज्यादा बकाया वाले $count ग्राहकों की सूची (High Dues List):"
        )
    }

    private suspend fun fetchShopSummaryResult(userQuery: String): AiAssistantResult {
        val allCustomers = folderRepository.allCustomers.first().filter { !it.isDeleted }
        val allTransactions = folderRepository.allTransactions.first()
        val allFolders = folderRepository.foldersWithCount.first().filter { !it.folder.isDeleted }

        var totalDues = 0.0
        var totalPaid = 0.0
        var unpaidCount = 0

        for (customer in allCustomers) {
            val custTx = allTransactions.filter { it.customerId == customer.id }
            val goodsProvided = custTx.filter { it.type == "GOODS_PROVIDED" }.sumOf { it.totalAmount }
            val paid = custTx.filter { it.type == "PAYMENT_DEPOSIT" || it.type == "PAYMENT_RECEIVED" }.sumOf { it.totalAmount }
            val dues = goodsProvided - paid

            totalPaid += paid
            if (dues > 0.01) {
                totalDues += dues
                unpaidCount++
            }
        }

        val summary = ShopSummaryInfo(
            totalFoldersCount = allFolders.size,
            totalCustomersCount = allCustomers.size,
            totalPendingDues = totalDues,
            totalCollectedPayments = totalPaid,
            unpaidCustomersCount = unpaidCount
        )

        val responseText = "⚡ दुकान का कुल हिसाब: ${summary.totalFoldersCount} फोल्डर, ${summary.totalCustomersCount} ग्राहक | कुल बकाया: ₹${summary.totalPendingDues.toInt()} | कुल जमा राशि: ₹${summary.totalCollectedPayments.toInt()}"

        return AiAssistantResult(
            queryText = userQuery,
            responseText = responseText,
            resultType = AiResultType.SHOP_SUMMARY,
            shopSummary = summary
        )
    }
}


