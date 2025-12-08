package com.ishabdullah.aiish.math

import timber.log.Timber

/**
 * MathReasoner - Deterministic mathematical problem solving
 * Ported from Genesis with step-by-step calculation
 */
class MathReasoner {

    private val steps = mutableListOf<MathStep>()

    fun solve(query: String): String {
        steps.clear()

        return when {
            isRateProblem(query) -> solveRateProblem(query)
            isDifferenceProblem(query) -> solveDifferenceProblem(query)
            isArithmetic(query) -> solveArithmetic(query)
            else -> "Math problem type not recognized"
        }
    }

    private fun isRateProblem(query: String): Boolean {
        val pattern = Regex("\\d+\\s+(machines?|workers?|cats?)")
        return pattern.find(query.lowercase()) != null
    }

    private fun isDifferenceProblem(query: String): Boolean {
        return query.lowercase().contains("more than") && query.lowercase().contains("cost")
    }

    private fun isArithmetic(query: String): Boolean {
        return query.matches(Regex(".*\\d+\\s*[+\\-*/]\\s*\\d+.*"))
    }

    private fun solveRateProblem(query: String): String {
        val numbers = extractNumbers(query)

        if (numbers.size < 5) {
            return "Insufficient data for rate problem"
        }

        val (workers, units, time, targetUnits, targetTime) = numbers.take(5)

        val ratePerWorker = units / (workers * time)
        steps.add(MathStep(1, "Calculate rate per worker", "$units / ($workers × $time) = $ratePerWorker"))

        val requiredRate = targetUnits / targetTime
        steps.add(MathStep(2, "Calculate required rate", "$targetUnits / $targetTime = $requiredRate"))

        val workersNeeded = requiredRate / ratePerWorker
        steps.add(MathStep(3, "Calculate workers needed", "$requiredRate / $ratePerWorker = $workersNeeded"))

        return formatSteps() + "\n\n**Answer: ${workersNeeded.toInt()} workers**"
    }

    private fun solveDifferenceProblem(query: String): String {
        val numbers = extractNumbers(query)

        if (numbers.size < 2) {
            return "Insufficient data for difference problem"
        }

        val total = numbers[0]
        val difference = numbers[1]

        val smaller = (total - difference) / 2
        steps.add(MathStep(1, "Solve for smaller item", "x = ($total - $difference) / 2 = $smaller"))

        val larger = smaller + difference
        steps.add(MathStep(2, "Calculate larger item", "$smaller + $difference = $larger"))

        return formatSteps() + "\n\n**Answer: Smaller = $$smaller, Larger = $$larger**"
    }

    private fun solveArithmetic(query: String): String {
        val result = try {
            evaluateExpression(query)
        } catch (e: Exception) {
            Timber.e(e, "Arithmetic evaluation failed")
            return "Could not evaluate expression"
        }

        steps.add(MathStep(1, "Evaluate", "$query = $result"))

        return formatSteps() + "\n\n**Answer: $result**"
    }

    private fun extractNumbers(text: String): List<Double> {
        val pattern = Regex("\\d+\\.?\\d*")
        return pattern.findAll(text).map { it.value.toDouble() }.toList()
    }

    private fun evaluateExpression(expr: String): Double {
        // Basic arithmetic parser (simplified)
        val cleaned = expr.replace(Regex("[^0-9+\\-*/.]"), "")
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun formatSteps(): String {
        return steps.joinToString("\n\n") { step ->
            "**Step ${step.number}**: ${step.description}\n${step.calculation}"
        }
    }
}

data class MathStep(
    val number: Int,
    val description: String,
    val calculation: String
)
