package pbn.internals;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A track of a single robot
 */
public class Track {
    private Deque<Recording> elements;
    private static final int limit = 10;

    public Track() {
        elements = new ArrayDeque<Recording>(limit);
    }

    public synchronized void add(Recording element) {
        if (elements.size() >= limit) {
            elements.pollLast();
        }
        elements.push(element);
    }

    public Recording top() {
        return elements.peekFirst();
    }

    @Override
    public String toString() {
        return "Track{" +
                "elements=" + elements +
                '}';
    }
}
