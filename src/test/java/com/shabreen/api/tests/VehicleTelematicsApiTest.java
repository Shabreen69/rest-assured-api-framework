package com.shabreen.api.tests;

import com.shabreen.api.client.ApiClient;
import com.shabreen.api.models.VehicleDataRequest;
import com.shabreen.api.models.TelematicsResponse;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * VehicleTelematicsApiTest — validates real-time vehicle telematics endpoints.
 *
 * Covers:
 *  - Battery state-of-charge ingestion
 *  - Location data accuracy
 *  - Anomaly detection alerts (deep discharge, overheating)
 *  - Schema validation
 *  - Data consistency across backend and companion app
 */
public class VehicleTelematicsApiTest {

    private static final String TELEMATICS_ENDPOINT  = "/api/v2/vehicles/{vehicleId}/telematics";
    private static final String ALERTS_ENDPOINT      = "/api/v2/vehicles/{vehicleId}/alerts";
    private static final String BATTERY_ENDPOINT     = "/api/v2/vehicles/{vehicleId}/battery";
    private static final String LOCATION_ENDPOINT    = "/api/v2/vehicles/{vehicleId}/location";

    private String vehicleId;

    @BeforeClass
    public void setup() {
        vehicleId = System.getProperty("test.vehicleId", "VH-TEST-001");
    }

    // ── Battery tests ────────────────────────────────────────────

    @Test(description = "Battery SOC should be between 0 and 100",
          groups = {"smoke", "telematics"})
    public void testBatteryStateOfCharge() {
        Response response = ApiClient.get(BATTERY_ENDPOINT.replace("{vehicleId}", vehicleId));

        assertThat("Status code should be 200", response.statusCode(), equalTo(200));
        float soc = response.jsonPath().getFloat("data.stateOfCharge");
        assertThat("SOC must be between 0 and 100", soc, allOf(greaterThanOrEqualTo(0f), lessThanOrEqualTo(100f)));
        assertThat("Battery temperature must be present", response.jsonPath().getString("data.temperatureCelsius"), notNullValue());
        assertThat("Voltage must be positive", response.jsonPath().getFloat("data.voltageV"), greaterThan(0f));
    }

    @Test(description = "Deep discharge alert should trigger when SOC < 5%",
          groups = {"regression", "telematics", "alerts"})
    public void testDeepDischargeAlertThreshold() {
        // Simulate vehicle data with critically low SOC
        VehicleDataRequest request = VehicleDataRequest.builder()
                .vehicleId(vehicleId)
                .stateOfCharge(3.5f)
                .temperatureCelsius(28.0f)
                .voltageV(44.1f)
                .build();

        Response response = ApiClient.post(
                TELEMATICS_ENDPOINT.replace("{vehicleId}", vehicleId), request);

        assertThat(response.statusCode(), equalTo(202));

        // Verify alert was generated
        Response alertsResponse = ApiClient.get(ALERTS_ENDPOINT.replace("{vehicleId}", vehicleId));
        assertThat("Deep discharge alert should be active",
                alertsResponse.jsonPath().getList("alerts.type"), hasItem("DEEP_DISCHARGE"));
        assertThat("Alert severity should be CRITICAL",
                alertsResponse.jsonPath().getString("alerts[0].severity"), equalTo("CRITICAL"));
    }

    @Test(description = "Overheating alert should trigger when battery temp > 55°C",
          groups = {"regression", "telematics", "alerts"})
    public void testOverheatingAlertThreshold() {
        VehicleDataRequest request = VehicleDataRequest.builder()
                .vehicleId(vehicleId)
                .stateOfCharge(75.0f)
                .temperatureCelsius(58.0f)  // Above 55°C threshold
                .voltageV(58.4f)
                .build();

        Response response = ApiClient.post(
                TELEMATICS_ENDPOINT.replace("{vehicleId}", vehicleId), request);
        assertThat(response.statusCode(), equalTo(202));

        Response alertsResponse = ApiClient.get(ALERTS_ENDPOINT.replace("{vehicleId}", vehicleId));
        assertThat("Overheating alert should be active",
                alertsResponse.jsonPath().getList("alerts.type"), hasItem("THERMAL_RUNAWAY_RISK"));
    }

    // ── Location tests ───────────────────────────────────────────

    @Test(description = "Location data should have valid GPS coordinates",
          groups = {"smoke", "telematics"})
    public void testLocationDataAccuracy() {
        Response response = ApiClient.get(LOCATION_ENDPOINT.replace("{vehicleId}", vehicleId));

        assertThat(response.statusCode(), equalTo(200));
        double latitude  = response.jsonPath().getDouble("data.latitude");
        double longitude = response.jsonPath().getDouble("data.longitude");

        // Valid GPS range
        assertThat("Latitude must be in valid range",  latitude,  allOf(greaterThanOrEqualTo(-90.0),  lessThanOrEqualTo(90.0)));
        assertThat("Longitude must be in valid range", longitude, allOf(greaterThanOrEqualTo(-180.0), lessThanOrEqualTo(180.0)));
        assertThat("Timestamp must be present", response.jsonPath().getString("data.timestamp"), notNullValue());
        assertThat("GPS accuracy should be < 10 metres",
                response.jsonPath().getFloat("data.accuracyMetres"), lessThan(10f));
    }

    // ── Schema validation ────────────────────────────────────────

    @Test(description = "Telematics response must conform to API schema",
          groups = {"regression", "schema"})
    public void testTelematicsResponseSchema() {
        Response response = ApiClient.get(TELEMATICS_ENDPOINT.replace("{vehicleId}", vehicleId));
        assertThat(response.statusCode(), equalTo(200));

        // Required top-level fields
        assertThat(response.jsonPath().getString("vehicleId"),      notNullValue());
        assertThat(response.jsonPath().getString("timestamp"),       notNullValue());
        assertThat(response.jsonPath().getString("data.battery"),   notNullValue());
        assertThat(response.jsonPath().getString("data.location"),  notNullValue());
        assertThat(response.jsonPath().getString("data.motor"),     notNullValue());
        assertThat(response.jsonPath().getString("meta.version"),   notNullValue());
    }

    // ── Negative tests ───────────────────────────────────────────

    @Test(description = "Unknown vehicle ID should return 404",
          groups = {"regression"})
    public void testUnknownVehicleReturns404() {
        Response response = ApiClient.get(TELEMATICS_ENDPOINT.replace("{vehicleId}", "INVALID-VH-99999"));
        assertThat(response.statusCode(), equalTo(404));
        assertThat(response.jsonPath().getString("error.code"), equalTo("VEHICLE_NOT_FOUND"));
    }

    @Test(description = "Missing auth token should return 401",
          groups = {"security", "regression"})
    public void testMissingAuthReturns401() {
        Response response = ApiClient.postNoAuth(
                TELEMATICS_ENDPOINT.replace("{vehicleId}", vehicleId),
                VehicleDataRequest.builder().vehicleId(vehicleId).stateOfCharge(50f).build());
        assertThat(response.statusCode(), equalTo(401));
    }

    // ── Performance ──────────────────────────────────────────────

    @Test(description = "Telematics endpoint should respond within 500ms",
          groups = {"performance"})
    public void testTelematicsResponseTime() {
        long start = System.currentTimeMillis();
        Response response = ApiClient.get(TELEMATICS_ENDPOINT.replace("{vehicleId}", vehicleId));
        long elapsed = System.currentTimeMillis() - start;
        assertThat(response.statusCode(), equalTo(200));
        assertThat("Response time should be under 500ms", elapsed, lessThan(500L));
    }
}
