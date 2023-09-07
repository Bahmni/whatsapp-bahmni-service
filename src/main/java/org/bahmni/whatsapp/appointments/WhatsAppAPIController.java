package org.bahmni.whatsapp.appointments;

import org.bahmni.whatsapp.appointments.helper.PatientName;
import org.bahmni.whatsapp.appointments.helper.PatientUuid;
import org.bahmni.whatsapp.appointments.helper.ReplyToPatient;
import org.bahmni.whatsapp.appointments.helper.SaveChosenSlot;
import org.bahmni.whatsapp.appointments.wa.templates.ActionTemplate;
import org.bahmni.whatsapp.appointments.wa.templates.ServiceTemplate;
import org.bahmni.whatsapp.appointments.wa.templates.SlotTemplate;
import org.bahmni.whatsapp.appointments.wa.templates.TextTemplate;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;

@RestController
public class WhatsAppAPIController {

    ServiceTemplate serviceTemplate = new ServiceTemplate();

    ActionTemplate actionTemplate = new ActionTemplate();

    SlotTemplate slotTemplate = new SlotTemplate();

    TextTemplate textTemplate = new TextTemplate();

    PatientName patientName = new PatientName();

    PatientUuid patientUuid = new PatientUuid();

    SaveChosenSlot saveChosenSlot = new SaveChosenSlot();

    ReplyToPatient replyToPatient = new ReplyToPatient();

    String patientUUID = "";
    String serviceUuid = "";
    String chosenService = "";

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

        String msgType = msg.getString("type");
        String from = msg.getString("from");

        String phone_number_id = "109855275525315";
        String token = "EAAJLW2eCmuQBO6IHch4KbJu1qPc1dPUZBjGUkaA87mXu7PIfwITU14xoMOPIfvNtOyvvRFAS9ABN8uV4iSjT24U432vzjiZAU8IIKyX978e6swrdMGjrbv9fZBB0zwwlmf4xia73avL5iEyhhrcZBEHOPndAcEhrM5xRACr1dHQOp4vLZBlhtAit33naZAoTaZAIm4aYlZAdjblRoIzAQObCZBRX8vR0ZD";

        if (msgType.equals("text")) {
            JSONObject data;

            String patientId = msg.getJSONObject("text").getString("body").toUpperCase();

            String fullName = patientName.fetchPatientName(patientId);

            if(fullName.equals("")){
                String reply_message = "Please re-enter your Identifier correctly!";
                data = textTemplate.createTextMessage(from, reply_message);
            }
            else {
                patientUUID = patientUuid.fetchPatientUuid(patientId);
                data = actionTemplate.createActionTemplate(from, fullName);
            }

            replyToPatient.sendMessage(phone_number_id, token, data);
        }
        else if (msgType.equals("interactive")) {
            String patientResponse = msg.getJSONObject("interactive").getJSONObject("list_reply").getString("title");
            int responseId = Integer.parseInt(msg.getJSONObject("interactive").getJSONObject("list_reply").getString("id"));

            JSONObject data;

            if(patientResponse.equals("Book an Appointment")){
                data = serviceTemplate.createServiceTemplate(from);

                replyToPatient.sendMessage(phone_number_id, token, data);
            }
            else if (patientResponse.equals(serviceTemplate.serviceNameMap.get(responseId))){
                serviceUuid = serviceTemplate.serviceMap.get(responseId);
                chosenService = serviceTemplate.serviceNameMap.get(responseId);

                data = slotTemplate.createSlotTemplate(from);

                replyToPatient.sendMessage(phone_number_id, token, data);
            }
            else if (patientResponse.equals("Tomorrow Morning") || patientResponse.equals("Tomorrow Afternoon") || patientResponse.equals("Tomorrow Evening")){
                String reply_message = "";

                switch (patientResponse) {
                    case "Tomorrow Morning":
                        reply_message = "Your Appointment for " + chosenService + " is scheduled for " + patientResponse.toLowerCase() + " from 10 am to 11 am. Your presence at the designated slot is kindly requested.";
                        break;
                    case "Tomorrow Afternoon":
                        reply_message = "Your Appointment for " + chosenService + " is scheduled for " + patientResponse.toLowerCase() + " from 2 pm to 3 pm. Your presence at the designated slot is kindly requested.";
                        break;
                    case "Tomorrow Evening":
                        reply_message = "Your Appointment for " + chosenService + " is scheduled for " + patientResponse.toLowerCase() + " from 6 pm to 7 pm. Your presence at the designated slot is kindly requested.";
                        break;
                }

                data = textTemplate.createTextMessage(from, reply_message);

                replyToPatient.sendMessage(phone_number_id, token, data);

                saveChosenSlot.saveAppointment(serviceUuid, patientUUID, patientResponse);
            }
            else {
                String reply_message = "Thanks for contacting Bahmni, This Feature will be live soon.";
                data = textTemplate.createTextMessage(from, reply_message);

                replyToPatient.sendMessage(phone_number_id, token, data);
            }
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }
}