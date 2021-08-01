package ubiquitaku.framelock;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;


public final class FrameLock extends JavaPlugin implements Listener {
    String prefix = "§l[FlameLock]§r";
    FileConfiguration config;
    DataBase db;
    int max;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        config = getConfig();
        db = new DataBase();
        max = config.getInt("max");
        Bukkit.getPluginManager().registerEvents(this,this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    //額縁が置かれたらロックする
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (blockCheck(e.getBlock())) {
            return;
        }
        db.add(e.getBlock().getLocation(),e.getPlayer().getName());
        e.getPlayer().sendMessage(prefix+"額縁をロックしました");
    }

    //額縁の破壊
    @EventHandler
    public void onFlameBreak(BlockBreakEvent e) {
        //額縁を破壊することが可能かチェック
        if (!db.check(e.getBlock().getLocation(),e.getPlayer())) {
            //だめなひとならキャンセル
            e.setCancelled(true);
            e.getPlayer().sendMessage(prefix+"あなたは破壊する権限を持っていません");
            return;
        }
        //いいひとならキャンセルせずdbから情報を削除
        db.remove(e.getBlock().getLocation());
        e.getPlayer().sendMessage(prefix+"額縁が破壊されたため保護の情報を削除しました");
    }

    //額縁のクリック
    @EventHandler
    public void inItem(PlayerInteractEvent e) {
        //プレイヤーがブロックを右クリックまたは左クリックしたとき
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK) {
            //ブロックが額縁か
            if (!blockCheck(e.getClickedBlock())) {
                return;
            }
            //ブロックの操作が可能な人か
            if (db.check(e.getClickedBlock().getLocation(),e.getPlayer())) {
                //だめならキャンセル
                e.setCancelled(true);
                e.getPlayer().sendMessage(prefix+"なにしとんねん");
            }
        }
    }

    //額縁系アイテムならtrue
    public boolean blockCheck(Block block) {
        if (block.getType() == Material.ITEM_FRAME || block.getType() == Material.GLOW_ITEM_FRAME) {
            return true;
        }
        return false;
    }
}
