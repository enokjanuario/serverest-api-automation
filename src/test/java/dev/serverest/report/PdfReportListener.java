package dev.serverest.report;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import dev.serverest.config.Environment;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

public class PdfReportListener implements TestExecutionListener {

    private static final BaseColor GREEN = new BaseColor(34, 139, 34);
    private static final BaseColor RED = new BaseColor(220, 20, 60);
    private static final BaseColor YELLOW = new BaseColor(218, 165, 32);
    private static final BaseColor HEADER_BG = new BaseColor(0, 51, 102);
    private static final BaseColor ROW_ALT_BG = new BaseColor(245, 247, 250);
    private static final BaseColor LIGHT_GREEN_BG = new BaseColor(220, 255, 220);
    private static final BaseColor LIGHT_RED_BG = new BaseColor(255, 220, 220);
    private static final BaseColor LIGHT_YELLOW_BG = new BaseColor(255, 255, 210);
    private static final BaseColor SECTION_BG = new BaseColor(230, 236, 245);
    private static final BaseColor BORDER_COLOR = new BaseColor(200, 200, 200);

    private static final Font TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, new BaseColor(0, 51, 102));
    private static final Font SUBTITLE_FONT = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font SECTION_TITLE_FONT = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(0, 51, 102));
    private static final Font TABLE_HEADER_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD, BaseColor.WHITE);
    private static final Font TABLE_CELL_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.NORMAL, BaseColor.BLACK);
    private static final Font TABLE_CELL_BOLD = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BaseColor.BLACK);
    private static final Font SUMMARY_SUBLABEL_FONT = new Font(Font.FontFamily.HELVETICA, 9, Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font SMALL_FONT = new Font(Font.FontFamily.HELVETICA, 7, Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font ERROR_FONT = new Font(Font.FontFamily.COURIER, 7, Font.NORMAL, RED);
    private static final Font FOOTER_FONT = new Font(Font.FontFamily.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY);

    private static final Path PARTIAL_DIR = Path.of("target", "test-results-partial");
    private static final Path LOCK_FILE = Path.of("target", "test-report.lock");
    private static final String FIELD_SEP = "\t";

    private final java.util.List<TestResult> results = new ArrayList<>();
    private final String forkId = UUID.randomUUID().toString().substring(0, 8);
    private long suiteStartTime;
    private LocalDateTime executionDateTime;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        suiteStartTime = System.currentTimeMillis();
        executionDateTime = LocalDateTime.now();
        cleanPartialResults();
    }

    private void cleanPartialResults() {
        try {
            if (Files.exists(PARTIAL_DIR)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(PARTIAL_DIR, "fork-*.tsv")) {
                    for (Path file : stream) {
                        Files.deleteIfExists(file);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            TestResult result = new TestResult();
            result.className = extractClassName(testIdentifier.getUniqueId());
            result.displayName = testIdentifier.getDisplayName();
            result.methodName = extractMethodName(testIdentifier.getUniqueId());
            result.startTime = System.currentTimeMillis();
            results.add(result);
        }
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
        if (testIdentifier.isTest()) {
            results.stream()
                    .filter(r -> r.displayName.equals(testIdentifier.getDisplayName()) && r.status == null)
                    .reduce((first, second) -> second)
                    .ifPresent(r -> {
                        r.duration = System.currentTimeMillis() - r.startTime;
                        r.status = switch (testExecutionResult.getStatus()) {
                            case SUCCESSFUL -> "Aprovado";
                            case FAILED -> "Falhou";
                            case ABORTED -> "Ignorado";
                        };
                        testExecutionResult.getThrowable().ifPresent(t -> {
                            r.errorMessage = t.getMessage();
                            r.errorClass = t.getClass().getSimpleName();
                        });
                    });
        }
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        if (testIdentifier.isTest()) {
            TestResult result = new TestResult();
            result.className = extractClassName(testIdentifier.getUniqueId());
            result.displayName = testIdentifier.getDisplayName();
            result.methodName = extractMethodName(testIdentifier.getUniqueId());
            result.status = "Ignorado";
            result.duration = 0;
            result.errorMessage = reason;
            results.add(result);
        }
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        if (results.isEmpty()) {
            return;
        }
        try {
            writePartialResults();
            Thread.sleep(2000);
            generateConsolidatedPdf();
        } catch (Exception e) {
            System.err.println("Falha ao gerar relat\u00f3rio PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void writePartialResults() throws IOException {
        Files.createDirectories(PARTIAL_DIR);
        Path partialFile = PARTIAL_DIR.resolve("fork-" + forkId + ".tsv");

        try (BufferedWriter writer = Files.newBufferedWriter(partialFile)) {
            writer.write("className" + FIELD_SEP + "displayName" + FIELD_SEP + "methodName" + FIELD_SEP
                    + "status" + FIELD_SEP + "duration" + FIELD_SEP + "errorClass" + FIELD_SEP + "errorMessage");
            writer.newLine();
            for (TestResult r : results) {
                writer.write(sanitize(r.className) + FIELD_SEP
                        + sanitize(r.displayName) + FIELD_SEP
                        + sanitize(r.methodName) + FIELD_SEP
                        + sanitize(r.status) + FIELD_SEP
                        + r.duration + FIELD_SEP
                        + sanitize(r.errorClass) + FIELD_SEP
                        + sanitize(r.errorMessage));
                writer.newLine();
            }
        }
    }

    private java.util.List<TestResult> readAllPartialResults() throws IOException {
        java.util.List<TestResult> all = new ArrayList<>();

        if (!Files.exists(PARTIAL_DIR)) {
            return all;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(PARTIAL_DIR, "fork-*.tsv")) {
            for (Path file : stream) {
                try (BufferedReader reader = Files.newBufferedReader(file)) {
                    String header = reader.readLine();
                    if (header == null) continue;

                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(FIELD_SEP, -1);
                        if (parts.length < 5) continue;

                        TestResult r = new TestResult();
                        r.className = desanitize(parts[0]);
                        r.displayName = desanitize(parts[1]);
                        r.methodName = desanitize(parts[2]);
                        r.status = desanitize(parts[3]);
                        r.duration = Long.parseLong(parts[4]);
                        if (parts.length > 5) r.errorClass = desanitize(parts[5]);
                        if (parts.length > 6) r.errorMessage = desanitize(parts[6]);
                        all.add(r);
                    }
                }
            }
        }
        return all;
    }

    private void generateConsolidatedPdf() throws Exception {
        Path targetDir = Path.of("target");
        Files.createDirectories(targetDir);
        Path outputPath = targetDir.resolve("test-report.pdf");

        Files.createDirectories(LOCK_FILE.getParent());
        try (FileChannel channel = FileChannel.open(LOCK_FILE,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             FileLock lock = channel.lock()) {

            java.util.List<TestResult> allResults = readAllPartialResults();

            if (allResults.isEmpty()) {
                allResults = new ArrayList<>(results);
            }

            generatePdf(allResults, outputPath);
        }
    }

    private void generatePdf(java.util.List<TestResult> allResults, Path outputPath) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPath.toFile()));
        writer.setPageEvent(new HeaderFooterPageEvent());
        document.open();

        addCoverSection(document);
        addEnvironmentSection(document);
        addSummaryCards(document, allResults);
        addPassRateBar(document, allResults);
        addResultsByClass(document, allResults);
        addDetailedTable(document, allResults);
        addSlowestTests(document, allResults);
        addFailureDetails(document, allResults);

        document.close();
        System.out.println("Relat\u00f3rio PDF gerado em: " + outputPath.toAbsolutePath()
                + " (" + allResults.size() + " testes)");
    }

    private void addCoverSection(Document document) throws Exception {
        addLogo(document);

        Paragraph title = new Paragraph("Relat\u00f3rio de Testes Automatizados", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(4);
        document.add(title);

        Paragraph subtitle = new Paragraph("ServeRest API - Automa\u00e7\u00e3o de Testes",
                new Font(Font.FontFamily.HELVETICA, 13, Font.NORMAL, new BaseColor(100, 100, 100)));
        subtitle.setAlignment(Element.ALIGN_CENTER);
        subtitle.setSpacingAfter(6);
        document.add(subtitle);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'as' HH:mm:ss");
        String dateTime = executionDateTime.format(formatter);
        long totalDuration = System.currentTimeMillis() - suiteStartTime;

        Paragraph info = new Paragraph();
        info.setAlignment(Element.ALIGN_CENTER);
        info.add(new Chunk("Executado em: " + dateTime + "  |  Dura\u00e7\u00e3o total: " + formatDuration(totalDuration), SUBTITLE_FONT));
        info.setSpacingAfter(16);
        document.add(info);

        addSeparator(document);
    }

    private void addEnvironmentSection(Document document) throws DocumentException {
        addSectionTitle(document, "Ambiente de Execu\u00e7\u00e3o");

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.5f, 4f});

        addEnvRow(table, "URL Base", Environment.getBaseUrl());
        addEnvRow(table, "Java", System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        addEnvRow(table, "Sistema Operacional", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        addEnvRow(table, "Arquitetura", System.getProperty("os.arch"));
        addEnvRow(table, "Encoding", System.getProperty("file.encoding"));
        addEnvRow(table, "Timezone", TimeZone.getDefault().getID());

        table.setSpacingAfter(16);
        document.add(table);
    }

    private void addEnvRow(PdfPTable table, String label, String value) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, TABLE_CELL_BOLD));
        labelCell.setBackgroundColor(SECTION_BG);
        labelCell.setPadding(6);
        labelCell.setBorderColor(BORDER_COLOR);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, TABLE_CELL_FONT));
        valueCell.setPadding(6);
        valueCell.setBorderColor(BORDER_COLOR);
        table.addCell(valueCell);
    }

    private void addSummaryCards(Document document, java.util.List<TestResult> allResults) throws DocumentException {
        addSectionTitle(document, "Resumo da Execu\u00e7\u00e3o");

        long passed = allResults.stream().filter(r -> "Aprovado".equals(r.status)).count();
        long failed = allResults.stream().filter(r -> "Falhou".equals(r.status)).count();
        long skipped = allResults.stream().filter(r -> "Ignorado".equals(r.status)).count();
        long total = allResults.size();
        double passRate = total > 0 ? (double) passed / total * 100 : 0;

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1, 1, 1, 1});

        addSummaryCard(table, "Total", String.valueOf(total), new BaseColor(240, 240, 240), BaseColor.BLACK);
        addSummaryCard(table, "Aprovados", String.valueOf(passed), LIGHT_GREEN_BG, GREEN);
        addSummaryCard(table, "Falhas", String.valueOf(failed), LIGHT_RED_BG, RED);
        addSummaryCard(table, "Ignorados", String.valueOf(skipped), LIGHT_YELLOW_BG, YELLOW);
        addSummaryCard(table, "Taxa de Sucesso", String.format("%.1f%%", passRate), BaseColor.WHITE, passRate >= 80 ? GREEN : RED);

        table.setSpacingAfter(16);
        document.add(table);
    }

    private void addSummaryCard(PdfPTable table, String label, String value, BaseColor bg, BaseColor valueColor) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(10);
        cell.setBorderColor(BORDER_COLOR);

        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        Font vFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, valueColor);
        p.add(new Chunk(value, vFont));
        p.add(Chunk.NEWLINE);
        p.add(new Chunk(label, SUMMARY_SUBLABEL_FONT));

        cell.addElement(p);
        table.addCell(cell);
    }

    private void addPassRateBar(Document document, java.util.List<TestResult> allResults) throws DocumentException {
        long passed = allResults.stream().filter(r -> "Aprovado".equals(r.status)).count();
        long failed = allResults.stream().filter(r -> "Falhou".equals(r.status)).count();
        long skipped = allResults.stream().filter(r -> "Ignorado".equals(r.status)).count();
        long total = allResults.size();

        if (total == 0) return;

        float passWidth = (float) passed / total;
        float failWidth = (float) failed / total;
        float skipWidth = (float) skipped / total;

        PdfPTable bar = new PdfPTable(3);
        bar.setWidthPercentage(100);
        float[] widths = new float[]{
                Math.max(passWidth, 0.01f),
                Math.max(failWidth, 0.01f),
                Math.max(skipWidth, 0.01f)
        };
        bar.setWidths(widths);

        addBarSegment(bar, passed > 0 ? passed + " aprovados" : "", new BaseColor(76, 175, 80));
        addBarSegment(bar, failed > 0 ? failed + " falhas" : "", new BaseColor(244, 67, 54));
        addBarSegment(bar, skipped > 0 ? skipped + " ignorados" : "", new BaseColor(255, 193, 7));

        bar.setSpacingAfter(20);
        document.add(bar);
    }

    private void addBarSegment(PdfPTable table, String text, BaseColor color) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, BaseColor.WHITE)));
        cell.setBackgroundColor(color);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(6);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setMinimumHeight(22);
        table.addCell(cell);
    }

    private void addResultsByClass(Document document, java.util.List<TestResult> allResults) throws DocumentException {
        addSectionTitle(document, "Resultados por Classe de Teste");

        Map<String, java.util.List<TestResult>> grouped = allResults.stream()
                .collect(Collectors.groupingBy(r -> r.className, LinkedHashMap::new, Collectors.toList()));

        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 1f, 1f, 1f, 1f, 1.5f});

        addDetailHeaderCell(table, "Classe");
        addDetailHeaderCell(table, "Total");
        addDetailHeaderCell(table, "Aprovados");
        addDetailHeaderCell(table, "Falhas");
        addDetailHeaderCell(table, "Ignorados");
        addDetailHeaderCell(table, "Dura\u00e7\u00e3o");

        int row = 0;
        for (Map.Entry<String, java.util.List<TestResult>> entry : grouped.entrySet()) {
            java.util.List<TestResult> classResults = entry.getValue();
            long classPassed = classResults.stream().filter(r -> "Aprovado".equals(r.status)).count();
            long classFailed = classResults.stream().filter(r -> "Falhou".equals(r.status)).count();
            long classSkipped = classResults.stream().filter(r -> "Ignorado".equals(r.status)).count();
            long classDuration = classResults.stream().mapToLong(r -> r.duration).sum();

            BaseColor rowBg = (row % 2 == 0) ? BaseColor.WHITE : ROW_ALT_BG;

            addDetailCell(table, entry.getKey(), rowBg, TABLE_CELL_BOLD, Element.ALIGN_LEFT);
            addDetailCell(table, String.valueOf(classResults.size()), rowBg, TABLE_CELL_FONT, Element.ALIGN_CENTER);
            addDetailCell(table, String.valueOf(classPassed), rowBg, new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, GREEN), Element.ALIGN_CENTER);
            addDetailCell(table, String.valueOf(classFailed), rowBg, classFailed > 0 ? new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, RED) : TABLE_CELL_FONT, Element.ALIGN_CENTER);
            addDetailCell(table, String.valueOf(classSkipped), rowBg, classSkipped > 0 ? new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, YELLOW) : TABLE_CELL_FONT, Element.ALIGN_CENTER);
            addDetailCell(table, formatDuration(classDuration), rowBg, TABLE_CELL_FONT, Element.ALIGN_CENTER);
            row++;
        }

        table.setSpacingAfter(20);
        document.add(table);
    }

    private void addDetailedTable(Document document, java.util.List<TestResult> allResults) throws DocumentException {
        addSectionTitle(document, "Detalhamento Completo dos Testes");

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 3.5f, 1.5f, 1f, 1f});

        addDetailHeaderCell(table, "Classe");
        addDetailHeaderCell(table, "Teste");
        addDetailHeaderCell(table, "M\u00e9todo");
        addDetailHeaderCell(table, "Status");
        addDetailHeaderCell(table, "Dura\u00e7\u00e3o");

        for (int i = 0; i < allResults.size(); i++) {
            TestResult r = allResults.get(i);
            BaseColor rowBg = (i % 2 == 0) ? BaseColor.WHITE : ROW_ALT_BG;

            addDetailCell(table, r.className, rowBg, TABLE_CELL_FONT, Element.ALIGN_LEFT);
            addDetailCell(table, r.displayName, rowBg, TABLE_CELL_FONT, Element.ALIGN_LEFT);
            addDetailCell(table, r.methodName != null ? r.methodName : "-", rowBg, SMALL_FONT, Element.ALIGN_LEFT);

            BaseColor statusColor = switch (r.status) {
                case "Aprovado" -> GREEN;
                case "Falhou" -> RED;
                default -> YELLOW;
            };
            BaseColor statusBg = switch (r.status) {
                case "Aprovado" -> LIGHT_GREEN_BG;
                case "Falhou" -> LIGHT_RED_BG;
                default -> LIGHT_YELLOW_BG;
            };
            Font statusFont = new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, statusColor);
            PdfPCell statusCell = new PdfPCell(new Phrase(r.status, statusFont));
            statusCell.setBackgroundColor(statusBg);
            statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            statusCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            statusCell.setPadding(4);
            statusCell.setBorderColor(BORDER_COLOR);
            table.addCell(statusCell);

            addDetailCell(table, formatDuration(r.duration), rowBg, TABLE_CELL_FONT, Element.ALIGN_CENTER);
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private void addSlowestTests(Document document, java.util.List<TestResult> allResults) throws DocumentException {
        addSectionTitle(document, "Top 10 Testes Mais Lentos");

        java.util.List<TestResult> sorted = allResults.stream()
                .sorted(Comparator.comparingLong((TestResult r) -> r.duration).reversed())
                .limit(10)
                .collect(Collectors.toList());

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.5f, 2.5f, 3.5f, 1.5f});

        addDetailHeaderCell(table, "#");
        addDetailHeaderCell(table, "Classe");
        addDetailHeaderCell(table, "Teste");
        addDetailHeaderCell(table, "Dura\u00e7\u00e3o");

        for (int i = 0; i < sorted.size(); i++) {
            TestResult r = sorted.get(i);
            BaseColor rowBg = (i % 2 == 0) ? BaseColor.WHITE : ROW_ALT_BG;

            addDetailCell(table, String.valueOf(i + 1), rowBg, TABLE_CELL_BOLD, Element.ALIGN_CENTER);
            addDetailCell(table, r.className, rowBg, TABLE_CELL_FONT, Element.ALIGN_LEFT);
            addDetailCell(table, r.displayName, rowBg, TABLE_CELL_FONT, Element.ALIGN_LEFT);

            Font durationFont = i < 3
                    ? new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, new BaseColor(255, 87, 34))
                    : TABLE_CELL_FONT;
            addDetailCell(table, formatDuration(r.duration), rowBg, durationFont, Element.ALIGN_CENTER);
        }

        table.setSpacingAfter(20);
        document.add(table);
    }

    private void addFailureDetails(Document document, java.util.List<TestResult> allResults) throws DocumentException {
        java.util.List<TestResult> failures = allResults.stream()
                .filter(r -> "Falhou".equals(r.status) && r.errorMessage != null)
                .collect(Collectors.toList());

        if (failures.isEmpty()) {
            addSectionTitle(document, "Detalhes de Falhas");
            Paragraph noFailures = new Paragraph("Nenhuma falha encontrada nesta execu\u00e7\u00e3o.",
                    new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC, GREEN));
            noFailures.setSpacingAfter(16);
            document.add(noFailures);
            return;
        }

        addSectionTitle(document, "Detalhes de Falhas (" + failures.size() + ")");

        for (int i = 0; i < failures.size(); i++) {
            TestResult r = failures.get(i);

            PdfPTable card = new PdfPTable(1);
            card.setWidthPercentage(100);

            PdfPCell headerCell = new PdfPCell();
            headerCell.setBackgroundColor(LIGHT_RED_BG);
            headerCell.setPadding(8);
            headerCell.setBorderColor(RED);
            headerCell.setBorderWidth(1);

            Paragraph header = new Paragraph();
            header.add(new Chunk((i + 1) + ". " + r.className + " > ", TABLE_CELL_BOLD));
            header.add(new Chunk(r.displayName, new Font(Font.FontFamily.HELVETICA, 8, Font.BOLD, RED)));
            headerCell.addElement(header);
            card.addCell(headerCell);

            PdfPCell bodyCell = new PdfPCell();
            bodyCell.setPadding(8);
            bodyCell.setBorderColor(BORDER_COLOR);

            Paragraph body = new Paragraph();
            body.add(new Chunk("Tipo: ", TABLE_CELL_BOLD));
            body.add(new Chunk(r.errorClass != null ? r.errorClass : "Desconhecido", TABLE_CELL_FONT));
            body.add(Chunk.NEWLINE);
            body.add(new Chunk("Mensagem: ", TABLE_CELL_BOLD));
            String msg = r.errorMessage != null ? r.errorMessage : "Sem detalhes";
            if (msg.length() > 500) {
                msg = msg.substring(0, 500) + "...";
            }
            body.add(new Chunk(msg, ERROR_FONT));
            body.add(Chunk.NEWLINE);
            body.add(new Chunk("Dura\u00e7\u00e3o: ", TABLE_CELL_BOLD));
            body.add(new Chunk(formatDuration(r.duration), TABLE_CELL_FONT));

            bodyCell.addElement(body);
            card.addCell(bodyCell);

            card.setSpacingAfter(10);
            document.add(card);
        }
    }

    private void addLogo(Document document) throws Exception {
        InputStream logoStream = getClass().getClassLoader().getResourceAsStream("logo_carrefour.png");
        if (logoStream == null) {
            logoStream = getClass().getClassLoader().getResourceAsStream("allure/logo_carrefour.png");
        }
        if (logoStream == null) {
            Path logoPath = Path.of("logo_carrefour.png");
            if (Files.exists(logoPath)) {
                byte[] logoBytes = Files.readAllBytes(logoPath);
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(140, 70);
                logo.setAlignment(Element.ALIGN_CENTER);
                document.add(logo);
                document.add(Chunk.NEWLINE);
                return;
            }
            return;
        }
        byte[] logoBytes = logoStream.readAllBytes();
        logoStream.close();
        Image logo = Image.getInstance(logoBytes);
        logo.scaleToFit(140, 70);
        logo.setAlignment(Element.ALIGN_CENTER);
        document.add(logo);
        document.add(Chunk.NEWLINE);
    }

    private void addSectionTitle(Document document, String text) throws DocumentException {
        Paragraph title = new Paragraph(text, SECTION_TITLE_FONT);
        title.setSpacingBefore(12);
        title.setSpacingAfter(8);
        document.add(title);

        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorderWidthBottom(2);
        lineCell.setBorderColorBottom(new BaseColor(0, 51, 102));
        lineCell.setBorderWidthTop(0);
        lineCell.setBorderWidthLeft(0);
        lineCell.setBorderWidthRight(0);
        lineCell.setFixedHeight(2);
        line.addCell(lineCell);
        line.setSpacingAfter(8);
        document.add(line);
    }

    private void addSeparator(Document document) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setBorderWidthBottom(1);
        lineCell.setBorderColorBottom(BORDER_COLOR);
        lineCell.setBorderWidthTop(0);
        lineCell.setBorderWidthLeft(0);
        lineCell.setBorderWidthRight(0);
        lineCell.setFixedHeight(1);
        line.addCell(lineCell);
        line.setSpacingAfter(12);
        document.add(line);
    }

    private void addDetailHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, TABLE_HEADER_FONT));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(6);
        cell.setBorderColor(HEADER_BG);
        table.addCell(cell);
    }

    private void addDetailCell(PdfPTable table, String text, BaseColor bg, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bg);
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5);
        cell.setBorderColor(BORDER_COLOR);
        table.addCell(cell);
    }

    private String extractClassName(String uniqueId) {
        if (uniqueId.contains("[class:")) {
            int start = uniqueId.lastIndexOf("[class:") + 7;
            int end = uniqueId.indexOf("]", start);
            if (end > start) {
                String fqcn = uniqueId.substring(start, end);
                return fqcn.substring(fqcn.lastIndexOf(".") + 1);
            }
        }
        return "Desconhecido";
    }

    private String extractMethodName(String uniqueId) {
        if (uniqueId.contains("[method:")) {
            int start = uniqueId.lastIndexOf("[method:") + 8;
            int end = uniqueId.indexOf("(", start);
            if (end < 0) {
                end = uniqueId.indexOf("]", start);
            }
            if (end > start) {
                return uniqueId.substring(start, end);
            }
        }
        return null;
    }

    private String formatDuration(long millis) {
        if (millis < 1000) {
            return millis + "ms";
        }
        long seconds = millis / 1000;
        long remaining = millis % 1000;
        if (seconds < 60) {
            return seconds + "s " + remaining + "ms";
        }
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return minutes + "m " + seconds + "s";
    }

    private static String sanitize(String value) {
        if (value == null) return "";
        return value.replace("\t", "    ").replace("\n", "\\n").replace("\r", "");
    }

    private static String desanitize(String value) {
        if (value == null || value.isEmpty()) return null;
        return value.replace("\\n", "\n");
    }

    private class HeaderFooterPageEvent extends PdfPageEventHelper {

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();

            float pageWidth = document.getPageSize().getWidth();
            float leftMargin = document.leftMargin();
            float rightMargin = document.rightMargin();
            float contentWidth = pageWidth - leftMargin - rightMargin;

            cb.setColorStroke(new BaseColor(200, 200, 200));
            cb.setLineWidth(0.5f);
            cb.moveTo(leftMargin, 42);
            cb.lineTo(leftMargin + contentWidth, 42);
            cb.stroke();

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase("ServeRest API - Relat\u00f3rio de Testes Automatizados", FOOTER_FONT),
                    leftMargin, 30, 0);

            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("P\u00e1gina " + writer.getPageNumber(), FOOTER_FONT),
                    leftMargin + contentWidth, 30, 0);
        }
    }

    private static class TestResult {
        String className;
        String displayName;
        String methodName;
        String status;
        String errorMessage;
        String errorClass;
        long startTime;
        long duration;
    }
}
