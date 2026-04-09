package com.treinamento.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

/**
 * Formata LocalDateTime para o padrao "yyyy-MM-dd HH:mm:ss" (match com a referencia Bun).
 */
fun LocalDateTime.toApiString(): String = this.format(DATE_FORMAT)
