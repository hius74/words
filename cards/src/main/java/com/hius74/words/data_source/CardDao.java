package com.hius74.words.data_source;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CardDao {

    @Query("SELECT * FROM cards WHERE id = :id")
    Card getById(long id);

    /**
     * Return ALL cards, may consume a lot of memory.
     * Used for testing.
     */
    @Query("SELECT * FROM cards")
    List<Card> getAll();

    /**
     * Get N first cards to repeat/learn.
     * @param limit max number of learn cards
     * @return cards
     */
    @Query("SELECT * FROM cards WHERE next_time < :maxTime ORDER BY next_time LIMIT :limit")
    List<Card> getCardsToLearn(long maxTime, int limit);

    @Insert()
    long insert(Card card);

    @Insert()
    long[] insert(Card... cards);

    @Update
    void update(Card... cards);

    /**
     * Update cards with finished previous stage
     * @param maxTime - maxTime to select learned cards
     * @param minStage - stage of finished learning cards
     */
    @Query("UPDATE cards SET next_time = 0 WHERE parent_id in (SELECT id FROM cards WHERE next_time <= :maxTime AND stage >= :minStage)")
    void updateNextStage(long maxTime, int minStage);

    @Delete
    void delete(Card... cards);
}
