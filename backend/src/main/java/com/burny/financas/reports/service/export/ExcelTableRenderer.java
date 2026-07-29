package com.burny.financas.reports.service.export;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * Shared `.xlsx` table writer for the report exports (design.md Decision 4). Uses {@link
 * SXSSFWorkbook} (streaming, bounded memory) rather than {@code XSSFWorkbook} so a wide date
 * range with many transactions doesn't hold the whole sheet in memory.
 */
final class ExcelTableRenderer {

    private static final int ROW_ACCESS_WINDOW = 100;

    private ExcelTableRenderer() {
    }

    static byte[] render(String sheetName, String[] headers, List<Object[]> rows) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(ROW_ACCESS_WINDOW)) {
            Sheet sheet = workbook.createSheet(sheetName);

            CellStyle headerStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            headerStyle.setFont(boldFont);

            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (Object[] rowData : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int col = 0; col < rowData.length; col++) {
                    writeCell(row.createCell(col), rowData[col]);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.dispose();
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to generate report Excel file", ex);
        }
    }

    private static void writeCell(Cell cell, Object value) {
        switch (value) {
            case null -> cell.setBlank();
            case BigDecimal bigDecimal -> cell.setCellValue(bigDecimal.doubleValue());
            case LocalDate localDate -> cell.setCellValue(localDate.toString());
            default -> cell.setCellValue(String.valueOf(value));
        }
    }
}
