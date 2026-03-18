package fr.umontpellier.iut.discordbot.lib;

import fr.umontpellier.iut.discordbot.Bot;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractEventListener extends ListenerAdapter implements ISharedBot {
    protected final Logger logger;
    private final Bot bot;

    public AbstractEventListener(Bot bot) {
        this.logger = LoggerFactory.getLogger(this.getClass());
        this.bot = bot;

        logger.info("Listener initialized !");
    }

    @NotNull
    @Override
    public Bot getBot() {
        return this.bot;
    }
}
