package org.bahmni.whatsapp.appointments.worker;

import org.bahmni.whatsapp.appointments.contract.patient.OpenMRSPatientFullRepresentation;
import org.bahmni.whatsapp.appointments.services.OpenMRSService;
import org.ict4h.atomfeed.client.domain.Event;
import org.ict4h.atomfeed.client.service.EventWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatientFeedWorker implements EventWorker {
    private static final Logger logger = LoggerFactory.getLogger(PatientFeedWorker.class);

    @Autowired
    private OpenMRSService openMRSService;

    public PatientFeedWorker() {
    }

    @Override
    public void process(Event event) {
        try {
            logger.info("Getting patient details ...");
            String patientUri = event.getContent();
            OpenMRSPatientFullRepresentation patientFR = openMRSService.getPatientFR(patientUri);
        } catch (Exception e) {
            logger.error("Failed to fetch patient details", e);
            throw new RuntimeException("Failed to fetch patient details", e);
        }
    }

    @Override
    public void cleanUp(Event event) {
    }
}
