package com.hightech.cryptofeed.loadCache

import com.hightech.cryptofeed.cache.CacheCryptoFeedUseCase
import com.hightech.cryptofeed.cache.CryptoFeedStore
import com.hightech.cryptofeed.cache.LocalCoinInfo
import com.hightech.cryptofeed.cache.LocalCryptoFeed
import com.hightech.cryptofeed.cache.LocalRaw
import com.hightech.cryptofeed.cache.LocalUsd
import com.hightech.cryptofeed.domain.CoinInfo
import com.hightech.cryptofeed.domain.CryptoFeed
import com.hightech.cryptofeed.domain.Raw
import com.hightech.cryptofeed.domain.Usd
import io.mockk.confirmVerified
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.UUID

class CacheLoadCryptoFeedUseCaseTest {



//    #### Primary Course (Happy Path):
//    1. Execute "Load Crypto Feed" command.
//    2. System retrieves feed data from cache.
//    3. System validates cache is less than one day old.
//    4. System creates crypto feed from cached data.
//    5. System delivers crypto feed.
//
//    #### Expired Cache Course (Sad Path):
//    1. System deletes cache.
//    2. System delivers no crypto feed.
//
//    #### Empty Cache Course (Sad Path):
//    1. System delivers no crypto feed.
//
//    #### Retrieval Error - Error Course (Sad Path):
//    1. System delivers error.

    private val store = spyk<CryptoFeedStore>()
    private lateinit var sut: CacheCryptoFeedUseCase

    private val timestamp = Date()

    @Before
    fun setUp() {
        sut = CacheCryptoFeedUseCase(store = store, timestamp)
    }


    @Test
    fun testLoadCryptoFeedCache() = runBlocking {
        verify(exactly = 0) {
            store.loadData()
        }


        confirmVerified(store)
    }

    @Test
    fun testRetrieveFeedDataFromCache() = runBlocking {

        assertEquals(uniqueItems().first.isNotEmpty(), true)
    }

    private fun uniqueCryptoFeed(): CryptoFeed {
        return CryptoFeed(
            CoinInfo(
                UUID.randomUUID().toString(),
                "any",
                "any",
                "any-url"
            ),
            Raw(
                Usd(
                    1.0,
                    1F,
                )
            )
        )
    }

    fun uniqueItems(): Pair<List<CryptoFeed>, List<LocalCryptoFeed>> {
        val cryptoFeeds = listOf(uniqueCryptoFeed(), uniqueCryptoFeed())
        val localCryptoFeed = cryptoFeeds.map {
            LocalCryptoFeed(
                coinInfo = LocalCoinInfo(
                    id = it.coinInfo.id,
                    name = it.coinInfo.name,
                    fullName = it.coinInfo.fullName,
                    imageUrl = it.coinInfo.imageUrl
                ),
                raw = LocalRaw(
                    usd = LocalUsd(
                        price = it.raw.usd.price,
                        changePctDay = it.raw.usd.changePctDay
                    )
                )
            )
        }
        return Pair(cryptoFeeds, localCryptoFeed)
    }
}