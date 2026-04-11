package com.treinamento.ctf.models

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String = "",
    val password: String = ""
)

@Serializable
data class RegisterRequest(
    val nome: String = "",
    val email: String = "",
    val password: String = "",
    val role: String = "user"
)

@Serializable
data class ForgotPasswordRequest(
    val email: String = ""
)

@Serializable
data class OrderCreateRequest(
    val items: List<OrderItem> = emptyList(),
    val total: Double = 0.0
)

@Serializable
data class OrderItem(
    val product_id: Int = 0,
    val quantity: Int = 0,
    val price: Double = 0.0
)

@Serializable
data class CouponApplyRequest(
    val code: String = ""
)

@Serializable
data class ReviewRequest(
    val text: String = "",
    val rating: Int = 5
)

@Serializable
data class UpdateUserRequest(
    val nome: String? = null,
    val email: String? = null,
    val password: String? = null,
    val telefone: String? = null,
    val endereco: String? = null
)

@Serializable
data class AdminCreateUserRequest(
    val nome: String = "",
    val email: String = "",
    val password: String = "",
    val role: String = "user"
)

@Serializable
data class ResetDbRequest(
    val key: String = ""
)
