package org.bahmni.whatsapp.appointments.services;

import org.bahmni.webclients.HttpClient;
import org.bahmni.whatsapp.appointments.client.ConnectionDetails;
import org.bahmni.whatsapp.appointments.client.WebClientFactory;
import org.bahmni.whatsapp.appointments.contract.patient.OpenMRSPatientFullRepresentation;
import org.bahmni.whatsapp.appointments.mappers.OpenMRSPatientMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;

@Component
public class OpenMRSService {

    public OpenMRSPatientFullRepresentation getPatientFR(String patientUrl) throws IOException, ParseException {
        HttpClient webClient = WebClientFactory.getClient();
        String urlPrefix = getURLPrefix();
        String patientJSON = webClient.get(URI.create(urlPrefix + patientUrl));
        return new OpenMRSPatientMapper().mapFullRepresentation(patientJSON);
    }

    private String getURLPrefix() {
        org.bahmni.webclients.ConnectionDetails connectionDetails = ConnectionDetails.get();
        String authenticationURI = connectionDetails.getAuthUrl();

        URL openMRSAuthURL;
        try {
            openMRSAuthURL = new URL(authenticationURI);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Is not a valid URI - " + authenticationURI);
        }
        return String.format("%s://%s", openMRSAuthURL.getProtocol(), openMRSAuthURL.getAuthority());
    }

}