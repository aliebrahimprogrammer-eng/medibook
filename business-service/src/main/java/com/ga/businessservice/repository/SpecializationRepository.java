package com.ga.businessservice.repository;

import com.ga.businessservice.model.entity.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpecializationRepository
        extends JpaRepository<Specialization, Long> {

    Optional<Specialization> findByName(String name);

    boolean existsByName(String name);
}
