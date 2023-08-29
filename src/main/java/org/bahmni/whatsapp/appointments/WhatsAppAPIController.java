package org.bahmni.whatsapp.appointments;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bahmni.webclients.ClientCookies;
import org.bahmni.whatsapp.appointments.services.FhirResourceService;
import org.bahmni.whatsapp.appointments.services.OpenmrsLoginImpl;
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
import java.util.*;

@RestController
public class WhatsAppAPIController {

    @Autowired
    OpenmrsLoginImpl openmrsLogin;

    @Autowired
    FhirResourceService fhirResourceService;

    HashMap<Integer,String> serviceMap= new HashMap<>();
    HashMap<Integer,String> serviceNameMap= new HashMap<>();

    String patientUuid = "";
    String patientId = "";

    public String fetchPatientName() throws IOException, ParseException {
//        String fhirBody = fhirResourceService.getResourceById("Patient", "5e91bcf8-aeec-4c7b-ab60-8c2aeb05cbee");
//        System.out.println("fhir body: " + fhirBody);
//
//        JSONObject resourceObject = new JSONObject(fhirBody);
//        System.out.println("resource body: " + resourceObject);

//        String firstName = resourceObject.getJSONArray("name").getJSONObject(0).getJSONArray("given").getString(0);
//        String familyName = resourceObject.getJSONArray("name").getJSONObject(0).getString("family");

        openmrsLogin.getConnection();
        ClientCookies cookies = openmrsLogin.getCookies();
        System.out.println("Cookie: " + cookies);

        String URI = "https://demo-lite.mybahmni.in/openmrs/ws/fhir2/R4/Patient?identifier=" + patientId;

        HttpGet request = new HttpGet(URI);

        String cookieValue = cookies.get("JSESSIONID");
        request.addHeader("Cookie", "JSESSIONID=" + cookieValue);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(request);

        String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
        System.out.println("response body: " + responseBody);

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

    public JSONObject createActionTemplate(String from, String fullName){
        String action_template_body = "Hello " + fullName + ",\n" +
                "\n" +
                "We appreciate your communication with Bahmni. Kindly choose an option from the provided list below to indicate your intended action.\n" +
                "\n" +
                "Thank you.";

        JSONObject data = new JSONObject();

        data.put("messaging_product", "whatsapp");
        data.put("to", from);
        data.put("type", "interactive");

        JSONObject interactiveObj = new JSONObject();
        interactiveObj.put("type", "list");

        JSONObject bodyObj = new JSONObject();
        bodyObj.put("text", action_template_body);

        interactiveObj.put("body", bodyObj);

        JSONObject footerObj = new JSONObject();
        footerObj.put("text", "Bahmni");

        interactiveObj.put("footer", footerObj);

        JSONObject actionObj = new JSONObject();
        actionObj.put("button", "Choose Action");

        JSONArray sectionsArray = new JSONArray();

        JSONObject sectionObj = new JSONObject();
        sectionObj.put("title", "Please choose an action.");

        JSONArray rowArray = new JSONArray();

        JSONObject rowObj1 = new JSONObject();
        rowObj1.put("id", "0");
        rowObj1.put("title", "Book an Appointment");

        rowArray.put(rowObj1);

        JSONObject rowObj2 = new JSONObject();
        rowObj2.put("id", "1");
        rowObj2.put("title", "Reschedule Appointment");

        rowArray.put(rowObj2);

        sectionObj.put("rows", rowArray);

        sectionsArray.put(sectionObj);

        actionObj.put("sections", sectionsArray);

        interactiveObj.put("action", actionObj);

        data.put("interactive", interactiveObj);

        return data;
    }

    public JSONObject createServiceTemplate(String from) throws IOException {
        openmrsLogin.getConnection();
        ClientCookies cookies = openmrsLogin.getCookies();
        System.out.println("Cookie: " + cookies);

        String URI = "https://demo-lite.mybahmni.in/openmrs/ws/rest/v1/appointmentService/all/full";

        HttpGet request = new HttpGet(URI);

        String cookieValue = cookies.get("JSESSIONID");
        request.addHeader("Cookie", "JSESSIONID=" + cookieValue);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(request);
        httpClient.close();

        String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
        System.out.println("response body: " + responseBody);

        JSONArray serviceArray = new JSONArray(responseBody);

        String service_template_body = "Please choose a service from the below list";

        JSONObject data = new JSONObject();

        data.put("messaging_product", "whatsapp");
        data.put("to", from);
        data.put("type", "interactive");

        JSONObject interactiveObj = new JSONObject();
        interactiveObj.put("type", "list");

        JSONObject bodyObj = new JSONObject();
        bodyObj.put("text", service_template_body);

        interactiveObj.put("body", bodyObj);

        JSONObject footerObj = new JSONObject();
        footerObj.put("text", "Bahmni");

        interactiveObj.put("footer", footerObj);

        JSONObject actionObj = new JSONObject();
        actionObj.put("button", "Choose Service");

        JSONArray sectionsArray = new JSONArray();

        JSONObject sectionObj = new JSONObject();
        sectionObj.put("title", "Please choose a Service.");

        JSONArray rowArray = new JSONArray();

        for (int i = 0; i < serviceArray.length(); i++) {
            JSONObject service = serviceArray.getJSONObject(i);

            String serviceName = service.getString("name");
            int serviceID = service.getInt("appointmentServiceId") - 1;

            serviceMap.put(serviceID, service.getString("uuid"));

//            serviceName = serviceName.substring(0, Math.min(serviceName.length(), 24));
            serviceName = ( serviceName.length () > 24 ) ? serviceName.substring ( 0 , 21 ).concat ( "…" ) : serviceName;

            serviceNameMap.put(serviceID, serviceName);

            JSONObject rowObj = new JSONObject();
            rowObj.put("id", Integer.toString(serviceID));
            rowObj.put("title", serviceName);

            rowArray.put(rowObj);
        }

        sectionObj.put("rows", rowArray);

        sectionsArray.put(sectionObj);

        actionObj.put("sections", sectionsArray);

        interactiveObj.put("action", actionObj);

        data.put("interactive", interactiveObj);

        return data;
    }

    public JSONObject createTextMessage(String from, String reply_message){

        JSONObject data = new JSONObject();

        data.put("messaging_product", "whatsapp");
        data.put("to", from);
        data.put("type", "text");

        JSONObject textBody = new JSONObject();
        textBody.put("body", reply_message);

        data.put("text", textBody);

        return data;
    }

    public String saveAppointment(String serviceUuid) throws IOException {
        openmrsLogin.getConnection();
        ClientCookies cookies = openmrsLogin.getCookies();
        System.out.println("Cookie: " + cookies);

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
        appointmentDetails.put("startDateTime", "2023-08-29T04:30:00.000Z");
        appointmentDetails.put("endDateTime", "2023-08-29T05:30:00.000Z");

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

        //Parsing the json response
        JSONObject responseJson = new JSONObject(jsonResponse);
        System.out.println("response json: " + responseJson);

        String message_id = "";
        // Extract the message ID (messages is an array)
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

        openmrsLogin.getConnection();
        ClientCookies cookies = openmrsLogin.getCookies();
        System.out.println("Cookie: " + cookies);

        if (mode.equals("subscribe") && token.equals("abc123")) {
            System.out.println("Webhook Verified");
            return new ResponseEntity<>(challenge, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Verification token or mode mismatch", HttpStatus.FORBIDDEN);
        }
    }

    //Handling of Notifications about patient's messages or message sent status changes from Cloud API
    @RequestMapping( method = RequestMethod.POST, value = "webhook")
    public ResponseEntity<String> notificationHandler(@RequestBody String requestBody) throws IOException, ParseException {

        JSONObject body = new JSONObject(requestBody);
        System.out.println("Json body: " + body);

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
            //This is the case when notification payload has details about message status and not the message itself.
            //i.e. if a message is sent, delivered or read by the patient or if there is an error in sending the message.
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
        String from = msg.getString("from"); // Phone Number of Patient

        String phone_number_id = "109855275525315";
        String token = "EAAJLW2eCmuQBOZBEVNsZAKGr1cgBxC9GgQwtTeXxxOZBOAbMxQUHIqt50YiU0xZBhK7TX0D0IIKCZCM23pFwgEI89R4cs0ZA7ZBlLThI7MwIhkjE7ZCcwZATcxviQDa69tuCjsyFaAmJayxnTHOR0mToWcheGPfCL8k4ZBVj4PTZAZBWOgeQZB9P2ojM4GsE9ZBldWCHI0BR2TGuPKT7OBlsg83FEHwck4rFMZD";

        if (msgType.equals("text")) {
            JSONObject data;
            System.out.println("Text message: " + msg);

            patientId = msg.getJSONObject("text").getString("body").toUpperCase();

            String fullName = fetchPatientName();

            if(fullName.equals("")){
                String reply_message = "Please re-enter your Identifier correctly!";
                data = createTextMessage(from, reply_message);

                String wa_id = sendMessage(phone_number_id, token, data);

                System.out.println("whatsapp message id: " + wa_id);
            }
            else {
                System.out.println("Patient Name: " + fullName);

                data = createActionTemplate(from, fullName);

                System.out.println("Data blob: " + data);
                String wa_id = sendMessage(phone_number_id, token, data);

                System.out.println("whatsapp message id: " + wa_id);
            }
        }
        else if (msgType.equals("interactive")) {
            System.out.println("Interactive message: " + msg);
            String patientResponse = msg.getJSONObject("interactive").getJSONObject("list_reply").getString("title");
            int responseId = Integer.parseInt(msg.getJSONObject("interactive").getJSONObject("list_reply").getString("id"));

            JSONObject data;

            if(patientResponse.equals("Book an Appointment")){
                data = createServiceTemplate(from);

                String wa_id = sendMessage(phone_number_id, token, data);

                System.out.println("whatsapp message id: " + wa_id);
            }
            else if (patientResponse.equals(serviceNameMap.get(responseId))){
                String serviceUuid = serviceMap.get(responseId);

                String reply_message = "Your Appointment for " + patientResponse + " is scheduled for tomorrow from 10am to 11am. Your presence at the designated time slot is kindly requested.";
                data = createTextMessage(from, reply_message);

                String wa_id = sendMessage(phone_number_id, token, data);

                String appointmentResponse = saveAppointment(serviceUuid);

                System.out.println("Saved Appointment response: " + appointmentResponse);

                System.out.println("whatsapp message id: " + wa_id);
            }
            else {
                String reply_message = "Thanks for contacting Bahmni, This Feature will be live soon.";
                data = createTextMessage(from, reply_message);

                String wa_id = sendMessage(phone_number_id, token, data);

                System.out.println("whatsapp message id: " + wa_id);
            }
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
