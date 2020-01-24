package degubi;

@FunctionalInterface
public interface ArrayIntFunction<T>{
    T apply(T[] array, int k);
}