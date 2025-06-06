package com.wordline.service;

import com.wordline.dto.CrazySupplierFlight;
import com.wordline.dto.FlightApiDto;
import com.wordline.model.Flight;
import com.wordline.repository.FlightApiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FlightApiServiceTestWithWebClient {

    @Mock
    private FlightApiRepository flightApiRepository;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestBodySpec requestBodySpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @InjectMocks
    private FlightApiService flightApiService;

    private FlightApiDto flightApiDto;
    private Flight flight;
    private CrazySupplierFlight crazySupplierFlight;

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

        crazySupplierFlight = new CrazySupplierFlight();
        crazySupplierFlight.setCarrier("CrazyAir");
        crazySupplierFlight.setBasePrice(100.0);
        crazySupplierFlight.setTax(50.0);
        crazySupplierFlight.setDepartureAirportName("JFK");
        crazySupplierFlight.setArrivalAirportName("LAX");
        crazySupplierFlight.setOutboundDateTime("2025-06-03");
        crazySupplierFlight.setInboundDateTime("2025-06-03");

        // Set mockApiFlag to true for most tests to use mocked response
        setField(flightApiService, "mockApiFlag", true);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
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

        verify(flightApiRepository, times(1)).existsById(1L);
        verify(flightApiRepository, times(1)).deleteById(1L);
    }

    @Test
    public void deleteFlight_notFound() {
        when(flightApiRepository.existsById(1L)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> flightApiService.deleteFlight(1L));
    }

    @Test
    public void getFlight_success() {
        when(flightApiRepository.findById(1L)).thenReturn(Optional.of(flight));

        ResponseEntity<Object> result = flightApiService.getFlight(1L);

        assertNotNull(result);
        assertEquals(result.getStatusCode().value(), HttpStatus.OK.value());
        verify(flightApiRepository, times(1)).findById(1L);
    }

    @Test
    public void getFlight_notFound() {
        when(flightApiRepository.findById(1L)).thenReturn(Optional.empty());
        assertEquals(HttpStatus.NOT_FOUND, flightApiService.getFlight(1L).getStatusCode());
    }

    @Test
    public void searchFlights_withLocalAndCrazySupplier_mocked() {
        when(flightApiRepository.findFlights(any(), any(), any(), any(), any())).thenReturn(List.of(flight));

        List<FlightApiDto> result = flightApiService.searchFlights("JFK", "LAX", null, null, null);

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(dto -> dto.getSupplier().equals("TestSupplier") &&
                dto.getAirline().equals("TestAir")));
        assertTrue(result.stream().anyMatch(dto -> dto.getSupplier().equals("CrazySupplier")));
        assertTrue(result.stream().allMatch(dto ->
                (dto.getDepartureAirport() == null || dto.getDepartureAirport().equals("JFK")) &&
                        (dto.getDestinationAirport() == null || dto.getDestinationAirport().equals("LAX"))));
        verify(flightApiRepository, times(1)).findFlights(any(), any(), any(), any(), any());
        // No WebClient interactions since mockApiFlag=true
        verify(webClient, never()).post();
    }

    @Test
    public void searchFlights_withMockedCrazySupplier_dates() {
        when(flightApiRepository.findFlights(any(), any(), any(), any(), any())).thenReturn(List.of(flight));

        List<FlightApiDto> result = flightApiService.searchFlights("JFK", "LAX", null,
                OffsetDateTime.parse("2025-06-03T10:00:00Z"),
                OffsetDateTime.parse("2025-06-03T13:00:00Z"));

        assertFalse(result.isEmpty());
        assertTrue(result.stream().anyMatch(dto -> dto.getSupplier().equals("CrazySupplier")));
        assertTrue(result.stream().anyMatch(dto -> dto.getDepartureAirport().equals("JFK")));
        assertTrue(result.stream().anyMatch(dto -> dto.getDestinationAirport().equals("LAX")));
        verify(flightApiRepository, times(1)).findFlights(any(), any(), any(), any(), any());
        verify(webClient, never()).post();
    }

}
