package com.hius74.words;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CardTest {

    @Test
    public void getNextRepeatPeriodTest() {
        Card card = new Card(0, Card.TYPE.SIMPLE, "", "", "", "", 1, 1000);

        card.errors = 0; // Нет ошибок
        assertEquals(2 * Card.DAY, card.getNextRepeatPeriod(0));

        card.errors = 1; // Одна ошибка, повторение на следующий день
        assertEquals(Card.DAY, card.getNextRepeatPeriod(0));

        card.errors = 10; // Много ошибок
        assertEquals(0, card.getNextRepeatPeriod(0));
    }
}