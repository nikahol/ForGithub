import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by Baufritz on 23.01.2016.
 *
 * Class Game
 *
 * Handles game rounds, attacking, defending
 *
 **/

public class Game {

    private Map<String, Territory> territories;     //Map of all Territories without continent information
    private Map<String, Territory> unclaimedLand;   //Map of unclaimed territories, for setup phase
    private Map<String, Territory> Player;          //Map of Player Territories
    private Map<String, Territory> AI;              //Map of AI territories

    private LinkedList<Continent> continents;       //List of all continents

    private MapWindow currentWindow;                //MapWindow in which this game takes place

    private Territory lastClicked;
    private Territory movingTarget;
    private Territory retreatTarget;

    private int phase;                              //0: claim, 1: reinforcements, 2: attack, 3: move, 4: AI turn
    private int bonustroops;                        //reinforces calculated at beginning of turn



    //Constructor, sets up all maps, and assigns a MapWindow
    public Game(MapParser map, MapWindow window){
        territories = map.getTerritories();
        unclaimedLand = new TreeMap<>(territories);
        continents = map.getContinents();
        currentWindow = window;
        Player = new TreeMap<>();
        AI = new TreeMap<>();
        phase = 0;
        lastClicked = null;
        movingTarget = null;
        retreatTarget = null;

    }

    public void clickEvent(Territory selected, MouseEvent me){
        switch(phase){
            case 0: //beginning phase, Player and AI acquire their territories.
                acquireTerritories(selected);
                break;
            case 1: //place reinforcements
                placeReinforcements(selected);
                break;
            case 2: //attack stuff
                attackNeighbors(selected);
                break;
            case 3: //move troops
                moveTroops(selected, me);
                break;
            case 4: //AI turn
                moveAI();
                break;
            case 5:
                retreat(selected, me);
        }

        switch (GameOver()){
            case 1:
                JOptionPane.showMessageDialog(null, "You win!");
                System.exit(0);
                break;
            case 2:
                JOptionPane.showMessageDialog(null, "You lose!");
                System.exit(0);
                break;
        }
    }

    //checks if the clicked territory is unclaimed, if yes claims it
    //then moves the game to the next phase if all territories on the map are claimed
    private void acquireTerritories(Territory selected){
        if (selected.getOwner() == 0) {
            acquire(selected, 1);                                   //Player aquires cliocked territory
            acquire(getRandomTerritory(unclaimedLand), 2)  ;       //AI aquires a random free territory
        }
        if (unclaimedLand.size() == 0){
            phase = 1;
            bonustroops = calculateReinforcements(Player, continents, 1);
            currentWindow.updateMap("Available Reinforcements: " + bonustroops);
        }
    }

    //reinforcing phase, places 1 territory on the clicked territory.
    private void placeReinforcements(Territory selected){
        if(Player.containsValue(selected)){
            selected.addTroops(1);
            bonustroops--;
            currentWindow.updateMap("Available Reinforcements: " + bonustroops);
        }
        if(bonustroops == 0){
            phase = 2;
            currentWindow.updateMap("All Reinforcements deployed. Click on a friendly territory to attack a neighbor or move.");
            lastClicked = null;
        }
    }

