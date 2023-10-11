package pdftableextractorlib;

import static java.util.Spliterator.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.function.*;
import java.util.stream.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.poi.ss.util.*;
import org.apache.poi.xssf.usermodel.*;
import technology.tabula.*;
import technology.tabula.extractors.*;

public final class PDFTableExtractor {
    public static final int SKIP_METHOD_NONE = 0;
    public static final int SKIP_METHOD_LEADING = 1;
    public static final int SKIP_METHOD_TRAILING = 2;

    public static XSSFWorkbook extract(boolean autosizeColumns, int emptyColumnSkipMethod, int emptyRowSkipMethod, IntPredicate rowComparisonFunction, IntPredicate columnComparisonFunction,
                                       PageNamingFunction pageNamingFunction, Path pdfFilePath) throws IOException {

        try(var input = PDDocument.load(pdfFilePath.toFile())) {
            return doExtract(autosizeColumns, emptyColumnSkipMethod, emptyRowSkipMethod, rowComparisonFunction, columnComparisonFunction, pageNamingFunction, input);
        }
    }

    public static XSSFWorkbook extract(boolean autosizeColumns, int emptyColumnSkipMethod, int emptyRowSkipMethod, IntPredicate rowComparisonFunction, IntPredicate columnComparisonFunction,
                                       PageNamingFunction pageNamingFunction, byte[] pdfData) throws IOException {

        try(var input = PDDocument.load(pdfData)) {
            return doExtract(autosizeColumns, emptyColumnSkipMethod, emptyRowSkipMethod, rowComparisonFunction, columnComparisonFunction, pageNamingFunction, input);
        }
    }

    @SuppressWarnings({"unchecked", "cast", "resource"})
    private static XSSFWorkbook doExtract(boolean autosizeColumns, int emptyColumnSkipMethod, int emptyRowSkipMethod, IntPredicate rowComparisonFunction, IntPredicate columnComparisonFunction,
                                         PageNamingFunction pageNamingFunction, PDDocument pdfInput) {

        var textExtractor = new SpreadsheetExtractionAlgorithm();
        var rawPages = StreamSupport.stream(Spliterators.spliteratorUnknownSize(new ObjectExtractor(pdfInput).extract(), ORDERED | IMMUTABLE), false)
                                    .map(textExtractor::extract)
                                    .toArray(List[]::new);

        var extractedPages = (List<Table>[]) rawPages;   // Gotta love Java's non generic arrays, this cast is fine
        var excelOutput = new XSSFWorkbook();

        IntStream.range(0, extractedPages.length)
                 .forEach(pageIndex -> {
                     var tables = extractedPages[pageIndex];

                     IntStream.range(0, tables.size())
                              .forEach(tableIndex -> writeTable(autosizeColumns, emptyColumnSkipMethod, emptyRowSkipMethod, rowComparisonFunction,
                                                                columnComparisonFunction, pageNamingFunction, excelOutput, pageIndex, tables, tableIndex));
                 });

        return excelOutput;
    }

    private static void writeTable(boolean autosizeColumns, int emptyColumnSkipMethod, int emptyRowSkipMethod, IntPredicate rowComparisonFunction, IntPredicate columnComparisonFunction,
                                   PageNamingFunction pageNamingFunction, XSSFWorkbook excelOutput, int pageIndex, List<Table> tables, int tableIndex) {

        var rawRows = tables.get(tableIndex).getRows();

        if(rawRows.size() > 0) {
            var rows = rawRows.stream()
                              .map(k -> k.stream().map(RectangularTextContainer::getText).toArray(String[]::new))
                              .toArray(String[][]::new);

            var removableColumnCountFromBegin = (emptyColumnSkipMethod & 1) == 0 ? 0 : calculateRemovableColumnCount(rows, RowWalkerFunction.walkForwards());
            var removableColumnCountFromEnd = (emptyColumnSkipMethod & 2) == 0 ? 0 : calculateRemovableColumnCount(rows, RowWalkerFunction.walkBackwards());
            var removableRowCountFromBegin = (emptyRowSkipMethod & 1) == 0 ? 0 : calculateRemovableRowCount(rows, RowProviderFunction.providingForwards());
            var removableRowCountFromEnd = (emptyRowSkipMethod & 2) == 0 ? 0 : calculateRemovableRowCount(rows, RowProviderFunction.providingBackwards());

            if(rowComparisonFunction.test(rows.length - removableRowCountFromBegin - removableRowCountFromEnd) &&
               columnComparisonFunction.test(rows[0].length - removableColumnCountFromBegin - removableColumnCountFromEnd)) {

                var pageSheet = excelOutput.createSheet(pageNamingFunction.apply(excelOutput, pageIndex, tableIndex));

                IntStream.range(removableRowCountFromBegin, rows.length - removableRowCountFromEnd)
                         .forEach(rowIndex -> writeRow(rows, removableColumnCountFromBegin, removableColumnCountFromEnd, pageSheet, rowIndex));

                if(autosizeColumns) {
                    IntStream.range(0, pageSheet.getRow(0).getPhysicalNumberOfCells())
                             .forEach(pageSheet::autoSizeColumn);
                }

                pageSheet.setActiveCell(CellAddress.A1);
            }
        }
    }

    private static void writeRow(String[][] rows, int removableColumnCountFromBegin, int removableColumnCountFromEnd, XSSFSheet pageSheet, int rowIndex) {
        var excelRow = pageSheet.createRow(rowIndex);

        IntStream.range(removableColumnCountFromBegin, rows[rowIndex].length - removableColumnCountFromEnd)
                 .forEach(columnIndex -> excelRow.createCell(columnIndex).setCellValue(rows[rowIndex][columnIndex]));
    }

    // This function walks rows forwards/backwards until it finds a non blank value
    // instead of walking columns downwards/upwards, avoiding jumping between arrays
    private static int calculateRemovableColumnCount(String[][] data, RowWalkerFunction walkerFunction) {
        return Arrays.stream(data)
                     .mapToInt(columnData -> countRemovableColumnsInRow(walkerFunction, columnData))
                     .min()
                     .orElse(0);
    }

    private static int countRemovableColumnsInRow(RowWalkerFunction walkerFunction, String[] columnData) {
        return IntStream.range(0, columnData.length)
                        .mapToObj(columnIndex -> walkerFunction.apply(columnData, columnIndex))
                        .takeWhile(String::isBlank)
                        .mapToInt(i -> 1)
                        .sum();
    }

    // This function walks rows forwards/backwards until it finds a row that is not fully empty
    private static int calculateRemovableRowCount(String[][] data, RowProviderFunction elementProvider) {
        return IntStream.range(0, data.length)
                        .takeWhile(k -> Arrays.stream(elementProvider.apply(data, k)).allMatch(String::isBlank))
                        .map(k -> 1)
                        .sum();
    }

    // 0: Counting, 1: PageOrdinal+TableOrdinal
    public static PageNamingFunction getPageNamingFunction(int namingMethod) {
        return namingMethod == 0 ? (workbook, pageIndex, tableIndex) -> (workbook.getNumberOfSheets() + 1) + "."
                                 : (workbook, pageIndex, tableIndex) -> (pageIndex + 1) + ". Page " + (tableIndex + 1) + ". Table";
    }

    // 0: Less/equal, 1: equal, 2: greater/equal
    public static IntPredicate getComparisonFunction(int comparisonMethod, int value) {
        return comparisonMethod == 0 ? k -> k <= value :
               comparisonMethod == 1 ? k -> k == value :
                                       k -> k >= value;
    }
}