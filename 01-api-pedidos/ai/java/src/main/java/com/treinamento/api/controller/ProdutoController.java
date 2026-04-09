package com.treinamento.api.controller;

import com.treinamento.api.dto.PaginatedResponse;
import com.treinamento.api.model.Produto;
import com.treinamento.api.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/produtos")
public class ProdutoController {

    private final ProdutoRepository produtoRepository;

    public ProdutoController(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<Produto>> listar(
            @RequestParam(required = false) String busca,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {

        PageRequest pageRequest = PageRequest.of(page - 1, limit);
        Page<Produto> resultado;

        if (busca != null && !busca.isBlank()) {
            resultado = produtoRepository.findByNomeContaining(busca, pageRequest);
        } else {
            resultado = produtoRepository.findAll(pageRequest);
        }

        PaginatedResponse<Produto> response = new PaginatedResponse<>(
                resultado.getContent(),
                page,
                limit,
                resultado.getTotalElements()
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        return produtoRepository.findById(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Produto não encontrado")));
    }

    @PostMapping
    public ResponseEntity<?> criar(@RequestBody Produto produto) {
        if (produto.getNome() == null || produto.getNome().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Nome é obrigatório"));
        }
        if (produto.getPreco() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Preço é obrigatório"));
        }
        Produto salvo = produtoRepository.save(produto);
        return ResponseEntity.status(201).body(salvo);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(@PathVariable Long id, @RequestBody Produto produto) {
        return produtoRepository.findById(id)
                .<ResponseEntity<?>>map(existente -> {
                    if (produto.getNome() != null) existente.setNome(produto.getNome());
                    if (produto.getDescricao() != null) existente.setDescricao(produto.getDescricao());
                    if (produto.getPreco() != null) existente.setPreco(produto.getPreco());
                    if (produto.getEstoque() != null) existente.setEstoque(produto.getEstoque());
                    if (produto.getCategoria() != null) existente.setCategoria(produto.getCategoria());
                    return ResponseEntity.ok(produtoRepository.save(existente));
                })
                .orElse(ResponseEntity.status(404).body(Map.of("error", "Produto não encontrado")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletar(@PathVariable Long id) {
        if (!produtoRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of("error", "Produto não encontrado"));
        }
        produtoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
