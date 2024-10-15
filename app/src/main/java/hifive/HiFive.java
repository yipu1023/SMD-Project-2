package hifive;// LuckyThirteen.java

import ch.aplu.jcardgame.*;
import ch.aplu.jgamegrid.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("serial")
public class HiFive extends CardGame {

    private HashMap<Integer, Player> index_playerType;
    private int humanIndex;
    Factory factory = Factory.getInstance();

    public enum Suit {
        SPADES ("S", 20), HEARTS ("H", 15),
        DIAMONDS ("D", 10), CLUBS ("C", 5);
        private String suitShortHand = "";
        private int bonusFactor = 1;
        Suit(String shortHand, int bonusFactor) {
            this.suitShortHand = shortHand;
            this.bonusFactor = bonusFactor;
        }

        public String getSuitShortHand() {
            return suitShortHand;
        }

        public int getBonusFactor() {
            return bonusFactor;
        }
    }

    public enum Rank {
        // Reverse order of rank importance (see rankGreater() below)
        ACE (1, new int[]{1, 11, 12, 13}),
        KING (13, new int[]{1, 3, 7, 9,11,13}),
        QUEEN (12, new int[]{6,7,8,9,12}),
        JACK (11, new int[]{1,2,3,4,11}),
        TEN (10, new int[]{10}), NINE (9, new int[]{9}),
        EIGHT (8, new int[]{8}), SEVEN (7, new int[]{7}),
        SIX (6, new int[]{6}), FIVE (5, new int[]{5}),
        FOUR (4, new int[]{4}), THREE (3, new int[]{3}),
        TWO (2, new int[]{2});

        private int rankCardValue = 1;
        private int[] scoreValue = new int[]{1};
        Rank(int rankCardValue, int[] scoreValue) {
            this.rankCardValue = rankCardValue;
            this.scoreValue = scoreValue;
        }

        public int getRankCardValue() {
            return rankCardValue;
        }

        public int[] getScoreCardValue() { return scoreValue; }

        public String getRankCardLog() {
            return String.format("%d", rankCardValue);
        }
    }

    final String trumpImage[] = {"bigspade.gif", "bigheart.gif", "bigdiamond.gif", "bigclub.gif"};

    static public final int seed = 30008;
    static final Random random = new Random(seed);
    private Properties properties;
    private StringBuilder logResult = new StringBuilder();
    private List<List<String>> playerAutoMovements = new ArrayList<>();

    public boolean rankGreater(Card card1, Card card2) {
        return card1.getRankId() < card2.getRankId(); // Warning: Reverse rank order of cards (see comment on enum)
    }

    private final String version = "1.0";
    public final int nbPlayers = 4;
    public final int nbStartCards = 2;
    public final int nbFaceUpCards = 2;
    private final int handWidth = 400;
    private final int trickWidth = 40;
    private static final int FIVE_GOAL = 5;
    private static final int FIVE_POINTS = 100;
    private static final int SUM_FIVE_POINTS = 60;
    private static final int DIFFERENCE_FIVE_POINTS = 20;
    private final Deck deck = new Deck(Suit.values(), Rank.values(), "cover");
    private final Location[] handLocations = {
            new Location(350, 625),
            new Location(75, 350),
            new Location(350, 75),
            new Location(625, 350)
    };
    private final Location[] scoreLocations = {
            new Location(575, 675),
            new Location(25, 575),
            new Location(575, 25),
            // new Location(650, 575)
            new Location(575, 575)
    };
    private Actor[] scoreActors = {null, null, null, null};
    private final Location trickLocation = new Location(350, 350);
    private final Location textLocation = new Location(350, 450);
    private int thinkingTime = 2000;
    private int delayTime = 600;
    private Hand[] hands;
    public void setStatus(String string) {
        setStatusText(string);
    }

    private int[] scores = new int[nbPlayers];

    private int[] autoIndexHands = new int [nbPlayers];
    private boolean isAuto = false;
    private Hand playingArea;
    private Hand pack;

    Font bigFont = new Font("Arial", Font.BOLD, 36);

    private void initScore() {
        for (int i = 0; i < nbPlayers; i++) {
            // scores[i] = 0;
            String text = "[" + String.valueOf(scores[i]) + "]";
            scoreActors[i] = new TextActor(text, Color.WHITE, bgColor, bigFont);
            addActor(scoreActors[i], scoreLocations[i]);
        }
    }

