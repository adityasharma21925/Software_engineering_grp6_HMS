package com.gray.hospital.controller;

import com.gray.hospital.controller.dto.PublishedRosterSummary;
import com.gray.hospital.controller.dto.RosterComplianceStatus;
import com.gray.hospital.controller.dto.WeeklyRosterRow;
import com.gray.hospital.entity.Roster;
import com.gray.hospital.service.RosterService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/roster")
public class RosterController {

    private final RosterService rosterService;

    public RosterController(RosterService rosterService){
        this.rosterService = rosterService;
    }

    // ✅ Add new roster entry
    @PostMapping("/add")
    public Roster addRoster(
            @RequestParam Long doctorId,
            @RequestParam Long roomId,
            @RequestParam String date,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end){

        return rosterService.addRoster(
                doctorId,
                roomId,
                LocalDate.parse(date),
                start,
                end
        );
    }

    @PostMapping("/add-standard")
    public Roster addStandardRoster(
            @RequestParam Long doctorId,
            @RequestParam Long roomId,
            @RequestParam String date){
        return rosterService.addStandardRoster(
                doctorId,
                roomId,
                LocalDate.parse(date)
        );
    }

    // ✅ Get roster by date (single clean endpoint)
    @GetMapping
    public List<Roster> getRoster(@RequestParam String date){
        return rosterService.getRosterByDate(LocalDate.parse(date));
    }

    @GetMapping("/week")
    public List<Roster> getWeeklyRoster(@RequestParam String startDate){
        return rosterService.getRosterForWeek(LocalDate.parse(startDate));
    }

    @GetMapping("/week-view")
    public List<WeeklyRosterRow> getWeeklyRosterView(@RequestParam String startDate){
        return rosterService.getWeeklyRosterView(LocalDate.parse(startDate));
    }

    @GetMapping("/week-view/doctor")
    public List<WeeklyRosterRow> getWeeklyRosterViewForDoctor(
            @RequestParam String startDate,
            @RequestParam Long doctorId){
        return rosterService.getWeeklyRosterViewForDoctor(LocalDate.parse(startDate), doctorId);
    }

    @GetMapping("/week-compliance")
    public RosterComplianceStatus getWeeklyCompliance(@RequestParam String startDate) {
        return rosterService.getWeeklyCompliance(LocalDate.parse(startDate));
    }

    @PostMapping("/publish-week")
    public PublishedRosterSummary publishWeek(@RequestParam String date) {
        return rosterService.publishWeek(LocalDate.parse(date));
    }

    @GetMapping("/published/week")
    public PublishedRosterSummary getPublishedWeek(@RequestParam String startDate) {
        return rosterService.publishWeek(LocalDate.parse(startDate));
    }
}
