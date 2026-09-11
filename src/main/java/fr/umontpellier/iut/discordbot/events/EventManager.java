package fr.umontpellier.iut.discordbot.events;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.lib.AbstractEventListener;
import fr.umontpellier.iut.discordbot.lib.ObjectManager;
import net.dv8tion.jda.api.JDABuilder;

import java.util.List;

public class EventManager extends ObjectManager<AbstractEventListener> {
	private final Bot bot;

	public EventManager(Bot bot) {
		super("fr.umontpellier.iut.discordbot.events", AbstractEventListener.class, new Object[]{bot}, Bot.class);
		this.bot = bot;
	}

	public void registerEvents(JDABuilder builder) {
		builder.addEventListeners(get().toArray());
	}

	public List<AbstractEventListener> getEventListeners() {
		return super.get();
	}
}
