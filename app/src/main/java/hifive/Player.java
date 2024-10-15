package hifive;
import ch.aplu.jcardgame.*;
import ch.aplu.jgamegrid.*;

import java.util.ArrayList;
import java.util.List;

public abstract class Player {

    public abstract Card playCard(ArrayList<Card> currCards, List<Card> cardsPlayed);
}