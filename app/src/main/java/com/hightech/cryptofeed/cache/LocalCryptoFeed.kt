package com.hightech.cryptofeed.cache

import java.util.UUID

data class LocalCryptoFeed(
    val coinInfo: LocalCoinInfo,
    val raw: LocalRaw
){
//    companion object {
//        val DEFAULT  = listOf(LocalCryptoFeed(
//            LocalCoinInfo((
//                    UUID.randomUUID().toString()),
//                "any",
//                "any",
//                "any-url"
//            ),
//            LocalRaw(
//                LocalUsd(
//                    1.0,
//                    1F,
//                )
//            )
//        ))
//    }
}

data class LocalCoinInfo(
    val id: String,
    val name: String,
    val fullName: String,
    val imageUrl: String
)

data class LocalRaw(
    val usd: LocalUsd
)

data class LocalUsd(
    val price: Double,
    val changePctDay: Float
)