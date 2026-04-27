package com.gestionaudit.utils;

import com.gestionaudit.models.Reclamation;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

/**
 * Builds QR content as plain text so phone cameras / QR apps show full reclamation details
 * (no separate server required). Content is UTF-8 and kept under safe QR size limits.
 */
public final class ReclamationQrPayload {

    /** Target max UTF-8 bytes for reliable QR generation (ZXing auto version). */
    private static final int MAX_UTF8_BYTES = 2300;
    private static final int DESC_PREVIEW_MAX = 450;

    private ReclamationQrPayload() {}

    /**
     * Multi-line French text: readable when the phone displays the decoded QR string.
     */
    public static String formatForScan(Reclamation r) {
        if (r == null) {
            return "GESTION AUDIT\n(Aucune donnée)";
        }
        StringBuilder sb = new StringBuilder(512);
        sb.append("GESTION AUDIT\n");
        sb.append("=============================\n");
        sb.append("FICHE RECLAMATION\n");
        sb.append("=============================\n\n");

        if (r.getId() > 0) {
            line(sb, "Numéro", String.valueOf(r.getId()));
        } else {
            line(sb, "Statut fiche", "Brouillon (non enregistrée)");
        }
        line(sb, "Titre", r.getTitre());
        line(sb, "Statut", r.getStatut());
        line(sb, "Priorité", r.getPriorite());
        line(sb, "Catégorie", r.getCategorie());
        sb.append("\n");
        line(sb, "Demandeur", r.getNom());
        line(sb, "E-mail", sanitizeEmailForCamera(r.getEmail()));
        line(sb, "Téléphone", sanitizePhoneForCamera(r.getTelephone()));
        sb.append("\n");
        if (r.getDateCreation() != null) {
            line(sb, "Date création", r.getDateCreation().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }
        sb.append("\n--- Description ---\n");
        sb.append(trimDescription(r.getDescription()));
        sb.append("\n\n=============================\n");
        sb.append("Scan : texte intégré (Gestion Audit)");

        return trimToMaxUtf8Bytes(sb.toString(), MAX_UTF8_BYTES);
    }

    private static void line(StringBuilder sb, String label, String value) {
        String v = (value == null || value.isBlank()) ? "—" : value.trim().replace("\r", " ").replace("\n", " ");
        sb.append(label).append(" : ").append(v).append('\n');
    }

    private static String trimDescription(String desc) {
        if (desc == null || desc.isBlank()) {
            return "—";
        }
        String d = desc.trim().replace("\r\n", "\n");
        if (d.length() <= DESC_PREVIEW_MAX) {
            return d;
        }
        return d.substring(0, DESC_PREVIEW_MAX) + "\n[... texte tronqué pour le QR ...]";
    }

    private static String trimToMaxUtf8Bytes(String text, int maxBytes) {
        if (text.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
            return text;
        }
        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            String sub = text.substring(0, mid);
            if (sub.getBytes(StandardCharsets.UTF_8).length <= maxBytes) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        String cut = text.substring(0, low).trim();
        return cut + "\n\n[... contenu tronqué (QR trop volumineux) ...]";
    }

    private static String sanitizeEmailForCamera(String email) {
        if (email == null || email.isBlank()) {
            return "—";
        }
        return email.trim()
                .replace("@", " [at] ")
                .replace(".", " [dot] ");
    }

    private static String sanitizePhoneForCamera(String phone) {
        if (phone == null || phone.isBlank()) {
            return "—";
        }
        String trimmed = phone.trim();
        return trimmed.replaceAll("(\\d)", "$1 ");
    }
}
