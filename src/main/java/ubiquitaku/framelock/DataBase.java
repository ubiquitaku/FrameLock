package ubiquitaku.framelock;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataBase {
    //<StringLocation,name>
    Map<String,String> map = new HashMap<>();
    MySQLManager mysql;
    int count;

    public DataBase(JavaPlugin plugin,int max) {
        //table作るとか色々
        this.count = max;
        mysql = new MySQLManager(plugin,plugin.getName());
        mysql.query("loc");
        mysql.query("name");
    }

    //登録
    public void add(Location location, String name) {
        map.put(makeString(location),name);
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
        for (String s : map.values()) {
            value = value+s+"/";
        }
        mysql.execute("insert into FlameLock (loc,name) values ("+key+","+value+");");
    }

    //deから読み出し
    public void dbLoad() {
        mysql.reConnect();
        List<String> key = new ArrayList<>();
        List<String> value = new ArrayList<>();
        for (String s : )
    }

    //額縁をいじることができる人か
    public boolean check(Location location, Player player) {
        if (!map.containsValue(location)) {
            return true;
        }
        if (map.get(makeString(location)).equals(player.getName()) || player.isOp()) {
            return true;
        }
        return false;
    }

    //上限数を超えていたらfalse
    public boolean count(String name) {
        if (count <= counter(name)) {
            return false;
        }
        return true;
    }

    //locationをStringに変換
    public String makeString(Location location) {
        return location.getWorld().getName()+location.getBlockX()+location.getBlockY()+location.getBlockZ();
    }

    //何個そのプレイヤー名が登録されているか
    public int counter(String name) {
        int c = 0;
        for (String value : map.values()){
            if (value.equals(name)) {
                c++;
            }
        }
        return c;
    }
}

