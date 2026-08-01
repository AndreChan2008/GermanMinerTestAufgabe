package de.andrechan2008.damagelogSystemGM.cmds;

import de.andrechan2008.damagelogSystemGM.DamagelogSystemGM;
import de.andrechan2008.damagelogSystemGM.builder.DamagelogBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Command '/damagelog', der den persoenlichen Damage-Log eines Spielers
 * formatiert im Chat ausgibt.
 *
 * <p>Die Auswertung wird nicht sofort, sondern erst {@link #DELAY_SECONDS}
 * Sekunden nach Ausfuehrung des Commands gesendet. In der Zwischenzeit
 * erhaelt der Spieler einen Hinweis, dass der Log noch ausgewertet wird.</p>
 */
public class DamageLogCommand implements CommandExecutor {

    private static final int DELAY_SECONDS = 45;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Dieser Command kann nur von Spielern ausgefuehrt werden.");
            return true;
        }

        player.sendMessage(MINI_MESSAGE.deserialize("<yellow>Der Damagelog wird ausgewertet, bitte warte kurz!</yellow>"));

        Bukkit.getScheduler().runTaskLater(DamagelogSystemGM.getInstance(), () -> sendDamageLog(player), DELAY_SECONDS * 20L);

        return true;
    }

    /**
     * Baut den formatierten Damage-Log fuer 'player' und sendet ihn an ihn.
     *
     * @param player Spieler, dessen Log ausgegeben und ausgewertet wird
     */
    private void sendDamageLog(Player player) {
        List<DamagelogBuilder.HitEntry> hits = DamagelogBuilder.getHits(player.getName());

        player.sendMessage(buildHeader(player.getName()));
        player.sendMessage(buildSubHeader());

        for (DamagelogBuilder.HitEntry hit : hits) {
            player.sendMessage(buildHitLine(player.getName(), hit));
        }
    }

    /**
     * Baut die Titelzeile des Damage-Logs.
     *
     * @param playerName Name des Spielers, dessen Log angezeigt wird
     * @return formatierte Titelzeile
     */
    private Component buildHeader(String playerName) {
        String template = "<gold><bold>» Damage-Log von <player> «</bold></gold>";

        TagResolver resolver = TagResolver.resolver(Placeholder.unparsed("player", playerName));

        return MINI_MESSAGE.deserialize(template, resolver);
    }

    /**
     * Baut die Legenden-Zeile, die Erlittenen und Zugefuegten Schaden erklaert.
     *
     * @return formatierte Legenden-Zeile
     */
    private Component buildSubHeader() {
        String template = "<red>•</red> <gray>Erlittener Schaden</gray> " + "<dark_gray>//</dark_gray> <green>•</green> <gray>Zugefügter Schaden</gray>";

        return MINI_MESSAGE.deserialize(template);
    }

    /**
     * Baut eine einzelne Hit-Zeile des Damage-Logs.
     *
     * @param playerName Name des Spielers, dessen Log angezeigt wird
     * @param hit        auszugebender Hit-Eintrag
     * @return formatierte Hit-Zeile mit Hover-Text auf Zeit und DMG/KILL-Teil
     */
    private Component buildHitLine(String playerName, DamagelogBuilder.HitEntry hit) {

        String bulletTag = hit.dealt() ? "green" : "red";
        String attackerName = hit.dealt() ? playerName : hit.opponent();
        String victimName = hit.dealt() ? hit.opponent() : playerName;

        String template = "<hit_time> "
                + "<" + bulletTag + ">•</" + bulletTag + "> "
                + "<red><attacker></red> <dark_gray>»</dark_gray> <gray><victim></gray> <type>";

        TagResolver resolver = TagResolver.resolver(
                Placeholder.component("hit_time", buildHitTimeComponent(hit)),
                Placeholder.unparsed("attacker", attackerName),
                Placeholder.unparsed("victim", victimName),
                Placeholder.component("type", buildTypeComponent(hit)));

        return MINI_MESSAGE.deserialize(template, resolver);
    }

    /**
     * Baut den Zeit-Teil einer Hit-Zeile.
     *
     * @param hit auszugebender Hit-Eintrag
     * @return Component mit ggf. gesetztem Hover-Event
     */
    private Component buildHitTimeComponent(DamagelogBuilder.HitEntry hit) {
        String lastHitTime = TIME_FORMAT.format(hit.lastHitTime());
        Component timeComponent = Component.text(lastHitTime, NamedTextColor.GRAY);

        if (hit.hitCount() > 1) {
            String firstHitTime = TIME_FORMAT.format(hit.firstHitTime());
            Component hoverText = Component.text("Erster Hit: " + firstHitTime, NamedTextColor.GRAY)
                    .appendNewline()
                    .append(Component.text("Letzter Hit: " + lastHitTime, NamedTextColor.GRAY));

            timeComponent = timeComponent.hoverEvent(HoverEvent.showText(hoverText));
        }

        return timeComponent;
    }

    /**
     * Baut den 'DMG'/'KILL'-Teil einer Hit-Zeile inklusive Hover-Text, der
     * den zuletzt verwendeten Gegenstand anzeigt.
     *
     * @param hit auszugebender Hit-Eintrag
     * @return Component mit Hover-Event
     */
    private Component buildTypeComponent(DamagelogBuilder.HitEntry hit) {
        String label = hit.killed() ? "KILL" : "DMG";
        if (!hit.killed() && hit.hitCount() > 1) {
            label = label + " " + hit.hitCount() + "x";
        }

        Component weaponComponent = null == hit.weapon() ? Component.text("Hand", NamedTextColor.GRAY) : hit.weapon();

        return Component.text(label, hit.killed() ? NamedTextColor.DARK_RED : NamedTextColor.RED).hoverEvent(HoverEvent.showText(weaponComponent));
    }
}