package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [MarketplaceSite::class], version = 1, exportSchema = false)
abstract class MarketplaceDatabase : RoomDatabase() {
    abstract fun marketplaceDao(): MarketplaceDao

    companion object {
        @Volatile
        private var INSTANCE: MarketplaceDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): MarketplaceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MarketplaceDatabase::class.java,
                    "market_bridge_database_v2"
                )
                .addCallback(MarketplaceDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class MarketplaceDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.marketplaceDao()
                    // Seed initial data
                    val defaultMarkets = listOf(
                        MarketplaceSite(
                            name = "Indian M&S",
                            currentUrl = "https://moviesbaba.lol/",
                            sourceOfTruthUrl = "https://mmodlist.org/",
                            selectorPattern = "a.official-link",
                            status = "ONLINE",
                            latencyMs = 45,
                            siteDescription = "Indian Movies & Series hub. Backups configured: moviesleech.bar, rogmovies.club.",
                            lastUpdated = System.currentTimeMillis()
                        ),
                        MarketplaceSite(
                            name = "Hollywood market",
                            currentUrl = "https://moviesmod.army/",
                            sourceOfTruthUrl = "https://katworld.net/",
                            selectorPattern = "a.official-link",
                            status = "ONLINE",
                            latencyMs = 38,
                            siteDescription = "Hollywood motion picture and series catalog. Backups configured: vegamovies.mq, new1.katmoviehd.cymru.",
                            lastUpdated = System.currentTimeMillis()
                        ),
                        MarketplaceSite(
                            name = "4K market - M&S",
                            currentUrl = "https://uhdmovies.food/",
                            sourceOfTruthUrl = "https://www.modlist.in/",
                            selectorPattern = "a.official-link",
                            status = "ONLINE",
                            latencyMs = 52,
                            siteDescription = "High Definition 4K film and multi-channel audio archive. Backups configured: katmovie4k.mov.",
                            lastUpdated = System.currentTimeMillis()
                        ),
                        MarketplaceSite(
                            name = "ANIME market",
                            currentUrl = "https://new.pikahd.co/",
                            sourceOfTruthUrl = "https://www.modlist.in/",
                            selectorPattern = "a.official-link",
                            status = "ONLINE",
                            latencyMs = 61,
                            siteDescription = "HD Quality anime, animation, drama listings. Backups configured: modlist.in/?type=animeflix, gokuhd.com.",
                            lastUpdated = System.currentTimeMillis()
                        ),
                        MarketplaceSite(
                            name = "KOREAN market",
                            currentUrl = "https://www.katdrama.net/",
                            sourceOfTruthUrl = "https://vglist.top/",
                            selectorPattern = "a.official-link",
                            status = "ONLINE",
                            latencyMs = 49,
                            siteDescription = "Korean drama, feature films, variety show entries, and updates.",
                            lastUpdated = System.currentTimeMillis()
                        ),
                        MarketplaceSite(
                            name = "Software market",
                            currentUrl = "https://softwareleech.com/",
                            sourceOfTruthUrl = "https://vglist.top/",
                            selectorPattern = "a.official-link",
                            status = "ONLINE",
                            latencyMs = 32,
                            siteDescription = "Full-speed direct index utility of essential system tools, development items.",
                            lastUpdated = System.currentTimeMillis()
                        ),
                        MarketplaceSite(
                            name = "Game market",
                            currentUrl = "https://gamesleech.com/",
                            sourceOfTruthUrl = "https://vglist.top/",
                            selectorPattern = "a.official-link",
                            status = "ONLINE",
                            latencyMs = 41,
                            siteDescription = "Compressed and repackaged modern console and desktop game installer files.",
                            lastUpdated = System.currentTimeMillis()
                        )
                    )
                    dao.insertDefaultMarketplaces(defaultMarkets)
                }
            }
        }
    }
}
