package com.treinamento.models

import com.treinamento.util.SmartDoubleSerializer
import kotlinx.serialization.SerialName
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
// Note: field names use @SerialName to match Bun reference exactly.
//   - most fields: snake_case (cliente_id, created_at, etc.)
//   - totalPages: camelCase (matches Bun)

@Serializable
data class Produto(
    val id: Int,
    val nome: String,
    val descricao: String?,
    @Serializable(with = SmartDoubleSerializer::class)
    val preco: Double,
    val estoque: Int,
    val categoria: String?,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class ProdutoRequest(
    val nome: String,
    val descricao: String? = null,
    @Serializable(with = SmartDoubleSerializer::class)
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
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class ClienteDetalhe(
    val id: Int,
    val nome: String,
    val email: String,
    val telefone: String?,
    @SerialName("created_at")
    val createdAt: String,
    val pedidos: List<PedidoSimples>,
    @SerialName("total_pedidos")
    val totalPedidos: Int,
    @SerialName("total_gasto")
    @Serializable(with = SmartDoubleSerializer::class)
    val totalGasto: Double
)

@Serializable
data class PedidoSimples(
    val id: Int,
    @SerialName("cliente_id")
    val clienteId: Int,
    val status: String,
    @Serializable(with = SmartDoubleSerializer::class)
    val total: Double,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class PedidoListItem(
    val id: Int,
    @SerialName("cliente_id")
    val clienteId: Int,
    val status: String,
    @Serializable(with = SmartDoubleSerializer::class)
    val total: Double,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("cliente_nome")
    val clienteNome: String
)

@Serializable
data class ItemPedidoDetalhe(
    val id: Int,
    @SerialName("pedido_id")
    val pedidoId: Int,
    @SerialName("produto_id")
    val produtoId: Int,
    val quantidade: Int,
    @SerialName("preco_unitario")
    @Serializable(with = SmartDoubleSerializer::class)
    val precoUnitario: Double,
    @SerialName("produto_nome")
    val produtoNome: String
)

@Serializable
data class PedidoDetalhe(
    val id: Int,
    @SerialName("cliente_id")
    val clienteId: Int,
    val status: String,
    @Serializable(with = SmartDoubleSerializer::class)
    val total: Double,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("cliente_nome")
    val clienteNome: String,
    @SerialName("cliente_email")
    val clienteEmail: String,
    @SerialName("cliente_telefone")
    val clienteTelefone: String?,
    val itens: List<ItemPedidoDetalhe>
)

@Serializable
data class Pedido(
    val id: Int,
    @SerialName("cliente_id")
    val clienteId: Int,
    val status: String,
    @Serializable(with = SmartDoubleSerializer::class)
    val total: Double,
    @SerialName("created_at")
    val createdAt: String
)

@Serializable
data class CriarPedidoRequest(
    @SerialName("cliente_id")
    val clienteId: Int,
    val itens: List<ItemRequest>
)

@Serializable
data class ItemRequest(
    @SerialName("produto_id")
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
    @SerialName("produto_id")
    val produtoId: Int,
    @SerialName("produto_nome")
    val produtoNome: String,
    @SerialName("total_vendido")
    val totalVendido: Int,
    @Serializable(with = SmartDoubleSerializer::class)
    val receita: Double
)

@Serializable
data class DashboardResumo(
    @SerialName("total_vendas")
    @Serializable(with = SmartDoubleSerializer::class)
    val totalVendas: Double,
    @SerialName("total_pedidos")
    val totalPedidos: Int,
    @SerialName("ticket_medio")
    @Serializable(with = SmartDoubleSerializer::class)
    val ticketMedio: Double,
    @SerialName("por_status")
    val porStatus: List<StatusCount>,
    @SerialName("top_produtos")
    val topProdutos: List<TopProduto>
)

@Serializable
data class ErrorResponse(
    val error: String,
    val message: String
)
