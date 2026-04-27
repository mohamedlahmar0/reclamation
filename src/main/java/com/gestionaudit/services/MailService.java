package com.gestionaudit.services;



import com.gestionaudit.models.Reclamation;



import javax.mail.*;

import javax.mail.internet.InternetAddress;

import javax.mail.internet.MimeMessage;

import java.time.format.DateTimeFormatter;

import java.util.Locale;

import java.util.Properties;



public class MailService {



    private final String username = "medlhr0@gmail.com";

    private final String password = "yqgshomsqteqelai";



    /** Destinataire des notifications de changement de statut (réclamations). */

    public static final String STATUS_NOTIFICATION_EMAIL = "medlhr81@gmail.com";



    private static String escapeHtml(String s) {

        if (s == null || s.isBlank()) {

            return "";

        }

        return s.replace("&", "&amp;")

                .replace("<", "&lt;")

                .replace(">", "&gt;")

                .replace("\"", "&quot;");

    }



    private static String statutLibelle(String code) {

        if (code == null) {

            return "—";

        }

        return switch (code.toLowerCase(Locale.ROOT)) {

            case Reclamation.STATUT_EN_ATTENTE -> "En attente";

            case Reclamation.STATUT_EN_COURS -> "En cours";

            case Reclamation.STATUT_RESOLUE -> "Résolue";

            case Reclamation.STATUT_CLOTUREE -> "Clôturée";

            default -> code;

        };

    }



    public void sendEmail(String to, String subject, String body) {

        Properties prop = new Properties();

        prop.put("mail.smtp.host", "smtp.gmail.com");

        prop.put("mail.smtp.port", "587");

        prop.put("mail.smtp.auth", "true");

        prop.put("mail.smtp.starttls.enable", "true");



        Session session = Session.getInstance(prop, new javax.mail.Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {

                return new PasswordAuthentication(username, password);

            }

        });



