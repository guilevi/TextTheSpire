package textTheSpire;

import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.cards.AbstractCard;

import basemod.BaseMod;
import basemod.interfaces.PostUpdateSubscriber;
import basemod.interfaces.PreUpdateSubscriber;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.map.MapRoomNode;
import com.megacrit.cardcrawl.monsters.AbstractMonster;
import com.megacrit.cardcrawl.orbs.*;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.powers.AbstractPower;
import com.megacrit.cardcrawl.relics.AbstractRelic;
import com.megacrit.cardcrawl.relics.RunicDome;
import com.megacrit.cardcrawl.rooms.*;
import com.megacrit.cardcrawl.screens.mainMenu.MainMenuScreen;
import com.megacrit.cardcrawl.shop.ShopScreen;
import com.megacrit.cardcrawl.shop.StorePotion;
import com.megacrit.cardcrawl.shop.StoreRelic;
import com.megacrit.cardcrawl.stances.NeutralStance;
import com.megacrit.cardcrawl.ui.buttons.LargeDialogOptionButton;

import com.megacrit.cardcrawl.ui.panels.EnergyPanel;
import communicationmod.ChoiceScreenUtils;
import communicationmod.CommandExecutor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;


@SpireInitializer
public class TextTheSpire implements PostUpdateSubscriber, PreUpdateSubscriber{

    int iter;

    private Window hand;
    private Window discard;
    private Window deck;
    private Window player;
    private Window monster;
    private Window event;
    private Window relic;
    private Window map;

    private JFrame prompt;
    private JTextField promptFrame;

    private String queuedCommand = "";
    private boolean hasQueuedCommand = false;

    private boolean haveRunic = false;

    private boolean checkedSave = false;
    private boolean haveSave;

