package ubiquitaku.framelock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DataBase {
    //<StringLocation,name>
    Map<String, UUID> map = new HashMap<>();
    Map<String, UUID> blockMap = new HashMap<>();
    MySQLManager mysql;
    int count;
    String tableName;

    public DataBase(JavaPlugin plugin,int max) {
        //table作るとか色々
        tableName = "framelockdata";
        this.count = max;
        mysql = new MySQLManager(plugin,plugin.getName());
//        mysql.execute("use " + plugin.getConfig().getString("mysql.db"));
//        mysql.close();
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
        mysql.reConnect();
        mysql.execute("delete from db.framelockdata");
        for (String key : map.keySet()) {
            UUID value = map.get(key);
            mysql.execute("insert into "+tableName+" (loc,uuid) values ("+key+","+value+");");
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
        for (String key : map.keySet()) {
//            Bukkit.broadcast(Component.text(key+" "+map.get(key)));
        }
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
        blockMap.put(makeBlockString(location),uuid);
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
        if (!blockMap.containsKey(makeBlockString(location))) {
            return true;
        }
        return false;
    }

    //保護してたらtrue
    public boolean containsBlock(Location location) {
        if (blockMap.containsKey(makeBlockString(location))) {
            return true;
        }
        return false;
    }
}