    //attacking phase, select a territory by clicking, then click another to attack.
    //if the second clicked territory is friendly, switch to moveTroops()
    private void attackNeighbors(Territory selected){
        if(lastClicked != null && !selected.equals(lastClicked)){
            if(lastClicked.Army() > 1) {
                // actions are only possible with more than one army in a territory
                if (Player.containsValue(selected ) && lastClicked.getNeighbors().containsValue(selected)) {
                    phase = 3;
                    movingTarget = selected;
                    System.out.println(movingTarget);
                    currentWindow.updateMap("Left click to move, right click to return troops. Click anywhere outside the territory to end your turn.");
                }else if(selected.getNeighbors().containsValue(lastClicked)){//ATTACK!
                    int remaining = attack(selected, lastClicked);
                    boolean aquired = true;
                    if (selected.Army() == 0) {
                        acquire(selected, 1);
                        Player.put(selected.getName(), selected);
                        AI.remove(selected.getName());
                        selected.addTroops(remaining - 1);
                        lastClicked.addTroops(-remaining);
                        retreatTarget = selected;
                        phase = 5;
                        System.out.println(lastClicked);
                        currentWindow.updateMap("Retreat Phase; Left click to move, right lick to return. Click outside territory to end Turn");
                        aquired = false;
                    }
                    if(aquired) currentWindow.updateMap("Attack on " + selected.getName() + " From " + lastClicked.getName());

                }
                else currentWindow.updateMap("These territories are not neighbors!");
            }
            else if (Player.containsValue(selected) && lastClicked.getNeighbors().containsValue(selected)){
                phase = 3;
                movingTarget = selected;
                System.out.println(movingTarget);
                currentWindow.updateMap("Left click to move, right click to return troops. Click any other territory to end your turn.");
            }else{
                currentWindow.updateMap("Land Not a Neighbor");
            }
        }
        //first click in the window
        else if(Player.containsValue(selected)){
            if(selected.Army() == 1) {
                currentWindow.updateMap("You can't attack or move with this territory");
            }else{
                currentWindow.updateMap("Territory " + selected );
                lastClicked = selected;
            }
        }
        checkIfTurnOver();
    }

    //checks if there are any moves for the player left to do.
    public void checkIfTurnOver(){
        boolean turnOver = true;
        for(Map.Entry<String,Territory> territories: Player.entrySet()){
            if(territories.getValue().Army() > 1) turnOver = false;
        }
        if(turnOver){
            phase = 4;
            JOptionPane.showMessageDialog(null, "You have no more Actions left. Computers turn.");
        }

    }

    //moving phase, right click to add armies to clicked territory,
    //left click to send armies back to the other territory.
    //if any other territory is clicked, it move to the next phase (AI turn)
    private void moveTroops(Territory selected, MouseEvent me){
        System.out.println(selected);
        if(selected.equals(movingTarget)){
            if(me.getButton() == 1 && lastClicked.Army() > 1){  //left click
                selected.addTroops(1);
                lastClicked.addTroops(-1);
                currentWindow.updateMap();
            }
            if(me.getButton() == 3 && selected.Army() > 1){  //right click
                selected.addTroops(-1);
                lastClicked.addTroops(1);
                currentWindow.updateMap();
            }
        }
        else{
            currentWindow.updateMap("Turn Ended. Computer Playing");
            phase = 4;

            clickEvent(null, null);
        }

    }

    //same actions as moveTroops, but after conquering a territory.
    public void retreat(Territory selected, MouseEvent me){
        System.out.println(selected);
        System.out.println(lastClicked);
        if(selected.equals(retreatTarget)){
            if(me.getButton() == 1 && lastClicked.Army() > 1){  //left click
                selected.addTroops(1);
                lastClicked.addTroops(-1);
                currentWindow.updateMap();
            }
            if(me.getButton() == 3 && selected.Army() > 1){  //right click
                selected.addTroops(-1);
                lastClicked.addTroops(1);
                currentWindow.updateMap();
            }
        }
        else{
            currentWindow.updateMap("Choose new Territory to attack or move with");
            phase = 2;
            lastClicked = null;
            clickEvent(null, null);
        }

    }

    //rolls dices for attacking and handles territory ownership in case of defeat
    private int attack(Territory opponent, Territory friendly){
        int opTroops = (opponent.Army() >= 2? 2: 1);
        int friendlyTroops = (friendly.Army() >= 4 ? 3: friendly.Army()-1);

        int[] opRolls = new int[opTroops];
        for(int i = 0; i < opRolls.length; i++){
            opRolls[i] = (int)(Math.random() * 6 + 0.5);
        }

        int[] friendlyRolls = new int[friendlyTroops];
        for(int i = 0; i < friendlyRolls.length; i++){
            friendlyRolls[i] = (int)(Math.random() * 6 + 0.5);
        }

        Arrays.sort(opRolls);
        Arrays.sort(friendlyRolls);

        for(int i = 1; i <= Math.min(opRolls.length, friendlyRolls.length); i++){
            if(opRolls[opRolls.length-i] < friendlyRolls[friendlyRolls.length -i]){
                opponent.addTroops(-1);
                opTroops--;
            }else{
                friendly.addTroops(-1);
                friendlyTroops--;
            }
        }
        return friendlyTroops;

    }

