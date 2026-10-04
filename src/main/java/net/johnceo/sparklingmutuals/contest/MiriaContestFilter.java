package net.johnceo.sparklingmutuals.contest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.world.scores.PlayerScoreEntry;

/** Selects Miria's bracket from the scoreboard's displayed contest sections. */
public final class MiriaContestFilter {
    private static final Pattern MIRIA_HEADER = Pattern.compile("^miria['\u2019]s contest(?=\\s|:|$)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONTEST_HEADER = Pattern.compile("\\bcontest\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORMATTING = Pattern.compile("\u00a7.");

    private MiriaContestFilter() {}

    public static Collection<PlayerScoreEntry> sortScores(Collection<PlayerScoreEntry> entries) {
        List<PlayerScoreEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingInt(PlayerScoreEntry::value).reversed()
                .thenComparing(PlayerScoreEntry::owner, String.CASE_INSENSITIVE_ORDER));
        return sorted;
    }

    public static List<String> miriaLines(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            if (!MIRIA_HEADER.matcher(clean(lines.get(i))).find()) continue;
            List<String> section = new ArrayList<>(2);
            // The contest heading has two following detail rows. Never cross
            // another contest heading when a section is absent or truncated.
            for (int j = i + 1; j < lines.size() && j <= i + 2; j++) {
                String line = clean(lines.get(j));
                if (CONTEST_HEADER.matcher(line).find()) break;
                section.add(line);
            }
            return section;
        }
        return List.of();
    }

    private static String clean(String line) {
        return FORMATTING.matcher(line).replaceAll("").trim();
    }
}
