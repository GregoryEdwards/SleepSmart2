package com.sleepsmart.app.classifier

import javax.inject.Inject

/**
 * Causal smoother that prevents implausible 1-epoch flips. Maintains the last
 * 5 estimates; if the current stage differs from both immediate neighbours and
 * from the modal stage of the last 5, replace it with the modal stage and lower
 * confidence by 0.10.
 */
class HmmSmoother @Inject constructor() {
    private val window = ArrayDeque<StageEstimate>() // raw inputs
    private val out = ArrayDeque<StageEstimate>()    // smoothed outputs
    private val cap = 5

    fun reset() { window.clear(); out.clear() }

    fun process(estimate: StageEstimate): StageEstimate {
        window.addLast(estimate)
        while (window.size > cap) window.removeFirst()

        if (window.size < 3) {
            out.addLast(estimate)
            while (out.size > cap) out.removeFirst()
            return estimate
        }

        val list = window.toList()
        val current = list.last()
        val left = list[list.size - 2]
        val rightCandidate: StageEstimate? = null // causal — no right neighbour available

        // Condition: current differs from immediate left neighbour AND from modal stage.
        val modal = list.modalStage()
        val differsFromNeighbour = current.stage != left.stage
        val differsFromModal = current.stage != modal

        return if (differsFromNeighbour && differsFromModal) {
            val replaced = StageEstimate(modal, (current.confidence - 0.10f).coerceAtLeast(0f))
            out.addLast(replaced)
            while (out.size > cap) out.removeFirst()
            replaced
        } else {
            out.addLast(current)
            while (out.size > cap) out.removeFirst()
            current
        }
    }

    private fun List<StageEstimate>.modalStage(): SleepStage {
        val counts = HashMap<SleepStage, Int>()
        for (e in this) counts.merge(e.stage, 1) { a, b -> a + b }
        return counts.maxBy { it.value }.key
    }
}
