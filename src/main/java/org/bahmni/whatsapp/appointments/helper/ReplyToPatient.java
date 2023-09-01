package org.bahmni.whatsapp.appointments.helper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class ReplyToPatient {
    public void sendMessage(String phoneNumberId, String token, JSONObject data) {
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

    }
}
