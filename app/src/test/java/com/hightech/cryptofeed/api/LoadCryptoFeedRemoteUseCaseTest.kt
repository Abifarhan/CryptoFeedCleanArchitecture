package com.hightech.cryptofeed.api

import app.cash.turbine.test
import com.hightech.cryptofeed.domain.BadRequest
import com.hightech.cryptofeed.domain.Connectivity
import com.hightech.cryptofeed.domain.CryptoFeed
import com.hightech.cryptofeed.domain.InternalServerError
import com.hightech.cryptofeed.domain.InvalidData
import com.hightech.cryptofeed.domain.LoadCryptoFeedResult
import com.hightech.cryptofeed.domain.NotFound
import com.hightech.cryptofeed.domain.Unexpected
import com.hightech.cryptofeed.domain.cryptoFeed
import io.mockk.clearAllMocks
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LoadCryptoFeedRemoteUseCaseTest {
    private val client = spyk<HttpClient>()
    private lateinit var sut: LoadCryptoFeedRemoteUseCase

    @Before
    fun setUp() {
        sut = LoadCryptoFeedRemoteUseCase(client)
    }

    @Test
    fun testInitDoesNotRequestData() {
        verify(exactly = 0) {
            client.get()
        }

        confirmVerified(client)
    }

    @Test
    fun testLoadRequestsData() = runBlocking {
        every {
            client.get()
        } returns flowOf()

        sut.load().test {
            awaitComplete()
        }

        verify(exactly = 1) {
            client.get()
        }

        confirmVerified(client)
    }

    @Test
    fun testLoadTwiceRequestsDataTwice() = runBlocking {
        every {
            client.get()
        } returns flowOf()

        sut.load().test {
            awaitComplete()
        }

        sut.load().test {
            awaitComplete()
        }

        verify(exactly = 2) {
            client.get()
        }

        confirmVerified(client)
    }


    @Test
    fun testLoadDeliversConnectivityErrorOnClientError() {
        expect(
            sut = sut,
            receivedHttpClientResult = HttpClientResult.Failure(ConnectivityException()),
            expectedResult = Connectivity(),
            exactly = 1,
        )
    }

    @Test
    fun testLoadDeliversInvalidDataError() {
        expect(
            sut = sut,
            receivedHttpClientResult = HttpClientResult.Failure(InvalidDataException()),
            expectedResult = InvalidData(),
            exactly = 1,
        )
    }

    @Test
    fun testLoadDeliversBadRequestError() {
        expect(
            sut = sut,
            receivedHttpClientResult = HttpClientResult.Failure(BadRequestException()),
            expectedResult = BadRequest(),
            exactly = 1,
        )
    }

    @Test
    fun testLoadDeliversNotFoundErrorOnClientError() {
        expect(
            sut = sut,
            receivedHttpClientResult = HttpClientResult.Failure(NotFoundException()),
            expectedResult = NotFound(),
            exactly = 1
        )
    }

    @Test
    fun testLoadDeliversInternalServerError() {
        expect(
            sut = sut,
            receivedHttpClientResult = HttpClientResult.Failure(InternalServerErrorException()),
            expectedResult = InternalServerError(),
            exactly = 1
        )
    }

    @Test
    fun testLoadDeliversUnexpectedError() {
        expect(
            sut = sut,
            receivedHttpClientResult = HttpClientResult.Failure((UnexpectedException())),
            expectedResult = Unexpected(),
            exactly = 1
        )
    }

    @Test
    fun testLoadDeliversCryptoFeedOnSuccessWithCryptoFeed() {
        expect(
            sut = sut,
            receivedHttpClientResult = HttpClientResult.Success(
                RemoteRootCryptoFeed(
                    remoteCryptoFeed
                )
            ),
            expectedResult = LoadCryptoFeedResult.Success(cryptoFeed),
            exactly = 1
        )
    }

    @Test
    fun testLoadDeliversEmptyCryptoFeedOnSuccessWithEmptyCryptoFeed() {
        val emptyRemoteCryptoFeed = emptyList<RemoteCryptoFeed>()
        val cryptoFeed = emptyList<CryptoFeed>()

        expect(
            sut = sut,
            receivedHttpClientResult = HttpClientResult.Success(
                RemoteRootCryptoFeed(
                    emptyRemoteCryptoFeed
                )
            ),
            expectedResult = LoadCryptoFeedResult.Success(cryptoFeed),
            exactly = 1
        )
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    private fun expect(
        sut: LoadCryptoFeedRemoteUseCase,
        receivedHttpClientResult: HttpClientResult,
        expectedResult: Any,
        exactly: Int = -1,
    ) = runBlocking {
        every {
            client.get()
        } returns flowOf(receivedHttpClientResult)

        sut.load().test {
            when (val receivedResult = awaitItem()) {
                is LoadCryptoFeedResult.Success -> {
                    assertEquals(
                        expectedResult,
                        receivedResult
                    )
                }

                is LoadCryptoFeedResult.Failure -> {
                    assertEquals(
                        expectedResult::class.java,
                        receivedResult.exception::class.java
                    )
                }
            }
            awaitComplete()
        }

        verify(exactly = exactly) {
            client.get()
        }

        confirmVerified(client)
    }
}