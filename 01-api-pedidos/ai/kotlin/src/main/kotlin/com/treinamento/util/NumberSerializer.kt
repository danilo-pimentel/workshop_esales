package com.treinamento.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.ExperimentalSerializationApi

/**
 * Serializer customizado para Double que omite o `.0` quando o valor e um numero inteiro.
 * Ex: 4500.0 -> 4500, 4500.5 -> 4500.5
 *
 * Isso garante compatibilidade com a referencia Bun, que serializa numeros inteiros sem ".0".
 */
@OptIn(ExperimentalSerializationApi::class)
object SmartDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("SmartDouble", PrimitiveKind.DOUBLE)

    override fun serialize(encoder: Encoder, value: Double) {
        if (encoder is JsonEncoder) {
            val literal = if (value == value.toLong().toDouble() && !value.isInfinite()) {
                // Numero inteiro - escrever sem decimais
                value.toLong().toString()
            } else {
                value.toString()
            }
            encoder.encodeJsonElement(JsonUnquotedLiteral(literal))
        } else {
            encoder.encodeDouble(value)
        }
    }

    override fun deserialize(decoder: Decoder): Double {
        return decoder.decodeDouble()
    }
}
