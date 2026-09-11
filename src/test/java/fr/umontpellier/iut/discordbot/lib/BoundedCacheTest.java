package fr.umontpellier.iut.discordbot.lib;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BoundedCacheTest {

    @Test
    void evictsOldestEntryWhenFull() {
        BoundedCache<String, Integer> cache = new BoundedCache<>(2);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);

        assertEquals(List.of("b", "c"), List.copyOf(cache.keySet()));
    }

    @Test
    void updatingEntryKeepsItsPosition() {
        BoundedCache<String, Integer> cache = new BoundedCache<>(2);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("a", 10);
        cache.put("c", 3);

        assertEquals(List.of("b", "c"), List.copyOf(cache.keySet()));
    }
}
