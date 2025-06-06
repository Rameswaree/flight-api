package com.wordline.service;

import com.wordline.dto.FlightApiDto;
import com.wordline.model.Flight;
import com.wordline.repository.FlightApiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FlightApiServiceTest {

    @Mock
    private FlightApiRepository flightApiRepository;

    @InjectMocks
    private FlightApiService flightApiService;

    private FlightApiDto flightApiDto;
    private Flight flight;

    @BeforeEach
    public void setUp() {
        flightApiDto = new FlightApiDto();
        flightApiDto.setAirline("TestAir");
        flightApiDto.setSupplier("TestSupplier");
        flightApiDto.setFare(100.0);
        flightApiDto.setDepartureAirport("JFK");
        flightApiDto.setDestinationAirport("LAX");
        flightApiDto.setDepartureTime(OffsetDateTime.parse("2025-06-03T10:00:00Z"));
        flightApiDto.setArrivalTime(OffsetDateTime.parse("2025-06-03T13:00:00Z"));

        flight = new Flight();
        flight.setId(1L);
        flight.setAirline("TestAir");
        flight.setSupplier("TestSupplier");
        flight.setFare(100.0);
        flight.setDepartureAirport("JFK");
        flight.setDestinationAirport("LAX");
        flight.setDepartureTime(OffsetDateTime.parse("2025-06-03T10:00:00Z"));
        flight.setArrivalTime(OffsetDateTime.parse("2025-06-03T13:00:00Z"));
    }

    @Test
    public void createFlight_success() {
        when(flightApiRepository.save(any(Flight.class))).thenReturn(flight);

        FlightApiDto result = flightApiService.createFlight(flightApiDto);

        assertNotNull(result);
        assertEquals(flightApiDto.getAirline(), result.getAirline());
        assertEquals(flightApiDto.getFare(), result.getFare());
        verify(flightApiRepository, times(1)).save(any(Flight.class));
    }

    @Test
    public void updateFlight_success() {
        when(flightApiRepository.findById(1L)).thenReturn(Optional.of(flight));
        when(flightApiRepository.save(any(Flight.class))).thenReturn(flight);

        FlightApiDto result = flightApiService.updateFlight(1L, flightApiDto);

        assertNotNull(result);
        assertEquals(flightApiDto.getAirline(), result.getAirline());
        verify(flightApiRepository, times(1)).findById(1L);
        verify(flightApiRepository, times(1)).save(any(Flight.class));
    }

    @Test
    public void updateFlight_notFound() {
        when(flightApiRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> flightApiService.updateFlight(1L, flightApiDto));
    }

    @Test
    public void deleteFlight_success() {
        when(flightApiRepository.existsById(1L)).thenReturn(true);

        flightApiService.deleteFlight(1L);

        verify(flightApiRepository, times(1)).deleteById(1L);
    }

    @Test
    public void deleteFlight_notFound() {
        when(flightApiRepository.existsById(1L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> flightApiService.deleteFlight(1L));
    }

    @Test
    public void searchFlights_withLocalAndCrazySupplier() {
        when(flightApiRepository.findFlights(any(), any(), any(), any(), any())).thenReturn(List.of(flight));

        List<FlightApiDto> result = flightApiService.searchFlights("JFK", "LAX", null, null, null);

        assertFalse(result.isEmpty()); // At least one local flight, plus 1-3 mocked CrazySupplier flights
        assertTrue(result.stream().anyMatch(dto -> dto.getSupplier().equals("TestSupplier") &&
                dto.getAirline().equals("TestAir")));
        assertTrue(result.stream().anyMatch(dto -> dto.getSupplier().equals("CrazySupplier")));
        assertTrue(result.stream().allMatch(dto ->
                (dto.getDepartureAirport().equals("JFK") || dto.getDepartureAirport() == null) &&
                        (dto.getDestinationAirport().equals("LAX") || dto.getDestinationAirport() == null)));
        verify(flightApiRepository, times(1)).findFlights(any(), any(), any(), any(), any());
    }

    @Test
    public void searchFlights_withMockedCrazySupplier() {
        when(flightApiRepository.findFlights(any(), any(), any(), any(), any())).thenReturn(List.of(flight));

        List<FlightApiDto> result = flightApiService.searchFlights("JFK", "LAX", null,
                OffsetDateTime.parse("2025-06-03T10:00:00Z"),
                OffsetDateTime.parse("2025-06-03T13:00:00Z"));

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(dto -> dto.getSupplier().equals("CrazySupplier")));
        assertTrue(result.stream().anyMatch(dto -> dto.getDepartureAirport().equals("JFK")));
        assertTrue(result.stream().anyMatch(dto -> dto.getDestinationAirport().equals("LAX")));
        verify(flightApiRepository, times(1)).findFlights(any(), any(), any(), any(), any());
    }
}