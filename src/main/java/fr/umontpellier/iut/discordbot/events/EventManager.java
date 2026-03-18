package fr.umontpellier.iut.discordbot.events;

import fr.umontpellier.iut.discordbot.Bot;
import fr.umontpellier.iut.discordbot.lib.AbstractEventListener;
import fr.umontpellier.iut.discordbot.lib.ObjectManager;

import java.util.List;

public class EventManager extends ObjectManager<AbstractEventListener> {
    private final Bot bot;

    public EventManager(Bot bot) {
        super("fr.umontpellier.iut.discordbot.events", AbstractEventListener.class, new Object[]{bot}, Bot.class);
        this.bot = bot;
    }

    public void registerEvents() {
        get().forEach(event -> bot.getJda().addEventListener(event));
    }

    public List<AbstractEventListener> getEventListeners() {
        return super.get();
    }
}
