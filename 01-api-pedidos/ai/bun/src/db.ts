import { Database } from "bun:sqlite";

export const db = new Database(":memory:");

function initSchema(): void {
  db.run(`PRAGMA journal_mode = WAL`);
  db.run(`PRAGMA foreign_keys = ON`);
  db.run(`PRAGMA case_sensitive_like = ON`);

  db.run(`
    CREATE TABLE IF NOT EXISTS produtos (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      nome TEXT NOT NULL,
      descricao TEXT,
      preco REAL NOT NULL,
      estoque INTEGER NOT NULL DEFAULT 0,
      categoria TEXT,
      created_at TEXT DEFAULT (datetime('now'))
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS clientes (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      nome TEXT NOT NULL,
      email TEXT NOT NULL UNIQUE,
      telefone TEXT,
      created_at TEXT DEFAULT (datetime('now'))
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS pedidos (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      cliente_id INTEGER NOT NULL REFERENCES clientes(id),
      status TEXT NOT NULL DEFAULT 'pendente',
      total REAL NOT NULL DEFAULT 0,
      created_at TEXT DEFAULT (datetime('now'))
    )
  `);

  db.run(`
    CREATE TABLE IF NOT EXISTS itens_pedido (
      id INTEGER PRIMARY KEY AUTOINCREMENT,
      pedido_id INTEGER NOT NULL REFERENCES pedidos(id),
      produto_id INTEGER NOT NULL REFERENCES produtos(id),
      quantidade INTEGER NOT NULL DEFAULT 1,
      preco_unitario REAL NOT NULL
    )
  `);
}

