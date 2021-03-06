package me.bobthe28th.capturethefart.ctf;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import me.bobthe28th.capturethefart.Main;
import me.bobthe28th.capturethefart.ctf.itemtypes.CTFItem;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CTFPlayer implements Listener {

    Player player;
    CTFTeam team = null;
    CTFItem[] hotbar = new CTFItem[9];
    Main plugin;
    int cooldownTask;
    CTFClass pClass = null;
    CTFFlag carriedFlag = null;
    ArmorStand flagOnHead = null;
    ArrayList<String> glowReason = new ArrayList<>();
    double healCooldownMax = 6.0;
    double healCooldown = 0.0;
    boolean onHealCooldown = false;
    boolean canUse = true;
    boolean selectingWizard = false;
    BukkitTask respawnTimer = null;

    BossBar enemyHealth;
    LivingEntity enemy;
    double enemyHealthCooldown = 0.0;
    boolean onEnemyHealthCooldown = false;

    int kills = 0;
    int deaths = 0;
    int fishCaught = 0;

    boolean isAlive;


    public CTFPlayer(Main plugin_, Player p) {
        player = p;
        plugin = plugin_;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        isAlive = true;
        Main.gameController.addScoreboard(this);
        player.setScoreboard(Main.gameController.getScoreboard(this));
        player.setLevel(0);
        player.setExp(0.0F);
        player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getDefaultValue());
        player.setFoodLevel(20);
        player.setSaturation(0F);
        player.setGameMode(GameMode.SURVIVAL);
        for (PotionEffect pEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(pEffect.getType());
        }
        player.setGlowing(false);
        for (Entity e : player.getPassengers()) {
            player.removePassenger(e);
            e.remove();
        }
        removeItems();
        enemyHealth = Bukkit.createBossBar(new NamespacedKey(plugin,"ctfbossbar" + p.getName()),"",BarColor.RED,BarStyle.SEGMENTED_10);
        enemyHealth.setVisible(false);
        enemyHealth.addPlayer(player);

        cooldownTask = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            int slot = player.getInventory().getHeldItemSlot();
            if (getItem(slot) != null && player.getGameMode() != GameMode.SPECTATOR) {
                getItem(slot).displayCooldowns();
            } else {
                Objects.requireNonNull(player.getPlayer()).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
            }
        }, 0, 2);

    }

    public int getKills() {
        return kills;
    }

    public int getDeaths() {
        return deaths;
    }

    public boolean getIsAlive() {
        return isAlive;
    }

    public void setTeam(CTFTeam t) {
        CTFTeam oldTeam = team;
        team = t;
        t.getTeam().addEntry(player.getName());
        Main.gameController.updateTeams();
        Main.gameController.updateScoreboardGlobal(ScoreboardRowGlobal.ALIVE,team);
        if (oldTeam != null) {
            Main.gameController.updateScoreboardGlobal(ScoreboardRowGlobal.ALIVE,oldTeam);
        }
        giveArmor();
    }

    public void leaveTeam() {
        if (team != null) {
            team.getTeam().removeEntry(player.getName());
            team = null;
        }
        if (pClass != null) {
            giveArmor();
        } else {
            removeArmor();
        }
    }

    public CTFTeam getTeam() {
        return team;
    }

    public CTFClass getpClass() {
        return pClass;
    }

    public boolean getSelectingWizard() {
        return selectingWizard;
    }

    public void setSelectingWizard(boolean selectingWizard_) {
        if (!selectingWizard && selectingWizard_) {

            String[] wizIconNames = new String[]{"Fire","Ice","Wind","End"};

            for (int i = 0; i < wizIconNames.length; i++) {
                ItemStack it = new ItemStack(Material.CLAY_BALL,1);
                ItemMeta meta = it.getItemMeta();
                if (meta != null) {
                    meta.setCustomModelData(i+1);
                    meta.setDisplayName(ChatColor.RESET + wizIconNames[i]);
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "ctfitem"), PersistentDataType.BYTE, (byte) 1);
                    meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "ctfwicon"), PersistentDataType.INTEGER, i);
                }
                it.setItemMeta(meta);
                player.getInventory().setItem(i,it);
            }
        }
        selectingWizard = selectingWizard_;
    }

    public void setCanUse(boolean canUse_) { canUse = canUse_; }

    public String getFormattedName() {
        if (team != null) {
            return team.getChatColor() + player.getName() + ChatColor.RESET;
        } else {
            return player.getName();
        }
    }

    public void pickupFlag(CTFFlag flag) {
        addGlow("flag");
        carriedFlag = flag;
        flagOnHead = player.getWorld().spawn(player.getLocation(), ArmorStand.class);
        flagOnHead.setInvisible(true);
        flagOnHead.setInvulnerable(true);
        flagOnHead.setMarker(true);
        flagOnHead.setSmall(true);
        if (flagOnHead.getEquipment() != null) {
            flagOnHead.getEquipment().setHelmet(new ItemStack(flag.getTeam().getBanner()));
        }

        flag.getTeam().getTeam().addEntry(flagOnHead.getUniqueId().toString());
        flagOnHead.setGlowing(true);

        player.addPassenger(flagOnHead);

    }

    public void addGlow(String reason) {
        player.addPotionEffect((new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, true, false, true)));
        if (!glowReason.contains(reason)) {
            glowReason.add(reason);
        }
    }

    public void removeGlow(String reason) {
        glowReason.remove(reason);
        if (glowReason.isEmpty()) {
            player.removePotionEffect(PotionEffectType.GLOWING);
        }
    }

    public boolean isCarringFlag() {
        return carriedFlag != null;
    }

    public void captureFlag() {
        carriedFlag.capture(this);
        removeGlow("flag");
        carriedFlag = null;
        flagOnHead.remove();
    }

    public void dropFlag() {
        removeGlow("flag");
        carriedFlag.fall(player.getLocation());
        carriedFlag = null;
        flagOnHead.remove();
    }

    public void kill(CTFPlayer p) {
        player.playSound(player,Sound.ITEM_AXE_SCRAPE,1F,1F);
        player.getWorld().spawnParticle(Particle.TOTEM,p.getPlayer().getLocation().clone().add(new Vector(0.0,1.0,0.0)),20,0.2,0.5,0.2,0.2);
        kills ++;
        Main.gameController.updateScoreboard(this,ScoreboardRow.KILLS);
    }

    public void death(boolean byEntity) {
        isAlive = false;
        if (team != null) {
            Main.gameController.updateScoreboardGlobal(ScoreboardRowGlobal.ALIVE, team);
        }
        deaths ++;
        fishCaught = 0;
        Main.gameController.updateScoreboard(this,ScoreboardRow.DEATHS);
        if (carriedFlag != null) {
            dropFlag();
        }
        startRespawnCooldown();
    }

    public void respawn() {
        isAlive = true;
        player.setGameMode(GameMode.SURVIVAL);
        player.setFireTicks(0);
        player.setHealth(20.0);
        player.setFreezeTicks(0);
        player.setArrowsInBody(0);
        if (player.getVehicle() != null) {
            player.getVehicle().removePassenger(player);
        }
        if (team != null) {
            if(!player.teleport(team.getSpawnLocation(), PlayerTeleportEvent.TeleportCause.PLUGIN)) {
                plugin.getLogger().info("Bad respawn");
                player.setHealth(0.0);
            }
            Main.gameController.updateScoreboardGlobal(ScoreboardRowGlobal.ALIVE,team);
        }
        for (PotionEffect pEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(pEffect.getType());
        }
        if (pClass != null) {
            pClass.givePassives();
        }

        player.sendTitle(" "," ",0,0,0);

        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,80,255,true,false,true));

    }

    public void startRespawnCooldown() {
        if (respawnTimer != null) {
            respawnTimer.cancel();
        }
        respawnTimer = new BukkitRunnable() {
            int t = 10;
            @Override
            public void run() {
                if (this.isCancelled()) {
                    this.cancel();
                } else {
                    if (t <= 0) {
                        respawn();
                        this.cancel();
                    } else {
                        net.md_5.bungee.api.ChatColor numColor;
                        switch (t) {
                            case 2:
                                numColor = net.md_5.bungee.api.ChatColor.of("#FF4E11");
                                break;
                            case 3:
                                numColor = net.md_5.bungee.api.ChatColor.of("#FF8E15");
                                break;
                            case 4:
                                numColor = net.md_5.bungee.api.ChatColor.YELLOW;
                                break;
                            default:
                                if (t >= 5) {
                                    numColor = net.md_5.bungee.api.ChatColor.GREEN;
                                } else {
                                    numColor = net.md_5.bungee.api.ChatColor.DARK_RED;
                                }
                        }

                        player.sendTitle(ChatColor.RED + "You Died!" + ChatColor.RESET, "Respawning in: " + numColor + t + net.md_5.bungee.api.ChatColor.RESET, 0, 30, 0);
                        t--;
                    }
                }
            }
        }.runTaskTimer(plugin,0L,20L);
    }

    public void remove() {
        removeNotList();
        Main.CTFPlayers.remove(player);
    }

    public void removeNotList() {
        if (team != null) {
            Main.gameController.updateScoreboardGlobal(ScoreboardRowGlobal.ALIVE, team);
        }
        leaveClass();
        leaveTeam();
        Main.gameController.updateTeams();
        enemyHealth.removeAll();
        Bukkit.getServer().getScheduler().cancelTask(cooldownTask);
        if (respawnTimer != null) {
            respawnTimer.cancel();
        }
        HandlerList.unregisterAll(this);
        if (Bukkit.getScoreboardManager() != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
        removeItems();
        Main.gameController.removeScoreboard(this);
    }

    public void giveArmor() {
        if (team != null) {
            ItemStack chestPlate = new ItemStack(Material.LEATHER_CHESTPLATE);
            LeatherArmorMeta lam = (LeatherArmorMeta) chestPlate.getItemMeta();
            if (lam != null) {
                lam.setColor(team.getColor());
                lam.getPersistentDataContainer().set(new NamespacedKey(plugin, "ctfitem"), PersistentDataType.BYTE, (byte) 1);
                chestPlate.setItemMeta(lam);
            }
            player.getInventory().setItem(EquipmentSlot.CHEST, chestPlate);
            if (pClass != null) {
                pClass.giveArmor();
            }
        }
    }

    public void removeItems() {
        for (int i = 0; i <= 35; i ++) {
            ItemStack invItem = player.getInventory().getItem(i);
            if (invItem != null) {
                ItemMeta meta = invItem.getItemMeta();
                if (meta != null) {
                    Byte ctfitemData = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "ctfitem"), PersistentDataType.BYTE);
                    if (ctfitemData != null && ctfitemData == (byte) 1) {
                        if (getItem(i) != null) {
                            hotbar[getItem(i).getDefaultSlot()] = null;
                        }
                        invItem.setAmount(0);
                    }
                }
            }
        }
        //offhand
        ItemStack invItem = player.getInventory().getItem(40);
        if (invItem != null) {
            ItemMeta meta = invItem.getItemMeta();
            if (meta != null) {
                Byte ctfitemData = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "ctfitem"), PersistentDataType.BYTE);
                if (ctfitemData != null && ctfitemData == (byte) 1) {
                    if (getItem(40) != null) {
                        hotbar[getItem(40).getDefaultSlot()] = null;
                    }
                    invItem.setAmount(0);
                }
            }
        }
        //armor
        if (player.getEquipment() != null) {
            for (ItemStack eq : player.getEquipment().getArmorContents()) {
                if (eq != null) {
                    ItemMeta meta = eq.getItemMeta();
                    if (meta != null) {
                        Byte ctfitemData = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "ctfitem"), PersistentDataType.BYTE);
                        if (ctfitemData != null && ctfitemData == (byte) 1) {
                            eq.setAmount(0);
                        }
                    }
                }
            }
        }

        player.updateInventory();
    }

    public void removeArmor() {
        if (player.getEquipment() != null) {
            for (ItemStack eq : player.getEquipment().getArmorContents()) {
                if (eq != null) {
                    ItemMeta meta = eq.getItemMeta();
                    if (meta != null) {
                        Byte ctfitemData = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "ctfitem"), PersistentDataType.BYTE);
                        if (ctfitemData != null && ctfitemData == (byte) 1) {
                            eq.setAmount(0);
                        }
                    }
                }
            }
        }

        player.updateInventory();
    }

    public int getFishCaught() {
        return fishCaught;
    }

    public void setFishCaught(int fc) {
        fishCaught = fc;
    }

    public Player getPlayer() {
        return player;
    }

	public <C extends CTFClass> void setClass(C cl) {
		if (pClass != null) {
            pClass.deselect();
        }
	    removeItems();
        for (PotionEffect pEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(pEffect.getType());
        }
        pClass = cl;
        pClass.giveItems();
        pClass.givePassives();
        giveArmor();
	}

    public void leaveClass() {
        if (pClass != null) {
            pClass.deselect();
        }
        removeItems();
        pClass = null;
        giveArmor();
        Main.gameController.removeScoreboard(this);
        for (PotionEffect pEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(pEffect.getType());
        }

    }

    public <I extends CTFItem> void giveItem(I it) {
        hotbar[it.getDefaultSlot()] = it;
        player.getInventory().setItem(it.getDefaultSlot(),it.getItem());
    }

    public <I extends CTFItem> ItemStack getItemStack(I it) {
        return player.getInventory().getItem(getItemSlot(it));
    }

    public <I extends CTFItem> int getItemSlot(I it) {
        for (CTFItem cit : hotbar) {
            if (cit == it) {
                return cit.getSlot();
            }
        }
        return -1;
    }

    public CTFItem getItem(Integer slot) {
        for (CTFItem cit : hotbar) {
            if (cit != null) {
                if (cit.getSlot() == slot) {
                    return cit;
                }
            }
        }
        return null;
    }

    public void regen() {
        long rate = 10L;
        new BukkitRunnable() {
            @Override
            public void run() {
                if (onHealCooldown) {
                    this.cancel();
                } else {
                    if (player.getHealth() < Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getDefaultValue()) {
                        PacketContainer packet = new PacketContainer(PacketType.Play.Server.UPDATE_HEALTH);
                        double healAmout = Math.min(Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getDefaultValue(), player.getHealth() + 1);
                        packet.getFloat().write(0, (float) healAmout);
                        packet.getIntegers().write(0, 20);
                        try {
                            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        player.setHealth(healAmout);
                    } else {
                        this.cancel();
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, rate);
    }

    public void startHealCooldown() {
        healCooldown = healCooldownMax;
        if (!onHealCooldown) {
            onHealCooldown = true;
            new BukkitRunnable() {
                @Override
                public void run() {
                    healCooldown -= 0.1;
                    healCooldown = Math.round(healCooldown * 10.0) / 10.0;
                    if (healCooldown <= 0) {
                        healCooldown = 0;
                        onHealCooldown = false;
                        regen();
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 2L, 2L);
        }
    }

    public void startEnemyHealthCooldown() {
        onEnemyHealthCooldown = true;
        new BukkitRunnable() {
            @Override
            public void run() {
                enemyHealthCooldown -= 0.1;
                enemyHealthCooldown = Math.round(enemyHealthCooldown*10.0)/10.0;
                if (enemyHealthCooldown <= 0) {
                    enemyHealthCooldown = 0;
                    enemy = null;
                    onEnemyHealthCooldown = false;
                    enemyHealth.setVisible(false);
                    this.cancel();
                }
            }
        }.runTaskTimer(plugin, 2L, 2L);
    }

    public void setEnemyHealthCooldown() {
        enemyHealthCooldown = 4.0;
        if (!onEnemyHealthCooldown) {
            startEnemyHealthCooldown();
        }
    }

    public void setEnemy(LivingEntity newEnemy) {
        enemy = newEnemy;
    }

    public void updateEnemyHealth(double healthProgress) {
        enemyHealth.setProgress(healthProgress);
        enemyHealth.setTitle(enemy.getName());
        enemyHealth.setVisible(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getPlayer() != player) return;
        if (event.getTo() != null) {
            for (CTFTeam sBoxTeam : Main.gameController.getMap().getSpawnMoveBoxes().keySet()) {
                if (team != null && sBoxTeam != team && Main.gameController.getMap().getSpawnMoveBoxes().get(sBoxTeam).contains(event.getTo().toVector())) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer() != player.getPlayer()) return;
        remove();
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getPlayer() != player) return;
        isAlive = true;
        player.setGameMode(GameMode.SURVIVAL);
        player.setFireTicks(0);
        player.setHealth(0.0);
        player.setFreezeTicks(0);
        player.setArrowsInBody(0);
        if (team != null) {
            Main.gameController.updateScoreboardGlobal(ScoreboardRowGlobal.ALIVE,team);
        }
        for (PotionEffect pEffect : player.getActivePotionEffects()) {
            player.removePotionEffect(pEffect.getType());
        }
        if (pClass != null) {
            pClass.givePassives();
        }
        player.sendTitle(" "," ",0,0,0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.DAMAGE_RESISTANCE,80,255,true,false,true));
        if (team != null) {
            event.setRespawnLocation(team.getSpawnLocation());
            Main.gameController.updateScoreboardGlobal(ScoreboardRowGlobal.ALIVE,team);
        }
    }

    @EventHandler
    public void onPlayerClick(PlayerInteractEvent event) {
        if (event.getPlayer() != player) return;

        if (selectingWizard && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
            ItemStack it = player.getInventory().getItemInMainHand();
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                Byte ctfitemData = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "ctfitem"), PersistentDataType.BYTE);
                if (ctfitemData != null && ctfitemData == (byte) 1) {
                    Integer icon = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, "ctfwicon"), PersistentDataType.INTEGER);
                    if (icon != null) {
                        try {
                            Constructor<?> constructor = Main.CTFClasses[Main.CTFClasses.length - 4 + icon].getConstructor(CTFPlayer.class, Main.class);
                            CTFClass c = (CTFClass) constructor.newInstance(this, plugin);
                            setClass(c);
                        } catch (Exception ignored) {}
                        setCanUse(false);
                        boolean allReady = true;
                        for (CTFPlayer p : Main.CTFPlayers.values()) {
                            if (p.getpClass() == null) {
                                Main.gameController.stopClassSelectTimer();
                                allReady = false;
                            }
                        }
                        if (allReady) {
                            Main.gameController.startClassSelectTimer();
                        }
                    }
                }
            }
        }

        if (!canUse) {
            event.setCancelled(true);
        } else if (player.getGameMode() != GameMode.SPECTATOR) {
            int slot = player.getInventory().getHeldItemSlot();
            if (getItem(slot) != null) {
                getItem(slot).onclickAction(event);
            }
        }
    }

    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        if (event.getPlayer() != player) return;
        if (!canUse) {
            event.setCancelled(true);
        } else {
            int slot = player.getInventory().getHeldItemSlot();
            if (getItem(slot) != null) {
                getItem(slot).onConsume(event);
            }
        }
    }

    @EventHandler
    public void onPlayerItemHeldEvent(PlayerItemHeldEvent event) {
        if (event.getPlayer() != player) return;
        int slot = event.getNewSlot();
        if (getItem(slot) != null) {
            getItem(slot).onHold(event);
        }

        //display cooldowns
        if (getItem(slot) != null) {
            getItem(slot).displayCooldowns();
        } else {
            Objects.requireNonNull(player.getPlayer()).spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
        }
    }

    @EventHandler
    public void onPlayerPlaceBlock(BlockPlaceEvent event) {
        if (event.getPlayer() != player) return;

        boolean inspawn = false;
        for (CTFTeam sBoxTeam : Main.gameController.getMap().getSpawnPlaceBoxes().keySet()) {
            if (team != null && sBoxTeam != team && Main.gameController.getMap().getSpawnPlaceBoxes().get(sBoxTeam).contains(event.getBlockPlaced().getLocation().toVector())) {
                inspawn = true;
            }
        }

        if (!canUse || inspawn) {
            event.setCancelled(true);
        } else {
            int slot = player.getInventory().getHeldItemSlot();
            if (getItem(slot) != null) {
                getItem(slot).onblockPlace(event);
            }
        }
    }

    @EventHandler
    public void onPlacePickupArrow(PlayerPickupArrowEvent event) {
        if (event.getPlayer() != player) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player p && event.getClickedInventory() != null) {
            if (p != player) return;
            List<ItemStack> items = new ArrayList<>();
            items.add(event.getCurrentItem());
            items.add(event.getCursor());
            items.add((event.getClick() == ClickType.NUMBER_KEY) ? event.getWhoClicked().getInventory().getItem(event.getHotbarButton()) : event.getCurrentItem());
            for(ItemStack item : items) {
                if(item != null && item.hasItemMeta()) {
                    if (Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer().get(new NamespacedKey(plugin,"ctfitem"),  PersistentDataType.BYTE) != null) {
                        event.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (event.getPlayer() != player) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (event.getPlayer() != player) return;
        ItemStack item = event.getItemDrop().getItemStack();
        if(item.hasItemMeta()) {
            if (Objects.requireNonNull(item.getItemMeta()).getPersistentDataContainer().get(new NamespacedKey(plugin,"ctfitem"),  PersistentDataType.BYTE) != null) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPlayerBreakBlock(BlockBreakEvent event) {
        if (event.getPlayer() != player) return;
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (Main.breakableBlocks.containsKey(event.getBlock())) {
                if (Main.breakableBlocks.get(event.getBlock()) == team) {
                    pClass.breakBlock(event.getBlock());
                }
                event.setDropItems(false);
                Main.breakableBlocks.remove(event.getBlock());
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!event.isCancelled()) {
            if (event.getEntity() instanceof Player pf) {
                if (pf == player) {
                    startHealCooldown();
                }
            }
            if (event.getEntity() instanceof LivingEntity lEntity) {
                if (lEntity != enemy) return;
                updateEnemyHealth(Math.min(1.0,Math.max(0.0, (lEntity.getHealth() - event.getFinalDamage()) / Objects.requireNonNull(lEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue())));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!event.isCancelled()) {
            if (event.getDamager() instanceof Player pDamager && event.getEntity() instanceof LivingEntity lEntity) {
                if (pDamager != player) return;

                if (lEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                    enemy = lEntity;
                    setEnemyHealthCooldown();
                    updateEnemyHealth(Math.min(1.0,Math.max(0.0, (lEntity.getHealth() - event.getFinalDamage()) / Objects.requireNonNull(lEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue())));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player pf) {
            if (pf == player) {
                if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED) {
                    event.setCancelled(true);
                }
            }
        }
        if (event.getEntity() instanceof LivingEntity lEntity) {
            if (lEntity != enemy) return;
            updateEnemyHealth(Math.min(1.0,(lEntity.getHealth() + event.getAmount()) / Objects.requireNonNull(lEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH)).getValue()));
        }
    }

    @EventHandler
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player pf) {
            if (pf != player) return;
            player.setFoodLevel(20);
            player.setSaturation(0F);
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerItemDamage(PlayerItemDamageEvent event) {
        if (event.getPlayer() != player) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDismount(EntityDismountEvent event) {
        if (event.getDismounted() instanceof Player p) {
            if (p != player) return;
            if (event.getEntity() == flagOnHead) {
                event.setCancelled(true);
            }
        }
    }

//    @EventHandler
//    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
//        if (event.getEntity() instanceof Player pf) {
//            if (pf != player) return;
//            if (pf.getLocation().add(new Vector(0.0,-0.5,0.0)).getBlock().getType().isAir() || pf.getVelocity().getY() > 0.1) {
//                event.setCancelled(true);
//            }
//        }
//    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getPlayer() != player) return;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && (event.getClickedBlock().getBlockData() instanceof Door || event.getClickedBlock().getBlockData() instanceof TrapDoor)) {
            event.setCancelled(true); //Door and trapdoor opening
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null && event.getItem() != null && event.getItem().getType().toString().endsWith("_AXE") && event.getClickedBlock().getType().toString().startsWith("WAXED")) {
            event.setCancelled(true); //Scraping off wax
        } else if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) && event.getItem() != null && event.getItem().getType() == Material.GLASS_BOTTLE) {
            event.setCancelled(true); //Fill bottle with water
        }
    }

    @EventHandler
    public void onPlayerArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (event.getPlayer() != player) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getPlayer() != player) return;
        if (event.getRightClicked() instanceof ItemFrame || event.getRightClicked() instanceof GlowItemFrame) {
            event.setCancelled(true);
        }
    }
}
