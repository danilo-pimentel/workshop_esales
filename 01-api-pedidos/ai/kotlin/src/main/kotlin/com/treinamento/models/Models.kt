package com.treinamento.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

// ─── Exposed Table Objects ────────────────────────────────────────────────────

object Produtos : IntIdTable("produtos") {
    val nome        = varchar("nome", 255)
    val descricao   = text("descricao").nullable()
    val preco       = double("preco")
    val estoque     = integer("estoque").default(0)
    val categoria   = varchar("categoria", 100).nullable()
    val createdAt   = datetime("created_at")
}

object Clientes : IntIdTable("clientes") {
    val nome      = varchar("nome", 255)
    val email     = varchar("email", 255).uniqueIndex()
    val telefone  = varchar("telefone", 20).nullable()
    val createdAt = datetime("created_at")
}

object Pedidos : IntIdTable("pedidos") {
    val clienteId = integer("cliente_id").references(Clientes.id)
    val status    = varchar("status", 50).default("pendente")
    val total     = double("total").default(0.0)
    val createdAt = datetime("created_at")
}

object ItensPedido : IntIdTable("itens_pedido") {
    val pedidoId      = integer("pedido_id").references(Pedidos.id)
    val produtoId     = integer("produto_id").references(Produtos.id)
    val quantidade    = integer("quantidade").default(1)
    val precoUnitario = double("preco_unitario")
}

// ─── Data Classes (Serializable) ──────────────────────────────────────────────

@Serializable
data class Produto(
    val id: Int,
    val nome: String,
    val descricao: String?,
    val preco: Double,
    val estoque: Int,
    val categoria: String?,
    val createdAt: String
)

@Serializable
data class ProdutoRequest(
    val nome: String,
    val descricao: String? = null,
    val preco: Double,
    val estoque: Int = 0,
    val categoria: String? = null
)

@Serializable
data class Cliente(
    val id: Int,
    val nome: String,
    val email: String,
    val telefone: String?,
    val createdAt: String
)

@Serializable
data class ClienteDetalhe(
    val id: Int,
    val nome: String,
    val email: String,
    val telefone: String?,
    val createdAt: String,
    val pedidos: List<PedidoSimples>,
    val totalPedidos: Int,
    val totalGasto: Double
)

@Serializable
data class PedidoSimples(
    val id: Int,
    val clienteId: Int,
    val status: String,
    val total: Double,
    val createdAt: String
)

@Serializable
data class PedidoListItem(
    val id: Int,
    val clienteId: Int,
    val status: String,
    val total: Double,
    val createdAt: String,
    val clienteNome: String
)

@Serializable
data class ItemPedidoDetalhe(
    val id: Int,
    val pedidoId: Int,
    val produtoId: Int,
    val quantidade: Int,
    val precoUnitario: Double,
    val produtoNome: String
)

@Serializable
data class PedidoDetalhe(
    val id: Int,
    val clienteId: Int,
    val status: String,
    val total: Double,
    val createdAt: String,
    val clienteNome: String,
    val clienteEmail: String,
    val clienteTelefone: String?,
    val itens: List<ItemPedidoDetalhe>
)

@Serializable
data class Pedido(
    val id: Int,
    val clienteId: Int,
    val status: String,
    val total: Double,
    val createdAt: String
)

@Serializable
data class CriarPedidoRequest(
    val clienteId: Int,
    val itens: List<ItemRequest>
)

@Serializable
data class ItemRequest(
    val produtoId: Int,
    val quantidade: Int = 1
)

@Serializable
data class AtualizarStatusRequest(
    val status: String
)

@Serializable
data class PaginatedResponse<T>(
    val data: List<T>,
    val page: Int,
    val limit: Int,
    val total: Int,
    val totalPages: Int
)

@Serializable
data class StatusCount(
    val status: String,
    val quantidade: Int
)

@Serializable
data class TopProduto(
    val produtoId: Int,
    val produtoNome: String,
    val totalVendido: Int,
    val receita: Double
)

@Serializable
data class DashboardResumo(
    val totalVendas: Double,
    val totalPedidos: Int,
    val ticketMedio: Double,
    val porStatus: List<StatusCount>,
    val topProdutos: List<TopProduto>
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)
