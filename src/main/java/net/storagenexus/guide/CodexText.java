package net.storagenexus.guide;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The Codex's text logic, kept apart from drawing so it can be unit tested: turning plain text into text with links, and
 * matching search queries.
 *
 * <p>Links are found, not written: wherever a topic's text names another topic (its title or one of its aliases, as a whole
 * word, in any letter case), that text becomes a link to it. Only the first mention of each topic in a topic links, so a
 * page doesn't turn into a sea of underlines.
 */
public final class CodexText {
    /** Words that link to a topic. */
    public record Phrase(String text, String topic) {}

    /** A run of text; {@code link} is the topic it opens, or null for plain text. */
    public record Segment(String text, String link) {}

    /** The phrases to look for, longest first so "Nexus Link Module" wins over "Nexus Link". */
    public static List<Phrase> sort(List<Phrase> phrases) {
        List<Phrase> sorted = new ArrayList<>(phrases);
        sorted.removeIf(p -> p.text().isBlank());
        sorted.sort(Comparator.comparingInt((Phrase p) -> p.text().length()).reversed());
        return sorted;
    }

    /**
     * Splits a line into plain text and links. {@code linked} holds the topics already linked on this page (it is updated),
     * and {@code self} is the topic being shown, which never links to itself.
     */
    public static List<Segment> links(String line, List<Phrase> sortedPhrases, Set<String> linked, String self) {
        List<Segment> out = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        int i = 0;
        while (i < line.length()) {
            Phrase hit = null;
            if (i == 0 || !Character.isLetterOrDigit(line.charAt(i - 1))) {
                for (Phrase p : sortedPhrases) {
                    int end = i + p.text().length();
                    if (p.topic().equals(self) || linked.contains(p.topic())) continue;
                    if (end > line.length() || !line.regionMatches(true, i, p.text(), 0, p.text().length())) continue;
                    if (end < line.length() && Character.isLetterOrDigit(line.charAt(end))) continue;
                    hit = p;
                    break;
                }
            }
            if (hit == null) {
                plain.append(line.charAt(i++));
                continue;
            }
            if (!plain.isEmpty()) {
                out.add(new Segment(plain.toString(), null));
                plain.setLength(0);
            }
            out.add(new Segment(line.substring(i, i + hit.text().length()), hit.topic()));
            linked.add(hit.topic());
            i += hit.text().length();
        }
        if (!plain.isEmpty()) out.add(new Segment(plain.toString(), null));
        return out;
    }

    /**
     * How well a topic matches a search: 2 if every word of the query is in its title, 1 if every word is somewhere in its
     * title or text, 0 if not.
     */
    public static int match(String query, String title, String body) {
        String[] words = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        if (words.length == 0 || words[0].isEmpty()) return 0;
        String t = title.toLowerCase(Locale.ROOT), all = t + " " + body.toLowerCase(Locale.ROOT);
        boolean inTitle = true;
        for (String w : words) {
            if (!all.contains(w)) return 0;
            if (!t.contains(w)) inTitle = false;
        }
        return inTitle ? 2 : 1;
    }

    private CodexText() {}
}
