package com.treinamento.api.dto;

import java.util.List;

public record PedidoRequest(
        Long clienteId,
        List<ItemRequest> itens
) {
    public record ItemRequest(
            Long produtoId,
            Integer quantidade
    ) {}
}