    private int findIndexWithHigestScore(int[] scoreArray) {
        int maxScore = -1;
        int maxScoreIndex = 0;
        for (int i = 0; i < scoreArray.length; i++) {
            if (maxScore < scoreArray[i]) {
                maxScoreIndex = i;
                maxScore = scoreArray[i];
            }
        }

        return maxScoreIndex;
    }

    private void calculateScoreEndOfRound() {
        List<Integer> isThirteenChecks = Arrays.asList(0, 0, 0, 0);
        for (int i = 0; i < hands.length; i++) {
            scores[i] = scoreForHiFive(i);
        }
    }

    private void updateScore(int player) {
        removeActor(scoreActors[player]);
        int displayScore = Math.max(scores[player], 0);
        String text = "P" + player + "[" + displayScore + "]";
        scoreActors[player] = new TextActor(text, Color.WHITE, bgColor, bigFont);
        addActor(scoreActors[player], scoreLocations[player]);
    }

    private void initScores() {
        Arrays.fill(scores, 0);
    }

    private Card selected;

    private void initGame() {
        hands = new Hand[nbPlayers];
        for (int i = 0; i < nbPlayers; i++) {
            hands[i] = new Hand(deck);
        }
        playingArea = new Hand(deck);

        //正在发牌
        dealingOut(hands, nbPlayers, nbStartCards, nbFaceUpCards);
        playingArea.setView(this, new RowLayout(trickLocation, (playingArea.getNumberOfCards() + 2) * trickWidth));
        playingArea.draw();

        for (int i = 0; i < nbPlayers; i++) {
            hands[i].sort(Hand.SortType.SUITPRIORITY, false);
        }

        index_playerType = new HashMap<>();
        humanIndex = -1;

        for (int i = 0; i < nbPlayers; i++) {
            String playerString = "players." + i;
            //如果在property file中对应的players.x有写东西，则导入
            if (!properties.getProperty(playerString).isEmpty()) {
                String type = properties.getProperty(playerString);
                if (type.equals("human")) {
                    humanIndex = i;
                }else {
                    index_playerType.put(i,factory.createPlayer(type));
                }
            }
        }

        //如果property file没 human，并且property file里没填满四个player 则自己添加一个
        if (humanIndex == -1 && index_playerType.size() != 4) {
            for (int i = 0; i < nbPlayers; i++) {
                if (!index_playerType.containsKey(i)) {
                    humanIndex = i;
                    break;
                }
            }
        }

        //如果我们读完了properyt，也确保了human被添加后，map还是没满，则用 random player 补充
        int numberOfKeys = index_playerType.size();
        if (numberOfKeys < 3) {
            for (int i = 0; i < nbPlayers; i++) {
                if (!index_playerType.containsKey(i) && i != humanIndex) {
                    index_playerType.put(i,factory.createPlayer("random"));
                }
            }
        }



        //当有human player存在时
        if (numberOfKeys != 4) {
            // 将默认的0改为humanindex
            CardListener cardListener = new CardAdapter()  // Human Player plays card
            {
                public void leftDoubleClicked(Card card) {
                    selected = card;
                    hands[humanIndex].setTouchEnabled(false);
                }
            };
            hands[humanIndex].addCardListener(cardListener);
        }



        // graphics
        RowLayout[] layouts = new RowLayout[nbPlayers];
        for (int i = 0; i < nbPlayers; i++) {
            layouts[i] = new RowLayout(handLocations[i], handWidth);
            layouts[i].setRotationAngle(90 * i);
            // layouts[i].setStepDelay(10);
            hands[i].setView(this, layouts[i]);
            hands[i].setTargetArea(new TargetArea(trickLocation));
            hands[i].draw();
        }
    }


    // return random Enum value
    public static <T extends Enum<?>> T randomEnum(Class<T> clazz) {
        int x = random.nextInt(clazz.getEnumConstants().length);
        return clazz.getEnumConstants()[x];
    }

    // return random Card from ArrayList
    public static Card randomCard(ArrayList<Card> list) {
        int x = random.nextInt(list.size());
        return list.get(x);
    }

    public Card getRandomCard(Hand hand) {
        dealACardToHand(hand);

        delay(thinkingTime);

        int x = random.nextInt(hand.getCardList().size());
        return hand.getCardList().get(x);
    }

    private Rank getRankFromString(String cardName) {
        String rankString = cardName.substring(0, cardName.length() - 1);
        Integer rankValue = Integer.parseInt(rankString);

        for (Rank rank : Rank.values()) {
            if (rank.getRankCardValue() == rankValue) {
                return rank;
            }
        }

        return Rank.ACE;
    }

