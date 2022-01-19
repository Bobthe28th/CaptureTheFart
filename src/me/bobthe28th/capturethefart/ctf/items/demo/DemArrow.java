package me.bobthe28th.capturethefart.ctf.items.demo;

import me.bobthe28th.capturethefart.Main;
import me.bobthe28th.capturethefart.ctf.CTFPlayer;
import me.bobthe28th.capturethefart.ctf.itemtypes.CTFBuildUpItem;
import org.bukkit.Material;

public class DemArrow extends CTFBuildUpItem {

    public DemArrow(CTFPlayer player_, Main plugin_) {
        super("Rocket", Material.ARROW, 8, 1, 0, player_, plugin_);
    }

}