    //calculates the amount of reinforces at the beginning of a turn.
    //reinforces = [number of territories] / 3 + [bonustroops if a whole continent is owned]
    private int calculateReinforcements(Map<String, Territory> territories, LinkedList<Continent> continents, int player){
        int terrbonus = territories.size()/3;

        for(Continent cont : continents){
            terrbonus += cont.getBonusTroops(player);
        }

        return terrbonus;
    }

    //acquires the chosen territory for the player
    //does not check whether or not the territory is claimed by another player
    public void acquire(Territory selected, int player){
            selected.setOwner(player);
            selected.addTroops(1);
        if(player == 1) {
            Player.put(selected.getName(), selected);
            unclaimedLand.remove(selected.getName());
        }
        if(player == 2){
            AI.put(selected.getName(), selected);
            unclaimedLand.remove(selected.getName());
        }
            currentWindow.updateMap("Player " + player + " aquired" + selected.getName());
    }

    //returns a random territory for AI turns.
    public Territory getRandomTerritory(Map<String, Territory> territories){

        int size = territories.size();
        int rand = (int)(Math.random() * (size-1) + 0.5);
        String terr = (String)territories.keySet().toArray()[rand];

        return territories.get(terr);

    }

    //checks if the game is over by checking who has how many territories
    //returns 0 if both parties own territories, 1 if the player owns all territories, 2 if AI has all territories
    public int GameOver(){

        if(Player.size() == territories.size()) return 1;
        if(AI.size() == territories.size()) return 2;

        return 0;
    }

    //Handles AI moves.
    private void moveAI(){
        System.out.println("AI TURN STARTED");

        int reinforcements = calculateReinforcements(AI, continents, 2);

        while(reinforcements-- > 0){
            String terr = getRandomTerritory(AI).getName();
            AI.get(terr).addTroops(1);
            System.out.println("AI placing 1 on " + terr);
        }

        Territory attacker = getMaxTroops(AI);
        Territory defender = getMinNeighborTroops(Player, attacker);

        int attacks = 1 +(int)(Math.random() * (attacker.Army()-1));

        for(int i = 0; i < attacks && defender.getOwner() != 2; i++){
            System.out.println("AI attacking " + defender.getName() + " with " + attacker.getName());
            int remaining = attack(defender,attacker);
            if(defender.Army() == 0){
                acquire(defender,2);
                Player.remove(defender.getName());
                AI.put(defender.getName(), defender);
                defender.addTroops(remaining - 1);
                attacker.addTroops(-remaining);
            }
        }

        phase = 1;
        bonustroops = calculateReinforcements(Player, continents,1);
        currentWindow.updateMap("Computer finished. It's your turn. you have " + bonustroops + " reinforcements");
        System.out.println("AI TURN END");

    }


    private Territory getMaxTroops(Map<String, Territory> territories){
        int maxTroops = 0;
        Territory territory = null;
        for(Map.Entry<String, Territory> Entry: territories.entrySet()){
            if(Entry.getValue().existsOpponent() && Entry.getValue().Army() > maxTroops) {
                territory = Entry.getValue();
                maxTroops = territory.Army();
            }
        }
        return territory;
    }

    private Territory getMinNeighborTroops(Map<String, Territory> opponent, Territory attacker){
        int minTroops = -1;
        Territory territory = null;
        for(Map.Entry<String, Territory> Entry: opponent.entrySet()){
            if(attacker.getNeighbors().containsValue(Entry.getValue())) {
                if (minTroops == -1) {
                    territory = Entry.getValue();
                    minTroops = territory.Army();
                }
                if (Entry.getValue().Army() < minTroops) {
                    territory = Entry.getValue();
                    minTroops = territory.Army();
                }
            }
        }
        return territory;
    }

}