    private Suit getSuitFromString(String cardName) {
        String rankString = cardName.substring(0, cardName.length() - 1);
        String suitString = cardName.substring(cardName.length() - 1, cardName.length());
        Integer rankValue = Integer.parseInt(rankString);

        for (Suit suit : Suit.values()) {
            if (suit.getSuitShortHand().equals(suitString)) {
                return suit;
            }
        }
        return Suit.CLUBS;
    }


    private Card getCardFromList(List<Card> cards, String cardName) {
        Rank cardRank = getRankFromString(cardName);
        Suit cardSuit = getSuitFromString(cardName);
        for (Card card: cards) {
            if (card.getSuit() == cardSuit
                    && card.getRank() == cardRank) {
                return card;
            }
        }

        return null;
    }

    private Card applyAutoMovement(Hand hand, String nextMovement) {
        if (pack.isEmpty()) return null;
        String[] cardStrings = nextMovement.split("-");
        String cardDealtString = cardStrings[0];
        Card dealt = getCardFromList(pack.getCardList(), cardDealtString);
        if (dealt != null) {
            dealt.removeFromHand(false);
            hand.insert(dealt, true);
        } else {
            System.out.println("cannot draw card: " + cardDealtString + " - hand: " + hand);
        }

        if (cardStrings.length > 1) {
            String cardDiscardString = cardStrings[1];
            return getCardFromList(hand.getCardList(), cardDiscardString);
        } else {
            return null;
        }
    }

    private int scoreForFive(List<Card> privateCards) {
        int maxScore = 0;
        for (int i = 0; i < privateCards.size(); i++) {
            Card card = privateCards.get(i);
            Rank rank = (Rank)card.getRank();
            Suit suit = (Suit)card.getSuit();
            if (rank.getRankCardValue() == FIVE_GOAL) {
                int score = FIVE_POINTS + suit.getBonusFactor();
                if (score > maxScore) {
                    maxScore = score;
                }
            }
        }

        return maxScore;
    }

    private int scoreForSumFive(List<Card> privateCards) {
        Card card1 = privateCards.get(0);
        Card card2 = privateCards.get(1);
        Rank rank1 = (Rank)card1.getRank();
        Rank rank2 = (Rank)card2.getRank();
//        if (rank1.getScoreCardValue() + rank2.getRankCardValue() == FIVE_GOAL) {
//            Suit suit1 = (Suit)card1.getSuit();
//            Suit suit2 = (Suit)card2.getSuit();
//            return SUM_FIVE_POINTS + suit1.getBonusFactor() + suit2.getBonusFactor();
//        }

        for (int value1 : rank1.getScoreCardValue()) {
            for (int value2 : rank2.getScoreCardValue()) {
                if (value1 + value2 == FIVE_GOAL) {
                    Suit suit1 = (Suit)card1.getSuit();
                    Suit suit2 = (Suit)card2.getSuit();
                    return SUM_FIVE_POINTS + suit1.getBonusFactor() + suit2.getBonusFactor();
                }
            }
        }

        return 0;
    }

    private int scoreForDifferenceFive(List<Card> privateCards) {
        Card card1 = privateCards.get(0);
        Card card2 = privateCards.get(1);
        Rank rank1 = (Rank)card1.getRank();
        Rank rank2 = (Rank)card2.getRank();
//        if (Math.abs(rank1.getRankCardValue() - rank2.getRankCardValue()) == FIVE_GOAL) {
//            Suit suit1 = (Suit)card1.getSuit();
//            Suit suit2 = (Suit)card2.getSuit();
//            return DIFFERENCE_FIVE_POINTS + suit1.getBonusFactor() + suit2.getBonusFactor();
//        }
        for (int value1 : rank1.getScoreCardValue()) {
            for (int value2 : rank2.getScoreCardValue()) {
                if (Math.abs(value1 - value2) == FIVE_GOAL) {
                    Suit suit1 = (Suit)card1.getSuit();
                    Suit suit2 = (Suit)card2.getSuit();
                    return DIFFERENCE_FIVE_POINTS + suit1.getBonusFactor() + suit2.getBonusFactor();
                }
            }
        }
        return 0;
    }

    private int scoreForNoFive(List<Card> privateCards) {
        Card card1 = privateCards.get(0);
        Card card2 = privateCards.get(1);
        Rank rank1 = (Rank)card1.getRank();
        Rank rank2 = (Rank)card2.getRank();

        return rank1.getRankCardValue() + rank2.getRankCardValue();
    }

