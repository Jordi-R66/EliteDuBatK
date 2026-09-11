package fr.umontpellier.iut.discordbot.studysuite;

import fr.umontpellier.iut.discordbot.studysuite.model.Assignment;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Met les devoirs en forme pour Discord, en composants V2 : un conteneur, et une section par devoir avec son bouton
 * au bout de la ligne. Les échéances sont de vrais instants : des timestamps Discord.
 */
public final class HomeworkFormatter {
    public static final Color COLOR = new Color(0xE67E22);

    /**
     * Discord limite un message à 40 composants. Le conteneur, l'en-tête et le pied en prennent 3, chaque devoir 3
     * (la section, son texte, son bouton) : 12 devoirs au plus.
     */
    public static final int MAX_LISTED = (Message.MAX_COMPONENT_COUNT_IN_COMPONENT_TREE - 3) / 3;
    /** Discord limite le texte d'un message V2 ; on garde de la marge pour l'en-tête et le pied. */
    private static final int MAX_TEXT = Message.MAX_CONTENT_LENGTH_COMPONENT_V2 - 400;
    private static final int MAX_DESCRIPTION_PREVIEW = 120;

    private HomeworkFormatter() {
    }

    /**
     * Les devoirs d'un membre, les plus proches d'abord, avec pour chacun le bouton que donne {@code button}.
     */
    public static Container list(List<Assignment> assignments, String siteUrl, Function<Assignment, Button> button) {
        List<Assignment> sorted = sort(assignments);
        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(TextDisplay.of("## [Devoirs à rendre](" + siteUrl + "/homework)"));

        if (sorted.isEmpty()) {
            children.add(TextDisplay.of("Rien à rendre pour l'instant 🎉"));
            return Container.of(children).withAccentColor(COLOR);
        }

        boolean severalGroups = sorted.stream().map(a -> a.studentGroup().id()).distinct().count() > 1;
        int text = 0;
        int shown = 0;
        for (Assignment assignment : sorted) {
            String line = line(assignment, severalGroups);
            if (shown == MAX_LISTED || text + line.length() > MAX_TEXT) break;
            children.add(Section.of(button.apply(assignment), TextDisplay.of(line)));
            text += line.length();
            shown++;
        }

        long done = sorted.stream().filter(Assignment::completedByMe).count();
        String footer = "-# " + done + "/" + sorted.size() + " fait(s)";
        if (shown < sorted.size()) {
            footer += " · et " + (sorted.size() - shown) + " autre(s) sur [le site](" + siteUrl + "/homework)";
        }
        children.add(TextDisplay.of(footer));
        return Container.of(children).withAccentColor(COLOR);
    }

    /** L'annonce d'un devoir qui vient d'être ajouté, pour la classe, avec un bouton pour le cocher. */
    public static Container created(Assignment assignment, String authorMention, String siteUrl, Button button) {
        StringBuilder text = new StringBuilder()
                .append("### 📚 Nouveau devoir pour ").append(assignment.studentGroup().label()).append('\n')
                .append("**").append(title(assignment)).append("**\n")
                .append("Pour le ").append(deadline(assignment));
        if (assignment.description() != null && !assignment.description().isBlank()) {
            text.append("\n\n").append(truncate(assignment.description().strip(), 1500));
        }

        return Container.of(
                Section.of(button, TextDisplay.of(text.toString())),
                Separator.createDivider(Separator.Spacing.SMALL),
                TextDisplay.of("-# Ajouté par " + authorMention + " · [voir sur StudySuite](" + siteUrl + "/homework)")
        ).withAccentColor(COLOR);
    }

    /** Un devoir sur trois lignes : état et titre, échéance, aperçu de la description. */
    static String line(Assignment assignment, boolean showGroup) {
        StringBuilder line = new StringBuilder()
                .append(assignment.completedByMe() ? "✅ ~~" : "⬜ **")
                .append(title(assignment))
                .append(assignment.completedByMe() ? "~~" : "**")
                .append('\n').append(deadline(assignment));
        if (showGroup) {
            line.append(" · ").append(assignment.studentGroup().label());
        }
        if (assignment.description() != null && !assignment.description().isBlank()) {
            line.append("\n-# ").append(truncate(assignment.description().strip().replaceAll("\\s+", " "), MAX_DESCRIPTION_PREVIEW));
        }
        return line.toString();
    }

    /** « R5.05 — Rendu TP3 », ou le titre seul sans matière. */
    public static String title(Assignment assignment) {
        String subject = assignment.subject();
        return subject == null || subject.isBlank() ? assignment.title() : subject.strip() + " — " + assignment.title();
    }

    /** « jeudi 15 septembre 2026 23:59 (dans 4 jours) », dans le fuseau de chacun. */
    static String deadline(Assignment assignment) {
        return TimeFormat.DATE_TIME_LONG.format(assignment.due()) + " (" + TimeFormat.RELATIVE.format(assignment.due()) + ")";
    }

    public static List<Assignment> sort(List<Assignment> assignments) {
        return assignments.stream()
                .sorted(Comparator.comparing(Assignment::due).thenComparing(Assignment::title))
                .toList();
    }

    static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
