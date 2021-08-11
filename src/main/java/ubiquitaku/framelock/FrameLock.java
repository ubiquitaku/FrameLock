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
            if (args.length == 0) {
                sender.sendMessage(prefix+"======================================");
                sender.sendMessage("/flock ");
                if (sender.isOp()) {
                    sender.sendMessage("OP---------------------------------------");
                    sender.sendMessage("/flock reload : config.ymlをリロードします");
                    sender.sendMessage("/flock set <数字 > : 額縁の最大数を設定します");
                    sender.sendMessage("OP---------------------------------------");
                }
                sender.sendMessage(prefix+"======================================");
                return true;
            }
            if (!sender.hasPermission("flock.op")) {
                return true;
            }
            if (args[0].equals("reload")) {
                db.dbSave();
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
            e.getPlayer().sendMessage(prefix+"あなたはこれ以上額縁を設置することができません");
            e.setCancelled(true);
            return;
        }
        if (db.containsBlock(blockVector(e.getEntity().getLocation()))) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(prefix+"既に保護された額縁が設置されているブロックには額縁を設置できません");
            return;
        }
        db.add(e.getEntity().getLocation(),e.getPlayer().getUniqueId());
        db.addBlock(blockVector(e.getEntity().getLocation()),e.getPlayer().getUniqueId());
        e.getPlayer().sendMessage(prefix+"額縁をロックしました");
    }

//    @EventHandler
//    public void breakFrame(EntityDeathEvent e) {
//        //額縁を破壊するのが可能なプレイヤーか
//        Bukkit.broadcast(Component.text(e.getEventName()));
//        if (!entityCheck(e.getEntityType())) {
//            Bukkit.getLogger().info("debug2");
//            return;
//        }
//        try {
//            if (!Bukkit.getOnlinePlayers().contains(e.getEntity().getKiller().getPlayer())) {
//                e.setCancelled(true);
//            }
//        } catch (NullPointerException exception) {
//            e.setCancelled(true);
//            return;
//        }
//        if (!db.check(e.getEntity().getLocation(),e.getEntity().getKiller())) {
//            //だめなひとならキャンセル
//            e.setCancelled(true);
//            e.getEntity().getKiller().sendMessage(prefix+"あなたは破壊する権限を持っていません");
//            return;
//        }
//        //いいひとならキャンセルせずdbから情報を削除
//        db.remove(e.getEntity().getLocation());
//        e.getEntity().getKiller().sendMessage(prefix+"額縁が破壊されたため保護の情報を削除しました");
//    }

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
            db.removeBlock(blockVector(e.getEntity().getLocation()));
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

//    @EventHandler
//    public void detonation(ItemSpawnEvent e) {
//        Bukkit.broadcast(Component.text(e.getEventName()));
//        if ()
//        Bukkit.broadcast(Component.text(e.getEntityType().name()));
//        if (entityCheck(e.getEntityType())) {
//            e.setCancelled(true);
//        }
//
//    }

//    @EventHandler
//    public void remove(ItemSpawnEvent e) {
//        Bukkit.broadcast(Component.text("we"));
//        if (!entityCheck(e.getEntityType())) {
//            return;
//        }
//        e.setCancelled(true);
//    }

//    @EventHandler
//    public void drop(EntityDropItemEvent e) {
//        Bukkit.broadcast(Component.text("we"));
//        if (!entityCheck(e.getEntityType())) {
//            return;
//        }
//        e.setCancelled(true);
//
//    }

//    @EventHandler
//    public void combust(EntityCombustEvent e) {
//        if (!entityCheck(e.getEntityType())) {
//            return;
//        }
//        e.setCancelled(true);
//        Bukkit.broadcast(Component.text("燃焼を無効化"));
//    }

//    @EventHandler
//    public void detonation(EntityDamageEvent e) {
//        if (!entityCheck(e.getEntityType())) {
//            return;
//        }
//    }

//    @EventHandler
//    public void detonation(EntityDamageEvent e) {
//        if (!entityCheck(e.getEntityType())) {
//            return;
//        }
//    }

//    @EventHandler
//    public void remove(EntityRemoveFromWorldEvent e) {
//        //額縁か
//        if (entityCheck(e.getEntityType())) {
//            Bukkit.broadcast(Component.text("aaa"));
//            if (!db.containsDB(e.getEntity().getLocation())) {
//
//            }
//        }
//    }

    //プレイヤー以外のエンティティが額縁を破壊するのをキャンセル
