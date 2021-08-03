package ubiquitaku.framelock;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;


public final class FrameLock extends JavaPlugin implements Listener {
    String prefix = "§l[FlameLock]§r";
    FileConfiguration config;
    DataBase db;
    int max;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        load();
        Bukkit.getPluginManager().registerEvents(this,this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        db.dbSave();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equals("flock")) {
            if (!sender.hasPermission("flock.op")) {
                sender.sendMessage("§c§lあなたはこのコマンドを実行する権限を持っていません");
                return true;
            }
            if (args.length == 0) {
                sender.sendMessage(prefix+"---------------------------------------");
                sender.sendMessage("/flock reload : config.ymlをリロードします");
                sender.sendMessage("/flock set <数字 > : 額縁の最大数を設定します");
                sender.sendMessage(prefix+"---------------------------------------");
                return true;
            }
            if (args[0].equals("reload")) {
                reloadConfig();
                load();
                sender.sendMessage(prefix+"リロード完了");
                return true;
            }
            if (args[0].equals("set")) {
                if (args.length != 2) {
                    sender.sendMessage(prefix+"/flock set <数字 > : 額縁の最大数を設定します");
                    return true;
                }
                config.set("max",args[1]);
                saveConfig();
                max = 5;
                db.count = Integer.parseInt(args[1]);
                sender.sendMessage(prefix+"最大設置数を"+args[1]+"に設定しました");
            }
        }
        return true;
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        //額縁が置かれたらロックする&上限数超えてたらキャンセル
        if (blockCheck(e.getBlock())) {
            return;
        }
        if (!db.count(e.getPlayer().getUniqueId())) {
            e.getPlayer().sendMessage(prefix+"あなたはこれ以上額縁を設置することができません");
            return;
        }
        db.add(e.getBlock().getLocation(),e.getPlayer().getUniqueId());
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

    //設定ファイル再読み込み
    public void load() {
        config = getConfig();
        max = config.getInt("max");
        db = new DataBase(this,max);
        db.dbLoad();
    }

    //額縁系アイテムならtrue
    public boolean blockCheck(Block block) {
        if (block.getType() == Material.ITEM_FRAME || block.getType() == Material.GLOW_ITEM_FRAME) {
            return true;
        }
        return false;
    }
}
