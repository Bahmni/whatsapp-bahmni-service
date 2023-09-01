package org.bahmni.whatsapp.appointments.wa.templates;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bahmni.webclients.ClientCookies;
import org.bahmni.whatsapp.appointments.services.OpenmrsLoginImpl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.HashMap;

public class ServiceTemplate {

    @Autowired
    OpenmrsLoginImpl openmrsLogin = new OpenmrsLoginImpl();

    public HashMap<Integer,String> serviceMap= new HashMap<>();
    public HashMap<Integer,String> serviceNameMap= new HashMap<>();

    public JSONObject createServiceTemplate(String from) throws IOException {
        openmrsLogin.getConnection();
        ClientCookies cookies = openmrsLogin.getCookies();

        String URI = "https://demo-lite.mybahmni.in/openmrs/ws/rest/v1/appointmentService/all/full";

        HttpGet request = new HttpGet(URI);

        String cookieValue = cookies.get("JSESSIONID");
        request.addHeader("Cookie", "JSESSIONID=" + cookieValue);

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpResponse response = httpClient.execute(request);
        httpClient.close();

        String responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");

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
}
