package org.bahmni.whatsapp.appointments.wa.templates;

import org.json.JSONObject;

public class TextTemplate {
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
}
