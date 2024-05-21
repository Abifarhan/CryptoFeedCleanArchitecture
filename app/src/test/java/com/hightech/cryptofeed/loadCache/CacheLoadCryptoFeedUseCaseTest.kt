package com.hightech.cryptofeed.loadCache

import app.cash.turbine.test
import com.hightech.cryptofeed.api.ConnectivityException
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
        expect(
            sut = sut,
            receivedResult = LocalClientResult.Success(items),
            expectedResult = items.firstOrNull()?.coinInfo?.name ?: "",
            exactly = 1
        )
    }


    @Test
    fun createsCryptoFeedFromCacheDataAndDeliver() = runBlocking {
        val items = uniqueCryptoFeedLocal()
        expect(
            sut = sut,
            receivedResult = LocalClientResult.Success(items),
            expectedResult = items.firstOrNull()?.coinInfo?.name ?: "",
            exactly = 1,
        )
    }

    @Test
    fun testSystemDeleteCache() = runBlocking {
        every {
            store.deleteCache()
        } returns flowOf()


        sut.save(uniqueItems().first).test {
            awaitComplete()
        }

        verify(exactly = 1) {
            store.deleteCache()
        }

        confirmVerified(store)
    }

    @Test
    fun testSystemDeliverNoCryptoFeedAndError() = runBlocking {
        val items = emptyList<LocalCryptoFeed>()

        every {
            store.loadData()
        } returns flowOf(LocalClientResult.Success(items))


        sut.loadData().test {
            when(val receivedResult = awaitItem()){
                is LoadCryptoFeedResult.Success -> {
                    assertEquals(receivedResult.cryptoFeed.isEmpty(), true)
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
    fun testSystemDeliverError() = runBlocking {
        expect(
            sut = sut,
            receivedResult = LocalClientResult.Failure(anyException()),
            expectedResult = anyException(),
            exactly = 1
        )
    }

    private fun uniqueCryptoFeedLocal(): List<LocalCryptoFeed> {
        return listOf(
            LocalCryptoFeed(
                LocalCoinInfo(
                    (
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
            )
        )
    }

    private fun isDataLessThan24HoursOld(timestamp: Date): Boolean {
        val currentTime = Date()
        val difference = currentTime.time - timestamp.time
        val millisecondsIn24Hours = 24 * 60 * 60 * 1000
        return difference < millisecondsIn24Hours
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

    private fun expect(
        sut: CacheCryptoFeedUseCase,
        receivedResult: LocalClientResult,
        expectedResult: Any,
        exactly: Int = -1,
    ) = runBlocking {

        every {
            store.loadData()
        } returns flowOf(receivedResult)

        sut.loadData().test {
            when (val resultProcess = awaitItem()) {
                is LoadCryptoFeedResult.Success -> {
                    if (resultProcess.cryptoFeed.isEmpty()) {
                        assertEquals(resultProcess.cryptoFeed.isEmpty(), true)
                    } else {
                        assertEquals(
                            expectedResult,
                            resultProcess.cryptoFeed.firstOrNull()?.coinInfo?.name
                        )
                        assertEquals(isDataLessThan24HoursOld(timestamp), true)
                    }
                }

                is LoadCryptoFeedResult.Failure -> {
                    assertEquals(receivedResult, receivedResult)
                }
            }

            awaitComplete()
        }

        verify(exactly = exactly) {
            store.loadData()
        }
        confirmVerified(store)
    }

    private fun anyException(): Exception {
        return Exception()
    }
}