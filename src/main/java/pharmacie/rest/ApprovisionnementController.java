package pharmacie.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import pharmacie.service.ApprovisionnementService;

@RestController
@RequestMapping("/api/approvisionnement")
public class ApprovisionnementController {

    private final ApprovisionnementService service;

    public ApprovisionnementController(ApprovisionnementService service) {
        this.service = service;
    }

    @GetMapping("/lancer")
    public String lancer() {
        service.traiterApprovisionnement();
        return "Processus d'approvisionnement lance ! Verifie ta console et ta boite mail.";
    }
}