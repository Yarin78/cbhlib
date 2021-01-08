package se.yarin.cbhlib.entities;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Class responsible for searching and matching players in a database given a partial name
 */
public class PlayerSearcher {
    private static final Logger log = LoggerFactory.getLogger(PlayerSearcher.class);

    private static final int QUICK_SEARCH_MAX_HITS = 10000;
    private static final int QUICK_SEARCH_MIN_TRAVERSE = 10;
    private static final int QUICK_SEARCH_MAX_TIME_IN_MS = 50;

    private final PlayerBase playerBase;
    @Getter private final String firstName;
    @Getter private final String lastName;
    private final boolean caseSensitive;
    private final boolean exactMatch;
    private final String searchString;  // Logging purposes

    private static String resolveFirstName(String name) {
        if (name.contains(",")) {
            return name.substring(name.indexOf(",") + 1).strip();
        }
        if (name.contains(" ")) {
            return name.substring(0, name.indexOf(" ")).strip();
        }
        return "";
    }

    private static String resolveLastName(String name) {
        if (name.contains(",")) {
            return name.substring(0, name.indexOf(",")).strip();
        }
        if (name.contains(" ")) {
            return name.substring(name.indexOf(" ") + 1).strip();
        }
        return name;
    }

    public PlayerSearcher(PlayerBase playerBase, String name, boolean caseSensitive, boolean exactMatch) {
        this(playerBase, resolveLastName(name), resolveFirstName(name), caseSensitive, exactMatch);
    }

    public PlayerSearcher(PlayerBase playerBase, String lastName, String firstName, boolean caseSensitive, boolean exactMatch) {
        this.playerBase = playerBase;
        this.firstName = firstName;
        this.lastName = lastName;
        this.caseSensitive = caseSensitive;
        this.exactMatch = exactMatch;
        this.searchString = firstName.isEmpty() ? lastName : String.format("%s, %s", lastName, firstName);
    }

    private Stream<PlayerEntity> getBaseStream() {
        Stream<PlayerEntity> baseStream = playerBase.stream();
        if (this.caseSensitive) {
            baseStream = this.playerBase.prefixSearch(this.lastName);
        }
        return baseStream;
    }

    public Stream<Hit> search() {
        return getBaseStream().filter(this::matches).map(Hit::new);
    }

    /**
     * If it's possible to find all matching players quickly, return them; otherwise null.
     *
     * @return a list of all matching players, or null if a quick search couldn't provide a complete result quickly enough.
     */
    public List<PlayerEntity> quickSearch() {
        // The nature of this operation (aborting if taking too long) makes it unsuitable for streaming operations
        Iterator<PlayerEntity> stream = getBaseStream().iterator();
        // Start the timer after the initial prefix lookup since we might have a random high startup time
        long start = System.currentTimeMillis();
        ArrayList<PlayerEntity> result = new ArrayList<>();
        int steps = 0;
        while (stream.hasNext()) {
            PlayerEntity player = stream.next();
            steps += 1;
            if (matches(player)) {
                result.add(player);
                if (result.size() > QUICK_SEARCH_MAX_HITS) {
                    log.info(String.format("Player quick search for '%s' interrupted after %d ms due to too many hits (> %d)",
                            searchString, System.currentTimeMillis() - start, QUICK_SEARCH_MAX_HITS));
                    return null;
                }
            }
            if (steps >= QUICK_SEARCH_MIN_TRAVERSE) {
                long elapsed = System.currentTimeMillis() - start;
                if (elapsed > QUICK_SEARCH_MAX_TIME_IN_MS) {
                    log.info(String.format("Player quick search for '%s' interrupted after %d ms", searchString, elapsed));
                    return null;
                }
            }
        }
        log.info(String.format("Player quick search for '%s' finished in %d ms with %d hits",
                searchString, System.currentTimeMillis() - start, result.size()));
        return result;
    }

    public boolean matches(PlayerEntity player) {
        return matches(player.getLastName(), lastName) && matches(player.getFirstName(), firstName);
    }

    private boolean matches(String playerName, String searchName) {
        if (exactMatch) {
            return caseSensitive ? playerName.equals(searchName) : playerName.equalsIgnoreCase(searchName);
        }
        return caseSensitive ? playerName.startsWith(searchName) : playerName.toLowerCase().startsWith(searchName.toLowerCase());
    }

    public class Hit {
        @Getter
        private final PlayerEntity player;

        public Hit(PlayerEntity player) {
            this.player = player;
        }
    }
}