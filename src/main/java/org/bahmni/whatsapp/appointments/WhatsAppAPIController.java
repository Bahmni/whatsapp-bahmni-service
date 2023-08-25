package org.bahmni.whatsapp.appointments;

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

        openmrsLogin.getConnection();
        ClientCookies cookies = openmrsLogin.getCookies();

//        OpenMRSPatientFullRepresentation patient_info = openMRSService.getPatientFR("https://demo-lite.mybahmni.in/openmrs/ws/atomfeed/patient/recent");
//        System.out.println(patient_info);

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
                                                @RequestParam("hub.verify_token") String token) {

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

        if (msg.getString("type").equals("text")) {
            String from = msg.getString("from");
            String reply_message = "Thanks for contacting Bahmni, Appointments Booking Feature will be live soon.";

            JSONObject data = new JSONObject();
            data.put("messaging_product", "whatsapp");
            data.put("to", from);
            data.put("type", "text");

            JSONObject textBody = new JSONObject();
            textBody.put("body", reply_message);

            data.put("text", textBody);

            String phone_number_id = "109855275525315";
            String token = "EAAJLW2eCmuQBOxEgy8ZAwGUgd4UZBcoPhZBkSKJGhtbZAvdpW6GLL3tE90nFd9VO4WBySlH9o3pH7ZAmNiWIEdHlc1l0UAqBpsQAibtKU4ZAELF75rSUttKCwuBJuMNEcynFeArbVZCXMesFsON0NoVoIMdJzq6bMhIGnzgR3bdeuWUVGGqZAS3UXyjOYvAz58uuyHvoj4DMBVZBpWogItuSp1qKo0jMZD";
            String wa_id = sendMessage(phone_number_id, token, data);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
