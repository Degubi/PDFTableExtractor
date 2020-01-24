package degubi;

import java.util.function.*;

public final class PageExtractionFunctions {
    
    public final IntPredicate rowComparisonFunction;
    public final IntPredicate columnComparisonFunction;
    public final PageNamingFunction pageNamingFunction;
    
    public PageExtractionFunctions(int rowComparisonMethod, int rowsPerPage, int columnComparisonMethod, int columnsPerPage,
                                   int pageNamingMethod) {
        
        this.rowComparisonFunction = getComparisonFunction(rowComparisonMethod, rowsPerPage);
        this.columnComparisonFunction = getComparisonFunction(columnComparisonMethod, columnsPerPage);
        this.pageNamingFunction = getPageNamingFunction(pageNamingMethod);
    }
    
    
    private static PageNamingFunction getPageNamingFunction(int namingMethod) {
        return namingMethod == 0 ? (workbook, ignore1, ignore2) -> (workbook.getNumberOfSheets() + 1) + "."
                                 : (workbook, pageIndex, tableIndex) -> (pageIndex + 1) + ". Page " + (tableIndex + 1) + ". Table";
    }
    
    private static IntPredicate getComparisonFunction(int comparisonMethod, int value) {
        return comparisonMethod == 0 ? k -> k <= value :
               comparisonMethod == 1 ? k -> k == value :
                                       k -> k >= value;
    }
}