package com.orientsec.easysocket;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    private Set<String> set = new HashSet<>();

    @Test
    public void addition_isCorrect() {
        set.add("a");
        set.add("b");
        set.add("c");
        set.add("d");
        for (String s : set) {
            if (s.equals("b")) {
                set.remove(s);
                set.add("e");
            }
            System.out.println(s);
        }
        for (String s : set) {
            System.out.println(s);
        }
    }

}