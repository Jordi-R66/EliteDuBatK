package fr.umontpellier.iut.discordbot.lib;

import fr.umontpellier.iut.discordbot.Bot;
import org.jetbrains.annotations.NotNull;

public interface ISharedBot {
    @NotNull Bot getBot();
}
