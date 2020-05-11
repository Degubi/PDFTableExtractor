package degubi;

@FunctionalInterface
public interface RowWalkerFunction {
    String apply(String[] data, int columnIndex);
    
    static RowWalkerFunction walkForwards() {
        return (row, columnIndex) -> row[columnIndex];
    }
    
    static RowWalkerFunction walkBackwards() {
        return (row, columnIndex) -> row[row.length - 1 - columnIndex];
    }
}