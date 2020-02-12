package textTheSpire;

import com.megacrit.cardcrawl.cards.AbstractCard;
import com.megacrit.cardcrawl.cards.CardGroup;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.potions.AbstractPotion;
import com.megacrit.cardcrawl.rooms.AbstractRoom;
import org.eclipse.swt.widgets.Display;

import java.util.ArrayList;

public class Hand {

    public Window hand;


    public Hand(Display display){
        hand = new Window(display,"Hand",300,300);
    }

    public void update(){

        StringBuilder s = new StringBuilder();

        //If not in dungeon
        if(CardCrawlGame.dungeon == null || !CardCrawlGame.isInARun()){
            hand.setText(s.toString());
            return;
        }

        //If in combat
        if(AbstractDungeon.getCurrRoom().phase == AbstractRoom.RoomPhase.COMBAT){

            CardGroup h = AbstractDungeon.player.hand;

            int count = 1;
            for(AbstractCard c : h.group){
                s.append(count).append(":").append(c.name).append("-").append(handCost(c)).append(", ");
                count++;
            }
            if(count > 1)
                s = new StringBuilder(s.substring(0, s.length() - 2));

            s.append("\r\n");

            ArrayList<AbstractPotion> pl = AbstractDungeon.player.potions;
            count = 0;
            if (pl.size() > 0) {
                s.append("Potions: ");
                for (AbstractPotion po : pl) {
                    s.append(count).append(":").append(po.name).append(", ");
                    count++;
                }
                if(count > 0)
                    s = new StringBuilder(s.substring(0, s.length() - 2));
                s.append("\r\n");
            }

            hand.setText(s.toString());

        }else{
            //If not in combat
            hand.setText(s.toString());
        }
    }

    /*
    Params:
        c - Any card in your hand
    Returns:
        Current cost of c
     */
    public int handCost(AbstractCard c){
        if (c.freeToPlay()) {
            return 0;
        } else{
            return c.costForTurn;
        }
    }


}