package hifive;

import ch.aplu.jcardgame.Card;

import java.util.ArrayList;
import java.util.List;

public class BasicPlayer extends Player{
    @Override
    public Card playCard(ArrayList<Card> currCards, List<Card> cardsPlayed) {
        Card resultCard = null;
        ArrayList<Card> valueCards = new ArrayList<>();
        ArrayList<Card> picCards = new ArrayList<>();
        //先拿到所有的value以及pic card
        for (Card card : currCards) {
            HiFive.Rank rank = (HiFive.Rank)card.getRank();

            if (2<=rank.getRankCardValue() && rank.getRankCardValue() <=10) {
                valueCards.add(card);
            }else {
                picCards.add(card);
            }
        }
        if (valueCards.size() != 0) {
            ArrayList<Card> smallSuitCards = new ArrayList<>();
            int smallestSuit = 1000;
            //先找最小suit
            for (Card card : valueCards) {
                HiFive.Suit suit = (HiFive.Suit)card.getSuit();

                int suitValue = suit.getBonusFactor();

                if (suitValue < smallestSuit) {
                    smallestSuit = suitValue;
                }
            }
            for (Card card : valueCards) {
                HiFive.Suit suit = (HiFive.Suit)card.getSuit();

                int suitValue = suit.getBonusFactor();

                if (suitValue == smallestSuit) {
                    smallSuitCards.add(card);
                }
            }
            //可能存在好几张最小suit的卡，打出rank最高的
            int highestRank = -1;
            for (Card card : smallSuitCards) {
                HiFive.Rank rank = (HiFive.Rank)card.getRank();
                if (rank.getRankCardValue() > highestRank) {
                    highestRank = rank.getRankCardValue();
                    resultCard = card;
                }
            }
        }
        //如果valuecards，就只考虑pic cards
        else {
            int lowestRank = 100;
            ArrayList<Card> smallRankCards = new ArrayList<>();
            for (Card card : picCards) {
                HiFive.Rank rank = (HiFive.Rank)card.getRank();

                if (rank.getRankCardValue() < lowestRank) {
                    lowestRank = rank.getRankCardValue();
                }
            }

            for (Card card : picCards) {
                HiFive.Rank rank = (HiFive.Rank)card.getRank();
                if (rank.getRankCardValue() == lowestRank) {
                    smallRankCards.add(card);
                }
            }

            //如果存在好几张最低rank，找suit最小的
            int  lowestSuit = 1000;
            for (Card card : smallRankCards) {
                HiFive.Suit suit = (HiFive.Suit)card.getSuit();
                if (suit.getBonusFactor() < lowestSuit) {
                    lowestSuit = suit.getBonusFactor();
                    resultCard = card;
                }

            }
        }

        return resultCard;
    }
}
