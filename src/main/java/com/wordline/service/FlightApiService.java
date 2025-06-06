package com.wordline.service;

import com.wordline.dto.CrazySupplierFlight;
import com.wordline.dto.CrazySupplierRequest;
import com.wordline.dto.FlightApiDto;
import com.wordline.model.Flight;
import com.wordline.repository.FlightApiRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FlightApiService {
    private final FlightApiRepository flightApiRepository;
    private final RestTemplate restTemplate;
    private final WebClient webClient;

    @Value("${crazy.supplier.api.url:https://api.crazy-supplier.com/flights}")
    private String crazySupplierApiUrl;

    @Value("${crazy.supplier.api.mock.flag:true}")
    private Boolean mockApiFlag;

    public FlightApiService(FlightApiRepository flightApiRepository, RestTemplate restTemplate, WebClient webClient) {
        this.flightApiRepository = flightApiRepository;
        this.restTemplate = restTemplate;
        this.webClient = webClient;
    }

    private static final Random RANDOM = new Random();

    @Transactional
    public FlightApiDto createFlight(FlightApiDto flightApiDto) {
        Flight flight = toEntity(flightApiDto);
        Flight savedFlight = flightApiRepository.save(flight);
        return toDTO(savedFlight);
    }

    @Transactional
    public FlightApiDto updateFlight(Long id, FlightApiDto flightApiDto) {
        Flight flight = flightApiRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found"));
        updateEntity(flight, flightApiDto);
        Flight updatedFlight = flightApiRepository.save(flight);
        return toDTO(updatedFlight);
    }

    @Transactional
    public void deleteFlight(Long id) {
        if (!flightApiRepository.existsById(id)) {
            throw new RuntimeException("Flight not found");
        }
        flightApiRepository.deleteById(id);
    }

    public ResponseEntity<Object> getFlight(Long id) {
        Optional<Flight> flight = flightApiRepository.findById(id);
        return flight.<ResponseEntity<Object>>map(value -> ResponseEntity.ok(toDTO(value))).orElseGet(() -> new ResponseEntity<>("Flight not found", HttpStatusCode.valueOf(404)));
    }

    public List<FlightApiDto> fetchAllFlights(int pageNumber, int pageSize) {
        Page<Flight> localFlights = flightApiRepository.findAll(PageRequest.of(pageNumber, pageSize));
        return localFlights.stream()
                .map(this::toDTO)
                .toList();
    }

    public List<FlightApiDto> searchFlights(String origin, String destination, String airline,
                                         OffsetDateTime departureTime, OffsetDateTime arrivalTime) {
        //Here these are getting fetched from the database.
        List<FlightApiDto> localFlights = flightApiRepository.findFlights(origin, destination, airline, departureTime, arrivalTime)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());


        Long maxLocalFlights = localFlights.getLast().getId();

        //Then these are getting fetched from the Mock
        List<FlightApiDto> crazySupplierFlights = fetchCrazySupplierFlights(origin, destination, departureTime, arrivalTime, airline, maxLocalFlights);

        //Then they are getting merged.
        localFlights.addAll(crazySupplierFlights);
        return localFlights;
    }

    private List<FlightApiDto> fetchCrazySupplierFlights(String origin, String destination,
                                                      OffsetDateTime departureTime, OffsetDateTime arrivalTime, String airline, Long maxLocalFlights) {
        CrazySupplierRequest request = new CrazySupplierRequest();
        request.setAirline(airline);
        request.setFrom(origin);
        request.setTo(destination);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
        if (departureTime != null) {
            request.setOutboundDate(departureTime.atZoneSameInstant(ZoneId.of("CET"))
                    .format(dateFormatter));
        }
        if (arrivalTime != null) {
            request.setInboundDate(arrivalTime.atZoneSameInstant(ZoneId.of("CET"))
                    .format(dateFormatter));
        }

        if (mockApiFlag != null && !mockApiFlag) {
            CrazySupplierFlight[] response = webClient.post()
                    .uri(crazySupplierApiUrl)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            Mono.error(new RuntimeException("API error: " + clientResponse.statusCode())))
                    .bodyToMono(CrazySupplierFlight[].class)
                    .block();

            if (response == null) {
                return List.of();
            }
            return Arrays.stream(response)
                    .map(this::toFlightDTO)
                    .toList();
        } else {
            // Mock CrazySupplier API response
            List<CrazySupplierFlight> mockedResponse = generateMockedCrazySupplierResponse(request, maxLocalFlights);
            return mockedResponse.stream()
                    .map(this::toFlightDTO)
                    .toList();
        }
    }


    private List<CrazySupplierFlight> generateMockedCrazySupplierResponse(CrazySupplierRequest request, Long maxLocalFlights) {
        List<CrazySupplierFlight> flights = new ArrayList<>();

        // Generate 1-3 random flights for variety
        int numFlights = RANDOM.nextInt(3) + 1;
        for (int i = 0; i < numFlights; i++) {
            CrazySupplierFlight flight = new CrazySupplierFlight();
            flight.setId(maxLocalFlights + 1);
            flight.setCarrier(request.getAirline());
            flight.setBasePrice(100.0 + RANDOM.nextDouble() * 400.0); // Random base price between 100 and 500
            flight.setTax(20.0 + RANDOM.nextDouble() * 50.0); // Random tax between 20 and 70
            flight.setDepartureAirportName(request.getFrom() != null ? request.getFrom() : "JFK");
            flight.setArrivalAirportName(request.getTo() != null ? request.getTo() : "LAX");

            // Use provided dates or generate realistic ones
            String outboundDate = request.getOutboundDate() != null
                    ? request.getOutboundDate()
                    : OffsetDateTime.now(ZoneId.of("CET")).plusDays(RANDOM.nextInt(7)).format(DateTimeFormatter.ISO_LOCAL_DATE);
            String inboundDate = request.getInboundDate() != null
                    ? request.getInboundDate()
                    : OffsetDateTime.now(ZoneId.of("CET")).plusDays(RANDOM.nextInt(7) + 7).format(DateTimeFormatter.ISO_LOCAL_DATE);

            flight.setOutboundDateTime(outboundDate);
            flight.setInboundDateTime(inboundDate);

            flights.add(flight);
        }

        return flights;
    }

    private FlightApiDto toFlightDTO(CrazySupplierFlight crazySupplierFlight) {
        FlightApiDto flightApiDto = new FlightApiDto();
        flightApiDto.setAirline(crazySupplierFlight.getCarrier());
        flightApiDto.setSupplier("CrazySupplier");
        flightApiDto.setFare(crazySupplierFlight.getBasePrice() + crazySupplierFlight.getTax());
        flightApiDto.setDepartureAirport(crazySupplierFlight.getDepartureAirportName());
        flightApiDto.setDestinationAirport(crazySupplierFlight.getArrivalAirportName());
        // Convert CET to UTC
        flightApiDto.setDepartureTime(OffsetDateTime.parse(crazySupplierFlight.getOutboundDateTime() + "T00:00:00+01:00")
                .withOffsetSameInstant(ZoneId.of("UTC").getRules().getOffset(OffsetDateTime.now().toInstant())));
        flightApiDto.setArrivalTime(OffsetDateTime.parse(crazySupplierFlight.getInboundDateTime() + "T00:00:00+01:00")
                .withOffsetSameInstant(ZoneId.of("UTC").getRules().getOffset(OffsetDateTime.now().toInstant())));
        return flightApiDto;
    }

    private Flight toEntity(FlightApiDto flightApiDto) {
        Flight flight = new Flight();
        flight.setAirline(flightApiDto.getAirline());
        flight.setSupplier(flightApiDto.getSupplier());
        flight.setFare(flightApiDto.getFare());
        flight.setDepartureAirport(flightApiDto.getDepartureAirport());
        flight.setDestinationAirport(flightApiDto.getDestinationAirport());
        flight.setDepartureTime(flightApiDto.getDepartureTime());
        flight.setArrivalTime(flightApiDto.getArrivalTime());
        return flight;
    }

    private FlightApiDto toDTO(Flight flight) {
        FlightApiDto flightApiDto = new FlightApiDto();
        flightApiDto.setId(flight.getId());
        flightApiDto.setAirline(flight.getAirline());
        flightApiDto.setSupplier(flight.getSupplier());
        flightApiDto.setFare(flight.getFare());
        flightApiDto.setDepartureAirport(flight.getDepartureAirport());
        flightApiDto.setDestinationAirport(flight.getDestinationAirport());
        flightApiDto.setDepartureTime(flight.getDepartureTime());
        flightApiDto.setArrivalTime(flight.getArrivalTime());
        return flightApiDto;
    }

    private void updateEntity(Flight flight, FlightApiDto flightApiDto) {
        flight.setAirline(flightApiDto.getAirline());
        flight.setSupplier(flightApiDto.getSupplier());
        flight.setFare(flightApiDto.getFare());
        flight.setDepartureAirport(flightApiDto.getDepartureAirport());
        flight.setDestinationAirport(flightApiDto.getDestinationAirport());
        flight.setDepartureTime(flightApiDto.getDepartureTime());
        flight.setArrivalTime(flightApiDto.getArrivalTime());
    }
}
