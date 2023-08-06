package algs4;

import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

public class GetableSet<E> extends HashSet<E> {
    public GetableSet(int initialCapacity) {
        super(initialCapacity);
    }

    public GetableSet() {
    }

    public E get(E e) {
        Stream<E> eStream = super.stream().filter(elem -> elem.equals(e));
        Optional<E> returnedElem = eStream.findFirst();
        if (returnedElem.isEmpty()) {
            throw new NoSuchElementException("The element you asked for does not exist in this set");
        }
        return returnedElem.get();
    }
}
