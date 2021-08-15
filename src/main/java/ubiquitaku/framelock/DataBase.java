package ubiquitaku.framelock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DataBase {
    //<StringLocation,name>
    Map<String, UUID> map = new HashMap<>();
    List<String> blockMap = new ArrayList<>();
    MySQLManager mysql;
    int count;
    String tableName;
    JavaPlugin pl;
    int period = 10; // n秒ごとに保存します
    BukkitTask task;

    public DataBase(JavaPlugin plugin,int max) {
        //table作るとか色々
        pl = plugin;
        tableName = "framelockdata";
        this.count = max;
        mysql = new MySQLManager(plugin,plugin.getName());
        createTable();
        runAutoSave();
//        mysql.execute("use " + plugin.getConfig().getString("mysql.db"));
//        mysql.close();
    }

    public void createTable() {
        mysql.execute("create table if not exists framelockdata ("+
                "loc varchar(60), " +
                "uuid varchar(60)" +
                ") engine=InnoDB default charset=utf8;");
    }

    // 定期的に保存します
    public void runAutoSave() {
        task = Bukkit.getScheduler().runTaskTimer(pl, () -> {
            mysql.execute("delete from db.framelockdata;");
            map.forEach(((loc, uuid) -> mysql.execute("insert into framelockdata (loc,uuid) values ('"+loc+"','"+uuid+"');")));
        }, period * 20L, period * 20L);
    }
    // キャンセルします
    public void cancelAutoSave() {
        if (task == null || task.isCancelled()) return;
        task.cancel();
    }

    //dbに保存
    public void saveMap() {
        new BukkitRunnable() {
            @Override
            public void run() {
                map.forEach(((loc, uuid) -> mysql.execute("insert into framelockdata (loc,uuid) values ('"+loc+"','"+uuid+"');")));
            }
        }.runTask(pl);
    }
//    public void saveMap() {
//        Bukkit.getScheduler().runTask(pl, () -> {
//            map.forEach(((loc, uuid) -> mysql.execute("insert into framelockdata (loc,uuid) values ("+loc+","+uuid+");")));
//        });
//    }

    //dbから読み出し
    public void loadMap() {
        map = new HashMap<>();
        blockMap = new ArrayList<>();
        try {
            ResultSet set = mysql.query("select * from framelockdata;");
            while (set.next()) map.put(set.getString("loc"), UUID.fromString(set.getString("uuid")));
            set.close();
        } catch (SQLException e) {
            Bukkit.getLogger().warning("frameの取得に失敗しました");
        }
        if (map.keySet().size() != 0) {
            for (String string : map.keySet()) {
                blockMap.add(makeString(blockVector(makeLocation(string))));
            }
        }

    }

    //登録
    public void add(Location location, UUID uuid) {
        map.put(makeString(location),uuid);
    }

    //削除
    public void remove(Location location) {
        map.remove(makeString(location));
    }

    //dbに保存
    public void dbSave() {
        mysql.execute("delete from db.framelockdata;");
        for (String key : map.keySet()) {
            UUID value = map.get(key);
            mysql.execute("insert into db.framerockdata (loc,uuid) values ("+key+","+value+");");
        }
        //blockの方も追加しないと…
//        mysql.close();
    }

    //dbから読み出し
    public void dbLoad() {
        try {
            ResultSet set = mysql.query("select * from "+tableName);
            map = new HashMap<>();
            while (set.next()) {
                String key = set.getString("loc");
                UUID uuid = UUID.fromString(set.getString("uuid"));
                // uuidやkeyはカラム名です(੭ु´･ω･`)੭ु⁾⁾
                map.put(key, uuid);
            }
            //blockの方も追加しないと…
            set.close();
        } catch (SQLException e) {
            Bukkit.getLogger().warning("keyの取得に失敗しました");
        }
//        mysql.close();
    }

    //額縁をいじることができる人か
    public boolean check(Location location, Player player) {
        if (!map.containsKey(makeString(location))) {
            return true;
        }
        if (map.get(makeString(location)).equals(player.getUniqueId()) || player.isOp()) {
            return true;
        }
        return false;
    }

    //上のエンティティバージョン
    public boolean checkEntity(Location location, Entity entity) {
//        for (String key : map.keySet()) {
//            Bukkit.broadcast(Component.text(key+" "+map.get(key)));
//        }
        if (!map.containsKey(makeString(location))) {
//            Bukkit.broadcast(Component.text(String.valueOf(map.get(makeString(location)))));
//            Bukkit.broadcast(Component.text("あ"));
            return true;
        }
        if (map.get(makeString(location)).equals(entity.getUniqueId()) || entity.isOp()) {
            return true;
        }
        return false;
    }

    //上限数を超えていたらfalse
    public boolean count(UUID uuid) {
        if (count <= counter(uuid)) {
            return false;
        }
        return true;
    }

    //locationをStringに変換
    public String makeString(Location location) {
        return location.getWorld().getName()+"/"+location.getBlockX()+"/"+location.getBlockY()+"/"+location.getBlockZ()+"/"+location.getYaw()+"/"+location.getPitch();
    }

    //Stringからlocation
    public Location makeLocation(String locationString) {
        String[] str = locationString.split("/");
        return new Location(Bukkit.getWorld(str[0]),Integer.parseInt(str[1]),Integer.parseInt(str[2]),Integer.parseInt(str[3]),Integer.parseInt(str[4]),Integer.parseInt(str[5]));
    }

    //何個そのプレイヤーが登録されているか
    public int counter(UUID uuid) {
        int c = 0;
        for (UUID value : map.values()){
            if (value.equals(uuid)) {
                c++;
            }
        }
        return c;
    }

    //mapに存在しているか
    public boolean containsDB(Location location) {
        if (map.containsKey(makeString(location))) {
            return true;
        }
        return false;
    }

    //debug用
    public void resetDB() {
        map = new HashMap<>();
        mysql.execute("delete from db.framelockdata");
    }

    //blockの座標を登録します
    public void addBlock(Location location,UUID uuid) {
        blockMap.add(makeBlockString(location));
    }

    //blockの座標を削除します
    public void removeBlock(Location location) {
        blockMap.remove(makeBlockString(location));
    }

    //block専用のlocationをstringに変換するものです
    public String makeBlockString(Location location) {
        return location.getWorld().getName()+"/"+location.getBlockX()+"/"+location.getBlockY()+"/"+location.getBlockZ();
    }

    //ブロック破壊できるならtrue,できないならfalse
    public boolean isBreakBlock(Location location) {
        if (!blockMap.contains(makeBlockString(location))) {
            return true;
        }
        return false;
    }

    //保護してたらtrue
    public boolean containsBlock(Location location) {
        if (blockMap.contains(makeBlockString(location))) {
            return true;
        }
        return false;
    }

    //保護だけの場所の保護を消します
    public void refresh() {
        for (String loc : map.keySet()) {
            //どうすんだろこれ…
        }
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
}

