package com.example.data.remote

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class GeminiParsedResponse {
    data class FunctionCallRequest(
        val functionName: String,
        val args: Map<String, Any?>
    ) : GeminiParsedResponse()

    data class TextResponse(
        val text: String
    ) : GeminiParsedResponse()

    data class ErrorResponse(
        val errorMessage: String
    ) : GeminiParsedResponse()
}

class GeminiAssistantService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun processQueryWithGemini(
        userQuery: String,
        existingCustomerNames: List<String> = emptyList()
    ): GeminiParsedResponse = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiAssistant", "Gemini API key is not configured or placeholder.")
            return@withContext GeminiParsedResponse.ErrorResponse("API_KEY_MISSING")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        try {
            val customerListSnippet = if (existingCustomerNames.isNotEmpty()) {
                "Known customer names in database: [${existingCustomerNames.take(50).joinToString(", ")}]"
            } else {
                ""
            }

            val requestJson = JSONObject().apply {
                // Contents
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", userQuery)
                            })
                        })
                    })
                })

                // System Instruction
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", """
                                You are an AI Voice & Text Assistant for an Indian Customer Management and Ledger (Khata) app.
                                The user can speak or type in English, Hinglish, or Hindi.
                                $customerListSnippet
                                
                                CRITICAL RULES:
                                1. TODAY'S NEW CUSTOMERS: If user asks "आज कितने नए कस्टमर जुड़े", "आज कितने नए ग्राहक जुड़े", "नए कस्टमर", "new customers today", "नए ग्राहक", call `getTodaysNewCustomers`.
                                2. TODAY'S RECOVERY / COLLECTION: If user asks "आज की कुल कितनी वसूली आई है", "आज कितने पैसे आए", "आज का कलेक्शन", "आज की वसूली", "who paid today", call `getTodayPayments`.
                                3. WEEKLY COLLECTION: If user asks for weekly recovery, last 7 days, "हफ्ते का हिसाब", "इस हफ्ते की वसूली", call `getWeeklyCollection`.
                                4. MONTHLY SUMMARY: If user asks for monthly summary, "महीने का हिसाब", "मंथली वाइज", "महीने की वसूली", call `getMonthlySummary`.
                                5. AREA / FOLDER-WISE: If user asks for collection or summary of a specific area or folder (e.g. "घाना का कलेक्शन", "आधार", "ताल", "अधारताल", "रांझी", "सदर", etc.), call `getAreaFolderCollection`.
                                6. UNPAID / DEFAULTERS: If user asks "जिनने पेमेंट नहीं करी", "किसने पेमेंट नहीं की", "किस-किस का पैसा बाकी है", "बकाया वाले ग्राहक", call `getUnpaidCustomers`.
                                7. SPECIFIC CUSTOMER: If user mentions ANY customer name (e.g. "Anju", "Search Ramesh", "Find Rahul", "अंजू का खाता", "रमेश की डिटेल्स"), ALWAYS call `getCustomerDetails`.
                                8. HIGHEST DUES: If user asks about highest dues or most pending balance, call `getHighDuesCustomers`.
                                9. SHOP SUMMARY: If user asks about overall shop summary or total dues, call `getShopSummary`.
                                
                                Always prefer calling function tools when answering queries about customers, ledgers, or shop statistics.
                            """.trimIndent())
                        })
                    })
                })

                // Function Declarations (Tools)
                put("tools", JSONArray().apply {
                    put(JSONObject().apply {
                        put("functionDeclarations", JSONArray().apply {
                            // 1. getUnpaidCustomers
                            put(JSONObject().apply {
                                put("name", "getUnpaidCustomers")
                                put("description", "Fetches list of customers who have unpaid dues or no payment in requested month.")
                                put("parameters", JSONObject().apply {
                                    put("type", "OBJECT")
                                    put("properties", JSONObject().apply {
                                        put("month", JSONObject().apply {
                                            put("type", "INTEGER")
                                            put("description", "1-12 for specific month if requested in query")
                                        })
                                        put("year", JSONObject().apply {
                                            put("type", "INTEGER")
                                            put("description", "Year, e.g., 2026")
                                        })
                                    })
                                })
                            })

                            // 2. getCustomerDetails
                            put(JSONObject().apply {
                                put("name", "getCustomerDetails")
                                put("description", "Fetches complete details, total goods purchased, bill amount, dues, and transaction history for a specific customer name or search query in English or Hindi.")
                                put("parameters", JSONObject().apply {
                                    put("type", "OBJECT")
                                    put("properties", JSONObject().apply {
                                        put("customerName", JSONObject().apply {
                                            put("type", "STRING")
                                            put("description", "Name or partial name of the customer to search (e.g., 'Anju', 'Ramesh', 'Karuna')")
                                        })
                                    })
                                    put("required", JSONArray().apply { put("customerName") })
                                })
                            })

                            // 3. getHighDuesCustomers
                            put(JSONObject().apply {
                                put("name", "getHighDuesCustomers")
                                put("description", "Fetches list of customers ordered by highest unpaid balance/dues.")
                                put("parameters", JSONObject().apply {
                                    put("type", "OBJECT")
                                    put("properties", JSONObject())
                                })
                            })

                            // 4. getShopSummary
                            put(JSONObject().apply {
                                put("name", "getShopSummary")
                                put("description", "Fetches overall shop financial summary including total folders, total customers, total pending dues, and total collected payments.")
                                put("parameters", JSONObject().apply {
                                    put("type", "OBJECT")
                                    put("properties", JSONObject())
                                })
                            })

                            // 4b. getTodaysNewCustomers
                            put(JSONObject().apply {
                                put("name", "getTodaysNewCustomers")
                                put("description", "Fetches the count and full details/list of new customers who joined or were added today.")
                                put("parameters", JSONObject().apply {
                                    put("type", "OBJECT")
                                    put("properties", JSONObject())
                                })
                            })

                            // 5. getTodayPayments
                            put(JSONObject().apply {
                                put("name", "getTodayPayments")
                                put("description", "Fetches all payments received today, total amount collected today, and list of customers who made payments today.")
                                put("parameters", JSONObject().apply {
                                    put("type", "OBJECT")
                                    put("properties", JSONObject())
                                })
                            })

                            // 5b. getWeeklyCollection
                            put(JSONObject().apply {
                                put("name", "getWeeklyCollection")
                                put("description", "Fetches total payments collected in the past 7 days/this week, active customers, total goods given, and list of customer payments.")
                                put("parameters", JSONObject().apply {
                                    put("type", "OBJECT")
                                    put("properties", JSONObject())
                                })
                            })

                            // 6. getMonthlySummary
                            put(JSONObject().apply {
                                put("name", "getMonthlySummary")
                                put("description", "Fetches full month financial summary including total collections this month, total goods given, active customers, and dues.")
                                put("parameters", JSONObject().apply {
                                    put("type", "OBJECT")
                                    put("properties", JSONObject().apply {
                                        put("month", JSONObject().apply {
                                            put("type", "INTEGER")
                                            put("description", "1-12 for month (optional, defaults to current month)")
                                        })
                                        put("year", JSONObject().apply {
                                            put("type", "INTEGER")
                                            put("description", "Year, e.g. 2026 (optional)")
                                        })
                                    })
                                })
                            })

                            // 7. getAreaFolderCollection
                            put(JSONObject().apply {
                                put("name", "getAreaFolderCollection")
                                put("description", "Fetches total collection, total dues, customer count, and breakdown for a specific Area or Folder (e.g. 'Ghana', 'घाना', 'Adhartal', 'अधारताल', 'Ranjhi', etc.)")
                                put("parameters", JSONObject().apply {
                                    put("type", "OBJECT")
                                    put("properties", JSONObject().apply {
                                        put("areaOrFolderName", JSONObject().apply {
                                            put("type", "STRING")
                                            put("description", "Name of the area or folder to look up collection for")
                                        })
                                    })
                                    put("required", JSONArray().apply { put("areaOrFolderName") })
                                })
                            })
                        })
                    })
                })
            }

            val httpRequest = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e("GeminiAssistant", "HTTP Error: ${response.code} body: $responseBody")
                return@withContext GeminiParsedResponse.ErrorResponse("HTTP_${response.code}")
            }

            val responseObj = JSONObject(responseBody)
            val candidates = responseObj.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return@withContext GeminiParsedResponse.ErrorResponse("NO_CANDIDATE")
            }

            val firstCandidate = candidates.getJSONObject(0)
            val content = firstCandidate.optJSONObject("content")
            val parts = content?.optJSONArray("parts")

            if (parts != null && parts.length() > 0) {
                val firstPart = parts.getJSONObject(0)

                // Check for functionCall
                if (firstPart.has("functionCall")) {
                    val functionCallObj = firstPart.getJSONObject("functionCall")
                    val functionName = functionCallObj.getString("name")
                    val argsObj = functionCallObj.optJSONObject("args")
                    val argsMap = mutableMapOf<String, Any?>()

                    argsObj?.keys()?.forEach { key ->
                        argsMap[key] = argsObj.get(key)
                    }

                    return@withContext GeminiParsedResponse.FunctionCallRequest(
                        functionName = functionName,
                        args = argsMap
                    )
                }

                // Standard text response
                if (firstPart.has("text")) {
                    val text = firstPart.getString("text")
                    return@withContext GeminiParsedResponse.TextResponse(text)
                }
            }

            return@withContext GeminiParsedResponse.ErrorResponse("UNPARSABLE_RESPONSE")

        } catch (e: Exception) {
            Log.e("GeminiAssistant", "Failed calling Gemini API", e)
            return@withContext GeminiParsedResponse.ErrorResponse(e.message ?: "UNKNOWN_ERROR")
        }
    }

    suspend fun generateNaturalExplanation(
        userQuery: String,
        functionName: String,
        executionSummaryJson: String
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "डेटा सफलतापूर्वक फेच किया गया।"
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        try {
            val promptText = """
                The user asked: "$userQuery"
                The system executed the function "$functionName" and obtained this database result:
                $executionSummaryJson
                
                Please write a polite, clear, 1-2 sentence response in polite Hindi (or Hinglish) explaining the key finding to the shopkeeper.
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", promptText) })
                        })
                    })
                })
            }

            val httpRequest = Request.Builder()
                .url(url)
                .post(requestJson.toString().toRequestBody(jsonMediaType))
                .build()

            val response = client.newCall(httpRequest).execute()
            val body = response.body?.string() ?: return@withContext "डेटा फेच कर लिया गया है।"
            val json = JSONObject(body)
            val text = json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.optJSONObject(0)
                ?.optString("text")

            text ?: "डेटा फेच कर लिया गया है।"
        } catch (e: Exception) {
            "डेटा फेच कर लिया गया है।"
        }
    }
}