function seedProdutos(): void {
  const produtos = [
    // Eletrônicos
    { nome: "Notebook Pro", descricao: "Notebook de alta performance com SSD 512GB", preco: 4500.00, estoque: 15, categoria: "Eletrônicos" },
    { nome: "Smartphone Galaxy X", descricao: "Celular top de linha com câmera 108MP", preco: 2999.90, estoque: 30, categoria: "Eletrônicos" },
    { nome: "Tablet Ultra", descricao: "Tablet 10 polegadas com 128GB", preco: 1299.00, estoque: 20, categoria: "Eletrônicos" },
    { nome: "Fone Bluetooth Premium", descricao: "Fone over-ear com cancelamento de ruído", preco: 499.90, estoque: 50, categoria: "Eletrônicos" },
    { nome: "Smart TV 55\"", descricao: "Televisão 4K com sistema Android", preco: 3200.00, estoque: 10, categoria: "Eletrônicos" },
    { nome: "Mouse Gamer RGB", descricao: "Mouse com DPI ajustável e 7 botões", preco: 189.90, estoque: 80, categoria: "Eletrônicos" },
    { nome: "Teclado Mecânico", descricao: "Teclado com switches azuis e retroiluminação", preco: 349.90, estoque: 40, categoria: "Eletrônicos" },
    { nome: "Webcam HD", descricao: "Câmera 1080p com microfone embutido", preco: 299.00, estoque: 35, categoria: "Eletrônicos" },
    { nome: "SSD 1TB", descricao: "Disco sólido externo USB 3.1", preco: 599.00, estoque: 25, categoria: "Eletrônicos" },
    { nome: "Monitor 27\"", descricao: "Monitor IPS Full HD 144Hz", preco: 1799.00, estoque: 12, categoria: "Eletrônicos" },
    // Roupas
    { nome: "Camiseta Básica", descricao: "Camiseta 100% algodão manga curta", preco: 49.90, estoque: 200, categoria: "Roupas" },
    { nome: "Calça Jeans Slim", descricao: "Calça jeans masculina corte slim", preco: 159.90, estoque: 80, categoria: "Roupas" },
    { nome: "Vestido Floral", descricao: "Vestido feminino estampa floral verão", preco: 129.90, estoque: 60, categoria: "Roupas" },
    { nome: "Moletom Canguru", descricao: "Moletom com capuz e bolso frontal", preco: 119.90, estoque: 90, categoria: "Roupas" },
    { nome: "Bermuda Tactel", descricao: "Bermuda esportiva secagem rápida", preco: 69.90, estoque: 120, categoria: "Roupas" },
    { nome: "Jaqueta Corta-Vento", descricao: "Jaqueta leve impermeável", preco: 189.90, estoque: 45, categoria: "Roupas" },
    { nome: "Polo Social", descricao: "Camisa polo masculina slim fit", preco: 89.90, estoque: 100, categoria: "Roupas" },
    { nome: "Blusa de Frio", descricao: "Blusa feminina tricô inverno", preco: 99.90, estoque: 70, categoria: "Roupas" },
    { nome: "Short Fitness", descricao: "Short feminino academia legging", preco: 59.90, estoque: 150, categoria: "Roupas" },
    { nome: "Tênis Casual", descricao: "Tênis unissex confortável estilo urbano", preco: 249.90, estoque: 55, categoria: "Roupas" },
    // Alimentos
    { nome: "Café Especial 500g", descricao: "Café torrado e moído origem única", preco: 39.90, estoque: 300, categoria: "Alimentos" },
    { nome: "Chocolate Premium", descricao: "Barra de chocolate 70% cacau 200g", preco: 19.90, estoque: 400, categoria: "Alimentos" },
    { nome: "Granola Artesanal", descricao: "Granola com frutas secas e mel 500g", preco: 29.90, estoque: 250, categoria: "Alimentos" },
    { nome: "Azeite Extra Virgem", descricao: "Azeite de oliva português 500ml", preco: 45.90, estoque: 180, categoria: "Alimentos" },
    { nome: "Mel Puro 1kg", descricao: "Mel silvestre orgânico certificado", preco: 55.00, estoque: 100, categoria: "Alimentos" },
    { nome: "Whey Protein 1kg", descricao: "Proteína de soro de leite sabor baunilha", preco: 189.90, estoque: 80, categoria: "Alimentos" },
    { nome: "Mix de Castanhas", descricao: "Mix premium de castanhas e nozes 400g", preco: 49.90, estoque: 220, categoria: "Alimentos" },
    { nome: "Vinagre Balsâmico", descricao: "Vinagre balsâmico envelhecido Módena 500ml", preco: 35.90, estoque: 130, categoria: "Alimentos" },
    { nome: "Chá Verde Premium", descricao: "Chá verde japonês matcha 100g", preco: 29.90, estoque: 200, categoria: "Alimentos" },
    { nome: "Pasta de Amendoim", descricao: "Pasta de amendoim integral sem açúcar 500g", preco: 24.90, estoque: 300, categoria: "Alimentos" },
    // Livros
    { nome: "Clean Code", descricao: "Código limpo - Robert C. Martin", preco: 89.90, estoque: 40, categoria: "Livros" },
    { nome: "O Poder do Hábito", descricao: "Charles Duhigg - Bestseller comportamental", preco: 49.90, estoque: 60, categoria: "Livros" },
    { nome: "Algoritmos Descomplicados", descricao: "Introdução a estruturas de dados", preco: 79.90, estoque: 35, categoria: "Livros" },
    { nome: "Design Patterns", descricao: "Gang of Four - Padrões de projeto", preco: 119.90, estoque: 25, categoria: "Livros" },
    { nome: "Sapiens", descricao: "Uma breve história da humanidade - Yuval Harari", preco: 54.90, estoque: 70, categoria: "Livros" },
    { nome: "A Arte da Guerra", descricao: "Sun Tzu - Estratégia e liderança", preco: 29.90, estoque: 90, categoria: "Livros" },
    { nome: "Mindset", descricao: "Carol Dweck - A nova psicologia do sucesso", preco: 44.90, estoque: 55, categoria: "Livros" },
    { nome: "JavaScript: O Guia Definitivo", descricao: "David Flanagan - 7ª edição", preco: 149.90, estoque: 30, categoria: "Livros" },
    { nome: "Domain-Driven Design", descricao: "Eric Evans - Tackling Complexity in Software", preco: 139.90, estoque: 20, categoria: "Livros" },
    { nome: "O Programador Pragmático", descricao: "Hunt & Thomas - De aprendiz a mestre", preco: 99.90, estoque: 45, categoria: "Livros" },
    // Esportes
    { nome: "Bola de Futebol Pro", descricao: "Bola oficial tamanho 5 couro sintético", preco: 149.90, estoque: 50, categoria: "Esportes" },
    { nome: "Raquete de Tênis", descricao: "Raquete alumínio para iniciantes 275g", preco: 199.90, estoque: 30, categoria: "Esportes" },
    { nome: "Luva de Boxe", descricao: "Luva couro legítimo 14oz treinamento", preco: 179.90, estoque: 40, categoria: "Esportes" },
    { nome: "Halteres 10kg (par)", descricao: "Par de halteres emborrachados 10kg", preco: 129.90, estoque: 35, categoria: "Esportes" },
    { nome: "Tapete de Yoga", descricao: "Tapete antiderrapante 6mm com bolsa", preco: 89.90, estoque: 80, categoria: "Esportes" },
    { nome: "Corda de Pular Speed", descricao: "Corda de pular profissional com rolamentos", preco: 59.90, estoque: 100, categoria: "Esportes" },
    { nome: "Garrafa Térmica 1L", descricao: "Garrafa inox com tampa hermética", preco: 79.90, estoque: 90, categoria: "Esportes" },
    { nome: "Óculos de Natação", descricao: "Óculos silicone anti-embaçante UV", preco: 49.90, estoque: 60, categoria: "Esportes" },
    { nome: "Mochila Esportiva 30L", descricao: "Mochila impermeável com porta notebook", preco: 219.90, estoque: 45, categoria: "Esportes" },
    { nome: "Bandagem Elástica", descricao: "Kit bandagem para proteção articular 5m", preco: 34.90, estoque: 150, categoria: "Esportes" },
  ];

  const insertProduto = db.prepare(`
    INSERT INTO produtos (nome, descricao, preco, estoque, categoria, created_at)
    VALUES (?, ?, ?, ?, ?, datetime('now', '-' || abs(random() % 365) || ' days'))
  `);

  for (const p of produtos) {
    insertProduto.run(p.nome, p.descricao, p.preco, p.estoque, p.categoria);
  }
}

