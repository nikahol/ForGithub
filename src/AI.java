import java.util.Map;

/**
 * Created by Nika on 04.02.2016.
 */
public class AI {
    private Map<String, Territory> owned;
    public AI(){
        owned = null;
    }
    public void addLand(Territory terr){
        owned.put(terr.getName(), terr);
    }
    public boolean owns(Territory terr){
        return owned.containsKey(terr.getName());
    }
    public Map<String, Territory> getOwned(){
        return owned;
    }
    public void obtain(Map<String, Territory> unclaimed){
        //TODO
    }
}