//    @EventHandler
//    public void noBust(EntityChangeBlockEvent e) {
//        Bukkit.broadcast(Component.text(e.getEventName()));
//        if (blockCheck(e.getBlock())) {
//            e.setCancelled(true);
//        }
//    }

    //額縁にエンティティが与えるダメージを防ぐ
//    @EventHandler
//    public void entityDamage(EntityDamageByEntityEvent e) {
//        Bukkit.broadcast(Component.text(e.getEventName()));
//        Bukkit.broadcast(Component.text(e.getEntity().getType().name()));
//        Bukkit.broadcast(Component.text(e.getDamager().getName()));
//        if (!entityCheck(e.getEntityType())) {
//            return;
//        }
//        if (!db.checkEntity(e.getEntity().getLocation(),e.getEntity())) {
//            e.setCancelled(true);
//        }
//
////        if (!entityCheck(e.getEntityType())) {
////            return;
////        }
////        if (!db.checkEntity(e.getEntity().getLocation(),e.getEntity())) {
////            e.getDamager().sendMessage(prefix+"あなたはこの額縁を破壊できません");
////            e.setCancelled(true);
////        }
//    }


//    @EventHandler
//    public void onPlace(BlockPlaceEvent e) {
//        //額縁が置かれたらロックする&上限数超えてたらキャンセル
//        Bukkit.getLogger().info("debug0");
//        if (!blockCheck(e.getBlock())) {
//            Bukkit.getLogger().info("debug1");
//            return;
//        }
//        if (!db.count(e.getPlayer().getUniqueId())) {
//            e.getPlayer().sendMessage(prefix+"あなたはこれ以上額縁を設置することができません");
//            e.setCancelled(true);
//            return;
//        }
//        db.add(e.getBlock().getLocation(),e.getPlayer().getUniqueId());
//        e.getPlayer().sendMessage(prefix+"額縁をロックしました");
//    }

    //額縁の裏のブロックの破壊
//    @EventHandler
//    public void onFlameBreak(BlockBreakEvent e) {
//        //額縁の裏のブロックを破壊することが可能かチェック
//        if (!blockCheck(e.getBlock())) {
//            Bukkit.getLogger().info("debug20");
//            return;
//        }
//        if (!db.check(e.getBlock().getLocation(),e.getPlayer())) {
//            //だめなひとならキャンセル
//            e.setCancelled(true);
//            e.getPlayer().sendMessage(prefix+"あなたは破壊する権限を持っていません");
//            return;
//        }
//        //いいひとならキャンセルせずdbから情報を削除
//        db.remove(e.getBlock().getLocation());
//        e.getPlayer().sendMessage(prefix+"額縁が破壊されたため保護の情報を削除しました");
//    }

    //額縁のクリック
//    @EventHandler
//    public void inItem(PlayerInteractEvent e) {
//        //プレイヤーがブロックを右クリックまたは左クリックしたとき
////        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_AIR) {
//            //ブロックが額縁か
//            if (!blockCheck(e.getClickedBlock())) {
//                return;
//            }
//            //ブロックの操作が可能な人か
//            if (db.check(e.getClickedBlock().getLocation(),e.getPlayer())) {
//                //だめならキャンセル
//                e.setCancelled(true);
//                e.getPlayer().sendMessage(prefix+"なにしとんねん");
//            }
////        }
//    }

    //設定ファイル再読み込み
    public void load() {
        config = getConfig();
        max = config.getInt("max");
        db = new DataBase(this,max);
        db.dbLoad();
    }

    //額縁系アイテムならtrue
//    public boolean blockCheck(Block block) {
//        if (block.getType() == Material.ITEM_FRAME || block.getType() == Material.GLOW_ITEM_FRAME) {
//            return true;
//        }
//        return false;
//    }

    //額縁系エンティティならtrue
    public boolean entityCheck(EntityType type) {
        if (type == EntityType.ITEM_FRAME || type == EntityType.GLOW_ITEM_FRAME) {
            return true;
        }
        return false;
    }

    //額縁のlocationを送ることでその背後に存在しているブロックのlocationを返す
    public Location blockVector(Location location) {
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        if (location.getPitch() == -90) {
            y = y-1;
        } else if (location.getPitch() == 90) {
            y = y+1;
        } else {
            switch ((int) location.getYaw()) {
                case -90:
                    x = x-1;
                    break;
                case 0:
                    z = z-1;
                    break;
                case 90:
                    x = x+1;
                    break;
                default:
                    z = z+1;
            }
        }
        return new Location(location.getWorld(),x,y,z,location.getYaw(),location.getPitch());
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