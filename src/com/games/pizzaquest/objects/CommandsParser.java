package com.games.pizzaquest.objects;

import com.games.pizzaquest.app.PizzaQuestApp;

import java.util.ArrayList;
import java.util.List;

import static com.games.pizzaquest.objects.MusicPlayer.*;

public class CommandsParser {
    private static ArrayList<Item> itemList = (ArrayList<Item>) ExternalFileReader.getItemListFromJson();
    private static double turns;
    private static int reputation;

    public static boolean processCommands(List<String> verbAndNounList, Gamestate gamestate, GameWindow window) {
        boolean validCommand = true;
        String noun = verbAndNounList.get(verbAndNounList.size() - 1);
        String verb = verbAndNounList.get(0);
        ArrayList<String> validDirections = new ArrayList<>();
        validDirections.add("north");
        validDirections.add("east");
        validDirections.add("west");
        validDirections.add("south");

        switch (verb) {
            case "quit":
                quitGame();
                break;
            case "go":
                if (noun.equals("") || !validDirections.contains(noun)) {
                    validCommand = false;
                    break;
                }

                String nextLoc = gamestate.getPlayerLocation().getNextLocation(noun);

                if (!nextLoc.equals("nothing")) {
                    gamestate.setPlayerLocation(PizzaQuestApp.getGameMap().get(nextLoc.toLowerCase()));
                    String message = String.format("You travel %s to %s.\n", noun, gamestate.getPlayerLocation().getName()) +
                            ("\n" + gamestate.getPlayerLocation().getDescription());
                    window.getGameLabel().setText(message);
                } else {
                    String message = window.getGameLabel().getText() +
                            String.format("\n\nThere is nothing to the %s.", noun);
                    window.getGameLabel().setText(message);
                    break;
                }

                increaseTurnCounter(gamestate);
                window.getLocationLabel().setText(window.setLocationLabel(gamestate));
                window.getInventoryLabel().setText(window.setInventoryLabel(gamestate));

                break;
            case "look":
                Item item = ExternalFileReader.getSingleItem(noun);

                if (itemList.contains(item)) {
                    window.getGameLabel().setText(item.getDescription());
                } else if (gamestate.getPlayerLocation().npc != null && gamestate.getPlayerLocation().npc.getName().equals(noun)) {
                    window.getGameLabel().setText(gamestate.getPlayerLocation().npc.getNpcDescription());
                } else {
                    window.getGameLabel().setText(gamestate.getPlayer().look(gamestate.getPlayerLocation()));
                }
                break;
            case "take":
                boolean itemFound = false;
                String message = null;
                //add item to inventory
                for (Item i : gamestate.getPlayerLocation().getItems()) {
                    if (i.getName().equals(noun) && !gamestate.isGodMode()) {
                        gamestate.getPlayer().addToInventory(noun);
                        itemFound = true;
                    }
                    else if (gamestate.isGodMode() && itemList.contains(i)){
                        gamestate.getPlayer().addToInventory(noun);
                        itemFound = true;
                    }
                }

                gamestate.getPlayerLocation().getItems().removeIf(i -> i.getName().equals(noun));

                if (!itemFound) {
                    if (gamestate.isGodMode()){
                        message = "\n\nEven with your godly powers, you cannot add " + noun + " to your inventory";
                    }
                    else {
                        message = "\n\nYou try to take the " + noun + " but you don't see it.";
                        validCommand = false;
                    }
                }
                else {
                    message = "\n\nYou take the " + noun;
                }

                window.getGameLabel().setText(window.getGameLabel().getText() + message);
                window.getInventoryLabel().setText(window.setInventoryLabel(gamestate));
                break;
            case "talk":
                //add item to inventory
                talk(gamestate, noun);
                String npcTalk = noun;
                npcTalk = String.format("\n\nYou approach %s and start a conversation.\n\n", npcTalk);
                if (npcTalk != null) {
                    StringBuilder currentText = new StringBuilder(window.getGameLabel().getText());
                    currentText.append(npcTalk);
                    currentText.append(CommandsParser.talk(gamestate, verbAndNounList.get(1)));
                    window.getGameLabel().setText(currentText.toString());
                }
                else{
                    window.getGameLabel().setText("Who is " + noun + "? They are not in this location. Are you okay?");
                }
                break;
            case "give":
                //removes item from inventory
                if (noun.equals("")) {
                    validCommand = false;
                    break;
                }
                if (gamestate.getPlayerLocation().npc != null && gamestate.getPlayer().getInventory().contains(new Item(noun))) {
                    int repAdd = gamestate.getPlayerLocation().npc.processItem(noun);
                    if (repAdd > 0) {
                        reputation += repAdd;
                        window.updateReputation(reputation);
                        gamestate.getPlayer().removeFromInventory(noun);
                        window.getGameLabel().setText(window.getGameLabel().getText() +
                                String.format("\n\nYou give the %s to %s. \nThey thank you and your reputation increases!",
                                              noun, gamestate.getPlayerLocation().npc.getName()));
                    }
                    else {
                        window.getGameLabel().setText(window.getGameLabel().getText() +
                              String.format("\n\n%s is uninterested with that item.", gamestate.getPlayerLocation().npc.getName()));
                    }

                }
                else{
                    window.getGameLabel().setText(window.getGameLabel().getText() +
                            String.format("\n\n%s is not in your inventory therefore you cannot give it away!",
                                    noun));
                }

                window.getInventoryLabel().setText(window.setInventoryLabel(gamestate));
                break;
            case "help":
                    window.getGameLabel().setText(ExternalFileReader.gameInstructions(gamestate));
                break;
            case "reset":
                //resetGame();
                break;
            case "mute":
                stopMusic();
                break;
            case "unmute":
                if(!clip.isRunning()){
                    playMusic(window.getCurrentVolume());
                }
                break;
            case "god":
                gamestate.setGodMode(true);
                break;
            default:
                String response = String.format("\n\nI don't understand '%s'%n\n", verbAndNounList) +
                        "Type help if you need some guidance on command structure!";
                window.getGameLabel().setText(response);
                validCommand = false;
                break;
        }
        // Make a gameover check after the command is processed.
        gamestate.checkGameOver((int) turns, reputation);
        return validCommand;
    }

    public static void quitGame() {
        System.exit(0);
    }

    private static String talk(Gamestate gamestate, String noun) {
        Location playerLocation = gamestate.getPlayerLocation();
        if (playerLocation.npc != null && playerLocation.npc.getName().equals(noun)) {
            return playerLocation.npc.giveQuest();
        }

        return String.format("Wait a minute, there is no one name \"%s\" here. Who are you trying to talk to?", noun);
    }

    public static ArrayList<Item> getItemList() {
        return itemList;
    }

    public static double getTurns() {
        return turns;
    }

    public static int getReputation() {
        return reputation;
    }

    private static void increaseTurnCounter(Gamestate gamestate){
        if (gamestate.isGodMode()) {
            turns = 0;
        }
        else if (gamestate.getPlayer().getInventory().contains(new Item("moped"))){
            turns += 0.25;
        }
        else if (gamestate.getPlayer().getInventory().contains(new Item("horse"))){
            turns += 0.50;
        }
        else {
            turns += 1;
        }
    }

}