package com.example.data.model

import com.example.data.entity.CustomerEntity
import com.example.data.entity.TransactionEntity

enum class AiResultType {
    UNPAID_CUSTOMERS,
    CUSTOMER_DETAILS,
    HIGH_DUES,
    SHOP_SUMMARY,
    SIMILAR_CUSTOMERS,
    TODAYS_PAYMENTS,
    TODAYS_NEW_CUSTOMERS,
    WEEKLY_COLLECTION,
    MONTHLY_SUMMARY,
    AREA_FOLDER_COLLECTION,
    GENERAL_TEXT
}

data class CustomerWithDuesInfo(
    val customer: CustomerEntity,
    val folderName: String,
    val totalGoodsPurchased: Double,
    val totalPaid: Double,
    val remainingDues: Double,
    val lastTransactionDate: Long
)

data class TodaysNewCustomersSummaryInfo(
    val title: String = "आज के नए ग्राहक (Today's New Customers)",
    val dateFormatted: String,
    val totalCount: Int,
    val customers: List<CustomerWithDuesInfo>
)

data class PaymentTransactionWithCustomer(
    val transaction: TransactionEntity,
    val customer: CustomerEntity,
    val folderName: String
)

data class TodayPaymentsSummaryInfo(
    val title: String = "आज की वसूली (Today's Collection)",
    val dateFormatted: String,
    val totalReceivedToday: Double,
    val todayPaymentsCount: Int,
    val uniqueCustomersCount: Int,
    val payments: List<PaymentTransactionWithCustomer>
)

data class WeeklySummaryInfo(
    val title: String = "इस हफ्ते की कुल वसूली (Weekly Collection)",
    val dateRangeFormatted: String,
    val totalCollectedInWeek: Double,
    val totalGoodsGivenInWeek: Double,
    val activeCustomersInWeek: Int,
    val paymentsCount: Int,
    val payments: List<PaymentTransactionWithCustomer> = emptyList()
)

data class MonthlySummaryInfo(
    val monthName: String,
    val year: Int,
    val totalCollectedInMonth: Double,
    val totalGoodsGivenInMonth: Double,
    val activeCustomersInMonth: Int,
    val unpaidCustomersInMonth: Int,
    val totalOutstandingDues: Double,
    val payments: List<PaymentTransactionWithCustomer> = emptyList()
)

data class AreaFolderSummaryInfo(
    val folderName: String,
    val areaName: String,
    val folderId: Long,
    val totalCustomers: Int,
    val totalCollection: Double,
    val totalGoods: Double,
    val totalRemainingDues: Double,
    val customers: List<CustomerWithDuesInfo> = emptyList()
)

data class CustomerDetailReport(
    val customer: CustomerEntity,
    val folderName: String,
    val totalGoodsPurchased: Double,
    val totalPaid: Double,
    val remainingDues: Double,
    val transactions: List<TransactionEntity>,
    val similarCustomers: List<CustomerWithDuesInfo> = emptyList()
)

data class ShopSummaryInfo(
    val totalFoldersCount: Int,
    val totalCustomersCount: Int,
    val totalPendingDues: Double,
    val totalCollectedPayments: Double,
    val unpaidCustomersCount: Int
)

data class AiAssistantResult(
    val queryText: String,
    val responseText: String,
    val resultType: AiResultType,
    val unpaidCustomers: List<CustomerWithDuesInfo> = emptyList(),
    val customerReport: CustomerDetailReport? = null,
    val similarCustomers: List<CustomerWithDuesInfo> = emptyList(),
    val shopSummary: ShopSummaryInfo? = null,
    val todayPaymentsSummary: TodayPaymentsSummaryInfo? = null,
    val todaysNewCustomersSummary: TodaysNewCustomersSummaryInfo? = null,
    val weeklySummary: WeeklySummaryInfo? = null,
    val monthlySummary: MonthlySummaryInfo? = null,
    val areaFolderSummary: AreaFolderSummaryInfo? = null
)
