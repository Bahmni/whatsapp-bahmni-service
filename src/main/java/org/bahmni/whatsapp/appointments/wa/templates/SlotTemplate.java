package org.bahmni.whatsapp.appointments.wa.templates;

import org.json.JSONArray;
import org.json.JSONObject;

public class SlotTemplate {
    public JSONObject createSlotTemplate(String from){
        String slot_template_body = "Please choose a suitable slot for your appointment.";

        JSONObject data = new JSONObject();

        data.put("messaging_product", "whatsapp");
        data.put("to", from);
        data.put("type", "interactive");

        JSONObject interactiveObj = new JSONObject();
        interactiveObj.put("type", "list");

        JSONObject bodyObj = new JSONObject();
        bodyObj.put("text", slot_template_body);

        interactiveObj.put("body", bodyObj);

        JSONObject footerObj = new JSONObject();
        footerObj.put("text", "Bahmni");

        interactiveObj.put("footer", footerObj);

        JSONObject actionObj = new JSONObject();
        actionObj.put("button", "Choose Slot");

        JSONArray sectionsArray = new JSONArray();

        JSONObject sectionObj = new JSONObject();
        sectionObj.put("title", "Please choose a Slot.");

        JSONArray rowArray = new JSONArray();

        JSONObject rowObj1 = new JSONObject();
        rowObj1.put("id", "0");
        rowObj1.put("title", "Tomorrow Morning");

        rowArray.put(rowObj1);

        JSONObject rowObj2 = new JSONObject();
        rowObj2.put("id", "1");
        rowObj2.put("title", "Tomorrow Afternoon");

        rowArray.put(rowObj2);

        JSONObject rowObj3 = new JSONObject();
        rowObj3.put("id", "2");
        rowObj3.put("title", "Tomorrow Evening");

        rowArray.put(rowObj3);

        sectionObj.put("rows", rowArray);

        sectionsArray.put(sectionObj);

        actionObj.put("sections", sectionsArray);

        interactiveObj.put("action", actionObj);

        data.put("interactive", interactiveObj);

        return data;
    }
}