    public TextTheSpire() {

        iter = 0;
        BaseMod.subscribe(this);

        hand = new Window("Hand",300,300);
        discard = new Window("Discard",300,300);
        deck = new Window("Deck",300,300);
        player = new Window("Player",300,300);
        monster = new Window("Monster", 500, 300);
        event = new Window("Event", 300, 300);
        relic = new Window("Relic",300,300);
        map = new Window("Map", 550, 425);

        prompt = new JFrame("Prompt");
        prompt.setResizable(true);
        prompt.setSize(300, 100);
        prompt.setLocation(600, 800);
        promptFrame = new JTextField("");
        promptFrame.setEditable(true);
        prompt.setSize(300, 100);
        prompt.add(promptFrame);
        prompt.setVisible(true);

        //Sends commands to parser
        promptFrame.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(ActionEvent event) {
                queueInput(promptFrame.getText());
                promptFrame.setText("");
                promptFrame.repaint();
            }
        });

    }

    public static void initialize() {
        new TextTheSpire();
    }

    public void queueInput(String input){
        queuedCommand = input;
        hasQueuedCommand = true;
    }

    @Override
    public void receivePreUpdate() {
        if (hasQueuedCommand) {
            parsePrompt(queuedCommand);
            hasQueuedCommand = false;
        }
    }

    public void parsePrompt(String input){

        if(input.equals("quit")){
            Gdx.app.exit();
        }

        if(!CommandExecutor.isInDungeon() && CardCrawlGame.characterManager.anySaveFileExists() && input.equals("continue")){

            CardCrawlGame.mainMenuScreen.screen = MainMenuScreen.CurScreen.NONE;
            CardCrawlGame.mainMenuScreen.hideMenuButtons();
            CardCrawlGame.mainMenuScreen.darken();

            CardCrawlGame.loadingSave = true;
            CardCrawlGame.chosenCharacter = (CardCrawlGame.characterManager.loadChosenCharacter()).chosenClass;
            CardCrawlGame.mainMenuScreen.isFadingOut = true;
            CardCrawlGame.mainMenuScreen.fadeOutMusic();
            Settings.isDailyRun = false;
            Settings.isTrial = false;
            ModHelper.setModsFalse();

            return;

        }

        AbstractDungeon d = CardCrawlGame.dungeon;
        String[] tokens = input.split("\\s+");

        if (tokens[0].equals("start") && !CardCrawlGame.characterManager.anySaveFileExists()) {
            try {
                CommandExecutor.executeCommand(input);
                return;
            } catch (Exception e) {
                return;
            }
        }else if(tokens[0].equals("restart") && CardCrawlGame.characterManager.anySaveFileExists()){
            try {
                CommandExecutor.executeCommand(input.substring(2));
                return;
            } catch (Exception e) {
                return;
            }
        }

        if(!CommandExecutor.isInDungeon())
            return;

        if (tokens[0].equals("potion")) {
            try {
                CommandExecutor.executeCommand(input);
                return;
            } catch (Exception e) {
                return;
            }
        }

        if (ChoiceScreenUtils.isConfirmButtonAvailable() && input.equals(ChoiceScreenUtils.getConfirmButtonText())) {
            ChoiceScreenUtils.pressConfirmButton();
            return;
        }
        if (ChoiceScreenUtils.isCancelButtonAvailable() && input.equals(ChoiceScreenUtils.getCancelButtonText())) {
            ChoiceScreenUtils.pressCancelButton();
            return;
        }

        if (d != null && AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT) {
            input = input.toLowerCase();

            if (tokens[0].equals("play")) {
                try {
                    CommandExecutor.executeCommand(input);
                } catch (Exception e) {
                    return;
                }
            } else if (tokens[0].equals("end")) {
                try {
                    CommandExecutor.executeCommand(tokens[0]);
                } catch (Exception e) {
                    return;
                }
            }
        } else {
            int in;
            try {
                in = Integer.parseInt(input) - 1;
                ChoiceScreenUtils.executeChoice(in);
            } catch (Exception e) {
                return;
            }
        }

    }

    @Override
    public void receivePostUpdate() {

        if(iter < 30){
            iter++;
            return;
        }
        iter = 0;

        updateDeck();
        updateDiscard();
        updateHand();
        updateMonsters();
        updatePlayer();
        updateRelic();
        updateMap();

        updateEvent();

        specialUpdates();

    }

    public void updateMap(){

        if(CommandExecutor.isInDungeon() && ChoiceScreenUtils.getCurrentChoiceType() == ChoiceScreenUtils.ChoiceType.MAP) {

            String s = "";
            ArrayList<ArrayList<MapRoomNode>> m = AbstractDungeon.map;
            s += "Current= Floor:" + (AbstractDungeon.currMapNode.y+1) + " X:" + AbstractDungeon.currMapNode.x + "\r\n";

            if(AbstractDungeon.currMapNode.y == -1 || (AbstractDungeon.player.hasRelic("WingedGreaves") && (AbstractDungeon.player.getRelic("WingedGreaves")).counter > 0)) {
                for (int i = m.size() - 1; i >= (AbstractDungeon.currMapNode.y + 1); i--) {

                    s += "Floor:" + (i + 1) + " ";
                    for (MapRoomNode n : m.get(i)) {

                        if (n.hasEdges()) {
                            if (i > 0) {

                                s += nodeType(n) + n.x + "{";
                                for (MapRoomNode child : m.get(i - 1)) {
                                    if (child.hasEdges() && child.isConnectedTo(n)) {
                                        s += child.x + ",";
                                    }
                                }
                                s = s.substring(0, s.length() - 1);
                                s += "} ";

                            } else {
                                s += nodeType(n) + n.x + " ";
                            }
                        }

                    }
                    s += "\r\n";

                }
            }else{

                String limitedMap = "";
                String limitedFloor;
                String limitedNode;

                ArrayList<MapRoomNode> current = new ArrayList<MapRoomNode>();
                ArrayList<MapRoomNode> prev = new ArrayList<MapRoomNode>();

                prev.add(AbstractDungeon.currMapNode);

                for(int i=(AbstractDungeon.currMapNode.y + 1);i<m.size();i++ ){
                    limitedFloor = "Floor:" + (i+1) + " ";

                    for(MapRoomNode n : m.get(i)){
                        limitedNode = "";

                        for (MapRoomNode child : prev) {
                            if (child.isConnectedTo(n)) {
                                limitedNode += child.x + ",";
                            }
                        }
                        if(limitedNode.length() > 0) {
                            limitedNode = nodeType(n) + n.x + "{" + limitedNode.substring(0, limitedNode.length() - 1) + "} ";
                            limitedFloor += limitedNode;
                            current.add(n);
                        }

                    }

                    prev.clear();
                    prev.addAll(current);
                    current.clear();
                    limitedMap = limitedFloor + "\r\n" + limitedMap;

                }

                s += limitedMap;

            }

            map.setText(s);
            map.visible();

        }else{
            map.invisible();
        }
    }

    public void updateEvent(){

        String s = "";

        if(CommandExecutor.isInDungeon()){

            checkedSave = false;

            if(CardCrawlGame.dungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT){

                int count = 1;
                for (String c : ChoiceScreenUtils.getCurrentChoiceList()) {

                    s += count + ":" + c + " ";
                    count++;

                }

                if (s.length() > 0) {
                    event.setText(s);
                    event.visible();
                } else {
                    event.invisible();
                }

            }else{

                AbstractDungeon d = CardCrawlGame.dungeon;

                int count = 1;

                if (ChoiceScreenUtils.isConfirmButtonAvailable()) {
                    s += ChoiceScreenUtils.getConfirmButtonText() + "\r\n";
                }
                if (ChoiceScreenUtils.isCancelButtonAvailable()) {
                    s += ChoiceScreenUtils.getCancelButtonText() + "\r\n";
                }

                if (ChoiceScreenUtils.getCurrentChoiceType() == ChoiceScreenUtils.ChoiceType.EVENT) {

                    s += d.getCurrRoom().event.getClass().getSimpleName() + "\r\n";

                    if (d.getCurrRoom().event.imageEventText.optionList.size() > 0) {
                        for (LargeDialogOptionButton b : d.getCurrRoom().event.imageEventText.optionList) {
                            s += count + ":" + stripColor(b.msg) + "\r\n";
                            count++;
                        }
                    } else if (d.getCurrRoom().event.roomEventText.optionList.size() > 0) {
                        for (LargeDialogOptionButton b : d.getCurrRoom().event.roomEventText.optionList) {
                            s += count + ":" + stripColor(b.msg) + "\r\n";
                            count++;
                        }
                    }

                } else if (ChoiceScreenUtils.getCurrentChoiceType() == ChoiceScreenUtils.ChoiceType.SHOP_SCREEN) {

                    for (String c : priceShopScreenChoices()) {
                        s += count + ":" + c + " ";
                        count++;
                    }

                }else if (ChoiceScreenUtils.getCurrentChoiceType() == ChoiceScreenUtils.ChoiceType.MAP){

                    if(AbstractDungeon.firstRoomChosen)
                        s += "Floor:" + (AbstractDungeon.currMapNode.y + 1) + " X:" + AbstractDungeon.currMapNode.x + "\r\n";
                    else
                        s+= "Floor:0\r\n";

                    for(MapRoomNode n : ChoiceScreenUtils.getMapScreenNodeChoices()){
                        s += count + ":";
                        s += nodeType(n) + n.x + "\r\n";
                        count++;
                    }


                }else{
                    for (String c : ChoiceScreenUtils.getCurrentChoiceList()) {
                        s += count + ":" + c + " ";
                        count++;
                    }
                }

                event.setText(s);
                event.visible();

            }

        }else{

            if(!checkedSave) {
                if (CardCrawlGame.characterManager.anySaveFileExists()) {
                    s += "restart [class] [ascension] [seed]\r\n";
                    s += "continue\r\n";
                    haveSave = true;
                } else {
                    s += "start [class] [ascension] [seed]\r\n";
                    haveSave = false;
                }
                checkedSave = true;
            }else{
                if (haveSave) {
                    s += "restart [class] [ascension] [seed]\r\n";
                    s += "continue\r\n";
                } else {
                    s += "start [class] [ascension] [seed]\r\n";
                }
            }

            event.setText(s);
            event.visible();

        }

    }

    public String nodeType(MapRoomNode n){
        if(n.getRoom() instanceof MonsterRoomElite){
            if(n.hasEmeraldKey)
                return "EK-";
            else
                return "E-";
        }else if(n.getRoom() instanceof MonsterRoom){
            return "M-";
        }else if(n.getRoom() instanceof RestRoom){
            return "R-";
        }else if(n.getRoom() instanceof ShopRoom){
            return "S-";
        }else if(n.getRoom() instanceof TreasureRoom){
            return "T-";
        }else{
            return "U-";
        }
    }

    public static ArrayList<String> priceShopScreenChoices(){
        ArrayList<String> choices = new ArrayList<>();
        ArrayList<Object> shopItems = getAvailableShopItems();
        for (Object item : shopItems) {
            if (item instanceof String) {
                choices.add((String) item);
            } else if (item instanceof AbstractCard) {
                choices.add(((AbstractCard) item).name.toLowerCase() + "-" + ((AbstractCard) item).price);
            } else if (item instanceof StoreRelic) {
                choices.add(((StoreRelic)item).relic.name + "-" + ((StoreRelic) item).price);
            } else if (item instanceof StorePotion) {
                choices.add(((StorePotion)item).potion.name + "-" + ((StorePotion) item).price);
            }
        }
        return choices;
    }

    private static ArrayList<Object> getAvailableShopItems() {
        ArrayList<Object> choices = new ArrayList<>();
        ShopScreen screen = AbstractDungeon.shopScreen;
        if(screen.purgeAvailable && AbstractDungeon.player.gold >= ShopScreen.actualPurgeCost) {
            choices.add("purge-" + ShopScreen.actualPurgeCost);
        }
        for(AbstractCard card : ChoiceScreenUtils.getShopScreenCards()) {
            if(card.price <= AbstractDungeon.player.gold) {
                choices.add(card);
            }
        }
        for(StoreRelic relic : ChoiceScreenUtils.getShopScreenRelics()) {
            if(relic.price <= AbstractDungeon.player.gold) {
                choices.add(relic);
            }
        }
        for(StorePotion potion : ChoiceScreenUtils.getShopScreenPotions()) {
            if(potion.price <= AbstractDungeon.player.gold) {
                choices.add(potion);
            }
        }
        return choices;
    }

    public String stripColor(String input) {
        input = input.replace("#r", "");
        input = input.replace("#g", "");
        input = input.replace("#b", "");
        input = input.replace("#y", "");
        return input;
    }

    public void updateHand(){

        if(CardCrawlGame.dungeon == null || !CardCrawlGame.isInARun()){
            hand.invisible();
            return;
        }

        if(CardCrawlGame.dungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT){

            CardGroup h = CardCrawlGame.dungeon.player.hand;
            String s = "";
            int count = 1;
            for(AbstractCard c : h.group){
                s += count + ":" + c.name + "-" + handCost(c) + ", ";
                count++;
            }
            if(count > 1)
                s = s.substring(0, s.length()-2);

            s += "\r\n";

            ArrayList<AbstractPotion> pl = CardCrawlGame.dungeon.player.potions;
            count = 0;
            if (pl.size() > 0) {
                s = s + "Potions: ";
                for (AbstractPotion po : pl) {
                    s = s + count + ":" + po.name + ", ";
                    count++;
                }
                if(count > 0)
                    s = s.substring(0, s.length()-2);
                s = s + "\r\n";
            }

            hand.setText(s);
            hand.visible();

        }else{
            hand.invisible();
        }
    }

    public int handCost(AbstractCard c){
        if (c.freeToPlay()) {
            return 0;
        } else{
            return c.costForTurn;
        }
    }

    public void updateMonsters(){

        if(CardCrawlGame.dungeon == null || !CardCrawlGame.isInARun()){
            monster.invisible();
            return;
        }

        if(CardCrawlGame.dungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT){

            String s = "";
            int count = 0;

            for(AbstractMonster m : CardCrawlGame.dungeon.getCurrRoom().monsters.monsters){

                if(m.currentHealth > 0) {
                    s += count + ": " + m.name + "\r\n";
                    s += "Block: " + m.currentBlock + " ";
                    s += "HP: " + m.currentHealth + "/" + m.maxHealth + "\r\n";

                    if (!haveRunic)
                        s += monsterIntent(m);

                    ArrayList<AbstractPower> p = m.powers;
                    if(p.size() > 0) {
                        s += "Powers: ";
                        for (AbstractPower ap : p) {
                            s += ap.name + "-" + ap.amount + ", ";
                        }
                        s = s.substring(0, s.length()-2);
                    }

                    s += "\r\n\r\n";
                }
                count++;

            }

            monster.setText(s);
            monster.visible();

        }else{
            monster.invisible();
        }
    }

    public String monsterIntent(AbstractMonster m){

        AbstractMonster.Intent i = m.intent;
        int multi;

        if (i == AbstractMonster.Intent.ATTACK) {
            multi = getMulti(m);
            if(multi > 1)
                return "Intent: Attack " + m.getIntentDmg() + "x" + multi + "\r\n";
            else
                return "Intent: Attack " + m.getIntentDmg() + "\r\n";
        } else if (i == AbstractMonster.Intent.ATTACK_BUFF) {
            multi = getMulti(m);
            if(multi > 1)
                return "Intent: Attack/Buff " + m.getIntentDmg() + "x" + multi + "\r\n";
            else
                return "Intent: Attack/Buff " + m.getIntentDmg() + "\r\n";
        } else if (i == AbstractMonster.Intent.ATTACK_DEFEND) {
            multi = getMulti(m);
            if(multi > 1)
                return "Intent: Attack/Defend " + m.getIntentDmg() + "x" + multi + "\r\n";
            else
                return "Intent: Attack/Defend " + m.getIntentDmg() + "\r\n";
        } else if (i == AbstractMonster.Intent.ATTACK_DEBUFF) {
            multi = getMulti(m);
            if(multi > 1)
                return "Intent: Attack/Debuff " + m.getIntentDmg() + "x" + multi + "\r\n";
            else
                return "Intent: Attack/Debuff " + m.getIntentDmg() + "\r\n";
        } else if (i == AbstractMonster.Intent.BUFF) {
            return "Intent: Buff" + "\r\n";
        } else if (i == AbstractMonster.Intent.DEBUFF) {
            return "Intent: Debuff" + "\r\n";
        } else if (i == AbstractMonster.Intent.STRONG_DEBUFF) {
            return "Intent: Strong Debuff" + "\r\n";
        } else if (i == AbstractMonster.Intent.DEFEND) {
            return "Intent: Defend" + "\r\n";
        } else if (i == AbstractMonster.Intent.DEFEND_DEBUFF) {
            return "Intent: Defend/Debuff" + "\r\n";
        } else if (i == AbstractMonster.Intent.DEFEND_BUFF) {
            return "Intent: Defend/Buff" + "\r\n";
        } else if (i == AbstractMonster.Intent.ESCAPE) {
            return "Intent: Escape" + "\r\n";
        } else if (i == AbstractMonster.Intent.MAGIC) {
            return "Intent: MAGIC" + "\r\n";
        } else if (i == AbstractMonster.Intent.NONE) {
            return "Intent: NONE" + "\r\n";
        } else if (i == AbstractMonster.Intent.SLEEP) {
            return "Intent: Sleep" + "\r\n";
        } else if (i == AbstractMonster.Intent.STUN) {
            return "Intent: Stun" + "\r\n";
        } else if (i == AbstractMonster.Intent.UNKNOWN) {
            return "Intent: Unknown" + "\r\n";
        } else{
            return "Intent: Debug (Shouldn't Happen)" + "\r\n";
        }
    }

    public int getMulti(AbstractMonster m){

        return (int) basemod.ReflectionHacks.getPrivate(m, AbstractMonster.class, "intentMultiAmt");

    }

    public void updateDiscard(){

        if(CardCrawlGame.dungeon == null || !CardCrawlGame.isInARun()){
            discard.invisible();
            return;
        }

        if(CardCrawlGame.dungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT){

            CardGroup h = CardCrawlGame.dungeon.player.discardPile;
            String s = "";

            s += "Size: " + h.size() + "\r\n";

            if(h.size() > 0) {
                for (AbstractCard c : h.group) {
                    s += c.name + ", ";
                }
                s = s.substring(0, s.length() - 2);
            }

            discard.setText(s);
            discard.visible();

        }else{
            discard.invisible();
        }
    }

    public void updateDeck(){

        if(CardCrawlGame.dungeon == null || !CardCrawlGame.isInARun()){
            deck.invisible();
            return;
        }

        if(CardCrawlGame.dungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT){

            CardGroup h = CardCrawlGame.dungeon.player.drawPile;
            String s = "";

            s += "Size: " + h.size() + "\r\n";

            if(h.size() > 0) {
                for (AbstractCard c : h.group) {
                    s += c.name + ", ";
                }
                s = s.substring(0, s.length() - 2);
            }

            deck.setText(s);
            deck.visible();

        }else{
            CardGroup h = CardCrawlGame.dungeon.player.masterDeck;
            String s = "";

            s += "Size: " + h.size() + "\r\n";

            if(h.size() > 0) {
                for (AbstractCard c : h.group) {
                    s += c.name + ", ";
                }
                s = s.substring(0, s.length() - 2);
            }

            deck.setText(s);
            deck.visible();
        }
    }

    public void updatePlayer(){

        if(CardCrawlGame.dungeon == null || !CardCrawlGame.isInARun()){
            player.invisible();
            return;
        }

        AbstractPlayer p = CardCrawlGame.dungeon.player;
        String s = "";

        if(CardCrawlGame.dungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT){

            s += "Block: " + p.currentBlock + " ";
            s += "Health: " + p.currentHealth + "/" + p.maxHealth + "\r\n";
            s += "Energy: " + EnergyPanel.totalCount + "\r\n";

            if (p.chosenClass == AbstractPlayer.PlayerClass.DEFECT) {
                ArrayList<AbstractOrb> ol = p.orbs;
                for (AbstractOrb o : ol) {
                    if (o instanceof Dark) {
                        s += "D" + o.evokeAmount + " ";
                    } else if (o instanceof Lightning) {
                        s += "L ";
                    } else if (o instanceof Frost) {
                        s += "F ";
                    } else if (o instanceof Plasma) {
                        s += "P ";
                    } else {
                        s += "E ";
                    }
                }
                s += "\r\n";
            }

            if (!(p.stance instanceof NeutralStance)) {
                s += "Stance: " + p.stance.name + "\r\n";
            }

        }else{

            s += "Health: " + p.currentHealth + "/" + p.maxHealth + "\r\n";

            s+= "Gold:" + p.gold + "\r\n";

            ArrayList<AbstractPotion> pl = p.potions;

            if (pl.size() > 0) {
                s += "Potions: ";
                for (AbstractPotion po : pl) {
                    s += po.name + ", ";
                }
                s = s.substring(0, s.length() - 2);
            }
        }

        player.setText(s);
        player.visible();

    }

    public void updateRelic(){

        if(CardCrawlGame.dungeon == null || !CardCrawlGame.isInARun()){
            relic.invisible();
            return;
        }

        ArrayList<AbstractRelic> relics = CardCrawlGame.dungeon.player.relics;
        String s = "";

        for(AbstractRelic r : relics){

            if(!haveRunic && r instanceof  RunicDome)
                haveRunic = true;

            if(r.counter != -1){
                s += r.name + ":" + r.counter + ", ";
            }else{
                s += r.name + ", ";
            }
        }
        s = s.substring(0, s.length() - 2);

        relic.setText(s);
        relic.visible();

    }

    public void specialUpdates(){

        if(AbstractDungeon.shrineList.contains("Match and Keep!"))
            AbstractDungeon.shrineList.remove("Match and Keep!");

    }

}



















