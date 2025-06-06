package com.wordline.repository;

import com.wordline.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface FlightApiRepository extends JpaRepository<Flight, Long> {

    @Query("SELECT f FROM Flight f WHERE " + "(:origin IS NULL OR f.departureAirport = :origin) AND " + "(:destination IS NULL OR f.destinationAirport = :destination) AND " + "(:airline IS NULL OR f.airline = :airline) AND " + "(:departureTime IS NULL OR f.departureTime >= :departureTime) AND " + "(:arrivalTime IS NULL OR f.arrivalTime <= :arrivalTime)")
    List<Flight> findFlights(@Param("origin") String origin, @Param("destination") String destination, @Param("airline") String airline, @Param("departureTime") OffsetDateTime departureTime, @Param("arrivalTime") OffsetDateTime arrivalTime);
}
