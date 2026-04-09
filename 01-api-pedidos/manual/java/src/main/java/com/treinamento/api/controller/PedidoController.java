package com.treinamento.api.controller;

import com.treinamento.api.dto.PaginatedResponse;
import com.treinamento.api.dto.PedidoRequest;
import com.treinamento.api.model.Cliente;
import com.treinamento.api.model.ItemPedido;
import com.treinamento.api.model.Pedido;
import com.treinamento.api.model.Produto;
import com.treinamento.api.repository.ClienteRepository;
import com.treinamento.api.repository.ItemPedidoRepository;
import com.treinamento.api.repository.PedidoRepository;
import com.treinamento.api.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private static final List<String> STATUS_VALIDOS = List.of("pendente", "processando", "enviado", "entregue", "cancelado");

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final ItemPedidoRepository itemPedidoRepository;

    public PedidoController(PedidoRepository pedidoRepository,
                            ClienteRepository clienteRepository,
                            ProdutoRepository produtoRepository,
                            ItemPedidoRepository itemPedidoRepository) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.itemPedidoRepository = itemPedidoRepository;
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<Map<String, Object>>> listar(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String data_inicio,
            @RequestParam(required = false) String data_fim,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        PageRequest pageRequest = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "id"));
        Page<Pedido> resultado = pedidoRepository.findAll(pageRequest);

        List<Map<String, Object>> data = resultado.getContent().stream()
                .map(this::pedidoToMapWithClienteNome)
                .toList();

        PaginatedResponse<Map<String, Object>> response = new PaginatedResponse<>(
                data,
                page,
                limit,
                resultado.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        return pedidoRepository.findById(id)
                .<ResponseEntity<?>>map(pedido -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", pedido.getId());
                    response.put("cliente_id", pedido.getCliente().getId());
                    response.put("status", pedido.getStatus());
                    response.put("total", pedido.getTotal());
                    response.put("created_at", pedido.getCreatedAt());
                    response.put("cliente_nome", pedido.getCliente().getNome());
                    response.put("cliente_email", pedido.getCliente().getEmail());
                    response.put("cliente_telefone", pedido.getCliente().getTelefone());

                    List<ItemPedido> itens = itemPedidoRepository.findByPedidoId(id);
                    List<Map<String, Object>> itensData = itens.stream()
                            .map(item -> {
                                Map<String, Object> i = new HashMap<>();
                                i.put("id", item.getId());
                                i.put("pedido_id", pedido.getId());
                                i.put("produto_id", item.getProduto().getId());
                                i.put("quantidade", item.getQuantidade());
                                i.put("preco_unitario", item.getPrecoUnitario());
                                i.put("produto_nome", item.getProduto().getNome());
                                return i;
                            })
                            .toList();
                    response.put("itens", itensData);

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Pedido não encontrado")));
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody PedidoRequest request) {
        if (request.clienteId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cliente não encontrado"));
        }

        Cliente cliente = clienteRepository.findById(request.clienteId()).orElse(null);
        if (cliente == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cliente não encontrado"));
        }

        if (request.itens() == null || request.itens().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Pedido deve ter pelo menos um item"));
        }

        // Validate all products exist and have stock
        List<Produto> produtosValidados = new ArrayList<>();
        for (PedidoRequest.ItemRequest itemReq : request.itens()) {
            Produto produto = produtoRepository.findById(itemReq.produtoId()).orElse(null);
            if (produto == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Um ou mais produtos não encontrados"));
            }
            if (produto.getEstoque() < (itemReq.quantidade() != null ? itemReq.quantidade() : 1)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Estoque insuficiente para o produto " + produto.getNome()));
            }
            produtosValidados.add(produto);
        }

        double total = 0.0;
        for (int idx = 0; idx < request.itens().size(); idx++) {
            total += produtosValidados.get(idx).getPreco();
        }

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setStatus("pendente");
        pedido.setTotal(total);
        Pedido salvo = pedidoRepository.save(pedido);

        // Insert items and decrement stock
        for (int idx = 0; idx < request.itens().size(); idx++) {
            PedidoRequest.ItemRequest itemReq = request.itens().get(idx);
            Produto produto = produtosValidados.get(idx);
            int quantidade = itemReq.quantidade() != null ? itemReq.quantidade() : 1;

            ItemPedido item = new ItemPedido();
            item.setPedido(salvo);
            item.setProduto(produto);
            item.setQuantidade(quantidade);
            item.setPrecoUnitario(produto.getPreco());
            itemPedidoRepository.save(item);

            produto.setEstoque(produto.getEstoque() - quantidade);
            produtoRepository.save(produto);
        }

        // Return bare pedido (no items, no client info) — matches Bun behavior
        Map<String, Object> response = new HashMap<>();
        response.put("id", salvo.getId());
        response.put("cliente_id", salvo.getCliente().getId());
        response.put("status", salvo.getStatus());
        response.put("total", salvo.getTotal());
        response.put("created_at", salvo.getCreatedAt());

        return ResponseEntity.status(201).body(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> atualizarStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String novoStatus = body.get("status");
        if (novoStatus == null || novoStatus.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status é obrigatório"));
        }

        if (!STATUS_VALIDOS.contains(novoStatus)) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Status inválido. Valores permitidos: " + String.join(", ", STATUS_VALIDOS)));
        }

        return pedidoRepository.findById(id)
                .<ResponseEntity<?>>map(pedido -> {
                    pedido.setStatus(novoStatus);
                    Pedido salvo = pedidoRepository.save(pedido);
                    return ResponseEntity.ok(pedidoToMapWithClienteNome(salvo));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Pedido não encontrado")));
    }

    private Map<String, Object> pedidoToMapWithClienteNome(Pedido pedido) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", pedido.getId());
        map.put("cliente_id", pedido.getCliente().getId());
        map.put("status", pedido.getStatus());
        map.put("total", pedido.getTotal());
        map.put("created_at", pedido.getCreatedAt());
        map.put("cliente_nome", pedido.getCliente().getNome());
        return map;
    }
}
