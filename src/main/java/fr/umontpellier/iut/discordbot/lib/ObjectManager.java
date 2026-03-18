package fr.umontpellier.iut.discordbot.lib;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.reflections.scanners.Scanners.SubTypes;

public abstract class ObjectManager<T> {
	private final List<T> objects;
	private final Logger logger;

	public ObjectManager(String packageToScan, Class<T> originalClass, Object[] constructorArgs, Class<?>... constructorParameterTypes) {
		this.logger = LoggerFactory.getLogger(this.getClass());
		Reflections reflections = new Reflections(packageToScan);

		objects = new ArrayList<>();
		reflections.get(SubTypes.of(originalClass).asClass()).forEach(cls -> {
			if (cls.isInterface() || Modifier.isAbstract(cls.getModifiers())) {
				logger.debug("Skipping non-instantiable type {}", cls.getSimpleName());
				return;
			}

			try {
				Class<? extends T> typedClass = cls.asSubclass(originalClass);
				T instance = typedClass.getConstructor(constructorParameterTypes).newInstance(constructorArgs);
				objects.add(instance);
			} catch (Exception e) {
				logger.error("Failed to instantiate {}: {}", cls.getSimpleName(), e.getMessage());
			}
		});
		logger.info("Initialized {} {} instances", objects.size(), originalClass.getSimpleName());
	}

	public ObjectManager(String packageToScan, Class<T> originalClass) {
		this(packageToScan, originalClass, new Object[0]);
	}

	protected List<T> get() {
		return Collections.unmodifiableList(objects);
	}
}
