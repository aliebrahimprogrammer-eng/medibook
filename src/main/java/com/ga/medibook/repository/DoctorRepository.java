package com.ga.medibook.repository;

import com.ga.medibook.model.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    List<Doctor> searchByName(
            @Param("name") String name
    );

    @Query("""
        SELECT d FROM Doctor d
        JOIN d.user u
        JOIN UserProfile p ON p.user = u
        JOIN d.specialization s
        WHERE LOWER(s.name) = LOWER(:specialization)
    """)
    List<Doctor> searchBySpecialization(
            @Param("specialization") String specialization
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
    List<Doctor> searchByNameAndSpecialization(
            @Param("name") String name,
            @Param("specialization") String specialization
    );
}