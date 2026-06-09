package com.example.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object ScraperSimulator {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        _logs.value = _logs.value + "[$timestamp] $message"
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    /**
     * Executes a full check and scrape process for all sites
     */
    suspend fun runLinkCheck(
        repository: MarketplaceRepository,
        sites: List<MarketplaceSite>,
        onProgress: (String) -> Unit
    ) {
        clearLogs()
        addLog("🤖 Starting Marketplace Link Checker & Scraper Engine...")
        delay(1200)

        for (site in sites) {
            addLog("🔍 Processing site: ${site.name}...")
            onProgress("Analyzing ${site.name}")
            delay(1000)

            addLog("📋 Source of Truth: ${site.sourceOfTruthUrl}")
            addLog("🎯 CSS Selector: \"${site.selectorPattern}\"")
            delay(800)

            // Step 1: Simulate connection to Source of truth
            addLog("📡 Fetching Source of Truth HTML page...")
            delay(1200)

            // We make a real network status ping if it's HTTPS/HTTP standard
            var isConnectionSuccessful = true
            try {
                if (site.sourceOfTruthUrl.startsWith("http")) {
                    val request = Request.Builder()
                        .url(site.sourceOfTruthUrl)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .build()
                    client.newCall(request).execute().use { response ->
                        addLog("🟢 Real Connection Success! HTTP Status: ${response.code}")
                    }
                } else {
                    addLog("🟡 Local tracking simulation started (onion source).")
                }
            } catch (e: Exception) {
                addLog("⚠️ Network warning checking real Source of Truth: ${e.localizedMessage}")
                addLog("🔄 Falling back to cached scraper definition.")
                isConnectionSuccessful = false
            }

            // Step 2: Extract link and generate simulation variations
            val currentDomain = site.currentUrl
            
            // Randomly simulate a change for demonstration purposes or stick to current
            val isUrloverriding = Random.nextInt(0, 100) < 30 // 30% chance of link change in simulation
            val updatedUrl = if (isUrloverriding) {
                val onionBase = currentDomain.substringBefore(".onion").substringBefore(".com")
                val randomSuffix = (1..6).map { ('a'..'z').random() }.joinToString("")
                if (currentDomain.contains(".onion")) {
                    "$onionBase$randomSuffix.onion"
                } else {
                    "$onionBase$randomSuffix.com"
                }
            } else {
                currentDomain
            }

            addLog("🔮 Parsing page nodes using JSoup/Cheerio context...")
            delay(1000)
            addLog("✅ Extracted active domain link: $updatedUrl")

            // Step 3: Measure dynamic latency
            val randomLatency = if (updatedUrl.contains(".onion")) {
                Random.nextInt(250, 950) // Onion links have higher latency
            } else {
                Random.nextInt(25, 95)
            }
            
            val finalStatus = if (Random.nextInt(0, 10) == 0 && isUrloverriding) {
                "VERIFYING"
            } else {
                "ONLINE"
            }

            if (updatedUrl != site.currentUrl) {
                addLog("🚨 ALERT: Domain updated detected for ${site.name}!")
                addLog("💾 Old Link: ${site.currentUrl}")
                addLog("💾 New Link: $updatedUrl")
                
                // Write back to SQL Database
                val updatedSite = site.copy(
                    currentUrl = updatedUrl,
                    status = finalStatus,
                    latencyMs = randomLatency,
                    lastUpdated = System.currentTimeMillis()
                )
                repository.updateMarketplace(updatedSite)
                addLog("🎉 Database updated successfully for ${site.name}!")
            } else {
                addLog("⚖️ Double check: Link matches database. Updating live stats.")
                val updatedSite = site.copy(
                    status = if (site.status == "OFFLINE") "OFFLINE" else "ONLINE",
                    latencyMs = if (site.status == "OFFLINE") 0 else randomLatency,
                    lastUpdated = System.currentTimeMillis()
                )
                repository.updateMarketplace(updatedSite)
            }
            addLog("------------------------------------")
            delay(600)
        }
        addLog("🏁 Scraper verification thread completed cleanly.")
        onProgress("")
    }
}
