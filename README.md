# flight-api

The SpringBoot application provides APIs to manage flight data, including the ability to add, update, delete, and retrieve flight information.
Clients can also search and filter flight data based on origin, destination, airline, departure and arrival time.

## Pre-requisites
- Java JDK 21
- Spring Boot 3.5.0
- Maven
- MySQL database

## How to run the project
1. Clone the repository:
   ```bash
   git clone https://github.com/Rameswaree/flight-api
   ```
2. Before running the project, ensure you have a local MySQL database set up. Update the `application.properties` and the `application-test.properties` file with your database credentials.
   Given below is the properties file configuration:
   ```properties
    spring.datasource.url=jdbc:mysql://localhost:3308/flight_assignment_db?createDatabaseIfNotExist=true&autoReconnect=true&useSSL=false
    spring.datasource.username=root
    spring.datasource.password=CodeJava007
   ```'
3. Run the project using the following commands:
    ```bash
    mvn clean install
    mvn spring-boot:run
    ```
   Alternatively, you can run the project from the IDE by running the `FlightApiApplication.java` file.
4. You can execute the APIs using Swagger UI available at:
   ```
   http://localhost:8080/swagger-ui/index.html#/
   ```
5. Alternatively, you can use Postman to test the APIs. The Postman collection is available in the project directory.


## Solution

- The application generates a mock response for the external Crazy Supplier API since the provided API is dummy and does not return actual data. The response also contains original data from the database.
- A xlsx file containing 10000 records of flight data is provided in the resources directory. The application reads this file and populates the database with the flight data.
- The application once started for the first time, will read the xlsx file and populate the database with the flight data. The application will not read the file again unless the database is empty.

## Business validations:
 - When an airport that is not present in the database is retrieved, the API will return an error.
 - The same goes with the other fields in the search API. If no fields are provided, then the API will return an error with a message indicating that at least one field is required.