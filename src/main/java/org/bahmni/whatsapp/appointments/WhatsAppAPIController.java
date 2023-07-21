package org.bahmni.whatsapp.appointments;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
public class WhatsAppAPIController {

//    @RequestMapping(method = RequestMethod.GET, value = "test")
//    public String getMessage() {
//        return "hello world";
//    }

    public String sendMessage(String phoneNumberId, String token, JSONObject data) {
        String url = "https://graph.facebook.com/v15.0/" + phoneNumberId + "/messages?access_token=" + token;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(data.toString(), headers);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);

        String jsonResponse = responseEntity.getBody();

        //Parsing the json response
        JSONObject responseJson = new JSONObject(jsonResponse);

        String message_id = "";
        // Extract the message ID (messages is an array)
        JSONArray messages = responseJson.getJSONObject("data").getJSONArray("messages");
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
        if (mode.equals("subscribe") && token.equals("Hello")) {
            return new ResponseEntity<>(challenge, HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Verification token or mode mismatch", HttpStatus.FORBIDDEN);
        }
    }

    //Handling of Notifications about patient's messages or message sent status changes from Cloud API
    @RequestMapping( method = RequestMethod.POST, value = "webhook")
    public ResponseEntity<String> notificationHandler(@RequestBody String requestBody){
        System.out.println(requestBody);

        // Parse the JSON request body
        JSONObject event = new JSONObject(requestBody);
        JSONObject body = event.getJSONObject("body");

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
            String reply_message = "Hello from Bahmni"; // Reply message to sent back to Patient

            JSONObject data = new JSONObject();
            data.put("messaging_product", "whatsapp");
            data.put("to", from);
            data.put("type", "text");

            JSONObject textBody = new JSONObject();
            textBody.put("body", reply_message);

            data.put("text", textBody);

            //The data blob would look something like this (for reference):
            //                data = {
            //                        messaging_product: "whatsapp",
            //                        to: from,
            //                        type: "text",
            //                        text: { body: reply_message }
            //                };

            String phone_number_id = "YOUR_PHONE_NUMBER_ID";
            String token = "YOUR_ACCESS_TOKEN";
            String wa_id = sendMessage(phone_number_id, token, data);

            System.out.println("whatsapp message id: " + wa_id);
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
