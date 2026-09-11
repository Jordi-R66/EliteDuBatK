package fr.umontpellier.iut.discordbot.studysuite;

import fr.umontpellier.iut.discordbot.studysuite.model.Assignment;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.TimeFormat;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;

/** Met les devoirs en forme pour Discord. Les échéances sont de vrais instants : des timestamps Discord. */
public final class HomeworkFormatter {
    public static final Color COLOR = new Color(0xE67E22);
    /** Discord n'accepte pas plus d'options dans un menu déroulant. */
    public static final int MAX_LISTED = 25;

    private static final int MAX_DESCRIPTION_PREVIEW = 120;

    private HomeworkFormatter() {
    }

    /** Les devoirs d'un membre, les plus proches d'abord, cochés ou non. */
    public static MessageEmbed list(List<Assignment> assignments, String siteUrl) {
        List<Assignment> sorted = sort(assignments);
        EmbedBuilder embed = base(siteUrl).setTitle("Devoirs à rendre", siteUrl + "/homework");

        if (sorted.isEmpty()) {
            return embed.setDescription("Rien à rendre pour l'instant 🎉").build();
        }

        boolean severalGroups = sorted.stream().map(a -> a.studentGroup().id()).distinct().count() > 1;
        StringBuilder description = new StringBuilder();
        int shown = 0;
        for (Assignment assignment : sorted) {
            String block = line(assignment, severalGroups) + "\n";
            if (shown == MAX_LISTED || description.length() + block.length() > MessageEmbed.DESCRIPTION_MAX_LENGTH - 60) {
                description.append("*… et ").append(sorted.size() - shown).append(" autre(s), voir le site*");
                break;
            }
            description.append(block);
            shown++;
        }

        long done = sorted.stream().filter(Assignment::completedByMe).count();
        return embed.setDescription(description.toString().strip())
                .setFooter("StudySuite · " + done + "/" + sorted.size() + " fait(s) · coche-les ci-dessous")
                .build();
    }

    /** L'annonce d'un devoir qui vient d'être ajouté, pour la classe. */
    public static MessageEmbed created(Assignment assignment, String authorMention, String siteUrl) {
        EmbedBuilder embed = base(siteUrl)
                .setTitle(truncate("📚 " + title(assignment), MessageEmbed.TITLE_MAX_LENGTH), siteUrl + "/homework")
                .addField("Pour", deadline(assignment), true)
                .addField("Groupe", assignment.studentGroup().label(), true);
        if (assignment.description() != null && !assignment.description().isBlank()) {
            embed.setDescription(truncate(assignment.description().strip(), MessageEmbed.DESCRIPTION_MAX_LENGTH));
        }
        embed.addField("Ajouté par", authorMention, true);
        return embed.build();
    }

    static String line(Assignment assignment, boolean showGroup) {
        StringBuilder line = new StringBuilder()
                .append(assignment.completedByMe() ? "✅ ~~" : "⬜ **")
                .append(title(assignment))
                .append(assignment.completedByMe() ? "~~" : "**")
                .append(" · ").append(deadline(assignment));
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

    private static EmbedBuilder base(String siteUrl) {
        return new EmbedBuilder()
                .setColor(COLOR)
                .setFooter("StudySuite · " + siteUrl.replaceFirst("^https?://", ""));
    }

    static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
