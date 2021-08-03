package ubiquitaku.framelock;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class DataBase {
    //<StringLocation,name>
    Map<String, UUID> map = new HashMap<>();
    MySQLManager mysql;
    int count;

    public DataBase(JavaPlugin plugin,int max) {
        //table作るとか色々
        this.count = max;
        mysql = new MySQLManager(plugin,plugin.getName());
        mysql.query("loc");
        mysql.query("name");
        mysql.close();
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
        String key = null;
        String value = null;
        for (String s : map.keySet()) {
            key = key+s+"/";
        }
        for (UUID s : map.values()) {
            value = value+s+"/";
        }
        mysql.execute("insert into framelockdata (loc,uuid) values ("+key+","+value+");");
        mysql.close();
    }

    //deから読み出し
    public void dbLoad() {
        try {
            ResultSet set = mysql.query("select loc,uuid from framelockdata");
            map = new HashMap<>();
            while (set.next()) {
                String key = set.getString("loc");
                UUID uuid = UUID.fromString(set.getString("uuid"));
                // uuidやkeyはカラム名です(੭ु´･ω･`)੭ु⁾⁾
                map.put(key, uuid);
            }
            set.close();
        } catch (SQLException e) {
            Bukkit.getLogger().warning("keyの取得に失敗しました");
        }
        mysql.close();
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

    //上限数を超えていたらfalse
    public boolean count(UUID uuid) {
        if (count <= counter(uuid)) {
            return false;
        }
        return true;
    }

    //locationをStringに変換
    public String makeString(Location location) {
        return location.getWorld().getName()+location.getBlockX()+location.getBlockY()+location.getBlockZ();
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
}

