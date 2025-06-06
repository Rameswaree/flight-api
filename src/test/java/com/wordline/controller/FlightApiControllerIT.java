package com.wordline.controller;

import com.wordline.dto.FlightApiDto;
import com.wordline.repository.FlightApiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class FlightApiControllerIT {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    private FlightApiDto testFlightApiDto;

    @BeforeEach
    public void setUp() {
        baseUrl = "http://localhost:" + port + "/api/flights";
        testFlightApiDto = new FlightApiDto();
        testFlightApiDto.setAirline("TestAir");
        testFlightApiDto.setSupplier("TestSupplier");
        testFlightApiDto.setFare(100.0);
        testFlightApiDto.setDepartureAirport("JFK");
        testFlightApiDto.setDestinationAirport("LAX");
        testFlightApiDto.setDepartureTime(OffsetDateTime.parse("2025-06-03T10:00:00Z"));
        testFlightApiDto.setArrivalTime(OffsetDateTime.parse("2025-06-03T13:00:00Z"));
    }

    @Test
    public void testCreateFlight_success() {
        ResponseEntity<FlightApiDto> response = restTemplate.postForEntity(baseUrl, testFlightApiDto, FlightApiDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().getId());
        assertEquals(testFlightApiDto.getAirline(), response.getBody().getAirline());
        assertEquals(testFlightApiDto.getFare(), response.getBody().getFare());
    }

    @Test
    public void testGetFlight_success() {
        // Create a flight first
        ResponseEntity<FlightApiDto> createResponse = restTemplate.postForEntity(baseUrl, testFlightApiDto, FlightApiDto.class);
        Long id = createResponse.getBody().getId();

        ResponseEntity<FlightApiDto> response = restTemplate.getForEntity(baseUrl + "/" + id, FlightApiDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void testUpdateFlight_success() {
        // Create a flight first
        ResponseEntity<FlightApiDto> createResponse = restTemplate.postForEntity(baseUrl, testFlightApiDto, FlightApiDto.class);
        Long id = createResponse.getBody().getId();

        testFlightApiDto.setFare(200.0);
        HttpEntity<FlightApiDto> request = new HttpEntity<>(testFlightApiDto);
        ResponseEntity<FlightApiDto> response = restTemplate.exchange(baseUrl + "/" + id, HttpMethod.PUT, request, FlightApiDto.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(id, response.getBody().getId());
        assertEquals(200.0, response.getBody().getFare());
    }

    @Autowired
    private FlightApiRepository flightApiRepository;

    @Test
    public void testDeleteFlight_success() {
        // Create a flight first
        ResponseEntity<FlightApiDto> createResponse = restTemplate.postForEntity(baseUrl, testFlightApiDto, FlightApiDto.class);
        Long id = Objects.requireNonNull(createResponse.getBody()).getId();

        ResponseEntity<Void> response = restTemplate.exchange(baseUrl + "/" + id, HttpMethod.DELETE, null, Void.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    public void testFetchAllFlights_success() {
        // Create multiple flights
        restTemplate.postForEntity(baseUrl, testFlightApiDto, FlightApiDto.class);
        testFlightApiDto.setFare(150.0);
        restTemplate.postForEntity(baseUrl, testFlightApiDto, FlightApiDto.class);

        ResponseEntity<FlightApiDto[]> response = restTemplate.getForEntity(baseUrl + "/all?pageNum=1&pageSize=1", FlightApiDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().length); // Page size = 1
    }

    @Test
    public void testFetchAllFlights_invalidPageNum() {
        ResponseEntity<FlightApiDto[]> response = restTemplate.getForEntity(baseUrl + "/all?pageNum=0&pageSize=10", FlightApiDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // PageNum = 0 is corrected to 1 (pageNum - 1 = 0 in service)
    }

    @Test
    public void testSearchFlights_success() {
        // Create a flight first
        restTemplate.postForEntity(baseUrl, testFlightApiDto, FlightApiDto.class);

        String searchUrl = baseUrl + "/search?origin=JFK&destination=LAX";
        ResponseEntity<FlightApiDto[]> response = restTemplate.getForEntity(searchUrl, FlightApiDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length >= 1); // Includes local + mocked flights
        assertTrue(Arrays.stream(response.getBody()).anyMatch(dto ->
                dto.getDepartureAirport().equals("JFK") && dto.getDestinationAirport().equals("LAX")));
    }

    @Test
    public void testSearchFlights_airlineOnly() {
        // Create a flight first
        restTemplate.postForEntity(baseUrl, testFlightApiDto, FlightApiDto.class);

        String searchUrl = baseUrl + "/search?airline=TestAir";
        ResponseEntity<FlightApiDto[]> response = restTemplate.getForEntity(searchUrl, FlightApiDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(Arrays.stream(response.getBody()).anyMatch(dto -> dto.getAirline().equals("TestAir")));
    }

    @Test
    public void testSearchFlights_noParameters() {
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/search", String.class);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Please provide at least one search parameter", response.getBody());
    }

    @Test
    public void testSearchFlights_withDateParameters() {
        // Create a flight first
        restTemplate.postForEntity(baseUrl, testFlightApiDto, FlightApiDto.class);

        String searchUrl = baseUrl + "/search?departureTime=2025-06-03T10:00:00Z&arrivalTime=2025-06-03T13:00:00Z";
        ResponseEntity<FlightApiDto[]> response = restTemplate.getForEntity(searchUrl, FlightApiDto[].class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().length >= 1);
    }
}