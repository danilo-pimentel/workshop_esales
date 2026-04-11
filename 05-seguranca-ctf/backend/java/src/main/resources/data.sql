-- Seed Data

-- Admin user (ID 1)
INSERT INTO users (nome, email, password, role, telefone, cpf_last4, endereco) VALUES
('Administrador', 'admin@secureshop.com', 'Admin@2024!', 'admin', '(11) 99999-0001', '7890', 'Rua Augusta 1200, Sao Paulo - SP');

-- Regular users (IDs 2-6)
INSERT INTO users (nome, email, password, role, telefone, cpf_last4, endereco) VALUES
('Carlos Silva', 'carlos@secureshop.com', 'Senha123', 'user', '(11) 98765-4321', '1234', 'Av. Paulista 1000, Apto 42, Sao Paulo - SP');
INSERT INTO users (nome, email, password, role, telefone, cpf_last4, endereco) VALUES
('Ana Oliveira', 'ana@secureshop.com', 'Minhasenha1', 'user', '(21) 97654-3210', '5678', 'Rua Copacabana 500, Rio de Janeiro - RJ');
INSERT INTO users (nome, email, password, role, telefone, cpf_last4, endereco) VALUES
('Pedro Santos', 'pedro@secureshop.com', 'Pedro@456', 'user', '(31) 96543-2109', '9012', 'Rua da Bahia 1500, Belo Horizonte - MG');
INSERT INTO users (nome, email, password, role, telefone, cpf_last4, endereco) VALUES
('Mariana Costa', 'mariana@secureshop.com', 'MarCosta99', 'user', '(41) 95432-1098', '3456', 'Rua XV de Novembro 700, Curitiba - PR');
INSERT INTO users (nome, email, password, role, telefone, cpf_last4, endereco) VALUES
('Rafael Mendes', 'rafael@secureshop.com', 'Rafael2024', 'user', '(51) 94321-0987', '7891', 'Av. Borges de Medeiros 2300, Porto Alegre - RS');

-- Workshop participants (IDs 7-44)
INSERT INTO users (nome, email, password, role) VALUES ('Danilo Pimentel', 'danilo.pimentel@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Pedro Cardoso', 'pedro.cardoso@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Dienis Silva', 'dienis.silva@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Claudio Pereira', 'claudio.pereira@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Bruno Nunes', 'bruno.nunes@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Paulo Souza', 'paulo.souza@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Rodrigo Weiler', 'rodrigo.weiler@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Paulo Araujo', 'paulo.araujo@pagplan.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Wilson Abdala', 'wilson.abdala@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Guilherme Schlup', 'guilherme.schlup@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Marilia Soares', 'marilia.silva@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Victor Moreira', 'victor.moreira@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Marcelo Mattos', 'marcelo.mattos@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Rudimar Grass', 'rudimar.grass@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Leonardo Borges', 'leonardo.borges@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Thiago Miranda', 'thiago.miranda@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Guilherme Freitas', 'guilherme.freitas@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Cassio Cristiano', 'cassio.cristiano@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Lucas Jesus', 'lucas.jesus@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Luciano Sager', 'luciano.sager@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Rodrigo Quadros', 'rodrigo.quadros@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Marco Braida', 'marco.braida@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Eden Meireles', 'eden.meireles@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Marcelino Avelar', 'marcelino.avelar@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Eric Gottschalk', 'eric.gottschalk@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Luis Felipe Silva', 'luis.silva@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Linsmar Cruz', 'linsmar.cruz@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Moises Oliveira', 'moises.oliveira@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Felipe Girardi', 'felipe.girardi@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Alexandre Finger', 'alexandre.finger@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Dioner Seffrin', 'dioner.seffrin@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Filipe Freitas', 'filipe.freitas@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Susiane Basso', 'susiane.basso@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Camila Silva', 'camila.silva@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Fernando Garibotti', 'fernando.garibotti@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Taiomara Soares', 'taiomara.soares@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Vilson Santos', 'vilson.santos@esales.com.br', 'eSalesWorkshopAI-2026', 'user');
INSERT INTO users (nome, email, password, role) VALUES ('Pablo Gomez', 'pablo.gomez@esales.com.br', 'eSalesWorkshopAI-2026', 'user');

