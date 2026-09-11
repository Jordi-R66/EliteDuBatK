package fr.umontpellier.iut.discordbot.lib;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Map qui garde au plus {@code maxSize} entrées, en supprimant la plus ancienne.
 */
public class BoundedCache<K, V> extends LinkedHashMap<K, V> {
	private final int maxSize;

	public BoundedCache(int maxSize) {
		super(16, 0.75f, false);
		this.maxSize = maxSize;
	}

	@Override
	protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
		return size() > maxSize;
	}
}
