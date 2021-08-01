package ubiquitaku.framelock;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class DataBase {
    //<StringLocation,name>
    Map<String,String> map = new HashMap<>();
    int count;
    public DataBase(int max) {
        //table作るとか色々
        this.count = max;
    }

    //登録
    public void add(Location location, String name) {
        map.put(makeString(location),name);
    }

    //削除
    public void remove(Location location) {
        map.remove(makeString(location));
        //dbからも削除
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