-- Products (30 items)
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Notebook Pro 15', 'Notebook de alto desempenho com SSD 512GB', 4599.99, 'Eletronicos');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Mouse Ergonomico', 'Mouse sem fio com design ergonomico', 189.90, 'Perifericos');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Teclado Mecanico RGB', 'Teclado mecanico com iluminacao RGB', 349.00, 'Perifericos');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Monitor 4K 27"', 'Monitor 4K UHD com taxa de 144Hz', 2199.00, 'Monitores');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Headset Gamer', 'Fone de ouvido com microfone e som surround 7.1', 279.90, 'Audio');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Webcam HD 1080p', 'Camera para videoconferencia em Full HD', 249.00, 'Perifericos');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('SSD NVMe 1TB', 'Armazenamento ultrarapido NVMe Gen4', 599.00, 'Armazenamento');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Memoria RAM 32GB DDR5', 'Kit de memoria DDR5 6000MHz', 799.90, 'Memoria');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Placa de Video RTX 4070', 'GPU para jogos e criacao de conteudo', 3299.00, 'Hardware');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Fonte 750W 80+ Gold', 'Fonte de alimentacao modular 80 Plus Gold', 499.00, 'Hardware');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Gabinete ATX Mid-Tower', 'Gabinete com painel lateral em vidro temperado', 399.00, 'Hardware');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Cooler CPU 360mm AIO', 'Water cooler all-in-one com radiador de 360mm', 699.00, 'Refrigeracao');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Placa-mae Z790', 'Placa-mae para Intel 13a/14a geracao DDR5', 1499.00, 'Hardware');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Processador i9-14900K', 'Processador Intel Core i9 de 14a geracao', 3899.00, 'Processadores');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Processador Ryzen 9 7950X', 'AMD Ryzen 9 7950X 16-Core 32-Thread', 3299.00, 'Processadores');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Hub USB-C 10 em 1', 'Hub com HDMI, USB-A, USB-C, SD card e ethernet', 249.90, 'Perifericos');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Mesa para Escritorio', 'Mesa com suporte para monitor e organizacao', 1199.00, 'Mobiliario');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Cadeira Gamer Pro', 'Cadeira com apoio lombar e descanso de braco', 1599.00, 'Mobiliario');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Suporte Monitor Articulado', 'Suporte de mesa com braco articulado duplo', 399.00, 'Acessorios');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Mousepad XL', 'Mousepad extra large 90x40cm antiderrapante', 99.90, 'Acessorios');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Switch KVM 4 portas', 'Chaveador KVM HDMI para 4 computadores', 349.00, 'Rede');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Roteador Wi-Fi 6 AX6000', 'Roteador dual-band com Wi-Fi 6 e 6Gbps', 1299.00, 'Rede');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Cabo HDMI 2.1 2m', 'Cabo HDMI 2.1 certificado 4K@120Hz 8K@60Hz', 89.90, 'Cabos');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('No-break 1500VA', 'UPS com gerenciamento de energia e USB', 899.00, 'Energia');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Impressora Laser Color', 'Impressora laser colorida duplex Wi-Fi', 1899.00, 'Impressao');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Scanner de Mesa A4', 'Scanner otico com resolucao 1200dpi e Wi-Fi', 799.00, 'Impressao');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Pendrive 256GB USB 3.2', 'Pendrive de alta velocidade leitura 400MB/s', 89.90, 'Armazenamento');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('HD Externo 4TB USB 3.0', 'Disco rigido portatil 4TB com backup automatico', 599.00, 'Armazenamento');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Tablet 10" Android', 'Tablet com tela Full HD e bateria 7040mAh', 1099.00, 'Tablets');
INSERT INTO products (nome, descricao, preco, categoria) VALUES ('Smartphone 5G 256GB', 'Celular 5G camera 200MP e tela AMOLED 6.7"', 3499.00, 'Smartphones');

