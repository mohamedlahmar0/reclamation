package com.gestionaudit.utils;

import com.gestionaudit.models.Reclamation;
import com.google.zxing.WriterException;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExportUtils {

    private static final DeviceRgb ACCENT = new DeviceRgb(79, 70, 229);
    private static final DeviceRgb ACCENT_LIGHT = new DeviceRgb(238, 242, 255);
    private static final DeviceRgb HEADER_ROW = new DeviceRgb(30, 41, 59);

    /** Plain text for QR codes (readable on phone after scan). */
    public static String buildReclamationQrPayload(Reclamation r) {
        return ReclamationQrPayload.formatForScan(r);
    }

    public static void exportToPDF(Reclamation r, String filePath) throws IOException {
        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        document.setMargins(40, 48, 48, 48);

        // --- Header band ---
        Table headerBand = new Table(UnitValue.createPercentArray(new float[]{1f})).useAllAvailableWidth();
        com.itextpdf.layout.element.Cell band = new com.itextpdf.layout.element.Cell()
                .setBackgroundColor(ACCENT)
                .setBorder(new SolidBorder(ACCENT, 0))
                .setPadding(18)
                .add(new Paragraph("Gestion Audit")
                        .setFontSize(11)
                        .setFontColor(ColorConstants.WHITE)
                        .setBold()
                        .setMarginBottom(4))
                .add(new Paragraph("Fiche réclamation personnalisée")
                        .setFontSize(20)
                        .setFontColor(ColorConstants.WHITE)
                        .setBold());
        headerBand.addCell(band);
        document.add(headerBand);

        document.add(new Paragraph(" ")
                .setMarginTop(6)
                .setFontSize(4));

        String genAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        document.add(new Paragraph("Document généré le " + genAt + " · Réclamation n°" + r.getId())
                .setFontSize(10)
                .setFontColor(new DeviceRgb(100, 116, 139))
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginBottom(16));

        // --- Data table ---
        Table table = new Table(UnitValue.createPercentArray(new float[]{28, 72})).useAllAvailableWidth();
        table.setBorder(new SolidBorder(new DeviceRgb(226, 232, 240), 1));

        addRow(table, "Titre", r.getTitre());
        addRow(table, "Description", r.getDescription());
        addRow(table, "Statut", r.getStatut());
        addRow(table, "Priorité", r.getPriorite());
        addRow(table, "Catégorie", r.getCategorie());
        addRow(table, "Demandeur", r.getNom());
        addRow(table, "E-mail", r.getEmail());
        addRow(table, "Téléphone", r.getTelephone());
        addRow(table, "Date de création", r.getDateCreation() != null ? r.getDateCreation().toString() : "—");

        document.add(table);

        document.add(new Paragraph(" ")
                .setMarginTop(28));

        // --- QR footer (good UI block) ---
        Table qrBlock = new Table(UnitValue.createPercentArray(new float[]{1f})).useAllAvailableWidth();
        qrBlock.setBackgroundColor(ACCENT_LIGHT);
        qrBlock.setBorder(new SolidBorder(ACCENT, 1.5f));

        com.itextpdf.layout.element.Cell qrCell = new com.itextpdf.layout.element.Cell()
                .setPadding(20)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        qrCell.add(new Paragraph("Vérification — QR code réclamation")
                .setBold()
                .setFontSize(13)
                .setFontColor(HEADER_ROW)
                .setMarginBottom(6));
        qrCell.add(new Paragraph("Scannez pour retrouver l'identifiant et les références de cette fiche.")
                .setFontSize(10)
                .setFontColor(new DeviceRgb(71, 85, 105))
                .setMarginBottom(14));

        try {
            String payload = buildReclamationQrPayload(r);
            byte[] png = QRCodeGenerator.generateQRCodePngBytes(payload, 200, 200);
            Image qr = new Image(ImageDataFactory.create(png));
            qr.setHorizontalAlignment(HorizontalAlignment.CENTER);
            qr.scaleToFit(168, 168);
            qrCell.add(qr);
        } catch (WriterException e) {
            qrCell.add(new Paragraph("(QR code indisponible)")
                    .setFontColor(ColorConstants.RED)
                    .setFontSize(10));
        }

        qrCell.add(new Paragraph("Contenu encodé (aperçu)")
                .setFontSize(9)
                .setFontColor(new DeviceRgb(100, 116, 139))
                .setMarginTop(10));
        qrCell.add(new Paragraph(buildReclamationQrPayload(r))
                .setFontSize(8)
                .setFontColor(new DeviceRgb(71, 85, 105))
                .setTextAlignment(TextAlignment.CENTER));

        qrBlock.addCell(qrCell);
        document.add(qrBlock);

        document.add(new Paragraph("Gestion Audit — usage interne / client")
                .setFontSize(8)
                .setFontColor(new DeviceRgb(148, 163, 184))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20));

        document.close();
    }

    private static void addRow(Table table, String label, String value) {
        String v = value == null || value.isBlank() ? "—" : value;
        com.itextpdf.layout.element.Cell left = new com.itextpdf.layout.element.Cell()
                .setBackgroundColor(new DeviceRgb(248, 250, 252))
                .setBorder(new SolidBorder(new DeviceRgb(226, 232, 240), 0.5f))
                .setPadding(10)
                .add(new Paragraph(label)
                        .setBold()
                        .setFontSize(10)
                        .setFontColor(HEADER_ROW));
        com.itextpdf.layout.element.Cell right = new com.itextpdf.layout.element.Cell()
                .setBorder(new SolidBorder(new DeviceRgb(226, 232, 240), 0.5f))
                .setPadding(10)
                .add(new Paragraph(v).setFontSize(10).setFontColor(new DeviceRgb(30, 41, 59)));
        table.addCell(left);
        table.addCell(right);
    }

    public static void exportToExcel(Reclamation r, String filePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Réclamation " + r.getId());

        String[] headers = {"Champ", "Valeur"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            CellStyle style = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            style.setFont(font);
            cell.setCellStyle(style);
        }

        Object[][] data = {
                {"ID", r.getId()},
                {"Titre", r.getTitre()},
                {"Description", r.getDescription()},
                {"Statut", r.getStatut()},
                {"Priorité", r.getPriorite()},
                {"Catégorie", r.getCategorie()},
                {"Nom Client", r.getNom()},
                {"Email", r.getEmail()},
                {"Téléphone", r.getTelephone()},
                {"Date Création", r.getDateCreation().toString()}
        };

        int rowNum = 1;
        for (Object[] rowData : data) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rowData[0].toString());
            row.createCell(1).setCellValue(rowData[1] != null ? rowData[1].toString() : "");
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);

        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }
}
