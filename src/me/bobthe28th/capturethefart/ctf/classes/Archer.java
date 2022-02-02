package me.bobthe28th.capturethefart.ctf.classes;

import me.bobthe28th.capturethefart.Main;
import me.bobthe28th.capturethefart.ctf.CTFClass;
import me.bobthe28th.capturethefart.ctf.CTFPlayer;
import me.bobthe28th.capturethefart.ctf.items.archer.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

public class Archer extends CTFClass implements Listener {

    String name = "Archer";
    ArcBow bow;
    ArcArrow arrow;
    ArcGhostArrow ghostArrow;
    ArcPoisonArrow poisonArrow;
    ArcSonicArrow sonicArrow;

    public Archer(CTFPlayer player_, Main plugin_) {
        super("Archer",plugin_,player_);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void deselect() {
        HandlerList.unregisterAll(this);
    }

    @Override
    public String getFormattedName() {
        return ChatColor.GREEN + name + ChatColor.RESET;
    }

    @Override
    public void giveItems() {
        player.removeItems();

        bow = new ArcBow(player,plugin,4);
        arrow = new ArcArrow(bow,player,plugin,0);
        ghostArrow = new ArcGhostArrow(bow,player,plugin,1);
        poisonArrow = new ArcPoisonArrow(bow,player,plugin,2);
        sonicArrow = new ArcSonicArrow(bow,player,plugin,3);
        player.giveItem(bow);
        player.giveItem(arrow);
        player.giveItem(ghostArrow);
        player.giveItem(poisonArrow);
        player.giveItem(sonicArrow);
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getEntity() instanceof Player p) {
            if (p != player.getPlayer()) return;

            event.setConsumeItem(false);

            if (event.getProjectile() instanceof Arrow && event.getConsumable() != null && event.getConsumable().getItemMeta() != null && event.getConsumable().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin,"ctfname"),  PersistentDataType.STRING)) {
                String name = event.getConsumable().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin,"ctfname"),  PersistentDataType.STRING);
                if (name != null) {
                    Arrow a = (Arrow) event.getProjectile();
                    switch (name) {
                        case "Arrow" -> arrow.shoot(a);
                        case "Ghost Arrow" -> ghostArrow.shoot(a);
                        case "Poison Arrow" -> poisonArrow.shoot(a);
                        case "Sonic Arrow" -> sonicArrow.shoot(a);
                    }
                }
            }

            if (event.getConsumable() != null) {
                event.getConsumable().setAmount(event.getConsumable().getAmount() - 1);
            }
        }
    }
}
