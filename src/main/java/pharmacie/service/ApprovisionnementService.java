package pharmacie.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import pharmacie.dao.MedicamentRepository;
import pharmacie.entity.Categorie;
import pharmacie.entity.Fournisseur;
import pharmacie.entity.Medicament;

@Service
public class ApprovisionnementService {

    private final MedicamentRepository medicamentRepository;
    private final JavaMailSender mailSender;

    @Value("${mail.from:no-reply@pharmacie.com}")
    private String mailFrom;

    public ApprovisionnementService(MedicamentRepository medicamentRepository,
                                     @Nullable JavaMailSender mailSender) {
        this.medicamentRepository = medicamentRepository;
        this.mailSender = mailSender;
    }

    public List<Medicament> getMedicamentsAReapprovisionner() {
        return medicamentRepository.findAll().stream()
                .filter(m -> m.getUnitesEnStock() < m.getNiveauDeReappro())
                .collect(Collectors.toList());
    }

    public void traiterApprovisionnement() {
        System.out.println("DEBUG mailSender = " + mailSender);
        System.out.println("DEBUG mailFrom = " + mailFrom);

        List<Medicament> enRupture = getMedicamentsAReapprovisionner();

        System.out.println("DEBUG medicaments en rupture = " + enRupture.size());

        if (enRupture.isEmpty()) {
            System.out.println("Aucun medicament a reapprovisionner.");
            return;
        }

        Map<Fournisseur, Map<Categorie, List<Medicament>>> parFournisseur = new LinkedHashMap<>();

        for (Medicament m : enRupture) {
            if (m.getCategorie() == null) continue;
            Categorie cat = m.getCategorie();
            for (Fournisseur f : cat.getFournisseurs()) {
                parFournisseur
                    .computeIfAbsent(f, k -> new LinkedHashMap<>())
                    .computeIfAbsent(cat, k -> new ArrayList<>())
                    .add(m);
            }
        }

        parFournisseur.forEach(this::envoyerEmailFournisseur);
    }

    private void envoyerEmailFournisseur(Fournisseur fournisseur,
                                          Map<Categorie, List<Medicament>> parCategorie) {
        if (mailSender == null) {
            System.out.println("Mail non configure - email non envoye a : " + fournisseur.getNom());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(fournisseur.getEmail());
            helper.setSubject(" Demande de devis - Réapprovisionnement urgent");

            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'></head><body>");
            html.append("<div style='font-family: Arial, sans-serif; max-width: 700px; margin: auto; border: 1px solid #e0e0e0; border-radius: 10px; overflow: hidden;'>");

            // Header
            html.append("<div style='background: linear-gradient(135deg, #1a6b3a, #2ecc71); padding: 30px; text-align: center;'>");
            html.append("<h1 style='color: white; margin: 0; font-size: 28px;'> Pharmacie Centrale</h1>");
            html.append("<p style='color: #d4f5e2; margin: 5px 0 0 0; font-size: 14px;'>Système de gestion des approvisionnements</p>");
            html.append("</div>");

            // Body
            html.append("<div style='padding: 30px; background: #f9f9f9;'>");
            html.append("<p style='font-size: 16px; color: #333;'>Bonjour <strong>").append(fournisseur.getNom()).append("</strong>,</p>");
            html.append("<p style='color: #555; line-height: 1.6;'>Nous vous contactons car certains médicaments de notre pharmacie atteignent un niveau de stock critique. Nous vous prions de bien vouloir nous transmettre un devis pour les produits suivants :</p>");

            // Alerte
            html.append("<div style='background: #fff3cd; border-left: 5px solid #ffc107; padding: 15px; margin: 20px 0; border-radius: 5px;'>");
            html.append("<p style='margin: 0; color: #856404;'>⚠️ <strong>Réapprovisionnement urgent requis</strong></p>");
            html.append("</div>");

            // Tableau par catégorie
            for (Map.Entry<Categorie, List<Medicament>> entry : parCategorie.entrySet()) {
                html.append("<h3 style='color: #1a6b3a; border-bottom: 2px solid #2ecc71; padding-bottom: 8px;'> ").append(entry.getKey().getLibelle()).append("</h3>");
                html.append("<table style='width: 100%; border-collapse: collapse; margin-bottom: 20px; background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 5px rgba(0,0,0,0.1);'>");
                html.append("<thead>");
                html.append("<tr style='background: #1a6b3a; color: white;'>");
                html.append("<th style='padding: 12px 15px; text-align: left;'> Médicament</th>");
                html.append("<th style='padding: 12px 15px; text-align: center;'>Stock actuel</th>");
                html.append("<th style='padding: 12px 15px; text-align: center;'>Seuil minimum</th>");
                html.append("<th style='padding: 12px 15px; text-align: center;'>Quantité à commander</th>");
                html.append("</tr>");
                html.append("</thead><tbody>");

                for (Medicament m : entry.getValue()) {
                    int aCommander = m.getNiveauDeReappro() * 2 - m.getUnitesEnStock();
                    html.append("<tr style='border-bottom: 1px solid #e0e0e0;'>");
                    html.append("<td style='padding: 12px 15px; color: #333;'>").append(m.getNom()).append("</td>");
                    html.append("<td style='padding: 12px 15px; text-align: center; color: #e74c3c; font-weight: bold;'>").append(m.getUnitesEnStock()).append("</td>");
                    html.append("<td style='padding: 12px 15px; text-align: center; color: #f39c12;'>").append(m.getNiveauDeReappro()).append("</td>");
                    html.append("<td style='padding: 12px 15px; text-align: center; color: #1a6b3a; font-weight: bold;'>").append(aCommander).append("</td>");
                    html.append("</tr>");
                }
                html.append("</tbody></table>");
            }

            // Footer message
            html.append("<p style='color: #555; line-height: 1.6;'>Merci de nous transmettre votre devis dans les plus brefs délais.</p>");
            html.append("<p style='color: #555;'>Cordialement,<br><strong style='color: #1a6b3a;'>L'équipe de la Pharmacie Centrale</strong></p>");
            html.append("</div>");

            // Footer
            html.append("<div style='background: #1a6b3a; padding: 15px; text-align: center;'>");
            html.append("<p style='color: #d4f5e2; margin: 0; font-size: 12px;'>© 2026 Pharmacie Centrale - Tous droits réservés</p>");
            html.append("</div>");

            html.append("</div></body></html>");

            helper.setText(html.toString(), true);
            mailSender.send(message);
            System.out.println("Mail envoye a : " + fournisseur.getNom() + " <" + fournisseur.getEmail() + ">");

        } catch (Exception e) {
            System.err.println("Erreur d'envoi a " + fournisseur.getNom() + " : " + e.getMessage());
        }
    }
}