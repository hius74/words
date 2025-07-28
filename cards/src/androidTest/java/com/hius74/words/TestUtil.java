package com.hius74.words;

import com.hius74.words.data_source.Card;

public class TestUtil {

    private TestUtil() {}

    public static Card createCard(String name, int stage, long nextTime) {
        return createCard(name, stage, nextTime, 0);
    }

    public static Card createCard(String name, int stage, long nextTime, long parentId) {
        Card card = new Card();
        card.frontSide = "front_" + name;
        card.backSide = "back_" + name;
        card.answer = "answer_" + name;
        card.stage = stage;
        card.nextTime = nextTime;
        card.parentId = parentId;

        return card;
    }
}
