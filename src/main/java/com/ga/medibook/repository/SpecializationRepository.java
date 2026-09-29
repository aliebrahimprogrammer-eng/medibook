package com.ga.medibook.repository;

import com.ga.medibook.model.entity.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpecializationRepository
        extends JpaRepository<Specialization, Long> {

    Optional<Specialization> findByName(String name);

    boolean existsByName(String name);
}