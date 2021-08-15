package ubiquitaku.framelock;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


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
        db.cancelAutoSave();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equals("flock")) {
            if (args.length == 0) {
                sender.sendMessage(prefix+"======================================");
                sender.sendMessage("/flock refresh : 何らかの原因により保護を貫通して破壊された額縁がある場合その保護を削除します");
                if (sender.isOp()) {
                    sender.sendMessage("OP---------------------------------------");
                    sender.sendMessage("/flock reload : config.ymlをリロードします");
                    sender.sendMessage("/flock set <数字 > : 額縁の最大数を設定します");
                    sender.sendMessage("flock save : おーとせーぶを無視して保存します");
                    sender.sendMessage("OP---------------------------------------");
                }
                sender.sendMessage(prefix+"======================================");
                return true;
            }
            if (args[0].equals("refresh")) {
                db.refresh();
            }
            if (!sender.hasPermission("flock.op")) {
                return true;
            }
            if (args[0].equals("reload")) {
                db.saveMap();
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
                max = Integer.parseInt(args[1]);
                db.count = Integer.parseInt(args[1]);
                sender.sendMessage(prefix+"最大設置数を"+args[1]+"に設定しました");
            }
            //debug用
            if (args[0].equals("reset")) {
                db.resetDB();
                sender.sendMessage("リセット完了");
            }
            //debug
            if (args[0].equals("yaw")) {
                Player p = (Player) sender;
                Bukkit.broadcast(Component.text(p.getLocation().getYaw()));
            }
            if (args[0].equals("pi")) {
                Player p = (Player) sender;
                Bukkit.broadcast(Component.text(p.getLocation().getPitch()));
            }
        }
        return true;
    }

    @EventHandler
    public void placeFrame(HangingPlaceEvent e) {
        //額縁が置かれたらロックする&上限数超えてたらキャンセル
        if (e.isCancelled()) {
            return;
        }
        if (!entityCheck(e.getEntity().getType())) {
            Bukkit.getLogger().info("debug1");
            return;
        }
        if (!db.count(e.getPlayer().getUniqueId())) {
            if (e.getPlayer().isOp()) {
                e.getPlayer().sendMessage(prefix+"権限使って上限を超えて設置しています");
                return;
            }
            e.getPlayer().sendMessage(prefix+"あなたはこれ以上額縁を設置することができません");
            e.setCancelled(true);
            return;
        }
        if (db.containsBlock(db.blockVector(e.getEntity().getLocation()))) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(prefix+"既に保護された額縁が設置されているブロックには額縁を設置できません");
            return;
        }
        db.add(e.getEntity().getLocation(),e.getPlayer().getUniqueId());
        db.addBlock(db.blockVector(e.getEntity().getLocation()),e.getPlayer().getUniqueId());
        e.getPlayer().sendMessage(prefix+"額縁をロックしました");
    }

    //クリック対策&弓矢対策
    @EventHandler
    public void breakFrame(EntityDamageByEntityEvent e) {
        if (!entityCheck(e.getEntityType())) {
            return;
        }
        if (db.checkEntity(e.getEntity().getLocation(),e.getDamager())) {
            return;
        }
        e.setCancelled(true);
        e.getDamager().sendMessage(prefix+"あなたは破壊できません");
    }

    //回転防止
    @EventHandler
    public void click(PlayerInteractEntityEvent e) {
        if (e.getRightClicked() instanceof ItemFrame) {
            if (db.check(e.getRightClicked().getLocation(),e.getPlayer())) {
                return;
            }
            e.setCancelled(true);
            e.getPlayer().sendMessage(prefix+"あなたはこの額縁をいじる権限がありません");
        }
    }

    //破壊をキャンセル
    @EventHandler
    public void onBreak(HangingBreakByEntityEvent e) {
        if (db.checkEntity(e.getEntity().getLocation(),e.getRemover())) {
            db.remove(e.getEntity().getLocation());
            db.removeBlock(db.blockVector(e.getEntity().getLocation()));
            e.getRemover().sendMessage(prefix+"額縁が破壊されたため保護の情報を削除します");
            return;
        }
        e.setCancelled(true);
        e.getRemover().sendMessage(prefix+"あなたはこの額縁を破壊できません");
    }

    //ブロック破壊のキャンセル
    @EventHandler
    public void block(BlockBreakEvent e) {
        if (!db.isBreakBlock(e.getBlock().getLocation())) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(prefix+"あなたは額縁により一緒に保護されているブロックのため破壊できません");
        }
    }

    //設定ファイル再読み込み
    public void load() {
        config = getConfig();
        max = config.getInt("max");
        db = new DataBase(this,max);
        db.loadMap();
    }

    //額縁系エンティティならtrue
    public boolean entityCheck(EntityType type) {
        if (type == EntityType.ITEM_FRAME || type == EntityType.GLOW_ITEM_FRAME) {
            return true;
        }
        return false;
    }

    //破壊されたブロックのあらゆる方向に対して付着している額縁があった場合のlocationをStringlistにして返す
    public List<String> vectorBlockLocation(Location location) {
        String world = location.getWorld().getName();
        int x = location.getBlockX(),y = location.getBlockY(),z = location.getBlockZ();
        int yaw = (int) location.getYaw();
        int pitch = (int) location.getPitch();
        List<String> li = new ArrayList<>();
        li.add(makeStr(world,x,y,z,yaw,90));
        return li;
    }

    public String makeStr(String world,int x,int y,int z,int yaw,int pitch) {
        return world+"/"+x+"/"+y+"/"+z+"/"+yaw+"/"+pitch;
    }
}