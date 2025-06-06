package com.wordline.controller;

import com.wordline.dto.FlightApiDto;
import com.wordline.service.FlightApiService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
@AllArgsConstructor
public class FlightApiController {
    private final FlightApiService flightApiService;

    @GetMapping("/all")
    public ResponseEntity<List<FlightApiDto>> fetchAllFlights(@RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
                                                              @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        if (pageNum <= 0) {
            pageNum = 1;
        }
        return ResponseEntity.ok(flightApiService.fetchAllFlights(pageNum - 1, pageSize));
    }

    @PostMapping
    public ResponseEntity<FlightApiDto> createFlight(@RequestBody FlightApiDto flightApiDto) {
        return ResponseEntity.ok(flightApiService.createFlight(flightApiDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FlightApiDto> updateFlight(@PathVariable Long id, @RequestBody FlightApiDto flightApiDto) {
        return ResponseEntity.ok(flightApiService.updateFlight(id, flightApiDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlight(@PathVariable Long id) {
        flightApiService.deleteFlight(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getFlight(@PathVariable Long id) {
        return ResponseEntity.ok(flightApiService.getFlight(id));
    }

    @GetMapping("/search")
    public ResponseEntity<Object> searchFlights(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String airline,
            @RequestParam(required = false) OffsetDateTime departureTime,
            @RequestParam(required = false) OffsetDateTime arrivalTime) {

        if (origin == null && destination == null && airline == null && departureTime == null && arrivalTime == null) {
            return new ResponseEntity<>("Please provide at least one search parameter", HttpStatusCode.valueOf(400));
        }
        return ResponseEntity.ok(flightApiService.searchFlights(origin, destination, airline, departureTime, arrivalTime));
    }
}
