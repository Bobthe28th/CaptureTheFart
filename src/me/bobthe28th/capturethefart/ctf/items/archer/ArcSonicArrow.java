package me.bobthe28th.capturethefart.ctf.items.archer;

import me.bobthe28th.capturethefart.Main;
import me.bobthe28th.capturethefart.ctf.CTFPlayer;
import me.bobthe28th.capturethefart.ctf.itemtypes.CTFStackCooldownItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public class ArcSonicArrow extends CTFStackCooldownItem {

    ArcBow bow;

    public ArcSonicArrow(ArcBow bow_, CTFPlayer player_, Main plugin_, Integer defaultSlot_) {
        super("Sonic Arrow", Material.ARROW, 3, "Sonic Arrow", 5, Material.STICK, player_, plugin_, defaultSlot_);
        bow = bow_;
    }

    public void shoot(Arrow arrow) {
        if (getCooldown() == 0) {
            startCooldown();
        }
        arrow.setMetadata("dontKillOnLand", new FixedMetadataValue(plugin, true));
        arrow.setMetadata("ctfProjectile",new FixedMetadataValue(plugin,true));
        arrow.setMetadata("ctfProjectileType",new FixedMetadataValue(plugin,"archerarrow"));
        arrow.setMetadata("ArcherArrowType",new FixedMetadataValue(plugin,"sonic"));
        arrow.setCritical(false);
        new BukkitRunnable() {
            public void run() {
                if (arrow.isDead() || arrow.isOnGround()) {
                    if (arrow.isOnGround() || arrow.isDead()) {
                        land(arrow.getLocation(), arrow);
                    }
                    this.cancel();
                }
                for(Player p : Bukkit.getOnlinePlayers()){
                    p.spawnParticle(Particle.GLOW,arrow.getLocation(),1,0.0,0.0,0.0,0.0);
                }
            }
        }.runTaskTimer(plugin,0,1);
    }

    public void land(Location loc, Arrow arrow) {

        for(Player p : Bukkit.getOnlinePlayers()){
            p.spawnParticle(Particle.GLOW_SQUID_INK,loc,1,0.0,0.0,0.0,0.0);
        }

        double radius = 10.0;
        long time = 80L;
        boolean foundPerson = false;
        if (loc.getWorld() != null) {
            for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                if (e instanceof Player p) {
                    if (p.getLocation().distance(loc) <= radius) {
                        foundPerson = true;
                        if (Main.CTFPlayers.containsKey(p)) {
                            if (Main.CTFPlayers.get(p).getTeam() != player.getTeam()) {
                                Main.CTFPlayers.get(p).addGlow("sonic" + arrow.getUniqueId());
                                p.playSound(p.getLocation(),"minecraft:blueshield",1,1);
                            }
                            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                                final Player pg = p;
                                @Override
                                public void run() {
                                    if (Main.CTFPlayers.containsKey(pg)) {
                                        Main.CTFPlayers.get(pg).removeGlow("sonic" + arrow.getUniqueId());
                                    }
                                }
                            }, time);
                        }
                    }
                }
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, arrow::remove, time);
            if (foundPerson) {
                player.getPlayer().playSound(player.getPlayer().getLocation(), "minecraft:sonic", 1, 1);
            }
        }
    }

    @Override
    public void onclickAction(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_BLOCK || action == Action.RIGHT_CLICK_AIR) {
            setSlot(40);
            player.getPlayer().getInventory().setHeldItemSlot(player.getItemSlot(bow));
        }
    }
}
