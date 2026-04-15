package com.gray.hospital.repository;

import com.gray.hospital.entity.Roster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RosterRepository extends JpaRepository<Roster, Long> {

    List<Roster> findByDate(LocalDate date);

    List<Roster> findByDateBetween(LocalDate startDate, LocalDate endDate);

    List<Roster> findByDoctorDoctorIdAndDateBetween(Long doctorId, LocalDate startDate, LocalDate endDate);

    Optional<Roster> findByDoctorDoctorIdAndDate(Long doctorId, LocalDate date);

    boolean existsByDoctorDoctorIdAndDate(Long doctorId, LocalDate date);

    boolean existsByRoomIdAndDate(Long roomId, LocalDate date);

}
