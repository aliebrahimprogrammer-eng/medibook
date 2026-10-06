package com.ga.medibook.repository;

import com.ga.medibook.model.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByLicenseNumber(String licenseNumber);

    @Query("""
        SELECT d FROM Doctor d
        JOIN d.user u
        JOIN UserProfile p ON p.user = u
        JOIN d.specialization s
        WHERE LOWER(p.firstName) LIKE LOWER(CONCAT('%', :name, '%'))
           OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
    """)
    Page<Doctor> searchByName(
            @Param("name") String name,
            Pageable pageable
    );

    @Query("""
        SELECT d FROM Doctor d
        JOIN d.user u
        JOIN UserProfile p ON p.user = u
        JOIN d.specialization s
        WHERE LOWER(s.name) = LOWER(:specialization)
    """)
    Page<Doctor> searchBySpecialization(
            @Param("specialization") String specialization,
            Pageable pageable
    );

    @Query("""
        SELECT d FROM Doctor d
        JOIN d.user u
        JOIN UserProfile p ON p.user = u
        JOIN d.specialization s
        WHERE (
            LOWER(p.firstName) LIKE LOWER(CONCAT('%', :name, '%'))
            OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
        )
        AND LOWER(s.name) = LOWER(:specialization)
    """)
    Page<Doctor> searchByNameAndSpecialization(
            @Param("name") String name,
            @Param("specialization") String specialization,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Doctor d WHERE d.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") Long id);
}