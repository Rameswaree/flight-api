package com.wordline.controller;

import com.wordline.dto.FlightApiDto;
import com.wordline.service.FlightApiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FlightApiControllerTest {

    @Mock
    private FlightApiService flightApiService;

    @InjectMocks
    private FlightApiController controller;

    private FlightApiDto sampleFlight;

    @BeforeEach
    public void setUp() {
        sampleFlight = new FlightApiDto();
        sampleFlight.setId(1L);
        sampleFlight.setAirline("KLM");
        sampleFlight.setSupplier("WordlineSupplier");
        sampleFlight.setFare(599.99);
        sampleFlight.setDepartureAirport("AMS");
        sampleFlight.setDestinationAirport("JFK");
        sampleFlight.setDepartureTime(OffsetDateTime.parse("2025-07-01T10:00:00Z"));
        sampleFlight.setArrivalTime(OffsetDateTime.parse("2025-07-01T18:00:00Z"));
    }


    @Test
    public void fetchAllFlights_shouldReturnList() {
        when(flightApiService.fetchAllFlights(0, 10)).thenReturn(List.of(sampleFlight));

        ResponseEntity<List<FlightApiDto>> response = controller.fetchAllFlights(1, 10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals("KLM", response.getBody().getFirst().getAirline());
        assertEquals("AMS", response.getBody().getFirst().getDepartureAirport());
    }

    @Test
    public void createFlight_shouldReturnCreatedFlight() {
        when(flightApiService.createFlight(any(FlightApiDto.class))).thenReturn(sampleFlight);

        ResponseEntity<FlightApiDto> response = controller.createFlight(sampleFlight);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("JFK", response.getBody().getDestinationAirport());
        assertEquals(599.99, response.getBody().getFare());
    }

    @Test
    public void updateFlight_shouldReturnUpdatedFlight() {
        when(flightApiService.updateFlight(eq(1L), any(FlightApiDto.class))).thenReturn(sampleFlight);

        ResponseEntity<FlightApiDto> response = controller.updateFlight(1L, sampleFlight);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("WordlineSupplier", response.getBody().getSupplier());
    }

    @Test
    public void deleteFlight_shouldReturnNoContent() {
        doNothing().when(flightApiService).deleteFlight(1L);

        ResponseEntity<Void> response = controller.deleteFlight(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(flightApiService).deleteFlight(1L);
    }

    @Test
    public void searchFlights_withValidParams_shouldReturnResults() {
        when(flightApiService.searchFlights(eq("AMS"), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(sampleFlight));

        ResponseEntity<Object> response = controller.searchFlights("AMS", null, null, null, null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<FlightApiDto> results = (List<FlightApiDto>) response.getBody();
        assertEquals(1, results.size());
        assertEquals("AMS", results.getFirst().getDepartureAirport());
    }

    @Test
    public void searchFlights_withoutParams_shouldReturnBadRequest() {
        ResponseEntity<Object> response = controller.searchFlights(null, null, null, null, null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Please provide at least one search parameter", response.getBody());
    }
}
