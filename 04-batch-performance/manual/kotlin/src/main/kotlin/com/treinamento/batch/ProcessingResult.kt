package com.treinamento.batch

data class CategoryStats(
    val category: String,
    val count: Int,
    val total: Double,
    val average: Double
)

data class ProcessingResult(
    val processed: Int,
    val duplicates: Int,
    val rejected: Int,
    val fraudRings: Int,
    val report: List<CategoryStats>
)
