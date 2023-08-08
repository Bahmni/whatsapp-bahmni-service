package org.bahmni.whatsapp.appointments;

import org.bahmni.webclients.ClientCookies;
import org.bahmni.whatsapp.appointments.contract.patient.OpenMRSPatientFullRepresentation;
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

//    @RequestMapping(method = RequestMethod.GET, value = "test")
//    public String getMessage() {
//        return "hello world";
//    }
    @Autowired
OpenMRSService openMRSService;


    public String sendMessage(String phoneNumberId, String token, JSONObject data) throws IOException, ParseException {
        String url = "https://graph.facebook.com/v17.0/" + phoneNumberId + "/messages?access_token=" + token;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(data.toString(), headers);

//        openMRSService.getConnection();
//        ClientCookies cookies = openMRSService.getCookies();
//        System.out.println(cookies);

        OpenMRSPatientFullRepresentation patient_info = openMRSService.getPatientFR("https://demo-lite.mybahmni.in/openmrs/ws/atomfeed/patient/recent");
        System.out.println(patient_info);

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
                                                @RequestParam("hub.verify_token") String token) {
        System.out.println(mode);
        System.out.println(challenge);
        System.out.println(token);
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
            String from = msg.getString("from"); // Phone Number of Patient
            String reply_message = "Thanks for contacting Bahmni, Appointments Booking Feature will be live soon."; // Reply message to sent back to Patient

            JSONObject data = new JSONObject();
            data.put("messaging_product", "whatsapp");
            data.put("to", from);
            data.put("type", "text");

            JSONObject textBody = new JSONObject();
            textBody.put("body", reply_message);

            data.put("text", textBody);
            System.out.println("Data blob: " + data);

            //The data blob would look something like this (for reference):
            //                data = {
            //                        messaging_product: "whatsapp",
            //                        to: from,
            //                        type: "text",
            //                        text: { body: reply_message }
            //                };

            String phone_number_id = "109855275525315";
            String token = "EAAJLW2eCmuQBO11hvQZCGxyZATZBZBAj4EJCZCCFZC9WKCf9HwZCwS4eqeF0MBwfE07u4EoeMFpZC5NBVmFHB6NMjgh4w6XL3XACrUzwpn8rdr75MFbACE88WMnyGfPIhlInPU8V55aPFRLNR740XtykoaraUF2m2dsloUFDZBSHQ94oZCEulZCnZA33s79gSrZAUe68zVJGZAlHkyynozbNZCfzD7cmOIWGN0ZD";
            String wa_id = sendMessage(phone_number_id, token, data);

            System.out.println("whatsapp message id: " + wa_id);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
