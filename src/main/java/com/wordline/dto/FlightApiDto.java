package com.wordline.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlightApiDto {

    private Long id;
    private String airline;
    private String supplier;
    private Double fare;
    private String departureAirport;
    private String destinationAirport;
    private OffsetDateTime departureTime;
    private OffsetDateTime arrivalTime;
}
