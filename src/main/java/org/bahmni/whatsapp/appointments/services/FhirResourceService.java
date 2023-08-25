package org.bahmni.whatsapp.appointments.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
@PropertySource("classpath:application.properties")
public class FhirResourceService {

    @Value("${openmrs.host}")
    private String host;

    @Value("${openmrs.fhir.endpoint.prefix}")
    private String FHIR_END_POINT_PREFIX;

    @Value("${openmrs.username}")
    private String username;

    @Value("${openmrs.password}")
    private String password;

    private final RestTemplate restTemplate;

    @Autowired
    public FhirResourceService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getResourceById(String resourceType, String id) {
        String endPoint = host + FHIR_END_POINT_PREFIX + resourceType + "/" + id;
        System.out.println(endPoint);

        String credentials = username + ":" + password;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.add("Authorization", "Basic " + encodedCredentials);

        ResponseEntity<String> responseEntity = restTemplate.exchange(endPoint, HttpMethod.GET, new HttpEntity<>(null, requestHeaders), String.class);

        String body = responseEntity.getBody();
        IParser iParser = FhirContext.forR4().newJsonParser();
        iParser.setPrettyPrint(true);
        IBaseResource iBaseResource = iParser.parseResource(body);

        //cast to respective type
        if ("Patient".equals(resourceType)) {
            Patient patient = (Patient) iBaseResource;
            System.out.println("Patient ID " + patient.getId());
        } else if ("Condition".equals(resourceType)) {
            Condition condition = (Condition) iBaseResource;
            System.out.println("Condition ID " + condition.getId());
        }
        return body;
    }
}