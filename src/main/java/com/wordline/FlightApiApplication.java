package com.wordline;

import com.wordline.model.Flight;
import com.wordline.repository.FlightApiRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.InputStream;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SpringBootApplication
public class FlightApiApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(FlightApiApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().responseTimeout(Duration.ofSeconds(5))))
                .build();
    }

    @Autowired
    FlightApiRepository flightRepository;

    @Override
    public void run(String... args) throws Exception {
        if (flightRepository.count() == 0) { // Only populate if table is empty
            List<Flight> flights = readFlightsFromExcel("flights.xlsx"); // Call the new method
            flightRepository.saveAll(flights);
            System.out.println("Flights data seeded successfully!");
        } else {
            System.out.println("Flights table already contains data. Skipping seeding.");
        }
    }

    private List<Flight> readFlightsFromExcel(String fileName) throws Exception {
        List<Flight> flights = new ArrayList<>();
        // Use ClassPathResource to get the file from the resources folder
        ClassPathResource resource = new ClassPathResource(fileName);

        try (InputStream excelFile = resource.getInputStream();
             Workbook workbook = new XSSFWorkbook(excelFile)) { // Use XSSFWorkbook for .xlsx files

            Sheet sheet = workbook.getSheetAt(0); // Get the first sheet
            Iterator<Row> rowIterator = sheet.iterator();

            // Skip the header row
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME; // For parsing OffsetDateTime

            while (rowIterator.hasNext()) {
                Row currentRow = rowIterator.next();

                // Skip empty rows if necessary, though POI usually handles this gracefully.
                // For a more robust solution, you might check if the first cell is null or empty.
                if (currentRow.getCell(0) == null || currentRow.getCell(0).getCellType() == CellType.BLANK) {
                    continue;
                }

                Flight flight = new Flight();

                // Assuming column order: Airline, Supplier, Fare, Departure Airport, Destination Airport, Departure Time, Arrival Time
                // Adjust cell indices (0, 1, 2, ...) based on your Excel file's column order
                flight.setAirline(getStringCellValue(currentRow.getCell(0)));
                flight.setSupplier(getStringCellValue(currentRow.getCell(1)));
                flight.setFare(getDoubleCellValue(currentRow.getCell(2)));
                flight.setDepartureAirport(getStringCellValue(currentRow.getCell(3)));
                flight.setDestinationAirport(getStringCellValue(currentRow.getCell(4)));
                flight.setDepartureTime(getOffsetDateTimeCellValue(currentRow.getCell(5), formatter));
                flight.setArrivalTime(getOffsetDateTimeCellValue(currentRow.getCell(6), formatter));

                flights.add(flight);
            }
        }
        return flights;
    }

    // Helper method to get string cell value robustly
    private String getStringCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        cell.setCellType(CellType.STRING); // Set cell type to string to avoid issues with numeric cells that look like strings
        return cell.getStringCellValue();
    }

    // Helper method to get double cell value
    private Double getDoubleCellValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue());
            } catch (NumberFormatException e) {
                System.err.println("Warning: Could not parse fare '" + cell.getStringCellValue() + "' as double.");
                return null; // Or throw a specific exception
            }
        }
        return null;
    }

    // Helper method to get OffsetDateTime cell value
    private OffsetDateTime getOffsetDateTimeCellValue(Cell cell, DateTimeFormatter formatter) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.STRING) {
            try {
                return OffsetDateTime.parse(cell.getStringCellValue(), formatter);
            } catch (Exception e) {
                System.err.println("Warning: Could not parse date/time '" + cell.getStringCellValue() + "'. Error: " + e.getMessage());
                return null; // Or throw a specific exception
            }
        }
        return null;
    }
}
