package com.healthsuite.marketplace.service;

import com.healthsuite.marketplace.entity.Prescription;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/** Branded prescription PDF — printable for pharmacy use. */
@Service
public class RxPdfService {

    private static final Color BRAND = new Color(91, 61, 245);
    private static final Color INK = new Color(43, 35, 88);
    private static final Color MUTED = new Color(109, 104, 127);
    private static final Color STROKE = new Color(231, 228, 222);

    private static final Font H1 = new Font(Font.HELVETICA, 16, Font.BOLD, INK);
    private static final Font H2 = new Font(Font.HELVETICA, 10, Font.BOLD, BRAND);
    private static final Font BODY = new Font(Font.HELVETICA, 10, Font.NORMAL, INK);
    private static final Font SMALL = new Font(Font.HELVETICA, 8, Font.NORMAL, MUTED);
    private static final Font TH = new Font(Font.HELVETICA, 9, Font.BOLD, MUTED);

    public byte[] generate(Prescription p) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 48, 48, 48, 56);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Letterhead
            PdfPTable head = new PdfPTable(new float[]{3, 1});
            head.setWidthPercentage(100);
            PdfPCell left = borderless();
            left.addElement(new Paragraph("HealthSuite", new Font(Font.HELVETICA, 13, Font.BOLD, BRAND)));
            left.addElement(new Paragraph(p.getDoctorProfile().getFullName(), H1));
            String creds = joinNonBlank(" · ",
                p.getDoctorProfile().getSpecialty(), p.getDoctorProfile().getQualifications());
            left.addElement(new Paragraph(creds, SMALL));
            head.addCell(left);
            PdfPCell right = borderless();
            right.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Paragraph rx = new Paragraph("Rx", new Font(Font.TIMES_ROMAN, 26, Font.BOLDITALIC, BRAND));
            rx.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(rx);
            Paragraph date = new Paragraph(
                p.getCreatedAt().format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a")), SMALL);
            date.setAlignment(Element.ALIGN_RIGHT);
            right.addElement(date);
            head.addCell(right);
            doc.add(head);
            doc.add(divider());

            if (p.getDiagnosis() != null) {
                doc.add(section("DIAGNOSIS"));
                doc.add(new Paragraph(p.getDiagnosis(), BODY));
            }

            if (!p.getMedicines().isEmpty()) {
                doc.add(section("MEDICINES"));
                PdfPTable table = new PdfPTable(new float[]{3, 1.4f, 1.4f, 1.4f});
                table.setWidthPercentage(100);
                table.setSpacingBefore(4);
                for (String h : new String[]{"Medicine", "Dose", "Frequency", "Duration"}) {
                    PdfPCell c = new PdfPCell(new Phrase(h, TH));
                    c.setBackgroundColor(new Color(247, 246, 243));
                    c.setBorderColor(STROKE);
                    c.setPadding(6);
                    table.addCell(c);
                }
                for (var m : p.getMedicines()) {
                    String name = m.getInstructions() != null
                        ? m.getName() + "\n" + m.getInstructions()
                        : m.getName();
                    table.addCell(cell(name));
                    table.addCell(cell(nvl(m.getDosage())));
                    table.addCell(cell(nvl(m.getFrequency())));
                    table.addCell(cell(nvl(m.getDuration())));
                }
                doc.add(table);
            }

            if (!p.getTests().isEmpty()) {
                doc.add(section("RECOMMENDED TESTS"));
                com.lowagie.text.List list = new com.lowagie.text.List(false, 12);
                for (var t : p.getTests()) {
                    String line = t.getNote() != null ? t.getName() + " — " + t.getNote() : t.getName();
                    list.add(new ListItem(line, BODY));
                }
                doc.add(list);
            }

            if (p.getRemarks() != null) {
                doc.add(section("REMARKS & ADVICE"));
                doc.add(new Paragraph(p.getRemarks(), BODY));
            }

            if (p.getFollowUpDays() != null) {
                doc.add(section("FOLLOW-UP"));
                doc.add(new Paragraph("Follow up in " + p.getFollowUpDays() + " days.", BODY));
            }

            doc.add(Chunk.NEWLINE);
            doc.add(Chunk.NEWLINE);
            Paragraph sig = new Paragraph("_______________________\n"
                + p.getDoctorProfile().getFullName() + "\nDigital consultation via HealthSuite", SMALL);
            sig.setAlignment(Element.ALIGN_RIGHT);
            doc.add(sig);

            Paragraph foot = new Paragraph(
                "Consultation #" + p.getConsultation().getId()
                + " · Generated by HealthSuite · This prescription was issued after a video consultation.", SMALL);
            foot.setSpacingBefore(18);
            doc.add(foot);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate prescription PDF: " + e.getMessage(), e);
        }
    }

    private Paragraph section(String label) {
        Paragraph s = new Paragraph(label, H2);
        s.setSpacingBefore(14);
        s.setSpacingAfter(2);
        return s;
    }

    private PdfPCell cell(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, BODY));
        c.setBorderColor(STROKE);
        c.setPadding(6);
        return c;
    }

    private PdfPCell borderless() {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.NO_BORDER);
        return c;
    }

    private Element divider() {
        Paragraph p = new Paragraph();
        p.setSpacingBefore(8);
        p.add(new Chunk(new com.lowagie.text.pdf.draw.LineSeparator(0.7f, 100, STROKE, Element.ALIGN_CENTER, 0)));
        return p;
    }

    private String nvl(String s) { return s == null ? "—" : s; }

    private String joinNonBlank(String sep, String... parts) {
        return java.util.Arrays.stream(parts)
            .filter(x -> x != null && !x.isBlank())
            .collect(java.util.stream.Collectors.joining(sep));
    }
}
