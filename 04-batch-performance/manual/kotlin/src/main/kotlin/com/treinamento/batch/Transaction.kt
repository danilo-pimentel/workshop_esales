package com.treinamento.batch

data class Transaction(
    val id: Int,
    val amount: Double,
    val sourceAccount: String,
    val destAccount: String,
    val timestamp: Long,
    val category: String,
    val priority: Int
) {
    companion object {
        fun fromCsvLine(line: String): Transaction {
            val parts = line.split(",")
            require(parts.size == 7) { "Expected 7 CSV columns but got ${parts.size}: '$line'" }
            return Transaction(
                id = parts[0].trim().toInt(),
                amount = parts[1].trim().toDouble(),
                sourceAccount = parts[2].trim(),
                destAccount = parts[3].trim(),
                timestamp = parts[4].trim().toLong(),
                category = parts[5].trim(),
                priority = parts[6].trim().toInt()
            )
        }
    }

    fun toCsvLine(): String = "$id,$amount,$sourceAccount,$destAccount,$timestamp,$category,$priority"
}
