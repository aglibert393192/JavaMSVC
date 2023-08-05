package algs4;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


class RandomHashSetTest {

    @BeforeEach
    void setUp() {
    }


    @Test
    void seededIterator() {
        Random rng = new Random(42);
        RandomHashSet<Integer> set = new RandomHashSet<>(rng);
        for (int i = 0; i < 5; i++) {
            set.add(i);
        }
        Iterator<Integer> it = set.iterator();
        Vector<Integer> result = new Vector<>(5);
        while (it.hasNext()) {
            Integer edge = it.next();
            set.remove(edge);
            it = set.iterator();
            result.add(edge);
        }
        Vector<Integer> expected = new Vector<>(5);
        expected.add(1);
        expected.add(3);
        expected.add(0);
        expected.add(4);
        expected.add(2);

        assertEquals(expected, result);
    }
    @Test
    void differentIterators(){
        Random rng1 = new Random(42);
        Random rng2 = new Random(108);
        RandomHashSet<Integer> set1 = new RandomHashSet<>(rng1);
        RandomHashSet<Integer> set2 = new RandomHashSet<>(rng2);
        for (int i = 0; i < 5; i++) {
            set1.add(i);
            set2.add(i);
        }
        Iterator<Integer> it1 = set1.iterator();
        Vector<Integer> result1 = new Vector<>(5);
        while (it1.hasNext()) {
            Integer edge = it1.next();
            set1.remove(edge);
            it1 = set1.iterator();
            result1.add(edge);
        }

        Iterator<Integer> it2 = set1.iterator();
        Vector<Integer> result2 = new Vector<>(5);
        while (it2.hasNext()) {
            Integer edge = it2.next();
            set2.remove(edge);
            it2 = set2.iterator();
            result2.add(edge);
        }
        assertNotEquals(result1,result2);
    }
}