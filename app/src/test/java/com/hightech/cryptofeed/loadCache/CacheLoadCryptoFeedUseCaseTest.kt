package com.hightech.cryptofeed.loadCache

import app.cash.turbine.test
import com.hightech.cryptofeed.cache.CacheCryptoFeedUseCase
import com.hightech.cryptofeed.cache.CryptoFeedStore
import com.hightech.cryptofeed.cache.LocalClientResult
import com.hightech.cryptofeed.cache.LocalCoinInfo
import com.hightech.cryptofeed.cache.LocalCryptoFeed
import com.hightech.cryptofeed.cache.LocalRaw
import com.hightech.cryptofeed.cache.LocalUsd
import com.hightech.cryptofeed.domain.CoinInfo
import com.hightech.cryptofeed.domain.CryptoFeed
import com.hightech.cryptofeed.domain.LoadCryptoFeedResult
import com.hightech.cryptofeed.domain.Raw
import com.hightech.cryptofeed.domain.Usd
import io.mockk.coEvery
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
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

    private val store = mockk<CryptoFeedStore>()
    private lateinit var sut: CacheCryptoFeedUseCase

    private val timestamp = Date()


    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        sut = CacheCryptoFeedUseCase(store = store, timestamp)
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun testRetrieveAndLoadFeedDataFromCache() = runBlocking {


        every {
            store.loadData()
        } returns flowOf()


        sut.loadData().test {
            awaitComplete()
        }

        verify(exactly = 1) {
            store.loadData()
        }

        confirmVerified(store)
    }

    @Test
    fun validateCacheIsLessThanOneDayOld() = runBlocking {
        val items = uniqueCryptoFeedLocal()

        every {
            store.loadData()
        } returns flowOf(LocalClientResult.Success(items))


        sut.loadData().test {

            when(val receivedResult = awaitItem()){
                is LoadCryptoFeedResult.Success -> {
                    assertEquals(isDataLessThan24HoursOld(timestamp), true)
                }

                else -> {

                }
            }
            awaitComplete()
        }

        verify(exactly = 1) {
            store.loadData()
        }

        confirmVerified(store)
    }


    @Test
    fun createsCryptoFeedFromCacheDataAndDeliver() = runBlocking {
        val items = uniqueCryptoFeedLocal()

        every {
            store.loadData()
        } returns flowOf(LocalClientResult.Success(items))


        sut.loadData().test {

            when(val receivedResult = awaitItem()){
                is LoadCryptoFeedResult.Success -> {
                    assertEquals(items.firstOrNull()?.coinInfo?.name, receivedResult.cryptoFeed.firstOrNull()?.coinInfo?.name)
                }


                else -> {

                }
            }
            awaitComplete()
        }

        verify(exactly = 1) {
            store.loadData()
        }

        confirmVerified(store)
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

    private fun uniqueCryptoFeedLocal(): List<LocalCryptoFeed> {
        return listOf(LocalCryptoFeed(
            LocalCoinInfo((
                UUID.randomUUID().toString()),
                "any",
                "any",
                "any-url"
            ),
            LocalRaw(
                LocalUsd(
                    1.0,
                    1F,
                )
            )
        ))
    }

    private fun isDataLessThan24HoursOld(timestamp: Date): Boolean {
        val currentTime = Date()
        val difference = currentTime.time - timestamp.time
        val millisecondsIn24Hours = 24 * 60 * 60 * 1000
        return difference < millisecondsIn24Hours
    }
}