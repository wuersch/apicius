package dev.apicius.document.derivation;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * ADR-0010's canonical capability derivation as executable code — the single source shared by
 * the document writer (the engine adapter), the uniqueness check (the service), and recognition
 * (the projection), so they can never disagree. Pure and deterministic; no engine types.
 *
 * <p>Kept honest against the frontend's presentation mirror ({@code src/lib/derivation.ts}) by
 * the shared vectors in {@code src/test/resources/derivation/derivation-vectors.json}.
 */
public final class CanonicalDerivation {

    /**
     * The "cleanly derivable" rule (FEAT-005): starts with a letter; words of letters and
     * digits separated by single spaces. Carried by the request DTO's bean validation.
     */
    public static final String NAME_PATTERN = "^[A-Za-z][A-Za-z0-9]*( [A-Za-z0-9]+)*$";

    private CanonicalDerivation() {
    }

    /**
     * Derives naming, labels, and the chosen operations for a resource noun. {@code name} is
     * trimmed and matches {@link #NAME_PATTERN}; operations come out in dialog row order
     * regardless of the set's iteration order.
     */
    public static ResourceDerivation derive(String name, Set<Capability> capabilities) {
        List<String> words = List.of(name.split(" "));
        String schemaName = pascalCase(words);
        String collectionPath = collectionPath(words);
        String itemPath = collectionPath + "/{id}";

        String singular = displayNoun(words, false);
        String plural = displayNoun(words, true);
        // By first letter, not by sound — deterministic and wrong-but-consistent for the few
        // mismatches ("an user"), like the pluralizer's stance (ADR-0010).
        String article = isVowel(singular.charAt(0)) ? "an" : "a";
        String pascalSingular = schemaName;
        String pascalPlural = pascalCase(pluralizeLast(words));

        List<DerivedOperation> operations = new ArrayList<>();
        for (Capability capability : EnumSet.copyOf(capabilities)) {
            operations.add(switch (capability) {
                case BROWSE -> new DerivedOperation(capability, "GET", collectionPath,
                        "list" + pascalPlural, "Browse all " + plural,
                        "200", "The list of " + plural + ".");
                case LOOK_UP -> new DerivedOperation(capability, "GET", itemPath,
                        "get" + pascalSingular, "Look up one " + singular,
                        "200", "The " + singular + ".");
                case ADD -> new DerivedOperation(capability, "POST", collectionPath,
                        "create" + pascalSingular, "Add " + article + " " + singular,
                        "201", "The created " + singular + ".");
                case UPDATE -> new DerivedOperation(capability, "PATCH", itemPath,
                        "update" + pascalSingular, "Update " + article + " " + singular,
                        "200", "The updated " + singular + ".");
                case REMOVE -> new DerivedOperation(capability, "DELETE", itemPath,
                        "delete" + pascalSingular, "Remove " + article + " " + singular,
                        "204", "The " + singular + " was removed.");
            });
        }
        return new ResourceDerivation(schemaName, collectionPath, itemPath, singular,
                List.copyOf(operations));
    }

    /**
     * Recognition's candidate inverses of {@link #pascalCase}. PascalCase is lossy — the
     * schema name alone cannot distinguish {@code "iPhone"} from {@code "I phone"} (both
     * become {@code IPhone}, but derive {@code /iphones} vs {@code /i-phones}) — so
     * recognition tries each candidate against the paths actually present in the document;
     * the writer's path-uniqueness rule guarantees at most one can match. Ordered
     * most-likely-first; deduplicated. A noun mixing consecutive single-letter words with
     * multi-letter ones ({@code "AB c"}) can still evade all candidates — import-grade
     * recognition (PRIN-003, refuse-don't-mangle) supersedes this heuristic when it lands.
     */
    public static List<List<String>> recognitionCandidates(String schemaName) {
        LinkedHashSet<List<String>> candidates = new LinkedHashSet<>();
        candidates.add(wordsOf(schemaName));
        // A digit or internal capital inside one word ("Product2", "iPhone"→"IPhone").
        candidates.add(List.of(schemaName));
        // Consecutive single-letter words ("A B"→"AB"), invisible to wordsOf's run handling.
        candidates.add(splitEveryBoundary(schemaName));
        return List.copyOf(candidates);
    }

    /**
     * The most likely segmentation of a schema name back into words: boundaries where a word
     * plausibly began — an uppercase letter after a non-uppercase one, an acronym ending
     * ({@code APIKey} → {@code API·Key}), or a digit after a letter ({@code Category5} →
     * {@code Category·5}). One candidate among {@link #recognitionCandidates}.
     */
    public static List<String> wordsOf(String schemaName) {
        List<String> words = new ArrayList<>();
        int start = 0;
        for (int i = 1; i < schemaName.length(); i++) {
            char current = schemaName.charAt(i);
            char previous = schemaName.charAt(i - 1);
            boolean caseBoundary = Character.isUpperCase(current)
                    && (!Character.isUpperCase(previous)
                            || (i + 1 < schemaName.length() && Character.isLowerCase(schemaName.charAt(i + 1))));
            boolean digitBoundary = Character.isDigit(current) && Character.isLetter(previous);
            if (caseBoundary || digitBoundary) {
                words.add(schemaName.substring(start, i));
                start = i;
            }
        }
        words.add(schemaName.substring(start));
        return words;
    }

    /** Splits before every uppercase letter and digit-run start: {@code AB} → {@code A·B}. */
    private static List<String> splitEveryBoundary(String schemaName) {
        List<String> words = new ArrayList<>();
        int start = 0;
        for (int i = 1; i < schemaName.length(); i++) {
            char current = schemaName.charAt(i);
            char previous = schemaName.charAt(i - 1);
            if (Character.isUpperCase(current)
                    || (Character.isDigit(current) && Character.isLetter(previous))) {
                words.add(schemaName.substring(start, i));
                start = i;
            }
        }
        words.add(schemaName.substring(start));
        return words;
    }

    /** PascalCase preserving inner case, so acronyms survive ("API key" → "APIKey"). */
    private static String pascalCase(List<String> words) {
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    /** Kebab-case plural: words lowercased, last word pluralized ("Order item" → "/order-items"). */
    private static String collectionPath(List<String> words) {
        return "/" + String.join("-",
                pluralizeLast(words.stream().map(String::toLowerCase).toList()));
    }

    /**
     * The noun as labels display it: each word keeps its casing unless it is ordinary
     * ("Product" → "product"), so acronyms read naturally ("Browse all API keys").
     */
    private static String displayNoun(List<String> words, boolean plural) {
        List<String> display = words.stream().map(CanonicalDerivation::displayWord).toList();
        return String.join(" ", plural ? pluralizeLast(display) : display);
    }

    private static String displayWord(String word) {
        String rest = word.substring(1);
        boolean ordinary = rest.equals(rest.toLowerCase());
        return ordinary ? Character.toLowerCase(word.charAt(0)) + rest : word;
    }

    private static List<String> pluralizeLast(List<String> words) {
        List<String> result = new ArrayList<>(words);
        int last = result.size() - 1;
        result.set(last, EnglishPluralizer.pluralize(result.get(last)));
        return result;
    }

    private static boolean isVowel(char c) {
        return "aeiou".indexOf(Character.toLowerCase(c)) >= 0;
    }
}
