package br.com.sistemas.chamados.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.sistemas.chamados.entity.Tecnico;

public interface TecnicoRepository extends JpaRepository<Tecnico, Long> {
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
}
