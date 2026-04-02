package com.bcd.technotes.core.processing

class GuidedFilter(private val width: Int, private val height: Int) {

    private val n = width * height
    private val sat = DoubleArray(n)
    private val scratch = FloatArray(n)

    private fun boxFilter(input: FloatArray, radius: Int, output: FloatArray) {
        // Build summed area table (SAT) using DoubleArray for precision
        for (y in 0 until height) {
            for (x in 0 until width) {
                val idx = y * width + x
                sat[idx] = input[idx].toDouble() +
                    (if (x > 0) sat[idx - 1] else 0.0) +
                    (if (y > 0) sat[(y - 1) * width + x] else 0.0) -
                    (if (x > 0 && y > 0) sat[(y - 1) * width + x - 1] else 0.0)
            }
        }

        // Query box sums with clamped boundaries
        for (y in 0 until height) {
            for (x in 0 until width) {
                val x1 = (x - radius - 1).coerceAtLeast(-1)
                val y1 = (y - radius - 1).coerceAtLeast(-1)
                val x2 = (x + radius).coerceAtMost(width - 1)
                val y2 = (y + radius).coerceAtMost(height - 1)

                val area = (x2 - x1).toFloat() * (y2 - y1).toFloat()

                fun satVal(sy: Int, sx: Int): Double {
                    if (sy < 0 || sx < 0) return 0.0
                    return sat[sy * width + sx]
                }

                val sum = satVal(y2, x2) - satVal(y1, x2) - satVal(y2, x1) + satVal(y1, x1)
                output[y * width + x] = (sum / area).toFloat()
            }
        }
    }

    fun filter(luminance: FloatArray, radius: Int, eps: Float): FloatArray {
        require(luminance.size == n) { "Input size ${luminance.size} != $width x $height = $n" }

        // Step 1: meanI = boxFilter(I)
        val meanI = FloatArray(n)
        boxFilter(luminance, radius, meanI)

        // Step 2: corrII = boxFilter(I * I)
        for (i in 0 until n) {
            scratch[i] = luminance[i] * luminance[i]
        }
        val corrII = FloatArray(n)
        boxFilter(scratch, radius, corrII)

        // Step 3: a = varI / (varI + eps), b = meanI * (1 - a)
        // Reuse scratch for 'a', corrII for 'b'
        val a = scratch
        val b = corrII
        for (i in 0 until n) {
            val varI = b[i] - meanI[i] * meanI[i] // corrII - meanI^2
            a[i] = varI / (varI + eps)
            b[i] = meanI[i] * (1f - a[i])
        }

        // Step 4: meanA = boxFilter(a)
        val meanA = FloatArray(n)
        boxFilter(a, radius, meanA)

        // Step 5: meanB = boxFilter(b)
        val meanB = meanI // reuse meanI buffer
        boxFilter(b, radius, meanB)

        // Step 6: output = meanA * I + meanB
        val output = FloatArray(n)
        for (i in 0 until n) {
            output[i] = meanA[i] * luminance[i] + meanB[i]
        }
        return output
    }
}
