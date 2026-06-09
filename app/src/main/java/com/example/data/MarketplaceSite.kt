package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "marketplaces")
data class MarketplaceSite(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val currentUrl: String,
    val sourceOfTruthUrl: String,
    val selectorPattern: String = "a.official-link",
    val status: String = "ONLINE", // ONLINE, OFFLINE, VERIFYING, REDIRECTING
    val latencyMs: Int = 0,
    val siteDescription: String = "",
    val lastUpdated: Long = System.currentTimeMillis()
)
