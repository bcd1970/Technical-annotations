package com.bcd.technotes.core.processing

import kotlin.math.pow

const val LUM_R = 0.2126f
const val LUM_G = 0.7152f
const val LUM_B = 0.0722f

fun srgbToLinear(c: Float): Float {
    return if (c <= 0.04045f) {
        c / 12.92f
    } else {
        ((c + 0.055f) / 1.055f).pow(2.4f)
    }
}

fun linearToSrgb(c: Float): Float {
    return if (c <= 0.0031308f) {
        c * 12.92f
    } else {
        1.055f * c.pow(1f / 2.4f) - 0.055f
    }
}

fun extractLuminance(linearR: Float, linearG: Float, linearB: Float): Float {
    return LUM_R * linearR + LUM_G * linearG + LUM_B * linearB
}
