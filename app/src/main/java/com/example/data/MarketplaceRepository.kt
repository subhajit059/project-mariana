package com.example.data

import kotlinx.coroutines.flow.Flow

class MarketplaceRepository(private val marketplaceDao: MarketplaceDao) {
    val allMarketplaces: Flow<List<MarketplaceSite>> = marketplaceDao.getAllMarketplaces()

    suspend fun getMarketplaceById(id: Int): MarketplaceSite? {
        return marketplaceDao.getMarketplaceById(id)
    }

    suspend fun insertMarketplace(site: MarketplaceSite): Long {
        return marketplaceDao.insertMarketplace(site)
    }

    suspend fun updateMarketplace(site: MarketplaceSite) {
        return marketplaceDao.updateMarketplace(site)
    }

    suspend fun deleteMarketplace(site: MarketplaceSite) {
        return marketplaceDao.deleteMarketplace(site)
    }

    suspend fun deleteMarketplaceById(id: Int) {
        return marketplaceDao.deleteMarketplaceById(id)
    }
}
