package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketplaceDao {
    @Query("SELECT * FROM marketplaces ORDER BY name ASC")
    fun getAllMarketplaces(): Flow<List<MarketplaceSite>>

    @Query("SELECT * FROM marketplaces WHERE id = :id LIMIT 1")
    suspend fun getMarketplaceById(id: Int): MarketplaceSite?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketplace(site: MarketplaceSite): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDefaultMarketplaces(sites: List<MarketplaceSite>)

    @Update
    suspend fun updateMarketplace(site: MarketplaceSite)

    @Delete
    suspend fun deleteMarketplace(site: MarketplaceSite)

    @Query("DELETE FROM marketplaces WHERE id = :id")
    suspend fun deleteMarketplaceById(id: Int)
}
