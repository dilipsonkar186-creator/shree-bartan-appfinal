package com.example

import com.example.data.entity.CustomerEntity
import com.example.util.NameMatcher
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testExactAndEnglishNameMatching() {
    val customers = listOf(
      CustomerEntity(id = 1L, folderId = 1L, name = "Anju", phone = "9876543210"),
      CustomerEntity(id = 2L, folderId = 1L, name = "Ramesh Kumar", phone = "9876543211"),
      CustomerEntity(id = 3L, folderId = 1L, name = "Suresh", phone = "")
    )

    val matchAnju = NameMatcher.findBestMatch("Anju", customers)
    assertNotNull(matchAnju)
    assertEquals(1L, matchAnju?.customer?.id)

    val matchQuerySentence = NameMatcher.findBestMatch("Search for customer Anju please", customers)
    assertNotNull(matchQuerySentence)
    assertEquals(1L, matchQuerySentence?.customer?.id)

    val matchRamesh = NameMatcher.findBestMatch("Show Ramesh bill details", customers)
    assertNotNull(matchRamesh)
    assertEquals(2L, matchRamesh?.customer?.id)
  }

  @Test
  fun testDevanagariAndHinglishMatching() {
    val customers = listOf(
      CustomerEntity(id = 10L, folderId = 1L, name = "अंजू", phone = "9876543210"),
      CustomerEntity(id = 11L, folderId = 1L, name = "राहुल", phone = "9876543212"),
      CustomerEntity(id = 12L, folderId = 1L, name = "Mohit Verma", phone = ""),
      CustomerEntity(id = 13L, folderId = 1L, name = "RITESH SONKAR", phone = "917477011111"),
      CustomerEntity(id = 14L, folderId = 1L, name = "DILIP SONKAR", phone = "")
    )

    // User says "Anju" in English for customer stored in Hindi "अंजू"
    val matchAnjuEnglish = NameMatcher.findBestMatch("Anju", customers)
    assertNotNull(matchAnjuEnglish)
    assertEquals(10L, matchAnjuEnglish?.customer?.id)

    // User asks "अंजू का खाता दिखाओ"
    val matchAnjuHindi = NameMatcher.findBestMatch("अंजू का खाता दिखाओ", customers)
    assertNotNull(matchAnjuHindi)
    assertEquals(10L, matchAnjuHindi?.customer?.id)

    // User speaks in Hindi "रितेश सोनकर" -> Matches English name "RITESH SONKAR"
    val matchRiteshHindi = NameMatcher.findBestMatch("रितेश सोनकर", customers)
    assertNotNull(matchRiteshHindi)
    assertEquals(13L, matchRiteshHindi?.customer?.id)

    // User speaks in Hindi "सोनकर" -> Filters both Ritesh Sonkar and Dilip Sonkar
    val matchSonkar = NameMatcher.filterCustomers("सोनकर", customers)
    assertEquals(2, matchSonkar.size)

    // User speaks with slight spelling typo "Retesh Shonkar" -> Matches "RITESH SONKAR"
    val matchTypo = NameMatcher.findBestMatch("Retesh Shonkar", customers)
    assertNotNull(matchTypo)
    assertEquals(13L, matchTypo?.customer?.id)

    // User asks "Rahul details"
    val matchRahul = NameMatcher.findBestMatch("Rahul details", customers)
    assertNotNull(matchRahul)
    assertEquals(11L, matchRahul?.customer?.id)
  }

  @Test
  fun testFirstLetterAndPrefixSuggestions() {
    val customers = listOf(
      CustomerEntity(id = 20L, folderId = 1L, name = "Anju 1", phone = ""),
      CustomerEntity(id = 21L, folderId = 1L, name = "Anju 2", phone = ""),
      CustomerEntity(id = 22L, folderId = 1L, name = "Anjali", phone = ""),
      CustomerEntity(id = 23L, folderId = 1L, name = "Bablu", phone = "")
    )

    val anjuMatches = NameMatcher.findAllMatches("Anju", customers)
    assertTrue(anjuMatches.size >= 2)
    val names = anjuMatches.map { it.customer.name }
    assertTrue(names.contains("Anju 1"))
    assertTrue(names.contains("Anju 2"))
  }

  @Test
  fun testUmaSenSearchOrderingAndExclusions() {
    val customers = listOf(
      CustomerEntity(id = 1L, folderId = 1L, name = "BHARTI RAM SINGH", phone = ""),
      CustomerEntity(id = 2L, folderId = 1L, name = "RAJU CHOUDHARY KI MAA", phone = ""),
      CustomerEntity(id = 3L, folderId = 1L, name = "REKHA SINGH", phone = ""),
      CustomerEntity(id = 4L, folderId = 1L, name = "SAKSHI SINGH", phone = ""),
      CustomerEntity(id = 5L, folderId = 1L, name = "UMA SEN", phone = ""),
      CustomerEntity(id = 6L, folderId = 1L, name = "UMA DEVI", phone = ""),
      CustomerEntity(id = 7L, folderId = 1L, name = "UMESH VERMA", phone = ""),
      CustomerEntity(id = 8L, folderId = 1L, name = "UPENDRA", phone = "")
    )

    val filtered = NameMatcher.filterCustomers("UMA SEN", customers)
    val names = filtered.map { it.name }

    // 1. UMA SEN must be 1st
    assertEquals("UMA SEN", names.firstOrNull())

    // 2. UMA DEVI must be next (starts with UMA)
    assertTrue(names.contains("UMA DEVI"))

    // 3. Other U names (UMESH, UPENDRA) follow
    assertTrue(names.contains("UMESH VERMA"))

    // 4. Irrelevant names (BHARTI RAM SINGH, RAJU, REKHA, SAKSHI) MUST NOT be present
    assertFalse(names.contains("BHARTI RAM SINGH"))
    assertFalse(names.contains("RAJU CHOUDHARY KI MAA"))
    assertFalse(names.contains("REKHA SINGH"))
    assertFalse(names.contains("SAKSHI SINGH"))
  }

  @Test
  fun testRunningBalanceCalculation() {
    val tx1 = com.example.data.entity.TransactionEntity(
      id = 1L, customerId = 1L, folderId = 1L,
      type = "GOODS_PROVIDED", itemDescription = "Goods 1 (Tanki)", totalAmount = 500.0, dateMillis = 1000L
    )
    val tx2 = com.example.data.entity.TransactionEntity(
      id = 2L, customerId = 1L, folderId = 1L,
      type = "GOODS_RETURNED", itemDescription = "Returned Goods (Spoon)", totalAmount = 300.0, dateMillis = 2000L
    )
    val tx3 = com.example.data.entity.TransactionEntity(
      id = 3L, customerId = 1L, folderId = 1L,
      type = "GOODS_PROVIDED", itemDescription = "Goods 3 (Dhakkan)", totalAmount = 400.0, dateMillis = 3000L
    )

    val txs = listOf(tx3, tx1, tx2) // deliberately unordered
    val chronological = txs.sortedWith(compareBy<com.example.data.entity.TransactionEntity> { it.dateMillis }.thenBy { it.id })
    val runningBalances = mutableMapOf<Long, Double>()
    var curBal = 0.0
    for (tx in chronological) {
      when (tx.type) {
        "GOODS_PROVIDED" -> curBal += tx.totalAmount
        "PAYMENT_DEPOSIT" -> curBal -= tx.totalAmount
        "GOODS_RETURNED" -> { /* Returned item does not change running balance */ }
      }
      runningBalances[tx.id] = curBal
    }

    // Step 1: gave 500 (Tanki) -> balance becomes 500
    assertEquals(500.0, runningBalances[1L] ?: 0.0, 0.001)
    // Step 2: returned 300 (Spoon) -> balance remains 500 (does not subtract from Tanki 500)
    assertEquals(500.0, runningBalances[2L] ?: 0.0, 0.001)
    // Step 3: gave 400 (Dhakkan) -> balance becomes 500 + 400 = 900
    assertEquals(900.0, runningBalances[3L] ?: 0.0, 0.001)
  }

  @Test
  fun testPhoneNumberFormattingAndStorage() {
    // Test input formatting with India +91 prefill
    assertEquals("+91 ", com.example.util.PhoneNumberUtils.formatInputPhone(""))
    assertEquals("+91 9876543210", com.example.util.PhoneNumberUtils.formatInputPhone("9876543210"))
    assertEquals("+91 9876543210", com.example.util.PhoneNumberUtils.formatInputPhone("09876543210"))
    assertEquals("+91 9876543210", com.example.util.PhoneNumberUtils.formatInputPhone("+919876543210"))
    assertEquals("+91 9876543210", com.example.util.PhoneNumberUtils.formatInputPhone("+91 9876543210"))

    // Test normalizeForStorage
    assertEquals("", com.example.util.PhoneNumberUtils.normalizeForStorage(""))
    assertEquals("", com.example.util.PhoneNumberUtils.normalizeForStorage("+91"))
    assertEquals("", com.example.util.PhoneNumberUtils.normalizeForStorage("+91 "))
    assertEquals("+91 98765 43210", com.example.util.PhoneNumberUtils.normalizeForStorage("9876543210"))
    assertEquals("+91 98765 43210", com.example.util.PhoneNumberUtils.normalizeForStorage("+91 9876543210"))
    assertEquals("+91 98765 43210", com.example.util.PhoneNumberUtils.normalizeForStorage("+919876543210"))
  }

  @Test
  fun testOwnerAuthorizationEmailCheck() {
    val ownerEmail = com.example.ui.viewmodel.AuthViewModel.AUTHORIZED_OWNER_EMAIL
    assertEquals("dilip.sonkar.186@gmail.com", ownerEmail)

    // Test case insensitive match
    assertTrue(ownerEmail.equals("DILIP.SONKAR.186@GMAIL.COM", ignoreCase = true))
    assertTrue(ownerEmail.equals("dilip.sonkar.186@gmail.com", ignoreCase = true))

    // Unauthorized emails
    assertFalse(ownerEmail.equals("other.user@gmail.com", ignoreCase = true))
    assertFalse(ownerEmail.equals("guest@test.com", ignoreCase = true))
  }
}

