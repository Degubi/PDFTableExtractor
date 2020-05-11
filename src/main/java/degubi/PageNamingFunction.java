package degubi;

import org.apache.poi.xssf.usermodel.*;

@FunctionalInterface
public interface PageNamingFunction {
    String apply(XSSFWorkbook workbook, int pageIndex, int tableIndex);
}