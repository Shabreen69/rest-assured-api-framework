package com.shabreen.api.client;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.LogDetail;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import com.shabreen.api.config.ApiConfig;
import com.shabreen.api.auth.TokenManager;
import org.apache.http.HttpStatus;

import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * ApiClient — centralised REST Assured request builder.
 * All API calls route through here for consistent logging,
 * auth injection, and response validation.
 */
public class ApiClient {

    private static RequestSpecification requestSpec;
    private static ResponseSpecification responseSpec;

    static {
        RestAssured.baseURI = ApiConfig.getBaseUrl();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        requestSpec = new RequestSpecBuilder()
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON)
                .addHeader("x-api-version", ApiConfig.getApiVersion())
                .log(LogDetail.URI)
                .build();

        responseSpec = new ResponseSpecBuilder()
                .expectResponseTime(org.hamcrest.Matchers.lessThan(5000L))
                .build();
    }

    // ── Authenticated requests ──────────────────────────────────

    public static Response get(String endpoint) {
        return given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + TokenManager.getToken())
                .when()
                .get(endpoint)
                .then()
                .spec(responseSpec)
                .extract().response();
    }

    public static Response get(String endpoint, Map<String, Object> queryParams) {
        return given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + TokenManager.getToken())
                .queryParams(queryParams)
                .when()
                .get(endpoint)
                .then()
                .spec(responseSpec)
                .extract().response();
    }

    public static Response post(String endpoint, Object body) {
        return given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + TokenManager.getToken())
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .spec(responseSpec)
                .extract().response();
    }

    public static Response put(String endpoint, Object body) {
        return given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + TokenManager.getToken())
                .body(body)
                .when()
                .put(endpoint)
                .then()
                .spec(responseSpec)
                .extract().response();
    }

    public static Response patch(String endpoint, Object body) {
        return given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + TokenManager.getToken())
                .body(body)
                .when()
                .patch(endpoint)
                .then()
                .spec(responseSpec)
                .extract().response();
    }

    public static Response delete(String endpoint) {
        return given()
                .spec(requestSpec)
                .header("Authorization", "Bearer " + TokenManager.getToken())
                .when()
                .delete(endpoint)
                .then()
                .spec(responseSpec)
                .extract().response();
    }

    // ── Unauthenticated (for login / public endpoints) ──────────

    public static Response postNoAuth(String endpoint, Object body) {
        return given()
                .spec(requestSpec)
                .body(body)
                .when()
                .post(endpoint)
                .then()
                .extract().response();
    }

    // ── Multipart / file upload ─────────────────────────────────

    public static Response uploadFile(String endpoint, String filePath, String controlName) {
        return given()
                .header("Authorization", "Bearer " + TokenManager.getToken())
                .multiPart(controlName, new java.io.File(filePath))
                .when()
                .post(endpoint)
                .then()
                .extract().response();
    }
}
