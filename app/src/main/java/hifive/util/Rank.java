package hifive.util;

public enum Rank {
    // Reverse order of rank importance (see rankGreater() below)
    ACE(1, new int[]{1, 11, 12, 13}),
    KING(13, new int[]{1, 3, 7, 9, 11, 13}),
    QUEEN(12, new int[]{6, 7, 8, 9, 12}),
    JACK(11, new int[]{1, 2, 3, 4, 11}),
    TEN(10, new int[]{10}), NINE(9, new int[]{9}),
    EIGHT(8, new int[]{8}), SEVEN(7, new int[]{7}),
    SIX(6, new int[]{6}), FIVE(5, new int[]{5}),
    FOUR(4, new int[]{4}), THREE(3, new int[]{3}),
    TWO(2, new int[]{2});

    private int rankCardValue;
    private int[] scoreValue;

    Rank(int rankCardValue, int[] scoreValue) {
        this.rankCardValue = rankCardValue;
        this.scoreValue = scoreValue;
    }

    public int getRankCardValue() {
        return rankCardValue;
    }

    public int[] getScoreCardValue() {
        return scoreValue;
    }

    public String getRankCardLog() {
        return String.format("%d", rankCardValue);
    }
}
