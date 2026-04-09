package com.treinamento.api.repository;

import com.treinamento.api.model.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    @Query("SELECT p FROM Produto p WHERE p.nome LIKE %:busca%")
    Page<Produto> findByNomeContaining(@Param("busca") String busca, Pageable pageable);

    Page<Produto> findAll(Pageable pageable);
}
