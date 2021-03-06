package me.bobthe28th.capturethefart.ctf.items.archer;

import me.bobthe28th.capturethefart.Main;
import me.bobthe28th.capturethefart.ctf.CTFPlayer;
import me.bobthe28th.capturethefart.ctf.itemtypes.CTFBuildUpItem;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class ArcArrow extends CTFBuildUpItem {

    ArcBow bow;

    public ArcArrow(ArcBow bow_, CTFPlayer player_, Main plugin_, Integer defaultSlot_) {
        super("Arrow", Material.ARROW, 0, 1, 0, player_, plugin_, defaultSlot_);
        bow = bow_;
    }

    public void shoot(Arrow arrow) {
        if (!isOnCooldown()) {
            startCooldown();
        }
        arrow.setMetadata("ctfProjectile",new FixedMetadataValue(plugin,true));
        arrow.setMetadata("ctfProjectileType",new FixedMetadataValue(plugin,"archerarrow"));
        arrow.setMetadata("ArcherArrowType",new FixedMetadataValue(plugin,"arrow"));
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
