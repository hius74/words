package com.hius74.words;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.hius74.words.data_source.AppDatabase;
import com.hius74.words.data_source.Card;
import com.hius74.words.data_source.CardDao;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CardEntityReadWriteTest {
    private CardDao cardDao;
    private AppDatabase db;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        cardDao = db.getWordDao();
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void writeUserAndReadInList() {
        long card1 = cardDao.insert(TestUtil.createCard("name_1", 0, 100));
        assertTrue(card1 > 0);
        long card2 = cardDao.insert(TestUtil.createCard("name_2", 1, 200));
        assertTrue(card1 != card2);
        long card3 = cardDao.insert(TestUtil.createCard("name_3_after_name_1", 0, 300, card1));
        assertTrue(card2 != card3);
        long card4 = cardDao.insert(TestUtil.createCard("name_4_after_name_2", 0, 400, card2));
        assertTrue(card3 != card4);

        List<Card> allCards = cardDao.getAll();
        assertEquals(4, allCards.size());

        // First state of learning
        List<Card> learn1 = cardDao.getCardsToLearn(250, 100);
        assertEquals(2, learn1.size());

        // Finish to learn card1
        Card card = cardDao.getById(card1);
        card.stage = 100;
        cardDao.update(card);

        // Update depending cards
        cardDao.updateNextStage(250, 100);

        // Check cards to learn now
        List<Card> learn2 = cardDao.getCardsToLearn(250, 100);
        assertEquals(3, learn2.size());
    }
}
