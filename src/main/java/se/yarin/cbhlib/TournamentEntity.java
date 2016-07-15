package se.yarin.cbhlib;

import lombok.*;
import se.yarin.cbhlib.entities.Entity;
import se.yarin.chess.Date;

@Builder(toBuilder = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TournamentEntity implements Entity, Comparable<TournamentEntity> {
    @Getter
    private int id;

    @Getter
    @NonNull
    private String title;

    @Getter
    @NonNull
    private Date date = Date.today();

    @Getter
    private int category;

    @Getter
    private int rounds;

    @Getter
    @NonNull
    private TournamentType type = TournamentType.NONE;

    @Getter
    private boolean complete;

    @Getter
    private boolean threePointsWin;

    @Getter
    private boolean teamTournament;

    @Getter
    private boolean boardPoints;

    @Getter
    @NonNull
    private TournamentTimeControl timeControl = TournamentTimeControl.NORMAL;

    @Getter
    @NonNull
    private String place = "";

    @Getter
    @NonNull
    private Nation nation = Nation.NONE;

    // Missing here is City, latitude, longitude
    // Missing is also tiebreak rules
    // Maybe stored in another database?

    @Getter
    private int count;

    @Getter
    private int firstGameId;

    public TournamentEntity(@NonNull String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return title;
    }

    @Override
    public int compareTo(TournamentEntity o) {
        return title.compareTo(o.title);
    }

    @Override
    public TournamentEntity withNewId(int id) {
        return toBuilder().id(id).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TournamentEntity that = (TournamentEntity) o;

        return title.equals(that.title);
    }

    @Override
    public int hashCode() {
        return title.hashCode();
    }
}
