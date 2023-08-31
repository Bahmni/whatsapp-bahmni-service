package org.bahmni.whatsapp.appointments;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bahmni.webclients.ClientCookies;
import org.bahmni.whatsapp.appointments.services.OpenmrsLoginImpl;
import org.bahmni.whatsapp.appointments.wa.templates.ActionTemplate;
import org.bahmni.whatsapp.appointments.wa.templates.ServiceTemplate;
import org.bahmni.whatsapp.appointments.wa.templates.SlotTemplate;
import org.bahmni.whatsapp.appointments.wa.templates.TextTemplate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
public class WhatsAppAPIController {

    @Autowired
    OpenmrsLoginImpl openmrsLogin;

    ServiceTemplate serviceTemplate;

    ActionTemplate actionTemplate;

    SlotTemplate slotTemplate;

    TextTemplate textTemplate;

    String patientUuid = "";
    String patientId = "";

    String serviceUuid = "";

    String chosenService = "";

    public String fetchPatientName() throws IOException, ParseException {

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

        patientUuid = responseObject.getJSONArray("entry").getJSONObject(0).getJSONObject("resource").getString("id");

        String firstName = responseObject.getJSONArray("entry").getJSONObject(0).getJSONObject("resource").getJSONArray("name").getJSONObject(0).getJSONArray("given").getString(0);
        String familyName = responseObject.getJSONArray("entry").getJSONObject(0).getJSONObject("resource").getJSONArray("name").getJSONObject(0).getString("family");

        return firstName + " " + familyName;
    }

    public String saveAppointment(String serviceUuid, String patientResponse) throws IOException {
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

    public String sendMessage(String phoneNumberId, String token, JSONObject data) throws IOException, ParseException {
        String url = "https://graph.facebook.com/v17.0/" + phoneNumberId + "/messages?access_token=" + token;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(data.toString(), headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);

        String jsonResponse = responseEntity.getBody();

        JSONObject responseJson = new JSONObject(jsonResponse);

        String message_id = "";
        JSONArray messages = responseJson.getJSONArray("messages");
        if (messages.length() > 0) {
            JSONObject msg = messages.getJSONObject(0);
            message_id = msg.getString("id");
        }

        return message_id;
    }

    @RequestMapping(method = RequestMethod.GET, value = "webhook")
    public ResponseEntity<String> verifyWebhook(@RequestParam("hub.mode") String mode,
                                                @RequestParam("hub.challenge") String challenge,
                                                @RequestParam("hub.verify_token") String token) throws IOException, ParseException {

        if (mode.equals("subscribe") && token.equals("abc123")) {
            return new ResponseEntity<>(challenge, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Verification token or mode mismatch", HttpStatus.FORBIDDEN);
        }
    }

    @RequestMapping( method = RequestMethod.POST, value = "webhook")
    public ResponseEntity<String> notificationHandler(@RequestBody String requestBody) throws IOException, ParseException {

        JSONObject body = new JSONObject(requestBody);

        if (!body.has("object")) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        if (!body.has("entry") ||
                body.getJSONArray("entry").length() <= 0 ||
                !body.getJSONArray("entry").getJSONObject(0).has("changes") ||
                body.getJSONArray("entry").getJSONObject(0).getJSONArray("changes").length() <= 0 ||
                !body.getJSONArray("entry").getJSONObject(0).getJSONArray("changes").getJSONObject(0).has("value") ||
                !body.getJSONArray("entry").getJSONObject(0).getJSONArray("changes").getJSONObject(0).getJSONObject("value").has("messages") ||
                body.getJSONArray("entry").getJSONObject(0).getJSONArray("changes").getJSONObject(0).getJSONObject("value").getJSONArray("messages").length() <= 0
        ) {
            return new ResponseEntity<>(HttpStatus.OK);
        }

        JSONObject msg = body.getJSONArray("entry")
                .getJSONObject(0)
                .getJSONArray("changes")
                .getJSONObject(0)
                .getJSONObject("value")
                .getJSONArray("messages")
                .getJSONObject(0);

        String msgType = msg.getString("type");
        String from = msg.getString("from");

        String phone_number_id = "109855275525315";
        String token = "EAAJLW2eCmuQBO0RnZAHgjZCF666HscXxZBy7CwgmBmMTPgWTpwtrhDjaxwCLYqDLLnsYvEHRpZA7RqESdoOwje1PLOroZBdMD7BDZBg27gO6eKhRC2wpR7jw5C4KpxZCIixonGvCBhTZA3gk9ZBvnslDYTmyQxXqxaEpdGADJWQrZBvAYP242xMH7behlbiMARN23HEFpZAgSmzZB9sWPcJ05g5id0P5t1oZD";

        if (msgType.equals("text")) {
            JSONObject data;

            patientId = msg.getJSONObject("text").getString("body").toUpperCase();

            String fullName = fetchPatientName();

            if(fullName.equals("")){
                String reply_message = "Please re-enter your Identifier correctly!";
                data = textTemplate.createTextMessage(from, reply_message);
            }
            else {
                data = actionTemplate.createActionTemplate(from, fullName);
            }

            String wa_id = sendMessage(phone_number_id, token, data);
        }
        else if (msgType.equals("interactive")) {
            String patientResponse = msg.getJSONObject("interactive").getJSONObject("list_reply").getString("title");
            int responseId = Integer.parseInt(msg.getJSONObject("interactive").getJSONObject("list_reply").getString("id"));

            JSONObject data;

            if(patientResponse.equals("Book an Appointment")){
                data = serviceTemplate.createServiceTemplate(from);

                String wa_id = sendMessage(phone_number_id, token, data);
            }
            else if (patientResponse.equals(serviceTemplate.serviceNameMap.get(responseId))){
                serviceUuid = serviceTemplate.serviceMap.get(responseId);
                chosenService = serviceTemplate.serviceNameMap.get(responseId);

                data = slotTemplate.createSlotTemplate(from);

                String wa_id = sendMessage(phone_number_id, token, data);
            }
            else if (patientResponse.equals("Tomorrow Morning") || patientResponse.equals("Tomorrow Afternoon") || patientResponse.equals("Tomorrow Evening")){
                String reply_message = "";

                switch (patientResponse) {
                    case "Tomorrow Morning":
                        reply_message = "Your Appointment for " + chosenService + " is scheduled for " + patientResponse.toLowerCase() + " from 10am to 11am. Your presence at the designated slot is kindly requested.";
                        break;
                    case "Tomorrow Afternoon":
                        reply_message = "Your Appointment for " + chosenService + " is scheduled for " + patientResponse.toLowerCase() + " from 2pm to 3pm. Your presence at the designated slot is kindly requested.";
                        break;
                    case "Tomorrow Evening":
                        reply_message = "Your Appointment for " + chosenService + " is scheduled for " + patientResponse.toLowerCase() + " from 6pm to 7pm. Your presence at the designated slot is kindly requested.";
                        break;
                }

                data = textTemplate.createTextMessage(from, reply_message);

                String wa_id = sendMessage(phone_number_id, token, data);

                String appointmentResponse = saveAppointment(serviceUuid, patientResponse);
            }
            else {
                String reply_message = "Thanks for contacting Bahmni, This Feature will be live soon.";
                data = textTemplate.createTextMessage(from, reply_message);

                String wa_id = sendMessage(phone_number_id, token, data);
            }
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
