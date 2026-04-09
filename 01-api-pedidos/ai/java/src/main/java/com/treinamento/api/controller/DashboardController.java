package com.treinamento.api.controller;

import com.treinamento.api.repository.PedidoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final PedidoRepository pedidoRepository;

    public DashboardController(PedidoRepository pedidoRepository) {
        this.pedidoRepository = pedidoRepository;
    }

    @GetMapping("/resumo")
    public ResponseEntity<Map<String, Object>> resumo() {
        Double totalVendas = pedidoRepository.sumTotalVendas();
        Long totalPedidos = pedidoRepository.countPedidosAtivos();

        if (totalVendas == null) totalVendas = 0.0;
        if (totalPedidos == null) totalPedidos = 0L;

        double ticketMedio = totalPedidos > 0
                ? Math.round(totalVendas / totalPedidos * 100.0) / 100.0
                : 0.0;

        // por_status
        List<Object[]> statusCounts = pedidoRepository.countByStatus();
        List<Map<String, Object>> porStatus = new ArrayList<>();
        for (Object[] row : statusCounts) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("status", row[0]);
            entry.put("quantidade", row[1]);
            porStatus.add(entry);
        }

        // top_produtos (limit 10)
        List<Object[]> topProdutosRaw = pedidoRepository.findTopProdutos(PageRequest.of(0, 10));
        List<Map<String, Object>> topProdutos = new ArrayList<>();
        for (Object[] row : topProdutosRaw) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("produto_id", row[0]);
            entry.put("produto_nome", row[1]);
            entry.put("total_vendido", row[2]);
            double receita = ((Number) row[3]).doubleValue();
            entry.put("receita", Math.round(receita * 100.0) / 100.0);
            topProdutos.add(entry);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("total_vendas", Math.round(totalVendas * 100.0) / 100.0);
        response.put("total_pedidos", totalPedidos);
        response.put("ticket_medio", ticketMedio);
        response.put("por_status", porStatus);
        response.put("top_produtos", topProdutos);

        return ResponseEntity.ok(response);
    }
}
