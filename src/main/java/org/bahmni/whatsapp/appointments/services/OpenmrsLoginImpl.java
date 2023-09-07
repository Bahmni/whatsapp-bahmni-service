package org.bahmni.whatsapp.appointments.services;

import org.bahmni.webclients.ClientCookies;
import org.bahmni.webclients.HttpClient;
import org.bahmni.whatsapp.appointments.client.ConnectionDetails;
import org.bahmni.whatsapp.appointments.client.WebClientFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Component
public class OpenmrsLoginImpl {
    private ClientCookies cookies;

    public void getConnection() {
        HttpClient authenticatedWebClient = WebClientFactory.getClient();
        org.bahmni.webclients.ConnectionDetails connectionDetails = ConnectionDetails.get();
        String authUri = connectionDetails.getAuthUrl();
        getCookiesAfterConnection(authenticatedWebClient, authUri);
    }

    public void getCookiesAfterConnection(HttpClient authenticatedWebClient, String urlString) {
        try {
            this.cookies = authenticatedWebClient.getCookies(new URI(urlString));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Is not a valid URI - " + urlString);
        }
    }

    public ClientCookies getCookies() {
        return cookies;
    }
}
