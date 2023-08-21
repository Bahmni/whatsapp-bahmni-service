package org.bahmni.whatsapp.appointments;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.bahmni.webclients.ClientCookies;
import org.bahmni.whatsapp.appointments.contract.patient.OpenMRSPatientFullRepresentation;
import org.bahmni.whatsapp.appointments.services.OpenmrsLoginImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.bahmni.whatsapp.appointments.services.OpenMRSService;

import java.io.IOException;
import java.text.ParseException;

@RestController
public class WhatsAppAPIController {

    @Autowired
    OpenMRSService openMRSService;

    @Autowired
    OpenmrsLoginImpl openmrsLogin;


    public String sendMessage(String phoneNumberId, String token, JSONObject data) throws IOException, ParseException {
        String url = "https://graph.facebook.com/v17.0/" + phoneNumberId + "/messages?access_token=" + token;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(data.toString(), headers);

//        openmrsLogin.getConnection();
//        ClientCookies cookies = openmrsLogin.getCookies();
//        System.out.println(cookies);

//        OpenMRSPatientFullRepresentation patient_info = openMRSService.getPatientFR("https://demo-lite.mybahmni.in/openmrs/ws/atomfeed/patient/recent");
//        System.out.println(patient_info);

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

    //Webhook Verification and Configuration
    @RequestMapping(method = RequestMethod.GET, value = "webhook")
    public ResponseEntity<String> verifyWebhook(@RequestParam("hub.mode") String mode,
                                                @RequestParam("hub.challenge") String challenge,
                                                @RequestParam("hub.verify_token") String token) throws IOException, ParseException {
        System.out.println(mode);
        System.out.println(challenge);
        System.out.println(token);

//        OpenMRSPatientFullRepresentation patient_info = openMRSService.getPatientFR("https://demo-lite.mybahmni.in/openmrs/ws/atomfeed/patient/recent");
//        System.out.println(patient_info);

//        openmrsLogin.getConnection();
//        ClientCookies cookies = openmrsLogin.getCookies();
//        System.out.println("Cookie: " + cookies);

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
        System.out.println("requestBody: " + requestBody);

        // Parse the JSON request body
        JSONObject body = new JSONObject(requestBody);
        System.out.println("Json body: " + body);

        if (!body.has("object")) {
            // Return a '404 Not Found' if event is not from a WhatsApp API
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        //For reference of how a Notification payload object looks like: https://developers.facebook.com/docs/whatsapp/cloud-api/webhooks/components#webhooks-notification-payload-reference

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

        //check if a message sent by the patient is of type text or not.
        if (msg.getString("type").equals("text")) {
            System.out.println("message: " + msg);
            String from = msg.getString("from"); // Phone Number of Patient
            String reply_message = "Thanks for contacting Bahmni, Appointments Booking Feature will be live soon."; // Reply message to sent back to Patient

            openmrsLogin.getConnection();
            ClientCookies cookies = openmrsLogin.getCookies();
            System.out.println("Cookie: " + cookies);

            String patientUUID = "ABC200012";
            String loginLocationUuid= "833d0c66-e29a-4d31-ac13-ca9050d1bfa9";

            String URI = "https://demo-lite.mybahmni.in/openmrs/ws/rest/v1/bahmni/search/patient/lucene?identifier=" + patientUUID + "&loginLocationUuid=" + loginLocationUuid;

            HttpGet request = new HttpGet(URI);
            request.addHeader("Cookie", String.valueOf(cookies));

//            JSONObject params = new JSONObject();
//            params.put("Identifier", "ABC200012");
//            params.put("loginLocationUuid", "833d0c66-e29a-4d31-ac13-ca9050d1bfa9");
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpResponse response = httpClient.execute(request);
            httpClient.close();

            System.out.println("response: " + response);

            String first_template_body = "Hello Aniket,\n" +
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
            bodyObj.put("text", first_template_body);

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
            rowObj2.put("title", "Upcoming Appointments");

            rowArray.put(rowObj2);

            JSONObject rowObj3 = new JSONObject();
            rowObj3.put("id", "2");
            rowObj3.put("title", "Cancel an Appointment");

            rowArray.put(rowObj3);

            JSONObject rowObj4 = new JSONObject();
            rowObj4.put("id", "3");
            rowObj4.put("title", "Reschedule Appointment");

            rowArray.put(rowObj4);

            sectionObj.put("rows", rowArray);

            sectionsArray.put(sectionObj);

            actionObj.put("sections", sectionsArray);

            interactiveObj.put("action", actionObj);

            data.put("interactive", interactiveObj);

            //Json Object for sending a normal text message to patient
//            JSONObject textBody = new JSONObject();
//            textBody.put("body", reply_message);
//
//            data.put("text", textBody);
            System.out.println("Data blob: " + data);
            String phone_number_id = "109855275525315";
            String token = "EAAJLW2eCmuQBO4GlCmr0FVnqiav90W3NgV9SVdiUBeOkLVN9KXmVyU2QEfOkYMOUZCa5X7N49HPtQ8xfBAIsTZAw0NQOypFevJjFEwKm3EsLM8rDsG4gZAQMFRl9fAMzOG7swj38TZBZB5bzjGYVnw5vIpHEhFuJEdKv0Sm4WPSr5twAF9nws00puiFDGjeqRmYj6n8YZAjHxKMnMpNRpSRZA6unyUZD";
            String wa_id = sendMessage(phone_number_id, token, data);

            System.out.println("whatsapp message id: " + wa_id);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
