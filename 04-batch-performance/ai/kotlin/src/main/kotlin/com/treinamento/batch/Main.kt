package com.treinamento.batch

import java.io.File
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * CLI entry point for the batch processor.
 *
 * Usage:
 *   ./gradlew run --args="<csv-file>"
 *   ./gradlew run --args="--generate <path> <count>"
 *   ./gradlew run --args="--generate-and-run <path> <count>"
 */
fun main(args: Array<String>) {
    when {
        args.isEmpty() -> {
            printUsage()
            return
        }

        args[0] == "--generate" -> {
            if (args.size < 3) { printUsage(); return }
            val path  = args[1]
            val count = args[2].toIntOrNull() ?: run {
                System.err.println("ERROR: <count> must be an integer, got '${args[2]}'")
                return
            }
            DataGenerator.generateToPath(count, path)
        }

        args[0] == "--generate-and-run" -> {
            if (args.size < 3) { printUsage(); return }
            val path  = args[1]
            val count = args[2].toIntOrNull() ?: run {
                System.err.println("ERROR: <count> must be an integer, got '${args[2]}'")
                return
            }
            DataGenerator.generateToPath(count, path)
            processFile(path)
        }

        else -> processFile(args[0])
    }
}

// -- File processing ----------------------------------------------------------

private fun processFile(path: String) {
    val file = File(path)
    if (!file.exists()) {
        System.err.println("Error reading file \"$path\": File not found")
        System.err.println("Hint: generate a dataset first with --generate <path> <count>")
        return
    }

    println("Reading transactions from: $path")

    val transactions: List<Transaction> = try {
        file.bufferedReader().useLines { lines ->
            lines
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("id,") }
                .map { line ->
                    runCatching { Transaction.fromCsvLine(line) }.getOrNull()
                }
                .filterNotNull()
                .toList()
        }
    } catch (ex: Exception) {
        System.err.println("Error reading file \"$path\": ${ex.message}")
        return
    }

    val nf = NumberFormat.getNumberInstance(Locale.US)
    println("Loaded ${nf.format(transactions.size)} transactions.\n")

    if (transactions.isEmpty()) {
        println("No valid transactions found - nothing to process.")
        return
    }

    val dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    println("Inicio: ${LocalDateTime.now().format(dtf)}\n")

    val processor = BatchProcessor()

    val startTime = System.nanoTime()
    val result = processor.process(transactions)
    val elapsed = (System.nanoTime() - startTime) / 1_000_000_000.0
    val elapsedStr = "%.3f".format(java.util.Locale.US, elapsed)

    println("\nTermino: ${LocalDateTime.now().format(dtf)}\n")

    // Summary output
    println("=".repeat(60))
    println("  BATCH PROCESSING RESULTS")
    println("=".repeat(60))
    println("  Processed   : ${nf.format(result.processed)}")
    println("  Duplicates  : ${nf.format(result.duplicates)}")
    println("  Rejected    : ${nf.format(result.rejected)}")
    println("  Fraud rings : ${nf.format(result.fraudRings)}")
    println("  Time        : ${elapsedStr}s")
    println("=".repeat(60))

    println("\nCategory Report:")
    println("  " + listOf(
        "Category".padEnd(16),
        "Count".padStart(8),
        "Total".padStart(14),
        "Average".padStart(12)
    ).joinToString("  "))
    println("  " + "-".repeat(54))
    for (stat in result.report) {
        println("  " + listOf(
            stat.category.padEnd(16),
            stat.count.toString().padStart(8),
            "%.2f".format(java.util.Locale.US, stat.total).padStart(14),
            "%.2f".format(java.util.Locale.US, stat.average).padStart(12)
        ).joinToString("  "))
    }

    println()
}

// -- Help ---------------------------------------------------------------------

private fun printUsage() {
    println("""
        Batch Transaction Processor - Performance Challenge

        USAGE:
          ./gradlew run --args="<csv-file>"
              Process an existing CSV file.

          ./gradlew run --args="--generate <path> <count>"
              Generate a synthetic CSV with <count> transactions.

          ./gradlew run --args="--generate-and-run <path> <count>"
              Generate and immediately process.

        EXAMPLES:
          ./gradlew run --args="--generate data/transactions_10k.csv 10000"
          ./gradlew run --args="data/transactions_10k.csv"
          ./gradlew run --args="--generate-and-run data/transactions_1k.csv 1000"
    """.trimIndent())
}
