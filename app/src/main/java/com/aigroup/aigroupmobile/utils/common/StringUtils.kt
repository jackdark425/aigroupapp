package com.aigroup.aigroupmobile.utils.common

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

fun String.maxLength(maxLength: Int): String {
    return if (this.length > maxLength) {
        this.substring(0, maxLength) + "..."
    } else {
        this
    }
}

fun String.sanitize(plainLen: Int = 2, secretChar: Char = '*'): String {
    if (this.isEmpty()) {
        return this
    }

    if (this.length <= plainLen) {
        return secretChar.toString().repeat(10)
    }

    val plain = this.substring(0, plainLen)
    val secret = secretChar.toString().repeat(10)
    return plain + secret
}

fun String.simulateTextAnimate(
    randomizationTextLenPerIter: Int = 3,
    intervalMs: Long = 16,
): Flow<String> {
    var textLen = 0
    val simulateList = fold(emptyList<String>()) { acc, char ->
        when (textLen) {
            0 -> {
                textLen = Random.nextInt(1, randomizationTextLenPerIter)
                acc + char.toString()
            }
            else -> {
                textLen--
                acc.dropLast(1) + (acc.last() + char)
            }
        }
    }

    return flow {
        simulateList.forEach { char ->
            emit(char)
            delay(intervalMs)
        }
    }
}