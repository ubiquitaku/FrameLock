package ubiquitaku.framelock;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class DataBase {
    //<StringLocation,name>
    Map<String,String> map = new HashMap<>();
    public DataBase() {
        //table作るとか色々
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

    //locationをStringに変換
    public String makeString(Location location) {
        return location.getWorld().getName()+location.getBlockX()+location.getBlockY()+location.getBlockZ();
    }
}