function seedClientes(): void {
  const clientes = [
    { nome: "Ana Paula Ferreira", email: "ana.ferreira@email.com", telefone: "(11) 98765-4321" },
    { nome: "Carlos Eduardo Santos", email: "carlos.santos@email.com", telefone: "(21) 97654-3210" },
    { nome: "Mariana Costa Lima", email: "mariana.lima@email.com", telefone: "(31) 96543-2109" },
    { nome: "Rafael Oliveira Souza", email: "rafael.souza@email.com", telefone: "(41) 95432-1098" },
    { nome: "Fernanda Rodrigues", email: "fernanda.rodrigues@email.com", telefone: "(51) 94321-0987" },
    { nome: "Lucas Martins Pereira", email: "lucas.pereira@email.com", telefone: "(61) 93210-9876" },
    { nome: "Juliana Alves Gomes", email: "juliana.gomes@email.com", telefone: "(71) 92109-8765" },
    { nome: "Diego Barbosa Cardoso", email: "diego.cardoso@email.com", telefone: "(81) 91098-7654" },
    { nome: "Camila Ribeiro Nunes", email: "camila.nunes@email.com", telefone: "(85) 90987-6543" },
    { nome: "Thiago Mendes Castro", email: "thiago.castro@email.com", telefone: "(92) 99876-5432" },
    { nome: "Beatriz Carvalho Dias", email: "beatriz.dias@email.com", telefone: "(27) 98765-1234" },
    { nome: "Rodrigo Araújo Melo", email: "rodrigo.melo@email.com", telefone: "(83) 97654-2345" },
    { nome: "Isabela Teixeira Rocha", email: "isabela.rocha@email.com", telefone: "(48) 96543-3456" },
    { nome: "Felipe Nascimento Cruz", email: "felipe.cruz@email.com", telefone: "(62) 95432-4567" },
    { nome: "Larissa Monteiro Vieira", email: "larissa.vieira@email.com", telefone: "(67) 94321-5678" },
    { nome: "Gustavo Pinto Correia", email: "gustavo.correia@email.com", telefone: "(91) 93210-6789" },
    { nome: "Amanda Sousa Freitas", email: "amanda.freitas@email.com", telefone: "(98) 92109-7890" },
    { nome: "Leonardo Moraes Lopes", email: "leonardo.lopes@email.com", telefone: "(77) 91098-8901" },
    { nome: "Patricia Cunha Ramos", email: "patricia.ramos@email.com", telefone: "(79) 90987-9012" },
    { nome: "André Guimarães Silva", email: "andre.silva@email.com", telefone: "(68) 99876-0123" },
  ];

  const insertCliente = db.prepare(`
    INSERT INTO clientes (nome, email, telefone)
    VALUES (?, ?, ?)
  `);

  for (const c of clientes) {
    insertCliente.run(c.nome, c.email, c.telefone);
  }
}

function seedPedidos(): void {
  // Status distribution: 30 pendente, 25 processando, 20 enviado, 20 entregue, 5 cancelado
  const statusPool: string[] = [
    ...Array(30).fill("pendente"),
    ...Array(25).fill("processando"),
    ...Array(20).fill("enviado"),
    ...Array(20).fill("entregue"),
    ...Array(5).fill("cancelado"),
  ];

  const insertPedido = db.prepare(`
    INSERT INTO pedidos (cliente_id, status, total, created_at)
    VALUES (?, ?, ?, ?)
  `);

  const insertItem = db.prepare(`
    INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, preco_unitario)
    VALUES (?, ?, ?, ?)
  `);

  // Get all product prices upfront
  const allProdutos = db.query<{ id: number; preco: number }, []>(
    "SELECT id, preco FROM produtos"
  ).all();

  // Spread orders across 2024
  for (let i = 0; i < 100; i++) {
    const clienteId = (i % 20) + 1;
    const status = statusPool[i];

    // Spread across 2024 months
    const month = String((i % 12) + 1).padStart(2, "0");
    const day = String((i % 28) + 1).padStart(2, "0");
    const createdAt = `2024-${month}-${day} 10:${String(i % 60).padStart(2, "0")}:00`;

    // Insert pedido with placeholder total
    const pedidoResult = insertPedido.run(clienteId, status, 0, createdAt);
    const pedidoId = pedidoResult.lastInsertRowid as number;

    // Add 2-4 items per order (ensures 200+ total items for 100 orders)
    const numItems = 2 + (i % 3);
    let totalPedido = 0;

    for (let j = 0; j < numItems; j++) {
      const produtoIdx = (i * 3 + j * 7) % allProdutos.length;
      const produto = allProdutos[produtoIdx];
      const quantidade = 1 + (j % 5);

      insertItem.run(pedidoId, produto.id, quantidade, produto.preco);
      totalPedido += produto.preco;
    }

    db.run("UPDATE pedidos SET total = ? WHERE id = ?", [totalPedido, pedidoId]);
  }
}

export function initDatabase(): void {
  initSchema();
  seedProdutos();
  seedClientes();
  seedPedidos();
}
