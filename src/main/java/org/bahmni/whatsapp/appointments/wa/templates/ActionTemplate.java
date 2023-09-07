package org.bahmni.whatsapp.appointments.wa.templates;

import org.json.JSONArray;
import org.json.JSONObject;

public class ActionTemplate {
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
}