-- Orders (50 items)
INSERT INTO orders (user_id, total, status) VALUES (2, 189.90, 'pendente');
INSERT INTO orders (user_id, total, status) VALUES (3, 349.00, 'processando');
INSERT INTO orders (user_id, total, status) VALUES (4, 4599.99, 'enviado');
INSERT INTO orders (user_id, total, status) VALUES (5, 2199.00, 'entregue');
INSERT INTO orders (user_id, total, status) VALUES (6, 599.00, 'cancelado');
INSERT INTO orders (user_id, total, status) VALUES (2, 279.90, 'pendente');
INSERT INTO orders (user_id, total, status) VALUES (3, 799.90, 'processando');
INSERT INTO orders (user_id, total, status) VALUES (4, 3299.00, 'enviado');
INSERT INTO orders (user_id, total, status) VALUES (5, 499.00, 'entregue');
INSERT INTO orders (user_id, total, status) VALUES (6, 399.00, 'cancelado');
INSERT INTO orders (user_id, total, status) VALUES (2, 699.00, 'pendente');
INSERT INTO orders (user_id, total, status) VALUES (3, 1499.00, 'processando');
INSERT INTO orders (user_id, total, status) VALUES (4, 3899.00, 'enviado');
INSERT INTO orders (user_id, total, status) VALUES (5, 249.90, 'entregue');
INSERT INTO orders (user_id, total, status) VALUES (6, 1199.00, 'cancelado');
INSERT INTO orders (user_id, total, status) VALUES (2, 1599.00, 'pendente');
INSERT INTO orders (user_id, total, status) VALUES (3, 399.00, 'processando');
INSERT INTO orders (user_id, total, status) VALUES (4, 99.90, 'enviado');
INSERT INTO orders (user_id, total, status) VALUES (5, 349.00, 'entregue');
INSERT INTO orders (user_id, total, status) VALUES (6, 1299.00, 'cancelado');
INSERT INTO orders (user_id, total, status) VALUES (2, 89.90, 'pendente');
INSERT INTO orders (user_id, total, status) VALUES (3, 899.00, 'processando');
INSERT INTO orders (user_id, total, status) VALUES (4, 1899.00, 'enviado');
INSERT INTO orders (user_id, total, status) VALUES (5, 799.00, 'entregue');
INSERT INTO orders (user_id, total, status) VALUES (6, 89.90, 'cancelado');
INSERT INTO orders (user_id, total, status) VALUES (2, 599.00, 'pendente');
INSERT INTO orders (user_id, total, status) VALUES (3, 1099.00, 'processando');
INSERT INTO orders (user_id, total, status) VALUES (4, 3499.00, 'enviado');
INSERT INTO orders (user_id, total, status) VALUES (5, 249.00, 'entregue');
INSERT INTO orders (user_id, total, status) VALUES (6, 2199.00, 'cancelado');
INSERT INTO orders (user_id, total, status) VALUES (2, 4599.99, 'pendente');
INSERT INTO orders (user_id, total, status) VALUES (3, 189.90, 'processando');
INSERT INTO orders (user_id, total, status) VALUES (4, 349.00, 'enviado');
INSERT INTO orders (user_id, total, status) VALUES (5, 279.90, 'entregue');
INSERT INTO orders (user_id, total, status) VALUES (6, 799.90, 'cancelado');
INSERT INTO orders (user_id, total, status) VALUES (2, 3299.00, 'pendente');
INSERT INTO orders (user_id, total, status) VALUES (3, 499.00, 'processando');
INSERT INTO orders (user_id, total, status) VALUES (4, 399.00, 'enviado');
INSERT INTO orders (user_id, total, status) VALUES (5, 699.00, 'entregue');
INSERT INTO orders (user_id, total, status) VALUES (6, 1499.00, 'cancelado');
INSERT INTO orders (user_id, total, status) VALUES (2, 3899.00, 'pendente');
INSERT INTO orders (user_id, total, status) VALUES (3, 249.90, 'processando');
INSERT INTO orders (user_id, total, status) VALUES (4, 1199.00, 'enviado');
INSERT INTO orders (user_id, total, status) VALUES (5, 1599.00, 'entregue');
INSERT INTO orders (user_id, total, status) VALUES (6, 399.00, 'cancelado');
INSERT INTO orders (user_id, total, status) VALUES (2, 99.90, 'pendente');
INSERT INTO orders (user_id, total, status) VALUES (3, 349.00, 'processando');
INSERT INTO orders (user_id, total, status) VALUES (4, 1299.00, 'enviado');
INSERT INTO orders (user_id, total, status) VALUES (5, 89.90, 'entregue');
INSERT INTO orders (user_id, total, status) VALUES (6, 899.00, 'cancelado');

-- Reviews (5 items)
INSERT INTO reviews (product_id, user_id, user_name, rating, text) VALUES (1, 2, 'Carlos Silva', 5, 'Excelente notebook, muito rapido! Recomendo para desenvolvedores.');
INSERT INTO reviews (product_id, user_id, user_name, rating, text) VALUES (2, 3, 'Ana Oliveira', 4, 'Mouse confortavel, uso o dia inteiro sem dor. So poderia ter mais botoes.');
INSERT INTO reviews (product_id, user_id, user_name, rating, text) VALUES (3, 4, 'Pedro Santos', 5, 'Teclado fantastico! O som dos switches e muito satisfatorio.');
INSERT INTO reviews (product_id, user_id, user_name, rating, text) VALUES (4, 5, 'Mariana Costa', 3, 'Monitor bom mas chegou com um pixel morto. Suporte resolveu rapido.');
INSERT INTO reviews (product_id, user_id, user_name, rating, text) VALUES (5, 6, 'Rafael Mendes', 4, 'Som surround impressionante. Microfone poderia ser melhor.');

-- Coupons (3 items)
INSERT INTO coupons (code, discount, max_uses, uses, active) VALUES ('DESCONTO10', 10, 10, 0, 1);
INSERT INTO coupons (code, discount, max_uses, uses, active) VALUES ('PRIMEIRACOMPRA', 15, 1, 0, 1);
INSERT INTO coupons (code, discount, max_uses, uses, active) VALUES ('BLACKFRIDAY', 25, 100, 95, 1);
