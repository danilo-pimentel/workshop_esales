package com.treinamento.api.controller;

import com.treinamento.api.model.Cliente;
import com.treinamento.api.model.Pedido;
import com.treinamento.api.repository.ClienteRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteRepository clienteRepository;

    public ClienteController(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @GetMapping
    public ResponseEntity<List<Cliente>> listar() {
        List<Cliente> clientes = clienteRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        return clienteRepository.findById(id)
                .<ResponseEntity<?>>map(cliente -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", cliente.getId());
                    response.put("nome", cliente.getNome());
                    response.put("email", cliente.getEmail());
                    response.put("telefone", cliente.getTelefone());
                    response.put("created_at", cliente.getCreatedAt());

                    List<Map<String, Object>> pedidosList = cliente.getPedidos().stream()
                            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                            .map(pedido -> {
                                Map<String, Object> p = new HashMap<>();
                                p.put("id", pedido.getId());
                                p.put("cliente_id", pedido.getCliente().getId());
                                p.put("status", pedido.getStatus());
                                p.put("total", pedido.getTotal());
                                p.put("created_at", pedido.getCreatedAt());
                                return p;
                            })
                            .toList();

                    double totalGasto = cliente.getPedidos().stream()
                            .mapToDouble(Pedido::getTotal)
                            .sum();

                    response.put("pedidos", pedidosList);
                    response.put("total_pedidos", cliente.getPedidos().size());
                    response.put("total_gasto", Math.round(totalGasto * 100.0) / 100.0);

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Cliente não encontrado")));
    }

    @PostMapping
    public ResponseEntity<Cliente> criar(@RequestBody Cliente cliente) {
        if (cliente.getNome() == null || cliente.getNome().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (cliente.getEmail() == null || cliente.getEmail().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Cliente salvo = clienteRepository.save(cliente);
        return ResponseEntity.status(201).body(salvo);
    }
}