    private int scoreForHiFive(int playerIndex) {
        // 获取当前玩家手上的卡
        //对于wild card，我们只需要修改sum 和 diff
        List<Card> privateCards = hands[playerIndex].getCardList();
        int score1 = scoreForFive(privateCards);
        int score2 = scoreForSumFive(privateCards);
        int score3 = scoreForDifferenceFive(privateCards);
        int score4 = scoreForNoFive(privateCards);
        int [] scores = { score1, score2, score3, score4 };
        //四种得分方式中，选择最高的一个
        int finalScore = scores[findIndexWithHigestScore(scores)];
        return finalScore;
    }


    private void dealingOut(Hand[] hands, int nbPlayers, int nbCardsPerPlayer, int nbSharedCards) {
        pack = deck.toHand(false);

        for (int i = 0; i < nbPlayers; i++) {
            String initialCardsKey = "players." + i + ".initialcards";
            String initialCardsValue = properties.getProperty(initialCardsKey);
            if (initialCardsValue == null) {
                continue;
            }
            String[] initialCards = initialCardsValue.split(",");
            for (String initialCard: initialCards) {
                if (initialCard.length() <= 1) {
                    continue;
                }
                Card card = getCardFromList(pack.getCardList(), initialCard);
                if (card != null) {
                    card.removeFromHand(false);
                    hands[i].insert(card, false);
                }
            }
        }

        for (int i = 0; i < nbPlayers; i++) {
            int cardsToDealt = nbCardsPerPlayer - hands[i].getNumberOfCards();
            for (int j = 0; j < cardsToDealt; j++) {
                if (pack.isEmpty()) return;
                Card dealt = randomCard(pack.getCardList());
                dealt.removeFromHand(false);
                hands[i].insert(dealt, false);
            }
        }
    }

    private void dealACardToHand(Hand hand) {
        if (pack.isEmpty()) return;
        Card dealt = randomCard(pack.getCardList());
        dealt.removeFromHand(false);
        hand.insert(dealt, true);
    }

    private void addCardPlayedToLog(int player, List<Card> cards) {
        if (cards.size() < 2) {
            return;
        }
        logResult.append("P" + player + "-");

        for (int i = 0; i < cards.size(); i++) {
            Rank cardRank = (Rank) cards.get(i).getRank();
            Suit cardSuit = (Suit) cards.get(i).getSuit();
            logResult.append(cardRank.getRankCardLog() + cardSuit.getSuitShortHand());
            if (i < cards.size() - 1) {
                logResult.append("-");
            }
        }
        logResult.append(",");
    }

    private void addRoundInfoToLog(int roundNumber) {
        logResult.append("Round" + roundNumber + ":");
    }

    private void addEndOfRoundToLog() {
        logResult.append("Score:");
        for (int i = 0; i < scores.length; i++) {
            logResult.append(scores[i] + ",");
        }
        logResult.append("\n");
    }

    private void addEndOfGameToLog(List<Integer> winners) {
        logResult.append("EndGame:");
        for (int i = 0; i < scores.length; i++) {
            logResult.append(scores[i] + ",");
        }
        logResult.append("\n");
        logResult.append("Winners:" + String.join(", ", winners.stream().map(String::valueOf).collect(Collectors.toList())));
    }


