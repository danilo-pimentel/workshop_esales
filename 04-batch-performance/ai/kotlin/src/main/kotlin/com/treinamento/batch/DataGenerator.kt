package com.treinamento.batch

import java.io.File

/**
 * Generates synthetic CSV transaction files for benchmarking [BatchProcessor].
 *
 * IMPORTANT: This generator uses mulberry32 PRNG with seed=42 so that
 * ALL languages (Java, Kotlin, PHP, Bun) produce bit-identical CSV output.
 */
object DataGenerator {

    private const val ACCOUNTS_COUNT = 1_000
    private val CATEGORIES = arrayOf(
        "PAYMENT", "TRANSFER", "REFUND", "PURCHASE",
        "WITHDRAWAL", "DEPOSIT", "FEE", "INTEREST"
    )

    const val CSV_HEADER = "id,amount,source_account,dest_account,timestamp,category,priority"

    fun generate(count: Int, outputFile: File) {
        outputFile.parentFile?.mkdirs()
        val gen = Mulberry32(42)
        val base2024 = 1_704_067_200_000L

        data class PoolEntry(
            val amount: String, val source: String, val dest: String,
            val timestamp: Long, val category: String, val priority: Int
        )
        val recentPool = mutableListOf<PoolEntry>()
        val POOL_SIZE = 50

        outputFile.bufferedWriter().use { writer ->
            writer.write(CSV_HEADER + "\n")

            for (i in 1..count) {
                val isDuplicate = gen.nextRandom() < 0.02 && recentPool.isNotEmpty()
                val isOverdraft = gen.nextRandom() < 0.01

                val amount: String
                val source: String
                val dest: String
                val ts: Long
                val category: String
                val priority: Int

                if (isDuplicate) {
                    val poolIndex = (gen.nextRandom() * recentPool.size).toInt()
                    val original = recentPool[poolIndex]
                    val shiftMs = (gen.nextRandom() * 2_000).toInt()
                    ts = original.timestamp + shiftMs
                    amount = original.amount
                    source = original.source
                    dest = original.dest
                    category = original.category
                    priority = original.priority
                } else if (isOverdraft) {
                    amount = "60000.00"
                    source = gen.randomAccount()
                    dest = gen.randomAccount()
                    ts = gen.randomTimestamp(base2024)
                    category = gen.randomCategory()
                    priority = gen.randomPriority()
                } else {
                    amount = gen.randomAmount()
                    source = gen.randomAccount()
                    var d = gen.randomAccount()
                    while (d == source) d = gen.randomAccount()
                    dest = d
                    ts = gen.randomTimestamp(base2024)
                    category = gen.randomCategory()
                    priority = gen.randomPriority()
                }

                writer.write("$i,$amount,$source,$dest,$ts,$category,$priority\n")

                if (!isDuplicate) {
                    if (recentPool.size >= POOL_SIZE) {
                        recentPool.removeAt(0)
                    }
                    recentPool.add(PoolEntry(amount, source, dest, ts, category, priority))
                }
            }
        }
    }

    fun generateToPath(count: Int, path: String) {
        val file = File(path)
        println("Generating $count transactions -> $path ...")
        val start = System.currentTimeMillis()
        generate(count, file)
        println("Done in ${System.currentTimeMillis() - start} ms")
    }

    /**
     * mulberry32 — Seedable 32-bit PRNG. MUST be identical across all 4 languages.
     */
    private class Mulberry32(seed: Int) {
        private var state: Int = seed

        fun nextRandom(): Double {
            state += 0x6d2b79f5.toInt()
            var t = (state xor (state ushr 15)) * (1 or state)
            t = (t + ((t xor (t ushr 7)) * (61 or t))) xor t
            return (t xor (t ushr 14)).toUInt().toDouble() / 4294967296.0
        }

        fun randomAccount(): String = "ACC%04d".format((nextRandom() * ACCOUNTS_COUNT).toInt() + 1)

        fun randomAmount(): String {
            val amount = 10 + nextRandom() * 9990
            return "%.2f".format(java.util.Locale.US, amount)
        }

        fun randomTimestamp(baseMs: Long): Long {
            val offsetMs = (nextRandom() * 366.0 * 24 * 60 * 60 * 1000).toLong()
            return baseMs + offsetMs
        }

        fun randomCategory(): String = CATEGORIES[(nextRandom() * CATEGORIES.size).toInt()]

        fun randomPriority(): Int = (nextRandom() * 5).toInt() + 1
    }
}
