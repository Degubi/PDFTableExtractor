package pdftableextractorlib;

@FunctionalInterface
interface RowProviderFunction {
    String[] apply(String[][] data, int columnIndex);

    static RowProviderFunction providingForwards() {
        return (data, columnIndex) -> data[columnIndex];
    }

    static RowProviderFunction providingBackwards() {
        return (data, columnIndex) -> data[data.length - columnIndex - 1];
    }
}