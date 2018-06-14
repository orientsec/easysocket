package com.orientsec.easysocket;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        AtomicInteger i = new AtomicInteger(2);
        assertTrue(compareAndSet(i, 4, 1, 2, 3));
        assertEquals(4, i.get());
    }

    private boolean compareAndSet(AtomicInteger i, int value, int... range) {
        int prev;
        do {
            prev = i.get();
            if (range.length == 0) {
                return false;
            }
            boolean contains = false;
            for (int aRange : range) {
                if (prev == aRange) {
                    contains = true;
                    break;
                }
            }
            if (!contains) {
                return false;
            }
        } while (!i.compareAndSet(prev, value));
        return true;
    }
}