package com.wordline.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "flights", indexes = {@Index(name = "idx_airline", columnList = "airline"),
        @Index(name = "idx_departure_airport", columnList = "departure_airport"),
        @Index(name = "idx_destination_airport", columnList = "destination_airport"),
        @Index(name = "idx_departure_time", columnList = "departure_time"),
        @Index(name = "idx_arrival_time", columnList = "arrival_time"),
        @Index(name = "idx_departure_destination", columnList = "departure_airport,destination_airport")})
public class Flight {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String airline;

    @Column(nullable = false)
    private String supplier;

    @Column(nullable = false)
    private Double fare;

    @Column(name = "departure_airport", nullable = false, length = 3)
    private String departureAirport;

    @Column(name = "destination_airport", nullable = false, length = 3)
    private String destinationAirport;

    @Column(name = "departure_time", nullable = false)
    private OffsetDateTime departureTime;

    @Column(name = "arrival_time", nullable = false)
    private OffsetDateTime arrivalTime;
}
