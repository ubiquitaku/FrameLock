package ubiquitaku.framelock;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
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
        db.cancelAutoSave();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equals("flock")) {
            if (args.length == 0) {
                sender.sendMessage(prefix+"======================================");
//                sender.sendMessage("/flock refresh : 何らかの原因により保護を貫通して破壊された額縁がある場合その保護を削除します(作ろうと思ったけど無理でした)");
                if (sender.isOp()) {
                    sender.sendMessage("OP---------------------------------------");
                    sender.sendMessage("/flock reload : config.ymlをリロードします");
                    sender.sendMessage("/flock set <数字 > : 額縁の最大数を設定します");
                    sender.sendMessage("/flock save : おーとせーぶを無視して保存します");
                    sender.sendMessage("/flock dp : 貼り付けをキャンセルするブロックの一覧を表示します(バグってて見えません)");
                    sender.sendMessage("/flock dp <add/remove> : 貼り付けをキャンセルするブロックを追加、削除します");
                    sender.sendMessage("※メインハンドに持っているアイテム");
                    sender.sendMessage("OP---------------------------------------");
                }
                sender.sendMessage(prefix+"======================================");
                return true;
            }
            if (args[0].equals("refresh")) {
//                db.refresh((Player) sender);
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
            if (args[0].equals("save")) {
                db.runAutoSave();
            }
            if (args[0].equals("dp")) {
                if (args.length == 1) {
                    sender.sendMessage(prefix+"-----------------------------------");
                    db.dpList(sender);
                    sender.sendMessage(prefix+"-----------------------------------");
                    return true;
                }
                if (args[1].equals("add")) {
                    Player p = (Player) sender;
                    if (db.dontPlace.contains(p.getInventory().getItemInMainHand().getType())) {
                        sender.sendMessage(prefix+"既に追加されています");
                        return true;
                    }
                    db.dontPlace.add(p.getInventory().getItemInMainHand().getType());
                    sender.sendMessage(prefix+p.getInventory().getItemInMainHand().getType().name()+"を追加しました");
                    matStr();
                    saveConfig();
                    return true;
                }
                if (args[1].equals("remove")) {
                    Player p = (Player) sender;
                    if (!db.dontPlace.contains(p.getInventory().getItemInMainHand().getType())) {
                        sender.sendMessage(prefix+"登録されていないアイテムです");
                        return true;
                    }
                    db.dontPlace.remove(p.getInventory().getItemInMainHand().getType());
                    sender.sendMessage(prefix+p.getInventory().getItemInMainHand().getType().name()+"を削除しました");
                    matStr();
                    saveConfig();
                    return true;
                }
            }
            //debug用
            if (args[0].equals("reset")) {
                db.resetDB();
                sender.sendMessage("リセット完了");
            }
            //debug
            if (args[0].equals("yaw")) {
                Player p = (Player) sender;
                sender.sendMessage(prefix+p.getLocation().getYaw());
            }
            if (args[0].equals("pi")) {
                Player p = (Player) sender;
                sender.sendMessage(prefix+p.getLocation().getPitch());
            }
            //debug
            if (args[0].equals("list")) {
                sender.sendMessage(prefix+"現在使用できません");
                db.list(sender);
            }
        }
        return true;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void placeFrame(HangingPlaceEvent e) {
        //額縁が置かれたらロックする&上限数超えてたらキャンセル,設置をキャンセルするリストに入ってるのもキャンセル
        if (!entityCheck(e.getEntity().getType())) {
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
            if (e.getPlayer().isOp()) {
                e.getPlayer().sendMessage(prefix+"権限を使って保護がかかっているブロックに額縁を置きます");
                e.getPlayer().sendMessage(prefix+"この機能は額縁が何らかの理由で消滅した場合その額縁が存在した場所に貼り付けてはがすと保護の削除ができます");
                e.getPlayer().sendMessage(prefix+"§c§lこの額縁は保護されません");
                return;
            }
            e.setCancelled(true);
            e.getPlayer().sendMessage(prefix+"既に保護された額縁が設置されているブロックには額縁を設置できません");
            return;
        }
        if (db.dontPlace.contains(e.getBlock().getType())) {
            if (e.getPlayer().isOp()) {
                db.add(e.getEntity().getLocation(),e.getPlayer().getUniqueId());
                db.addBlock(db.blockVector(e.getEntity().getLocation()));
                e.getPlayer().sendMessage(prefix+"権限を使っておけないところに置いてます");
                return;
            }
            e.setCancelled(true);
            e.getPlayer().sendMessage(prefix+"そのブロックには設置できません");
            return;
        }
        db.add(e.getEntity().getLocation(),e.getPlayer().getUniqueId());
        db.addBlock(db.blockVector(e.getEntity().getLocation()));
        e.getPlayer().sendMessage(prefix+"額縁をロックしました");
    }

    //ピストンを使用不能に
    @EventHandler
    public void piston(BlockPistonExtendEvent e) {
        e.setCancelled(true);
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

    //設定ファイル読み込み
    public void load() {
        config = getConfig();
        max = config.getInt("max");
        db = new DataBase(this,max, (List<String>) config.getList("dontPlace"));
        List<Material> list = new ArrayList<>();
        for (String str : (List<String>) config.getList("dontPlace")) {
            list.add(Material.getMaterial(str));
        }
        db.dontPlace = list;
    }

    //額縁系エンティティならtrue
    public boolean entityCheck(EntityType type) {
        if (type == EntityType.ITEM_FRAME || type == EntityType.GLOW_ITEM_FRAME) {
            return true;
        }
        return false;
    }

    //List<Material>をList<String>(保存)に
    public void matStr() {
        List<String> list = new ArrayList<>();
        for (Material mat : db.dontPlace) {
            list.add(mat.name());
            Bukkit.broadcast(Component.text(mat.name()));
        }
        config.set("dontPlace",list);
        saveConfig();
    }
}