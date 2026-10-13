package ru.landilf.dactune.domain.engine

import kotlin.math.log10
import kotlin.math.sqrt

class SignalLevelAnalyzer {
    fun rmsDbfs(samples: FloatArray, count: Int): Double {
        require(count in 1..samples.size)
        var sum = 0.0
        for (index in 0 until count) {
            val value = samples[index].toDouble()
            sum += value * value
        }
        return 20.0 * log10(sqrt(sum / count).coerceAtLeast(1e-12))
    }
}