    private void playGame() {
        // End trump suit
        int winner = 0;
        int roundNumber = 1;
        for (int i = 0; i < nbPlayers; i++) updateScore(i);

        List<Card>cardsPlayed = new ArrayList<>();
        addRoundInfoToLog(roundNumber);

        int nextPlayer = 0;
        while(roundNumber <= 4) {
            selected = null;
            boolean finishedAuto = false;

            if (isAuto) {
                int nextPlayerAutoIndex = autoIndexHands[nextPlayer];
                List<String> nextPlayerMovement = playerAutoMovements.get(nextPlayer);
                String nextMovement = "";

                if (nextPlayerMovement.size() > nextPlayerAutoIndex) {
                    nextMovement = nextPlayerMovement.get(nextPlayerAutoIndex);
                    nextPlayerAutoIndex++;

                    autoIndexHands[nextPlayer] = nextPlayerAutoIndex;
                    Hand nextHand = hands[nextPlayer];

                    // Apply movement for player
                    selected = applyAutoMovement(nextHand, nextMovement);
                    delay(delayTime);
                    if (selected != null) {
                        selected.removeFromHand(true);
                    } else {
                        selected = getRandomCard(hands[nextPlayer]);
                        selected.removeFromHand(true);
                    }
                } else {
                    finishedAuto = true;
                }
            }

            //apply our strategy
            if (!isAuto || finishedAuto) {

                // human player
                if (humanIndex == nextPlayer) {
                    hands[humanIndex].setTouchEnabled(true);

                    setStatus("Player Human is playing. Please double click on a card to discard");
                    selected = null;
                    dealACardToHand(hands[humanIndex]);
                    while (null == selected) delay(delayTime);
                    selected.removeFromHand(true);
                }
                // other bot plays
                else {
                    setStatusText("Player " + nextPlayer + " thinking...");
                    //先从getRandomcard里把这两步拿出来
                    dealACardToHand(hands[nextPlayer]);
                    System.out.println("hands number");
                    System.out.println(hands[nextPlayer].getCardList().size());
                    System.out.println("hands number-----------------");
                    delay(thinkingTime);

                    //然后play card
                    ArrayList<Card> handCards = hands[nextPlayer].getCardList();
                    selected = index_playerType.get(nextPlayer).playCard(handCards,cardsPlayed);
                    selected.removeFromHand(true);

//                    selected = getRandomCard(hands[nextPlayer]);
//                    selected.removeFromHand(true);
                }
            }

            addCardPlayedToLog(nextPlayer, hands[nextPlayer].getCardList());
            if (selected != null) {
                cardsPlayed.add(selected);
                selected.setVerso(false);  // In case it is upside down
                delay(delayTime);
                // End Follow
            }

            scores[nextPlayer] = scoreForHiFive(nextPlayer);
            updateScore(nextPlayer);
            nextPlayer = (nextPlayer + 1) % nbPlayers;

            if (nextPlayer == 0) {
                roundNumber ++;
                addEndOfRoundToLog();

                if (roundNumber <= 4) {
                    addRoundInfoToLog(roundNumber);
                }
            }

            if (roundNumber > 4) {
                calculateScoreEndOfRound();
            }
            delay(delayTime);
        }
    }

    private void setupPlayerAutoMovements() {
        String player0AutoMovement = properties.getProperty("players.0.cardsPlayed");
        String player1AutoMovement = properties.getProperty("players.1.cardsPlayed");
        String player2AutoMovement = properties.getProperty("players.2.cardsPlayed");
        String player3AutoMovement = properties.getProperty("players.3.cardsPlayed");

        String[] playerMovements = new String[] {"", "", "", ""};
        if (player0AutoMovement != null) {
            playerMovements[0] = player0AutoMovement;
        }

        if (player1AutoMovement != null) {
            playerMovements[1] = player1AutoMovement;
        }

        if (player2AutoMovement != null) {
            playerMovements[2] = player2AutoMovement;
        }

        if (player3AutoMovement != null) {
            playerMovements[3] = player3AutoMovement;
        }

        for (int i = 0; i < playerMovements.length; i++) {
            String movementString = playerMovements[i];
            List<String> movements = Arrays.asList(movementString.split(","));
            playerAutoMovements.add(movements);
        }
    }

    public String runApp() {
        setTitle("LuckyThirteen (V" + version + ") Constructed for UofM SWEN30006 with JGameGrid (www.aplu.ch)");
        setStatusText("Initializing...");
        initScores();
        initScore();
        setupPlayerAutoMovements();
        // 这里面设置了 human player
        initGame();
        playGame();

        //游戏结束，结算
        // wild card应该是从这里开始
        for (int i = 0; i < nbPlayers; i++) updateScore(i);
        int maxScore = 0;
        for (int i = 0; i < nbPlayers; i++) if (scores[i] > maxScore) maxScore = scores[i];
        List<Integer> winners = new ArrayList<Integer>();
        for (int i = 0; i < nbPlayers; i++) if (scores[i] == maxScore) winners.add(i);
        String winText;
        if (winners.size() == 1) {
            winText = "Game over. Winner is player: " +
                    winners.iterator().next();
        } else {
            winText = "Game Over. Drawn winners are players: " +
                    String.join(", ", winners.stream().map(String::valueOf).collect(Collectors.toList()));
        }
        addActor(new Actor("sprites/gameover.gif"), textLocation);
        setStatusText(winText);
        refresh();
        addEndOfGameToLog(winners);

        return logResult.toString();
    }

    public HiFive(Properties properties) {
        super(700, 700, 30);
        this.properties = properties;
        isAuto = Boolean.parseBoolean(properties.getProperty("isAuto"));
        thinkingTime = Integer.parseInt(properties.getProperty("thinkingTime", "200"));
        delayTime = Integer.parseInt(properties.getProperty("delayTime", "50"));
    }

}
