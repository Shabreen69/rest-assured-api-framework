# REST Assured API Automation Framework

![Java](https://img.shields.io/badge/Java-11-orange)
![RestAssured](https://img.shields.io/badge/RestAssured-5.4-green)
![TestNG](https://img.shields.io/badge/TestNG-7.9-blue)
![Coverage](https://img.shields.io/badge/API_Coverage-30+_microservices-brightgreen)

A robust API test automation framework built with **REST Assured** and **TestNG**, designed for validating microservices at scale. Inspired by real-world telematics validation across **800,000+ EV vehicles**.

---

## Features

- Centralised `ApiClient` — single entry point for all HTTP methods
- JWT / OAuth2 token management with auto-refresh (`TokenManager`)
- Schema validation using JSON Schema and Hamcrest matchers
- Contract testing support for backward compatibility checks
- Data-driven tests with JSON test data files
- Response time SLA assertions built into every test
- Negative, security, and boundary value test coverage
- Allure reporting with request/response logs on failure

---

## Project Structure

```
rest-assured-api-framework/
├── src/
│   ├── main/java/com/shabreen/api/
│   │   ├── client/
│   │   │   └── ApiClient.java           # Centralised HTTP client
│   │   ├── auth/
│   │   │   └── TokenManager.java        # JWT / OAuth2 token cache
│   │   ├── models/
│   │   │   ├── VehicleDataRequest.java  # Request POJOs (Lombok)
│   │   │   └── TelematicsResponse.java  # Response POJOs
│   │   └── config/
│   │       └── ApiConfig.java           # Environment config
│   └── test/java/com/shabreen/api/tests/
│       ├── VehicleTelematicsApiTest.java  # Telematics validation
│       ├── BatteryAlertApiTest.java       # Alert threshold tests
│       ├── AuthApiTest.java               # Auth flow tests
│       └── ContractTest.java              # Backward compat checks
├── src/test/resources/
│   ├── schemas/telematics_schema.json
│   └── testdata/vehicle_test_data.json
└── pom.xml
```

---

## Key Test Areas

| Module | Tests | Coverage |
|--------|-------|----------|
| Vehicle Telematics | SOC, location, motor data | Schema + functional |
| Battery Alerts | Deep discharge, overheating thresholds | Boundary value |
| Authentication | JWT, OAuth2, token expiry | Security |
| Contract Tests | Schema evolution, backward compat | Regression |
| Performance | Response time SLAs (<500ms) | SLA enforcement |

---

## Run Tests

```bash
# All tests
mvn clean test

# Smoke suite only
mvn clean test -Dgroups=smoke

# Telematics tests against staging
mvn clean test -Denv=staging -Dgroups=telematics

# Run with specific vehicle ID
mvn clean test -Dtest.vehicleId=VH-PROD-00123
```

---

## Sample Test — Deep Discharge Alert

```java
@Test(description = "Deep discharge alert triggers when SOC < 5%")
public void testDeepDischargeAlertThreshold() {
    VehicleDataRequest request = VehicleDataRequest.builder()
            .vehicleId(vehicleId)
            .stateOfCharge(3.5f)   // critically low
            .temperatureCelsius(28.0f)
            .build();

    ApiClient.post("/api/v2/vehicles/{id}/telematics", request);

    Response alerts = ApiClient.get("/api/v2/vehicles/{id}/alerts");
    assertThat(alerts.jsonPath().getList("alerts.type"), hasItem("DEEP_DISCHARGE"));
    assertThat(alerts.jsonPath().getString("alerts[0].severity"), equalTo("CRITICAL"));
}
```

---

## Author

**Shabreen Taj** — SDET 2 / QA Lead  
Built and validated telematics pipelines for 800K+ Ola Electric vehicles.  
[LinkedIn](https://www.linkedin.com/in/shabreen-taj-62a941b2/)
