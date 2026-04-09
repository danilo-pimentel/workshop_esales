package com.treinamento.database

import com.treinamento.models.Clientes
import com.treinamento.models.ItensPedido
import com.treinamento.models.Pedidos
import com.treinamento.models.Produtos
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime

fun initDatabase() {
    // Use a temporary file that is deleted and recreated on each startup
    // (matches Bun reference behavior of fresh in-memory database)
    val dbFile = java.io.File(System.getProperty("java.io.tmpdir"), "pedidos-kotlin.db")
    if (dbFile.exists()) dbFile.delete()

    val config = org.sqlite.SQLiteConfig()
    config.setPragma(org.sqlite.SQLiteConfig.Pragma.CASE_SENSITIVE_LIKE, "true")
    val ds = org.sqlite.SQLiteDataSource(config)
    ds.url = "jdbc:sqlite:${dbFile.absolutePath}"
    Database.connect(ds)

    transaction {
        SchemaUtils.create(Produtos, Clientes, Pedidos, ItensPedido)
        seedData()
    }
}

private fun seedData() {
    if (Produtos.selectAll().count() > 0) return

    // ── Products (50) — exact match with Bun reference ──────────────────────────
    data class ProdutoSeed(val nome: String, val descricao: String, val preco: Double, val estoque: Int, val categoria: String)

    val produtosSeed = listOf(
        // Eletrônicos (10)
        ProdutoSeed("Notebook Pro", "Notebook de alta performance com SSD 512GB", 4500.00, 15, "Eletrônicos"),
        ProdutoSeed("Smartphone Galaxy X", "Celular top de linha com câmera 108MP", 2999.90, 30, "Eletrônicos"),
        ProdutoSeed("Tablet Ultra", "Tablet 10 polegadas com 128GB", 1299.00, 20, "Eletrônicos"),
        ProdutoSeed("Fone Bluetooth Premium", "Fone over-ear com cancelamento de ruído", 499.90, 50, "Eletrônicos"),
        ProdutoSeed("Smart TV 55\"", "Televisão 4K com sistema Android", 3200.00, 10, "Eletrônicos"),
        ProdutoSeed("Mouse Gamer RGB", "Mouse com DPI ajustável e 7 botões", 189.90, 80, "Eletrônicos"),
        ProdutoSeed("Teclado Mecânico", "Teclado com switches azuis e retroiluminação", 349.90, 40, "Eletrônicos"),
        ProdutoSeed("Webcam HD", "Câmera 1080p com microfone embutido", 299.00, 35, "Eletrônicos"),
        ProdutoSeed("SSD 1TB", "Disco sólido externo USB 3.1", 599.00, 25, "Eletrônicos"),
        ProdutoSeed("Monitor 27\"", "Monitor IPS Full HD 144Hz", 1799.00, 12, "Eletrônicos"),
        // Roupas (10)
        ProdutoSeed("Camiseta Básica", "Camiseta 100% algodão manga curta", 49.90, 200, "Roupas"),
        ProdutoSeed("Calça Jeans Slim", "Calça jeans masculina corte slim", 159.90, 80, "Roupas"),
        ProdutoSeed("Vestido Floral", "Vestido feminino estampa floral verão", 129.90, 60, "Roupas"),
        ProdutoSeed("Moletom Canguru", "Moletom com capuz e bolso frontal", 119.90, 90, "Roupas"),
        ProdutoSeed("Bermuda Tactel", "Bermuda esportiva secagem rápida", 69.90, 120, "Roupas"),
        ProdutoSeed("Jaqueta Corta-Vento", "Jaqueta leve impermeável", 189.90, 45, "Roupas"),
        ProdutoSeed("Polo Social", "Camisa polo masculina slim fit", 89.90, 100, "Roupas"),
        ProdutoSeed("Blusa de Frio", "Blusa feminina tricô inverno", 99.90, 70, "Roupas"),
        ProdutoSeed("Short Fitness", "Short feminino academia legging", 59.90, 150, "Roupas"),
        ProdutoSeed("Tênis Casual", "Tênis unissex confortável estilo urbano", 249.90, 55, "Roupas"),
        // Alimentos (10)
        ProdutoSeed("Café Especial 500g", "Café torrado e moído origem única", 39.90, 300, "Alimentos"),
        ProdutoSeed("Chocolate Premium", "Barra de chocolate 70% cacau 200g", 19.90, 400, "Alimentos"),
        ProdutoSeed("Granola Artesanal", "Granola com frutas secas e mel 500g", 29.90, 250, "Alimentos"),
        ProdutoSeed("Azeite Extra Virgem", "Azeite de oliva português 500ml", 45.90, 180, "Alimentos"),
        ProdutoSeed("Mel Puro 1kg", "Mel silvestre orgânico certificado", 55.00, 100, "Alimentos"),
        ProdutoSeed("Whey Protein 1kg", "Proteína de soro de leite sabor baunilha", 189.90, 80, "Alimentos"),
        ProdutoSeed("Mix de Castanhas", "Mix premium de castanhas e nozes 400g", 49.90, 220, "Alimentos"),
        ProdutoSeed("Vinagre Balsâmico", "Vinagre balsâmico envelhecido Módena 500ml", 35.90, 130, "Alimentos"),
        ProdutoSeed("Chá Verde Premium", "Chá verde japonês matcha 100g", 29.90, 200, "Alimentos"),
        ProdutoSeed("Pasta de Amendoim", "Pasta de amendoim integral sem açúcar 500g", 24.90, 300, "Alimentos"),
        // Livros (10)
        ProdutoSeed("Clean Code", "Código limpo - Robert C. Martin", 89.90, 40, "Livros"),
        ProdutoSeed("O Poder do Hábito", "Charles Duhigg - Bestseller comportamental", 49.90, 60, "Livros"),
        ProdutoSeed("Algoritmos Descomplicados", "Introdução a estruturas de dados", 79.90, 35, "Livros"),
        ProdutoSeed("Design Patterns", "Gang of Four - Padrões de projeto", 119.90, 25, "Livros"),
        ProdutoSeed("Sapiens", "Uma breve história da humanidade - Yuval Harari", 54.90, 70, "Livros"),
        ProdutoSeed("A Arte da Guerra", "Sun Tzu - Estratégia e liderança", 29.90, 90, "Livros"),
        ProdutoSeed("Mindset", "Carol Dweck - A nova psicologia do sucesso", 44.90, 55, "Livros"),
        ProdutoSeed("JavaScript: O Guia Definitivo", "David Flanagan - 7ª edição", 149.90, 30, "Livros"),
        ProdutoSeed("Domain-Driven Design", "Eric Evans - Tackling Complexity in Software", 139.90, 20, "Livros"),
        ProdutoSeed("O Programador Pragmático", "Hunt & Thomas - De aprendiz a mestre", 99.90, 45, "Livros"),
        // Esportes (10)
        ProdutoSeed("Bola de Futebol Pro", "Bola oficial tamanho 5 couro sintético", 149.90, 50, "Esportes"),
        ProdutoSeed("Raquete de Tênis", "Raquete alumínio para iniciantes 275g", 199.90, 30, "Esportes"),
        ProdutoSeed("Luva de Boxe", "Luva couro legítimo 14oz treinamento", 179.90, 40, "Esportes"),
        ProdutoSeed("Halteres 10kg (par)", "Par de halteres emborrachados 10kg", 129.90, 35, "Esportes"),
        ProdutoSeed("Tapete de Yoga", "Tapete antiderrapante 6mm com bolsa", 89.90, 80, "Esportes"),
        ProdutoSeed("Corda de Pular Speed", "Corda de pular profissional com rolamentos", 59.90, 100, "Esportes"),
        ProdutoSeed("Garrafa Térmica 1L", "Garrafa inox com tampa hermética", 79.90, 90, "Esportes"),
        ProdutoSeed("Óculos de Natação", "Óculos silicone anti-embaçante UV", 49.90, 60, "Esportes"),
        ProdutoSeed("Mochila Esportiva 30L", "Mochila impermeável com porta notebook", 219.90, 45, "Esportes"),
        ProdutoSeed("Bandagem Elástica", "Kit bandagem para proteção articular 5m", 34.90, 150, "Esportes")
    )

    val now = LocalDateTime.now()

    // Insert products — IDs will be 1..50
    produtosSeed.forEach { p ->
        Produtos.insertAndGetId {
            it[Produtos.nome]      = p.nome
            it[Produtos.descricao] = p.descricao
            it[Produtos.preco]     = p.preco
            it[Produtos.estoque]   = p.estoque
            it[Produtos.categoria] = p.categoria
            it[Produtos.createdAt] = now.minusDays((Math.abs(p.nome.hashCode()) % 365).toLong())
        }
    }

    // ── Customers (20) — exact match with Bun reference ──────────────────────────
    data class ClienteSeed(val nome: String, val email: String, val telefone: String)

    val clientesSeed = listOf(
        ClienteSeed("Ana Paula Ferreira", "ana.ferreira@email.com", "(11) 98765-4321"),
        ClienteSeed("Carlos Eduardo Santos", "carlos.santos@email.com", "(21) 97654-3210"),
        ClienteSeed("Mariana Costa Lima", "mariana.lima@email.com", "(31) 96543-2109"),
        ClienteSeed("Rafael Oliveira Souza", "rafael.souza@email.com", "(41) 95432-1098"),
        ClienteSeed("Fernanda Rodrigues", "fernanda.rodrigues@email.com", "(51) 94321-0987"),
        ClienteSeed("Lucas Martins Pereira", "lucas.pereira@email.com", "(61) 93210-9876"),
        ClienteSeed("Juliana Alves Gomes", "juliana.gomes@email.com", "(71) 92109-8765"),
        ClienteSeed("Diego Barbosa Cardoso", "diego.cardoso@email.com", "(81) 91098-7654"),
        ClienteSeed("Camila Ribeiro Nunes", "camila.nunes@email.com", "(85) 90987-6543"),
        ClienteSeed("Thiago Mendes Castro", "thiago.castro@email.com", "(92) 99876-5432"),
        ClienteSeed("Beatriz Carvalho Dias", "beatriz.dias@email.com", "(27) 98765-1234"),
        ClienteSeed("Rodrigo Araújo Melo", "rodrigo.melo@email.com", "(83) 97654-2345"),
        ClienteSeed("Isabela Teixeira Rocha", "isabela.rocha@email.com", "(48) 96543-3456"),
        ClienteSeed("Felipe Nascimento Cruz", "felipe.cruz@email.com", "(62) 95432-4567"),
        ClienteSeed("Larissa Monteiro Vieira", "larissa.vieira@email.com", "(67) 94321-5678"),
        ClienteSeed("Gustavo Pinto Correia", "gustavo.correia@email.com", "(91) 93210-6789"),
        ClienteSeed("Amanda Sousa Freitas", "amanda.freitas@email.com", "(98) 92109-7890"),
        ClienteSeed("Leonardo Moraes Lopes", "leonardo.lopes@email.com", "(77) 91098-8901"),
        ClienteSeed("Patricia Cunha Ramos", "patricia.ramos@email.com", "(79) 90987-9012"),
        ClienteSeed("André Guimarães Silva", "andre.silva@email.com", "(68) 99876-0123")
    )

    // Insert customers — IDs will be 1..20
    clientesSeed.forEach { c ->
        Clientes.insertAndGetId {
            it[Clientes.nome]      = c.nome
            it[Clientes.email]     = c.email
            it[Clientes.telefone]  = c.telefone
            it[Clientes.createdAt] = now
        }
    }

    // ── Orders (100) + Items (200+) — same algorithm as Bun reference ────────────
    // Status distribution: 30 pendente, 25 processando, 20 enviado, 20 entregue, 5 cancelado
    val statusPool = buildList {
        repeat(30) { add("pendente") }
        repeat(25) { add("processando") }
        repeat(20) { add("enviado") }
        repeat(20) { add("entregue") }
        repeat(5)  { add("cancelado") }
    }

    // Pre-fetch all product prices (IDs 1..50)
    data class ProdutoInfo(val id: Int, val preco: Double)
    val allProdutos = Produtos.selectAll().map { row ->
        ProdutoInfo(row[Produtos.id].value, row[Produtos.preco])
    }

    // Spread orders across 2024 — deterministic, same as Bun
    for (i in 0 until 100) {
        val clienteId = (i % 20) + 1
        val status = statusPool[i]

        // Spread across 2024 months
        val month = String.format("%02d", (i % 12) + 1)
        val day = String.format("%02d", (i % 28) + 1)
        val minute = i % 60
        val createdAt = LocalDateTime.parse("2024-${month}-${day}T10:${String.format("%02d", minute)}:00")

        // Insert pedido with placeholder total
        val pedidoId = Pedidos.insertAndGetId {
            it[Pedidos.clienteId] = clienteId
            it[Pedidos.status]    = status
            it[Pedidos.total]     = 0.0
            it[Pedidos.createdAt] = createdAt
        }.value

        // Add 2-4 items per order: numItems = 2 + (i % 3)
        val numItems = 2 + (i % 3)
        var totalPedido = 0.0

        for (j in 0 until numItems) {
            val produtoIdx = (i * 3 + j * 7) % allProdutos.size
            val produto = allProdutos[produtoIdx]
            val quantidade = 1 + (j % 5)

            ItensPedido.insert {
                it[ItensPedido.pedidoId]      = pedidoId
                it[ItensPedido.produtoId]     = produto.id
                it[ItensPedido.quantidade]    = quantidade
                it[ItensPedido.precoUnitario] = produto.preco
            }

            totalPedido += produto.preco
        }

        Pedidos.update({ Pedidos.id eq pedidoId }) {
            it[Pedidos.total] = totalPedido
        }
    }
}
