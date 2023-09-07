package org.bahmni.whatsapp.appointments.helper;

import org.bahmni.webclients.ClientCookies;
import org.bahmni.whatsapp.appointments.services.OpenmrsLoginImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SaveChosenSlot {

    @Autowired
    OpenmrsLoginImpl openmrsLogin = new OpenmrsLoginImpl();

    public String saveAppointment(String serviceUuid, String patientUuid, String patientResponse) throws IOException {
        openmrsLogin.getConnection();
        ClientCookies cookies = openmrsLogin.getCookies();

        String cookieValue = cookies.get("JSESSIONID");

        URL url = new URL ("https://demo-lite.mybahmni.in/openmrs/ws/rest/v1/appointment");

        HttpURLConnection con = (HttpURLConnection)url.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("Cookie", "JSESSIONID=" + cookieValue);
        con.setDoOutput(true);

        JSONObject appointmentDetails = new JSONObject();

        appointmentDetails.put("appointmentKind", "Scheduled");
        appointmentDetails.put("locationUuid", JSONObject.NULL);

        System.out.println("Patient UUID: " + patientUuid + " Service UUID: " + serviceUuid);

        appointmentDetails.put("patientUuid", patientUuid);
        appointmentDetails.put("serviceUuid", serviceUuid);
        appointmentDetails.put("providers", new JSONArray());

        LocalDate todayDate = LocalDate.now();
        String tomorrowDate = (todayDate.plusDays(1)).format(DateTimeFormatter.ISO_DATE);

        switch (patientResponse) {
            case "Tomorrow Morning":
                appointmentDetails.put("startDateTime", tomorrowDate + "T04:30:00.000Z");
                appointmentDetails.put("endDateTime", tomorrowDate + "T05:30:00.000Z");
                break;
            case "Tomorrow Afternoon":
                appointmentDetails.put("startDateTime", tomorrowDate + "T08:30:00.000Z");
                appointmentDetails.put("endDateTime", tomorrowDate + "T09:30:00.000Z");
                break;
            case "Tomorrow Evening":
                appointmentDetails.put("startDateTime", tomorrowDate + "T12:30:00.000Z");
                appointmentDetails.put("endDateTime", tomorrowDate + "T13:30:00.000Z");
                break;
        }

        String appointmentInput = appointmentDetails.toString();

        try(OutputStream os = con.getOutputStream()) {
            byte[] input = appointmentInput.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        try(BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }

            return String.valueOf(response);
        }
    }
}
