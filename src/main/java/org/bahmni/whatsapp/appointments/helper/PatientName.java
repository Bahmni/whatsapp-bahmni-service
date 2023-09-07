package org.bahmni.whatsapp.appointments.helper;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bahmni.webclients.ClientCookies;
import org.bahmni.whatsapp.appointments.services.OpenmrsLoginImpl;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;

public class PatientName {

    @Autowired
    OpenmrsLoginImpl openmrsLogin = new OpenmrsLoginImpl();

    public String fetchPatientName(String patientId) throws IOException {

        openmrsLogin.getConnection();
        ClientCookies cookies = openmrsLogin.getCookies();

        String URI = "https://demo-lite.mybahmni.in/openmrs/ws/fhir2/R4/Patient?identifier=" + patientId;

        HttpGet request = new HttpGet(URI);

        String cookieValue = cookies.get("JSESSIONID");
        request.addHeader("Cookie", "JSESSIONID=" + cookieValue);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(request);

        String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");

        httpClient.close();

        JSONObject responseObject = new JSONObject(responseBody);

        if(responseObject.getInt("total") == 0){
            return "";
        }

        String firstName = responseObject.getJSONArray("entry").getJSONObject(0).getJSONObject("resource").getJSONArray("name").getJSONObject(0).getJSONArray("given").getString(0);
        String familyName = responseObject.getJSONArray("entry").getJSONObject(0).getJSONObject("resource").getJSONArray("name").getJSONObject(0).getString("family");

        return firstName + " " + familyName;
    }
}
