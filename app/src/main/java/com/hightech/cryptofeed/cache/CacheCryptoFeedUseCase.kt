package com.hightech.cryptofeed.cache

import com.hightech.cryptofeed.api.BadRequestException
import com.hightech.cryptofeed.api.ConnectivityException
import com.hightech.cryptofeed.api.HttpClientResult
import com.hightech.cryptofeed.api.InternalServerErrorException
import com.hightech.cryptofeed.api.InvalidDataException
import com.hightech.cryptofeed.api.NotFoundException
import com.hightech.cryptofeed.api.RemoteCryptoFeed
import com.hightech.cryptofeed.api.UnexpectedException
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
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flow
import java.util.Date

typealias SaveResult = Exception?

class CacheCryptoFeedUseCase constructor(
    private val store: CryptoFeedStore,
    private val currentDate: Date
) {
    fun save(feed: List<CryptoFeed>): Flow<SaveResult> = flow {
        store.deleteCache().collect { deleteError ->
            if (deleteError != null) {
                emit(deleteError)
            } else {
                store.insert(feed.toLocal(), currentDate).collect { insertError ->
                    emit(insertError)
                }
            }
        }
    }

    private fun List<CryptoFeed>.toLocal(): List<LocalCryptoFeed> {
        return map {
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
    }

    fun loadData() : Flow<LoadCryptoFeedResult> = flow {
        store.loadData().collect{ result ->
            when (result) {
                is LocalClientResult.Success -> {
                    emit(LoadCryptoFeedResult.Success(result.root.toModels()))
                }
                is LocalClientResult.Failure -> {
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



    private fun List<LocalCryptoFeed>.toModels(): List<CryptoFeed> {
        return map {
            CryptoFeed(
                CoinInfo(
                    it.coinInfo.id,
                    it.coinInfo.name,
                    it.coinInfo.fullName,
                    it.coinInfo.imageUrl
                ),
                Raw(
                    Usd(
                        it.raw.usd.price,
                        it.raw.usd.changePctDay,
                    ),
                ),
            )
        }
    }

}


