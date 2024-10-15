package hifive;

import ch.aplu.jcardgame.Card;
import ch.aplu.jcardgame.Hand;

import java.util.ArrayList;
import java.util.List;

import static hifive.HiFive.random;

public class RandomPlayer extends Player {

    @Override
    public Card playCard(ArrayList<Card> currCards, List<Card> cardsPlayed) {
        int x = random.nextInt(currCards.size());
        return currCards.get(x);
    }

}
