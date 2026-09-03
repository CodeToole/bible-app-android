package com.waitaminutedigital.biblestudy

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform