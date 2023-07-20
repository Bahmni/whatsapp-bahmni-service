package org.bahmni.whatsapp.appointments;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping()
public class WhatsAppAPIController {
    @GetMapping("/test")
    public String getMessage() {
        return "hello world";
    }
    @GetMapping("/webhook")
    public ResponseEntity<String> webhookVerify(@RequestParam("hub.mode") String mode,
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
    @PostMapping("/webhook")
    public void getRequest(@RequestBody String json){
        System.out.println(json);
    }
}
