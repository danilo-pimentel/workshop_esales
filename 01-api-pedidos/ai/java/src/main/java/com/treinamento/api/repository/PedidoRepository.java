package com.treinamento.api.repository;

import com.treinamento.api.model.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Page<Pedido> findAll(Pageable pageable);

    @Query("SELECT SUM(p.total) FROM Pedido p WHERE p.status != 'cancelado'")
    Double sumTotalVendas();

    @Query("SELECT COUNT(p) FROM Pedido p WHERE p.status != 'cancelado'")
    Long countPedidosAtivos();

    @Query("SELECT p.status, COUNT(p) FROM Pedido p GROUP BY p.status ORDER BY COUNT(p) DESC")
    List<Object[]> countByStatus();

    @Query("SELECT ip.produto.id, ip.produto.nome, " +
           "SUM(ip.quantidade), " +
           "SUM(ip.quantidade * ip.precoUnitario) " +
           "FROM ItemPedido ip WHERE ip.pedido.status != 'cancelado' " +
           "GROUP BY ip.produto.id, ip.produto.nome " +
           "ORDER BY SUM(ip.quantidade) DESC")
    List<Object[]> findTopProdutos(Pageable pageable);
}
