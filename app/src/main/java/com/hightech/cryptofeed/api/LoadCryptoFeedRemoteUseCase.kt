package com.hightech.cryptofeed.api

import com.hightech.cryptofeed.domain.BadRequest
import com.hightech.cryptofeed.domain.CoinInfo
import com.hightech.cryptofeed.domain.Connectivity
import com.hightech.cryptofeed.domain.CryptoFeed
import com.hightech.cryptofeed.domain.InternalServerError
import com.hightech.cryptofeed.domain.InvalidData
import com.hightech.cryptofeed.domain.LoadCryptoFeedResult
import com.hightech.cryptofeed.domain.NotFound
import com.hightech.cryptofeed.domain.Raw
import com.hightech.cryptofeed.domain.Unexpected
import com.hightech.cryptofeed.domain.Usd
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LoadCryptoFeedRemoteUseCase constructor(
    private val client: HttpClient
) {
    fun load(): Flow<LoadCryptoFeedResult> = flow {
        client.get().collect { result ->
            when (result) {
                is HttpClientResult.Success -> {
                    emit(LoadCryptoFeedResult.Success(result.root.data.toModels()))
                }
                is HttpClientResult.Failure -> {
                    when (result.exception) {
                        is ConnectivityException -> {
                            emit(LoadCryptoFeedResult.Failure(Connectivity()))
                        }

                        is InvalidDataException -> {
                            emit(LoadCryptoFeedResult.Failure(InvalidData()))
                        }

                        is BadRequestException -> {
                            emit(LoadCryptoFeedResult.Failure(BadRequest()))
                        }

                        is NotFoundException -> {
                            emit(LoadCryptoFeedResult.Failure(NotFound()))
                        }

                        is InternalServerErrorException -> {
                            emit(LoadCryptoFeedResult.Failure(InternalServerError()))
                        }

                        is UnexpectedException -> {
                            emit(LoadCryptoFeedResult.Failure(Unexpected()))
                        }
                    }
                }
            }
        }
    }
}

private fun List<RemoteCryptoFeed>.toModels(): List<CryptoFeed> {
    return map {
        CryptoFeed(
            CoinInfo(
                it.remoteCoinInfo.id,
                it.remoteCoinInfo.name,
                it.remoteCoinInfo.fullName,
                it.remoteCoinInfo.imageUrl
            ),
            Raw(
                Usd(
                    it.remoteRaw.remoteUsd.price,
                    it.remoteRaw.remoteUsd.changePctDay,
                ),
            ),
        )
    }
}