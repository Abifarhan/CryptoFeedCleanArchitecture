package com.hightech.cryptofeed.cache

import kotlinx.coroutines.flow.Flow
import java.util.Date

interface CryptoFeedStore {
    fun deleteCache(): Flow<Exception?>
    fun insert(feeds: List<LocalCryptoFeed>, timestamp: Date): Flow<Exception?>

    fun loadData() : Flow<LocalClientResult>
}


sealed class LocalClientResult {
    data class Success(val root : List<LocalCryptoFeed>) : LocalClientResult()

    data class Failure(val exception: Exception) : LocalClientResult()
}