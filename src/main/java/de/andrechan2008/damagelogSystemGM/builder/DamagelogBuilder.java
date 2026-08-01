package de.andrechan2008.damagelogSystemGM.builder;

import net.kyori.adventure.text.Component;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sammelt Hits zwischen Spielern.
 *
 * <p>Fuer jeden Spieler wird eine Liste der Hits erstellt, die er erhalten
 * oder vergeben hat. Hits, die innerhalb von {@link #HITS_IN_SECONDS}
 * Sekunden zwischen den selben zwei Spielern (in gleicher Richtung)
 * stattfinden, werden zusammengefuegt, damit der Damagelog uebersichtlich
 * bleibt und nicht ueberfuellt wird. Eintraege bleiben bis zu
 * {@link #MAX_LOG_AGE_MINUTES} Minuten im Log, danach werden sie entfernt.</p>
 */
public class DamagelogBuilder {
    private static final int HITS_IN_SECONDS = 5;

    private static final int MAX_LOG_AGE_MINUTES = 3;

    private static final Map<String, List<HitEntry>> playerHits = new ConcurrentHashMap<>();


    /**
     * Erfasst einen Hit zwischen 'attacker' und 'victim' zum Zeitpunkt 'time'.
     *
     * <p>Der Hit wird sowohl im Log des Angreifers als
     * auch im Log des Opfers gespeichert. War der Hit
     * toedlich, wird dafuer immer ein neuer, alleinstehender Eintrag angelegt.</p>
     *
     * @param attacker Name des zuschlagenden Spielers
     * @param victim   Name des getroffenen Spielers
     * @param time     Zeitpunkt des Hits
     * @param kill     'true', falls dieser Hit den Tod von 'victim' verursacht hat
     * @param weapon   Name des verwendeten Gegenstands
     */
    public static void addHit(String attacker, String victim, LocalTime time, boolean kill, Component weapon) {
        addHitForPlayer(attacker, victim, true, time, kill, weapon);
        addHitForPlayer(victim, attacker, false, time, kill, weapon);
    }

    /**
     * Liefert eine Kopie des Hit-Logs eines einzelnen Spielers.
     *
     * @param player Name des Spielers
     * @return Kopie der Hit-Liste in chronologischer Reihenfolge
     */
    public static List<HitEntry> getHits(String player) {
        List<HitEntry> hits = playerHits.get(player);
        if (null == hits) {
            return Collections.emptyList();
        }

        synchronized(hits) {
            return new ArrayList<>(hits);
        }
    }

    /**
     * Entfernt bei allen Spielern die Hit-Eintraege, die aelter als
     * {@link #MAX_LOG_AGE_MINUTES} Minuten sind.
     */
    public static void removeExpiredHits() {
        LocalTime now = LocalTime.now();

        for (Map.Entry<String, List<HitEntry>> entry : playerHits.entrySet()) {
            List<HitEntry> hits = entry.getValue();

            synchronized(hits) {
                while(!hits.isEmpty() && getSecondsDifference(hits.getFirst().lastHitTime(), now) > MAX_LOG_AGE_MINUTES * 60) {
                    hits.removeFirst();
                }

                if (hits.isEmpty()) {
                    playerHits.remove(entry.getKey(), hits);
                }
            }
        }
    }

    /**
     * Fuegt im Log von 'player' einen Hit gegen bzw. von 'opponent' hinzu.
     *
     * @param player   Spieler, in dessen Log der Hit gespeichert wird
     * @param opponent Gegenspieler bei diesem Hit
     * @param dealt    'true', falls 'player' den Hit ausgeteilt hat,
     * 'false', falls 'player' ihn erhalten hat
     * @param time     Zeitpunkt des Hits
     * @param kill     'true', falls dieser Hit toedlich war
     * @param weapon   Name des verwendeten Gegenstands
     */
    private static void addHitForPlayer(String player, String opponent, boolean dealt, LocalTime time, boolean kill, Component weapon) {
        List<HitEntry> hits = playerHits.computeIfAbsent(player, k -> Collections.synchronizedList(new ArrayList<>()));

        synchronized(hits) {
            HitEntry lastEntry = hits.isEmpty() ? null : hits.getLast();

            boolean canMergeIntoLastEntry = !kill
                    && null != lastEntry
                    && !lastEntry.killed()
                    && lastEntry.opponent().equals(opponent)
                    && lastEntry.dealt() == dealt
                    && Objects.equals(lastEntry.weapon(), weapon)
                    && getSecondsDifference(lastEntry.lastHitTime(), time) <= HITS_IN_SECONDS;

            if (canMergeIntoLastEntry) {
                hits.set(hits.size() - 1, lastEntry.withNewHit(time, weapon));
            } else {
                hits.add(new HitEntry(opponent, dealt, time, time, 1, kill, weapon));
            }
        }
    }

    /**
     * Berechnet die Differenz in Sekunden zwischen zwei Tageszeitpunkten.
     *
     * @param earlier frueherer Zeitpunkt
     * @param later   spaeterer Zeitpunkt
     * @return Differenz in Sekunden, immer >= 0
     */
    private static long getSecondsDifference(LocalTime earlier, LocalTime later) {
        long earlierSeconds = earlier.toSecondOfDay();
        long laterSeconds = later.toSecondOfDay();

        if (laterSeconds < earlierSeconds) {
            laterSeconds += 24 * 3600;
        }

        return laterSeconds - earlierSeconds;
    }

    /**
     * Unveraenderlicher Log-Eintrag fuer einen oder mehrere zusammengefuehrte
     * Hits gegen denselben Gegner in derselben Richtung.
     *
     * @param opponent     Name des Gegenspielers
     * @param dealt        'true', falls die Hits ausgeteilt wurden,
     * 'false', falls sie erhalten wurden
     * @param firstHitTime Zeitpunkt des ersten Hits in diesem Eintrag
     * @param lastHitTime  Zeitpunkt des zuletzt zusammengefuehrten Hits
     * @param hitCount     Anzahl der in diesem Eintrag zusammengefuehrten
     * Hits, bei einem Kill immer 1
     * @param killed       'true', falls dieser Eintrag ein Kill ist; ein
     * Kill-Eintrag wird nie mit anderen Hits
     * zusammengefuehrt
     * @param weapon       Anzeigename des beim letzten zusammengefuehrten Hit
     * verwendeten Gegenstands, kann 'null' sein
     */
    public record HitEntry(String opponent, boolean dealt, LocalTime firstHitTime, LocalTime lastHitTime, int hitCount, boolean killed, Component weapon) {
        /**
         * Erstellt eine aktualisierte Kopie dieses Eintrags mit einem
         * zusaetzlichen, zusammengefuehrten Hit zum Zeitpunkt 'time'.
         *
         * @param time   Zeitpunkt des neuen Hits, darf nicht 'null' sein
         * @param weapon Anzeigename des bei diesem Hit verwendeten
         * Gegenstands, darf 'null' sein
         * @return neuer Eintrag mit erhoehtem 'hitCount', aktualisiertem
         * 'lastHitTime' und der neuen 'weapon'
         */
        public HitEntry withNewHit(LocalTime time, Component weapon) {
            return new HitEntry(opponent, dealt, firstHitTime, time, hitCount + 1, killed, weapon);
        }
    }
}