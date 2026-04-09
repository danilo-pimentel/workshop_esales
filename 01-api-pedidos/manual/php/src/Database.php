<?php

declare(strict_types=1);

namespace App;

use PDO;
use PDOException;

class Database
{
    private static ?PDO $instance = null;

    public static function getInstance(): PDO
    {
        if (self::$instance === null) {
            self::$instance = self::createConnection();
            self::createSchema(self::$instance);
            self::seedData(self::$instance);
        }

        return self::$instance;
    }

    public static function createTestInstance(): PDO
    {
        $pdo = self::createConnection();
        self::createSchema($pdo);
        self::seedData($pdo);
        return $pdo;
    }

    private static function createConnection(): PDO
    {
        $pdo = new PDO('sqlite::memory:');
        $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
        $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
        $pdo->exec('PRAGMA foreign_keys = ON');
        $pdo->exec('PRAGMA case_sensitive_like = ON');
        return $pdo;
    }

    private static function createSchema(PDO $pdo): void
    {
        $pdo->exec("
            CREATE TABLE IF NOT EXISTS produtos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                descricao TEXT,
                preco REAL NOT NULL,
                estoque INTEGER NOT NULL DEFAULT 0,
                categoria TEXT,
                created_at TEXT DEFAULT (datetime('now'))
            )
        ");

        $pdo->exec("
            CREATE TABLE IF NOT EXISTS clientes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nome TEXT NOT NULL,
                email TEXT NOT NULL UNIQUE,
                telefone TEXT,
                created_at TEXT DEFAULT (datetime('now'))
            )
        ");

        $pdo->exec("
            CREATE TABLE IF NOT EXISTS pedidos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                cliente_id INTEGER NOT NULL REFERENCES clientes(id),
                status TEXT NOT NULL DEFAULT 'pendente',
                total REAL NOT NULL DEFAULT 0,
                created_at TEXT DEFAULT (datetime('now'))
            )
        ");

        $pdo->exec("
            CREATE TABLE IF NOT EXISTS itens_pedido (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                pedido_id INTEGER NOT NULL REFERENCES pedidos(id),
                produto_id INTEGER NOT NULL REFERENCES produtos(id),
                quantidade INTEGER NOT NULL DEFAULT 1,
                preco_unitario REAL NOT NULL
            )
        ");
    }

    private static function seedData(PDO $pdo): void
    {
        // Seed products - 50 items across categories (exact match with Bun reference)
        $produtos = [
            // Eletronicos
            ['Notebook Pro', 'Notebook de alta performance com SSD 512GB', 4500.00, 15, 'Eletronicos'],
            ['Smartphone Galaxy X', 'Celular top de linha com camera 108MP', 2999.90, 30, 'Eletronicos'],
            ['Tablet Ultra', 'Tablet 10 polegadas com 128GB', 1299.00, 20, 'Eletronicos'],
            ['Fone Bluetooth Premium', 'Fone over-ear com cancelamento de ruido', 499.90, 50, 'Eletronicos'],
            ['Smart TV 55"', 'Televisao 4K com sistema Android', 3200.00, 10, 'Eletronicos'],
            ['Mouse Gamer RGB', 'Mouse com DPI ajustavel e 7 botoes', 189.90, 80, 'Eletronicos'],
            ['Teclado Mecanico', 'Teclado com switches azuis e retroiluminacao', 349.90, 40, 'Eletronicos'],
            ['Webcam HD', 'Camera 1080p com microfone embutido', 299.00, 35, 'Eletronicos'],
            ['SSD 1TB', 'Disco solido externo USB 3.1', 599.00, 25, 'Eletronicos'],
            ['Monitor 27"', 'Monitor IPS Full HD 144Hz', 1799.00, 12, 'Eletronicos'],
            // Roupas
            ['Camiseta Basica', 'Camiseta 100% algodao manga curta', 49.90, 200, 'Roupas'],
            ['Calca Jeans Slim', 'Calca jeans masculina corte slim', 159.90, 80, 'Roupas'],
            ['Vestido Floral', 'Vestido feminino estampa floral verao', 129.90, 60, 'Roupas'],
            ['Moletom Canguru', 'Moletom com capuz e bolso frontal', 119.90, 90, 'Roupas'],
            ['Bermuda Tactel', 'Bermuda esportiva secagem rapida', 69.90, 120, 'Roupas'],
            ['Jaqueta Corta-Vento', 'Jaqueta leve impermeavel', 189.90, 45, 'Roupas'],
            ['Polo Social', 'Camisa polo masculina slim fit', 89.90, 100, 'Roupas'],
            ['Blusa de Frio', 'Blusa feminina trico inverno', 99.90, 70, 'Roupas'],
            ['Short Fitness', 'Short feminino academia legging', 59.90, 150, 'Roupas'],
            ['Tenis Casual', 'Tenis unissex confortavel estilo urbano', 249.90, 55, 'Roupas'],
            // Alimentos
            ['Cafe Especial 500g', 'Cafe torrado e moido origem unica', 39.90, 300, 'Alimentos'],
            ['Chocolate Premium', 'Barra de chocolate 70% cacau 200g', 19.90, 400, 'Alimentos'],
            ['Granola Artesanal', 'Granola com frutas secas e mel 500g', 29.90, 250, 'Alimentos'],
            ['Azeite Extra Virgem', 'Azeite de oliva portugues 500ml', 45.90, 180, 'Alimentos'],
            ['Mel Puro 1kg', 'Mel silvestre organico certificado', 55.00, 100, 'Alimentos'],
            ['Whey Protein 1kg', 'Proteina de soro de leite sabor baunilha', 189.90, 80, 'Alimentos'],
            ['Mix de Castanhas', 'Mix premium de castanhas e nozes 400g', 49.90, 220, 'Alimentos'],
            ['Vinagre Balsamico', 'Vinagre balsamico envelhecido Modena 500ml', 35.90, 130, 'Alimentos'],
            ['Cha Verde Premium', 'Cha verde japones matcha 100g', 29.90, 200, 'Alimentos'],
            ['Pasta de Amendoim', 'Pasta de amendoim integral sem acucar 500g', 24.90, 300, 'Alimentos'],
            // Livros
            ['Clean Code', 'Codigo limpo - Robert C. Martin', 89.90, 40, 'Livros'],
            ['O Poder do Habito', 'Charles Duhigg - Bestseller comportamental', 49.90, 60, 'Livros'],
            ['Algoritmos Descomplicados', 'Introducao a estruturas de dados', 79.90, 35, 'Livros'],
            ['Design Patterns', 'Gang of Four - Padroes de projeto', 119.90, 25, 'Livros'],
            ['Sapiens', 'Uma breve historia da humanidade - Yuval Harari', 54.90, 70, 'Livros'],
            ['A Arte da Guerra', 'Sun Tzu - Estrategia e lideranca', 29.90, 90, 'Livros'],
            ['Mindset', 'Carol Dweck - A nova psicologia do sucesso', 44.90, 55, 'Livros'],
            ['JavaScript: O Guia Definitivo', 'David Flanagan - 7a edicao', 149.90, 30, 'Livros'],
            ['Domain-Driven Design', 'Eric Evans - Tackling Complexity in Software', 139.90, 20, 'Livros'],
            ['O Programador Pragmatico', 'Hunt & Thomas - De aprendiz a mestre', 99.90, 45, 'Livros'],
            // Esportes
            ['Bola de Futebol Pro', 'Bola oficial tamanho 5 couro sintetico', 149.90, 50, 'Esportes'],
            ['Raquete de Tenis', 'Raquete aluminio para iniciantes 275g', 199.90, 30, 'Esportes'],
            ['Luva de Boxe', 'Luva couro legitimo 14oz treinamento', 179.90, 40, 'Esportes'],
            ['Halteres 10kg (par)', 'Par de halteres emborrachados 10kg', 129.90, 35, 'Esportes'],
            ['Tapete de Yoga', 'Tapete antiderrapante 6mm com bolsa', 89.90, 80, 'Esportes'],
            ['Corda de Pular Speed', 'Corda de pular profissional com rolamentos', 59.90, 100, 'Esportes'],
            ['Garrafa Termica 1L', 'Garrafa inox com tampa hermetica', 79.90, 90, 'Esportes'],
            ['Oculos de Natacao', 'Oculos silicone anti-embacante UV', 49.90, 60, 'Esportes'],
            ['Mochila Esportiva 30L', 'Mochila impermeavel com porta notebook', 219.90, 45, 'Esportes'],
            ['Bandagem Elastica', 'Kit bandagem para protecao articular 5m', 34.90, 150, 'Esportes'],
        ];

        $stmtProduto = $pdo->prepare(
            "INSERT INTO produtos (nome, descricao, preco, estoque, categoria, created_at)
             VALUES (?, ?, ?, ?, ?, datetime('now', '-' || abs(random() % 365) || ' days'))"
        );
        foreach ($produtos as $p) {
            $stmtProduto->execute($p);
        }

        // Seed customers - 20 Brazilian customers (exact match with Bun reference)
        $clientes = [
            ['Ana Paula Ferreira', 'ana.ferreira@email.com', '(11) 98765-4321'],
            ['Carlos Eduardo Santos', 'carlos.santos@email.com', '(21) 97654-3210'],
            ['Mariana Costa Lima', 'mariana.lima@email.com', '(31) 96543-2109'],
            ['Rafael Oliveira Souza', 'rafael.souza@email.com', '(41) 95432-1098'],
            ['Fernanda Rodrigues', 'fernanda.rodrigues@email.com', '(51) 94321-0987'],
            ['Lucas Martins Pereira', 'lucas.pereira@email.com', '(61) 93210-9876'],
            ['Juliana Alves Gomes', 'juliana.gomes@email.com', '(71) 92109-8765'],
            ['Diego Barbosa Cardoso', 'diego.cardoso@email.com', '(81) 91098-7654'],
            ['Camila Ribeiro Nunes', 'camila.nunes@email.com', '(85) 90987-6543'],
            ['Thiago Mendes Castro', 'thiago.castro@email.com', '(92) 99876-5432'],
            ['Beatriz Carvalho Dias', 'beatriz.dias@email.com', '(27) 98765-1234'],
            ['Rodrigo Araujo Melo', 'rodrigo.melo@email.com', '(83) 97654-2345'],
            ['Isabela Teixeira Rocha', 'isabela.rocha@email.com', '(48) 96543-3456'],
            ['Felipe Nascimento Cruz', 'felipe.cruz@email.com', '(62) 95432-4567'],
            ['Larissa Monteiro Vieira', 'larissa.vieira@email.com', '(67) 94321-5678'],
            ['Gustavo Pinto Correia', 'gustavo.correia@email.com', '(91) 93210-6789'],
            ['Amanda Sousa Freitas', 'amanda.freitas@email.com', '(98) 92109-7890'],
            ['Leonardo Moraes Lopes', 'leonardo.lopes@email.com', '(77) 91098-8901'],
            ['Patricia Cunha Ramos', 'patricia.ramos@email.com', '(79) 90987-9012'],
            ['Andre Guimaraes Silva', 'andre.silva@email.com', '(68) 99876-0123'],
        ];

        $stmtCliente = $pdo->prepare(
            'INSERT INTO clientes (nome, email, telefone) VALUES (?, ?, ?)'
        );
        foreach ($clientes as $c) {
            $stmtCliente->execute($c);
        }

        // Seed 100 orders across 2024 (exact algorithm match with Bun reference)
        // Status distribution: 30 pendente, 25 processando, 20 enviado, 20 entregue, 5 cancelado
        $statusPool = array_merge(
            array_fill(0, 30, 'pendente'),
            array_fill(0, 25, 'processando'),
            array_fill(0, 20, 'enviado'),
            array_fill(0, 20, 'entregue'),
            array_fill(0, 5, 'cancelado')
        );

        // Get all product prices upfront
        $allProdutos = $pdo->query("SELECT id, preco FROM produtos")->fetchAll();

        $stmtPedido = $pdo->prepare(
            "INSERT INTO pedidos (cliente_id, status, total, created_at) VALUES (?, ?, ?, ?)"
        );
        $stmtItem = $pdo->prepare(
            'INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, preco_unitario) VALUES (?, ?, ?, ?)'
        );

        for ($i = 0; $i < 100; $i++) {
            $clienteId = ($i % 20) + 1;
            $status = $statusPool[$i];

            // Spread across 2024 months
            $month = str_pad((string)(($i % 12) + 1), 2, '0', STR_PAD_LEFT);
            $day = str_pad((string)(($i % 28) + 1), 2, '0', STR_PAD_LEFT);
            $minute = str_pad((string)($i % 60), 2, '0', STR_PAD_LEFT);
            $createdAt = "2024-{$month}-{$day} 10:{$minute}:00";

            // Insert pedido with placeholder total
            $stmtPedido->execute([$clienteId, $status, 0, $createdAt]);
            $pedidoId = (int)$pdo->lastInsertId();

            // Add 2-4 items per order
            $numItems = 2 + ($i % 3);
            $totalPedido = 0.0;

            for ($j = 0; $j < $numItems; $j++) {
                $produtoIdx = ($i * 3 + $j * 7) % count($allProdutos);
                $produto = $allProdutos[$produtoIdx];
                $quantidade = 1 + ($j % 5);

                $stmtItem->execute([$pedidoId, $produto['id'], $quantidade, $produto['preco']]);
                $totalPedido += (float)$produto['preco'];
            }

            $pdo->prepare("UPDATE pedidos SET total = ? WHERE id = ?")->execute([$totalPedido, $pedidoId]);
        }
    }
}
