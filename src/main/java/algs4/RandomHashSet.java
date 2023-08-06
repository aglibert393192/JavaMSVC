package algs4;

import java.util.*;
import java.util.stream.Stream;


class RandomHashSet<E> extends HashSet<E> {
    Random rng;

    public RandomHashSet(Random rng) {
        this.rng = rng;
    }

    /**
     * @return An iterator for the set.
     * Every call to this function returns a new iterator which randomly shuffled the elements of the set.
     */
    @Override
    public Iterator<E> iterator() {
        List<E> elemList = new java.util.ArrayList<>((List<E>) List.of(super.toArray()));
        Collections.shuffle(elemList, rng);
        return elemList.iterator();
    }
}
