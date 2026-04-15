package com.gray.hospital.service;

import com.gray.hospital.controller.dto.PublishedRosterSummary;
import com.gray.hospital.controller.dto.RosterComplianceStatus;
import com.gray.hospital.controller.dto.WeeklyRosterRow;
import com.gray.hospital.entity.Doctor;
import com.gray.hospital.entity.Nurse;
import com.gray.hospital.entity.Roster;
import com.gray.hospital.repository.DoctorRepository;
import com.gray.hospital.repository.NurseRepository;
import com.gray.hospital.repository.RosterRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class RosterService {

    public static final LocalTime DEFAULT_DAY_SHIFT_START = LocalTime.of(9, 0);
    public static final LocalTime DEFAULT_DAY_SHIFT_END = LocalTime.of(17, 0);
    public static final LocalTime DEFAULT_NIGHT_SHIFT_START = LocalTime.of(20, 0);
    public static final LocalTime DEFAULT_NIGHT_SHIFT_END = LocalTime.of(8, 0);
    public static final LocalTime VISITING_FIRST_SLOT_START = LocalTime.of(9, 0);
    public static final LocalTime VISITING_LAST_SLOT_END = LocalTime.of(17, 0);

    private final RosterRepository rosterRepository;
    private final DoctorRepository doctorRepository;
    private final NurseRepository nurseRepository;
    private final RosterPublicationService rosterPublicationService;

    public RosterService(RosterRepository rosterRepository,
                         DoctorRepository doctorRepository,
                         NurseRepository nurseRepository,
                         RosterPublicationService rosterPublicationService){

        this.rosterRepository = rosterRepository;
        this.doctorRepository = doctorRepository;
        this.nurseRepository = nurseRepository;
        this.rosterPublicationService = rosterPublicationService;
    }

    public Roster addRoster(Long doctorId,
                            Long roomId,
                            LocalDate date,
                            String start,
                            String end){
        if (rosterRepository.existsByDoctorDoctorIdAndDate(doctorId, date)) {
            throw new RuntimeException("Doctor already has a roster for this date");
        }

        if (rosterRepository.existsByRoomIdAndDate(roomId, date)) {
            throw new RuntimeException("Room already assigned for this date");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow();

        LocalTime startTime = resolveStartTime(start, doctor.getDoctorType());
        LocalTime endTime = resolveEndTime(end, doctor.getDoctorType(), startTime);
        validateDoctorSpecificRosterRules(doctor, date, startTime, endTime);

        Roster roster = new Roster();

        roster.setDoctor(doctor);
        roster.setRoomId(roomId);
        roster.setDate(date);
        roster.setStartTime(startTime);
        roster.setEndTime(endTime);

        Roster savedRoster = rosterRepository.save(roster);
        publishWeek(date);
        return savedRoster;
    }

    public Roster addStandardRoster(Long doctorId,
                                    Long roomId,
                                    LocalDate date){
        return addRoster(
                doctorId,
                roomId,
                date,
                DEFAULT_DAY_SHIFT_START.toString(),
                DEFAULT_DAY_SHIFT_END.toString()
        );
    }

    public List<Roster> getRosterForDay(LocalDate date){

        return rosterRepository.findByDate(date);

    }
    public List<Roster> getRosterByDate(LocalDate date){
        return rosterRepository.findByDate(date);
    }

    public List<Roster> getRosterForWeek(LocalDate startDate){
        return rosterRepository.findByDateBetween(startDate, startDate.plusDays(6));
    }

    public List<WeeklyRosterRow> getWeeklyRosterView(LocalDate startDate){
        LocalDate endDate = startDate.plusDays(6);
        List<Roster> weeklyRoster = rosterRepository.findByDateBetween(startDate, endDate);
        List<WeeklyRosterRow> rows = new ArrayList<>();

        weeklyRoster.stream()
                .sorted(Comparator.comparing(Roster::getDate)
                        .thenComparing(Roster::getStartTime)
                        .thenComparing(roster -> roster.getDoctor().getDoctorId()))
                .forEach(roster -> {
            Nurse nurse = nurseRepository.findByDoctorDoctorId(roster.getDoctor().getDoctorId())
                    .orElse(null);

            rows.add(new WeeklyRosterRow(
                    startDate.toString(),
                    endDate.toString(),
                    roster.getDate().toString(),
                    roster.getDoctor().getDoctorId(),
                    roster.getDoctor().getName(),
                    roster.getDoctor().getDoctorType(),
                    roster.getRoomId(),
                    roster.getStartTime().toString(),
                    roster.getEndTime().toString(),
                    nurse != null ? nurse.getNurseId() : null,
                    nurse != null ? nurse.getName() : "Not Assigned"
            ));
        });

        return rows;
    }

    public List<WeeklyRosterRow> getWeeklyRosterViewForDoctor(LocalDate startDate, Long doctorId){
        return getWeeklyRosterView(startDate).stream()
                .filter(row -> row.doctorId().equals(doctorId))
                .toList();
    }

    public RosterComplianceStatus getWeeklyCompliance(LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(6);
        List<Roster> weeklyRoster = rosterRepository.findByDateBetween(startDate, endDate);
        List<String> issues = new ArrayList<>();

        for (int offset = 0; offset < 7; offset++) {
            LocalDate currentDate = startDate.plusDays(offset);
            int dayRegularCount = 0;
            int nightRegularCount = 0;

            for (Roster roster : weeklyRoster) {
                if (!currentDate.equals(roster.getDate()) || !"REGULAR".equalsIgnoreCase(roster.getDoctor().getDoctorType())) {
                    continue;
                }

                if (overlaps(roster.getStartTime(), roster.getEndTime(), LocalTime.of(8, 0), LocalTime.of(20, 0))) {
                    dayRegularCount++;
                }

                if (isNightShift(roster.getStartTime(), roster.getEndTime())) {
                    nightRegularCount++;
                }
            }

            if (dayRegularCount < 3) {
                issues.add(currentDate + ": requires at least 3 regular doctors during 08:00-20:00, found " + dayRegularCount);
            }

            if (nightRegularCount < 1) {
                issues.add(currentDate + ": requires at least 1 regular doctor during 20:00-08:00, found " + nightRegularCount);
            }
        }

        for (Doctor doctor : doctorRepository.findByDoctorType("VISITING")) {
            List<Roster> doctorWeeklyRoster = rosterRepository.findByDoctorDoctorIdAndDateBetween(
                    doctor.getDoctorId(),
                    startDate,
                    endDate
            );

            if (doctorWeeklyRoster.size() != 2) {
                issues.add(doctor.getName() + ": visiting consultants must be rostered exactly twice a week, found " + doctorWeeklyRoster.size());
            }

            for (Roster roster : doctorWeeklyRoster) {
                if (getDurationHours(roster.getStartTime(), roster.getEndTime()) != 2) {
                    issues.add(doctor.getName() + ": visiting consultant roster on " + roster.getDate() + " must be exactly 2 hours");
                }
            }
        }

        boolean dayCoverageMet = issues.stream().noneMatch(issue -> issue.contains("08:00-20:00"));
        boolean nightCoverageMet = issues.stream().noneMatch(issue -> issue.contains("20:00-08:00"));
        boolean visitingRulesMet = issues.stream().noneMatch(issue -> issue.contains("visiting"));

        return new RosterComplianceStatus(
                startDate.toString(),
                endDate.toString(),
                dayCoverageMet,
                nightCoverageMet,
                visitingRulesMet,
                issues
        );
    }

    public PublishedRosterSummary publishWeek(LocalDate date) {
        LocalDate weekStart = date.minusDays(date.getDayOfWeek().getValue() - 1L);
        return rosterPublicationService.publishWeek(weekStart, getWeeklyRosterView(weekStart));
    }

    private LocalTime resolveStartTime(String start, String doctorType){
        if (start == null || start.isBlank()) {
            if ("VISITING".equalsIgnoreCase(doctorType)) {
                return VISITING_FIRST_SLOT_START;
            }
            return DEFAULT_DAY_SHIFT_START;
        }

        return LocalTime.parse(start);
    }

    private LocalTime resolveEndTime(String end, String doctorType, LocalTime startTime){
        if (end == null || end.isBlank()) {
            if ("VISITING".equalsIgnoreCase(doctorType)) {
                return startTime.plusHours(2);
            }
            return DEFAULT_DAY_SHIFT_END;
        }

        return LocalTime.parse(end);
    }

    private void validateDoctorSpecificRosterRules(Doctor doctor, LocalDate date, LocalTime startTime, LocalTime endTime) {
        if ("VISITING".equalsIgnoreCase(doctor.getDoctorType())) {
            validateVisitingRoster(doctor, date, startTime, endTime);
            return;
        }

        boolean dayShift = startTime.equals(DEFAULT_DAY_SHIFT_START) && endTime.equals(DEFAULT_DAY_SHIFT_END);
        boolean nightShift = startTime.equals(DEFAULT_NIGHT_SHIFT_START) && endTime.equals(DEFAULT_NIGHT_SHIFT_END);

        if (!dayShift && !nightShift) {
            throw new RuntimeException("Regular doctor roster must be 09:00-17:00 or 20:00-08:00");
        }
    }

    private void validateVisitingRoster(Doctor doctor, LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (isNightShift(startTime, endTime)) {
            throw new RuntimeException("Visiting consultant roster must be a daytime 2-hour slot");
        }

        if (getDurationHours(startTime, endTime) != 2) {
            throw new RuntimeException("Visiting consultant roster must be exactly 2 hours");
        }

        if (startTime.isBefore(VISITING_FIRST_SLOT_START) || endTime.isAfter(VISITING_LAST_SLOT_END)) {
            throw new RuntimeException("Visiting consultant roster must be within 09:00-17:00");
        }

        LocalDate weekStart = date.minusDays(date.getDayOfWeek().getValue() - 1L);
        LocalDate weekEnd = weekStart.plusDays(6);
        List<Roster> existingWeeklyRoster = rosterRepository.findByDoctorDoctorIdAndDateBetween(
                doctor.getDoctorId(),
                weekStart,
                weekEnd
        );

        if (existingWeeklyRoster.size() >= 2) {
            throw new RuntimeException("Visiting consultant can only be rostered twice in a week");
        }
    }

    private boolean overlaps(LocalTime rosterStart, LocalTime rosterEnd, LocalTime windowStart, LocalTime windowEnd) {
        if (isNightShift(rosterStart, rosterEnd)) {
            return false;
        }
        return rosterStart.isBefore(windowEnd) && rosterEnd.isAfter(windowStart);
    }

    private boolean isNightShift(LocalTime rosterStart, LocalTime rosterEnd) {
        return rosterEnd.isBefore(rosterStart);
    }

    private long getDurationHours(LocalTime startTime, LocalTime endTime) {
        if (isNightShift(startTime, endTime)) {
            return 24 - startTime.getHour() + endTime.getHour();
        }
        return java.time.Duration.between(startTime, endTime).toHours();
    }
}