        try {

            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(username));

            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));

            message.setSubject(subject);

            message.setText(body);



            Transport.send(message);

            System.out.println("Email sent successfully");

        } catch (MessagingException e) {

            e.printStackTrace();

        }

    }



    /**

     * Envoie un e-mail HTML (UTF-8).

     */

    public void sendHtmlEmail(String to, String subject, String htmlBody) {

        Properties prop = new Properties();

        prop.put("mail.smtp.host", "smtp.gmail.com");

        prop.put("mail.smtp.port", "587");

        prop.put("mail.smtp.auth", "true");

        prop.put("mail.smtp.starttls.enable", "true");



        Session session = Session.getInstance(prop, new javax.mail.Authenticator() {

            protected PasswordAuthentication getPasswordAuthentication() {

                return new PasswordAuthentication(username, password);

            }

        });



        try {

            Message message = new MimeMessage(session);

            message.setFrom(new InternetAddress(username));

            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));

            message.setSubject(subject);

            message.setContent(htmlBody, "text/html; charset=UTF-8");



            Transport.send(message);

            System.out.println("HTML email sent successfully to " + to);

        } catch (MessagingException e) {

            e.printStackTrace();

        }

    }



    /**

     * Notification personnalisée lorsque l’admin change le statut d’une réclamation (envoyée à {@link #STATUS_NOTIFICATION_EMAIL}).

     *

     * @param r                 réclamation après mise à jour (nouveau statut déjà défini)

     * @param ancienStatut      code statut avant changement

     * @param dernierMessage    message associé à l’envoi (réponse admin dans le chat), peut être vide

     */

    public void sendReclamationStatutChangedNotification(Reclamation r, String ancienStatut, String dernierMessage) {

        String nouveauStatut = r.getStatut();

        String subject = String.format("[Gestion Audit] Mise à jour — %s", r.getTitre());



        String dateStr = r.getDateCreation() != null

                ? r.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRENCH))

                : "—";



        String msgBlock = "";

        if (dernierMessage != null && !dernierMessage.isBlank()) {

            msgBlock = "<div style=\"margin-top:20px;padding:14px 16px;background:#f8fafc;border-radius:10px;border-left:4px solid #6366f1;\">"

                    + "<p style=\"margin:0 0 8px 0;font-size:12px;font-weight:700;color:#64748b;text-transform:uppercase;letter-spacing:0.04em;\">"

                    + "Message de l’administrateur</p>"

                    + "<p style=\"margin:0;font-size:15px;color:#0f172a;line-height:1.55;\">"

                    + escapeHtml(dernierMessage).replace("\n", "<br/>")

                    + "</p></div>";

        }



        String html = "<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\"/></head><body style=\"margin:0;padding:0;background:#eef2ff;font-family:Segoe UI,Roboto,Helvetica,Arial,sans-serif;\">"

                + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding:24px 12px;\">"

                + "<tr><td align=\"center\">"

                + "<table role=\"presentation\" width=\"100%\" style=\"max-width:560px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 8px 30px rgba(15,23,42,0.08);\">"

                + "<tr><td style=\"background:linear-gradient(135deg,#4f46e5,#6366f1);padding:22px 24px;\">"

                + "<p style=\"margin:0;font-size:11px;font-weight:700;letter-spacing:0.12em;color:rgba(255,255,255,0.85);\">GESTION AUDIT</p>"

                + "<h1 style=\"margin:8px 0 0 0;font-size:20px;font-weight:700;color:#ffffff;\">Changement de statut</h1>"

                + "</td></tr>"

                + "<tr><td style=\"padding:24px 24px 8px 24px;\">"

                + "<p style=\"margin:0 0 16px 0;font-size:15px;color:#334155;line-height:1.5;\">Bonjour,</p>"

                + "<p style=\"margin:0 0 20px 0;font-size:15px;color:#334155;line-height:1.55;\">"

                + "Une réclamation dont vous suivez les notifications a été <strong>mise à jour par un administrateur</strong>."

                + "</p>"

                + "<table role=\"presentation\" width=\"100%\" style=\"border-collapse:collapse;font-size:14px;color:#475569;\">"

                + row("Référence", "#" + r.getId())

                + row("Titre", escapeHtml(r.getTitre()))

                + row("Client", escapeHtml(r.getNom()))

                + row("E-mail déclarant", escapeHtml(r.getEmail() != null ? r.getEmail() : "—"))

                + row("Priorité", escapeHtml(r.getPriorite() != null ? r.getPriorite() : "—"))

                + row("Catégorie", escapeHtml(r.getCategorie() != null ? r.getCategorie() : "—"))

                + row("Créée le", escapeHtml(dateStr))

                + "</table>"

                + "<div style=\"margin-top:22px;padding:16px 18px;background:#ecfdf5;border-radius:12px;border:1px solid #a7f3d0;\">"

                + "<p style=\"margin:0 0 6px 0;font-size:12px;font-weight:700;color:#047857;text-transform:uppercase;letter-spacing:0.05em;\">Évolution du statut</p>"

                + "<p style=\"margin:0;font-size:16px;color:#064e3b;\">"

                + "<span style=\"text-decoration:line-through;opacity:0.85;\">" + escapeHtml(statutLibelle(ancienStatut)) + "</span>"

                + " &nbsp;<span style=\"color:#94a3b8;\">→</span>&nbsp; "

                + "<strong style=\"color:#059669;\">" + escapeHtml(statutLibelle(nouveauStatut)) + "</strong>"

                + "</p></div>"

                + msgBlock

                + "<p style=\"margin:24px 0 0 0;font-size:13px;color:#94a3b8;\">—<br/>Message automatique <strong>Gestion Audit</strong>. Ne pas répondre directement à cet e-mail.</p>"

                + "</td></tr></table></td></tr></table></body></html>";



        sendHtmlEmail(STATUS_NOTIFICATION_EMAIL, subject, html);

    }



    private static String row(String label, String value) {

        return "<tr><td style=\"padding:6px 0;border-bottom:1px solid #f1f5f9;width:38%;vertical-align:top;font-weight:600;color:#64748b;\">"

                + escapeHtml(label) + "</td><td style=\"padding:6px 0;border-bottom:1px solid #f1f5f9;color:#0f172a;\">"

                + value + "</td></tr>";

    }

}


