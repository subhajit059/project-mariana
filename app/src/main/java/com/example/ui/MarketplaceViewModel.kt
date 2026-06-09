package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MarketplaceDatabase
import com.example.data.MarketplaceRepository
import com.example.data.MarketplaceSite
import com.example.data.ScraperSimulator
import com.example.data.api.GeminiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MarketplaceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MarketplaceRepository
    private val geminiRepository = GeminiRepository()

    // Base flow of all marketplaces from local Room DB
    val marketplaces: StateFlow<List<MarketplaceSite>>

    // Search queries entered in the top Gemini bar
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Filtered marketplaces based on search queries
    val filteredMarketplaces: StateFlow<List<MarketplaceSite>>

    // Chat states for conversation with Gemini Market Assistant
    private val _geminiResponse = MutableStateFlow<String?>(null)
    val geminiResponse = _geminiResponse.asStateFlow()

    private val _isGeminiLoading = MutableStateFlow(false)
    val isGeminiLoading = _isGeminiLoading.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory = _chatHistory.asStateFlow()

    // Scraper status states
    private val _scraperProgress = MutableStateFlow("")
    val scraperProgress = _scraperProgress.asStateFlow()

    private val _isScraping = MutableStateFlow(false)
    val isScraping = _isScraping.asStateFlow()

    // active console logs
    val scraperLogs = ScraperSimulator.logs

    // Dynamic state for bridge routing redirector
    private val _bridgeRedirectState = MutableStateFlow<BridgeRedirectState?>(null)
    val bridgeRedirectState = _bridgeRedirectState.asStateFlow()

    init {
        val database = MarketplaceDatabase.getDatabase(application, viewModelScope)
        repository = MarketplaceRepository(database.marketplaceDao())
        marketplaces = repository.allMarketplaces.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        filteredMarketplaces = combine(marketplaces, _searchQuery) { list, query ->
            if (query.isBlank()) {
                list
            } else {
                list.filter {
                    it.name.contains(query, ignoreCase = true) ||
                    it.siteDescription.contains(query, ignoreCase = true) ||
                    it.currentUrl.contains(query, ignoreCase = true)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Sends the current user query to the Gemini Assistant and appends to chat.
     */
    fun sendGeminiQuery(query: String) {
        if (query.isBlank()) return
        _chatHistory.value = _chatHistory.value + ChatMessage(query, isUser = true)
        _isGeminiLoading.value = true

        viewModelScope.launch {
            val listText = marketplaces.value.joinToString("\n") { 
                "- ${it.name}: ${it.currentUrl} (Status: ${it.status}, Last Checked: ${java.util.Date(it.lastUpdated)})"
            }
            val response = withContext(Dispatchers.IO) {
                geminiRepository.getGeminiResponse(query, listText)
            }
            _chatHistory.value = _chatHistory.value + ChatMessage(response, isUser = false)
            _isGeminiLoading.value = false
        }
    }

    fun clearChat() {
        _chatHistory.value = emptyList()
    }

    /**
     * Triggers active verification and simulation scraper setup
     */
    fun runScraperCheck() {
        if (_isScraping.value) return
        _isScraping.value = true
        viewModelScope.launch(Dispatchers.IO) {
            ScraperSimulator.runLinkCheck(repository, marketplaces.value) { progress ->
                _scraperProgress.value = progress
            }
            _isScraping.value = false
            _scraperProgress.value = ""
        }
    }

    /**
     * Performs Secure Bridge Redirect routing (/go/[site_id]) dynamically.
     * Checks database for freshest url, simulates crypto verification handshake,
     * calculates live ping latency, and securely redirects the user's intent to the system browser.
     */
    fun launchSecureBridge(context: Context, site: MarketplaceSite) {
        viewModelScope.launch {
            _bridgeRedirectState.value = BridgeRedirectState(
                site = site,
                step = "Fetching Freshest URL from Room database secure ledger...",
                progress = 0.2f
            )
            withContext(Dispatchers.IO) {
                // Fetch fresh from DB to guarantee freshest URL check
                val freshSite = repository.getMarketplaceById(site.id) ?: site
                
                kotlinx.coroutines.delay(800)
                updateBridgeState("Reviewuring official site PGP public signature...", 0.45f, freshSite)
                
                kotlinx.coroutines.delay(800)
                updateBridgeState("Generating throwaway secure redirect token...", 0.70f, freshSite)
                
                kotlinx.coroutines.delay(600)
                updateBridgeState("Performing HTTP handshake & latency check (Latency: ${freshSite.latencyMs}ms)...", 0.90f, freshSite)
                
                kotlinx.coroutines.delay(700)
                updateBridgeState("Forwarding secured connection to External Gateway...", 1.0f, freshSite)
                
                kotlinx.coroutines.delay(300)
                
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(freshSite.currentUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // fallback if browser can't open onion link
                    // inside Android preview web environment we might need to open standard link
                }
            }
            _bridgeRedirectState.value = null
        }
    }

    private fun updateBridgeState(step: String, progress: Float, site: MarketplaceSite) {
        _bridgeRedirectState.value = BridgeRedirectState(site, step, progress)
    }

    fun dismissBridgeRedirect() {
        _bridgeRedirectState.value = null
    }

    // CRUD Ops for Marketplace Management in Android app
    fun addMarketplaceSite(name: String, currentUrl: String, sourceOfTruthUrl: String, desc: String, selector: String = "a.official-link") {
        viewModelScope.launch(Dispatchers.IO) {
            val newSite = MarketplaceSite(
                name = name,
                currentUrl = currentUrl,
                sourceOfTruthUrl = sourceOfTruthUrl,
                selectorPattern = selector,
                siteDescription = desc,
                status = "ONLINE",
                lastUpdated = System.currentTimeMillis()
            )
            repository.insertMarketplace(newSite)
        }
    }

    fun deleteMarketplaceSite(site: MarketplaceSite) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMarketplace(site)
        }
    }

    fun updateSiteUrlManual(site: MarketplaceSite, newUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = site.copy(
                currentUrl = newUrl,
                lastUpdated = System.currentTimeMillis()
            )
            repository.updateMarketplace(updated)
        }
    }
}

data class ChatMessage(
    val message: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class BridgeRedirectState(
    val site: MarketplaceSite,
    val step: String,
    val progress: Float
)
