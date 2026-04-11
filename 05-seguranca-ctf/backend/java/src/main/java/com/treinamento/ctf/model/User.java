package com.treinamento.ctf.model;

import java.time.LocalDateTime;

public class User {

    private Integer id;
    private String nome;
    private String email;
    private String password;
    private String role;
    private String telefone;
    private String cpfLast4;
    private String endereco;
    private LocalDateTime createdAt;

    public User() {}

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }

    public String getCpfLast4() { return cpfLast4; }
    public void setCpfLast4(String cpfLast4) { this.cpfLast4 = cpfLast4; }

    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
