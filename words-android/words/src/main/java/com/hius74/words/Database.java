package com.hius74.words;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Работа с БД
 *  Новые карточки храняться с отрицательными временами (новые карточки ближе к 0)
 *  <pre>
 *      зона новых | зона приступивших к изучению карт
 *      карт       |
 *      | | ||| |  |     |  |   | |
 *     -+-+-+++-+--+-----+--+---+-+--------------------------------> время
 *  </pre>
 *
 * @see <a href="https://developer.android.com/training/data-storage/sqlite">Save data using SQLite</a>
 * @see <a href="https://www.sqlitetutorial.net/tryit/query/sqlite-create-table/">Test SQL queries</a>
 */
public class Database {

    private static final String TAG = Database.class.getName();

    /** Тип карт по умолчанию при добавлении новых слов */
    private static final Card.TYPE DEFAULT_CARD_TYPE = Card.TYPE.ANSWER;

    /** Основной период повтора */
    private static final long REPEAT_PERIOD_MILLISECS = 3 * 60 * 60 * 1000;

    /** Защитный период (чтобы повтроять в примерно такое же время */
    private static final long SPARE_PERIOD_MILLISECS = 60 * 60 * 1000;

    /**
     * Расчет даты следующего повртора карт.
     * Реальная длителность повотора рассчитвается как удвоение счетчка.
     * Счетчик слов:
     *    -1: новое слово, еще не начато его изучение
     *     0: начато изучение карты, показан перевод слова
     *     1: закончена парвая попытка ввода слова на голландском
     *     2 и далее: последующеие успешные повторы
     *
     * @param count счетчк повторов слова (1 и более, так как неуспешные слована повторяются до тех
     *             порр пока не выучу в рамках текущего урока
     * @return время относительно текущего следующего повтора в миллисекундах.
     */
    private static long getNextRepeatPeriod(int count) {
        // Длительность повтора ограничена максимальным периодом
        if (count < 5) {
            return REPEAT_PERIOD_MILLISECS - SPARE_PERIOD_MILLISECS;
        } else {
            return 2 * REPEAT_PERIOD_MILLISECS - SPARE_PERIOD_MILLISECS;
        }
    }

    /** Для новых карт, значение по умолчанию  */
    private static final int DEFAULT_COUNT_VALUE = -1;

    static class DBHelper extends SQLiteOpenHelper {
        // If you change the database schema, you must increment the database version.
        private static final int DATABASE_VERSION = 3;
        private static final String DATABASE_NAME = "cards.db";

        public DBHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        public void onCreate(SQLiteDatabase db) {
            String sql = "CREATE TABLE cards (card_type INTEGER, front_text TEXT UNIQUE, back_text TEXT UNIQUE," +
                    "front_sentence TEXT, back_sentence TEXT, card_count INTEGER, card_next_time_check INTEGER," +
                    "total_ok INTEGER, total_error INTEGER)";
            db.execSQL(sql);
            // Индекс для поиска карт для повторения
            sql = "CREATE INDEX cards_time_check_idx ON cards(card_next_time_check)";
            db.execSQL(sql);
        }

        /**
         * Обновление БД
         * Версия БД указывается в gradle скрипте
         */
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.i(TAG, "Upgrading DB from " + oldVersion + " version to " + newVersion);
            boolean upgrade = false;
            if (oldVersion >= 1 && newVersion <= 2) {
                String sql = "ALTER TABLE cards ADD COLUMN front_sentence TEXT DEFAULT ''";
                db.execSQL(sql);
                sql = "ALTER TABLE cards ADD COLUMN back_sentence TEXT DEFAULT ''";
                db.execSQL(sql);
                Log.i(TAG, "Upgrade complete");
                upgrade = true;
            }
            if (oldVersion >= 1 && newVersion <= 3) {
                String sql = "ALTER TABLE cards ADD COLUMN total_ok INTEGER DEFAULT 0";
                db.execSQL(sql);
                sql = "ALTER TABLE cards ADD COLUMN total_error INTEGER DEFAULT 0";
                db.execSQL(sql);
                Log.i(TAG, "Upgrade complete");
                upgrade = true;
            }

            if (!upgrade) {
                throw new RuntimeException("Unknown how to upgrade from " + oldVersion + "version to " + newVersion);
            }
        }

        /**
         * Очистка всех данных в БД.
         * Используется при import из текстового файла
         */
        public void clear(SQLiteDatabase db) {
            String sql = "DELETE FROM cards";
            db.execSQL(sql);
        }
    }

    private final DBHelper dbHelper;

    public Database(Context context) {
         this.dbHelper = new DBHelper(context);
    }

    /**
     * Закрытие БД (по закрытию приложения)
     */
    public void close() {
        this.dbHelper.close();
    }

    /**
     * Очистка БД
     */
    public void clear() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        this.dbHelper.clear(db);
    }

    /**
     * Заполнение карт для повторения
     */
    public void cardsToRepeat(List<Card> cards, int numCards) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT rowid, card_type, front_text, back_text, front_sentence, back_sentence," +
                " card_count, card_next_time_check, total_ok, total_error FROM cards" +
                " WHERE card_next_time_check > 0 ORDER BY card_next_time_check LIMIT ?";
        try (Cursor cursor = db.rawQuery(sql, new String[] {Long.toString(numCards)})) {
            while (cursor.moveToNext()) {
                addToCards(cards, cursor);
            }
        }
    }

    /**
     * Выборка новых карт для заучивания
     */
    public void cardsToLearn(List<Card> cards, int numCards) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT rowid, card_type, front_text, back_text, front_sentence, back_sentence," +
                " card_count, card_next_time_check, total_ok, total_error FROM cards" +
                " WHERE card_next_time_check <= 0 ORDER BY card_next_time_check DESC LIMIT ?";
        try (Cursor cursor = db.rawQuery(sql, new String[] {Long.toString(numCards)})) {
            while (cursor.moveToNext()) {
                addToCards(cards, cursor);
            }
        }
    }

    /**
     * Вспомагательная функция добавление карты в конечный массив
     */
    private static void addToCards(List<Card> cards, Cursor cursor) {
        Card card = new Card(cursor.getLong(0), Card.TYPE.values()[cursor.getInt(1)],
                cursor.getString(2), cursor.getString(3),
                cursor.getString(4), cursor.getString(5),
                cursor.getInt(6), cursor.getLong(7),
                cursor.getInt(8), cursor.getInt(9));

        cards.add(card);
    }

    /**
     * Подсчет числа всего карт ожидаюх изучения
     */
    public int getTotalCardsToLearn() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT COUNT(*) FROM cards WHERE card_next_time_check <= 0";
        try (Cursor cursor = db.rawQuery(sql, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }

        // Если нет ни одной новой карты
        return 0;
    }

    /**
     * Подсчет числа всего карт ожидаюх изучения
     */
    public int getTotalCardsToRepeat() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT COUNT(*) FROM cards WHERE card_next_time_check > 0 AND card_next_time_check < ?";
        try (Cursor cursor = db.rawQuery(sql, new String[] {Long.toString(System.currentTimeMillis())})) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }

        // Если нет ни одной новой карты
        return 0;
    }

    public void updateCards(List<Card> cards) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "UPDATE cards SET card_count = ?, card_next_time_check = ?, total_ok = ?," +
                " total_error = ? WHERE rowid = ?";
        SQLiteStatement stmt = db.compileStatement(sql);
        long time = System.currentTimeMillis();
        db.beginTransaction();
        try {
            for (Card card : cards) {
                stmt.bindLong(1, card.count);
                stmt.bindLong(2, time + getNextRepeatPeriod(card.count));
                stmt.bindLong(3, card.totalOk);
                stmt.bindLong(4, card.totalError);
                stmt.bindLong(5, card.rowId);

                long res = stmt.executeInsert();
                Log.v(TAG, "Update card: " + card.frontText + ", result: " + res);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Добавление новых слов в БД.
     * Игнорируется добавление новых карточек с уже известной лицевой стороной
     *
     * @param cards новые карточки
     * @return число добавленых карточек
     */
    public int insertCards(List<Card> cards) {
        int count = 0;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String sql = "INSERT INTO cards(card_type, front_text, back_text, front_sentence, back_sentence," +
                " card_count, card_next_time_check, total_ok, total_error) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        SQLiteStatement stmt = db.compileStatement(sql);
        db.beginTransaction();
        try {
            for(Card card : cards) {
                stmt.bindLong(1, card.type.ordinal());
                stmt.bindString(2, card.frontText);
                stmt.bindString(3, card.backText);
                stmt.bindString(4, card.frontSentence);
                stmt.bindString(5, card.backSentence);
                stmt.bindLong(6, card.count);
                stmt.bindLong(7, card.nextCheckTime);
                stmt.bindLong(8, card.totalOk);
                stmt.bindLong(9, card.totalError);

                try {
                    long res = stmt.executeInsert();
                    ++count;
                    Log.v(TAG, "Insert card: " + card.frontText + ", result: " + res);
                } catch (SQLiteConstraintException e) {
                    Log.i(TAG, "Card already exist: " + card.frontText);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return count;
    }

    /**
     * Выборка мнимального времени для добавления новых карт.
     * Если все карты изучены, то возвращает -1
     */
    public long getMinTimeNewCard() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT MIN(card_next_time_check) FROM cards";
        try (Cursor cursor = db.rawQuery(sql, null)) {
           if (cursor.moveToFirst()) {
               return Math.min(-1, cursor.getLong(0));
           }
       }

       // Если нет ни одной новой карты
       return -1;
    }

    /**
     * Сохранение БД во внешний файл
     */
    public int exportDb(BufferedWriter writer) throws IOException {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT card_type, front_text, back_text, front_sentence, back_sentence, card_count," +
                " card_next_time_check, total_ok, total_error FROM cards";
        try (Cursor cursor = db.rawQuery(sql, null)) {
            int count = 0;
            while (cursor.moveToNext()) {
                String line = String.format(Locale.getDefault(), "%c|%s|%s|%s|%s|%d|%d|%d|%d%n",
                        Card.TYPE.values()[cursor.getInt(0)].name,
                        encodeText(cursor.getString(2)), encodeText(cursor.getString(1)),
                        encodeText(cursor.getString(4)), encodeText(cursor.getString(3)),
                        cursor.getInt(5), cursor.getLong(6), cursor.getLong(7),
                        cursor.getLong(8));
                writer.write(line);
                ++count;
            }

            return count;
        }
    }

    private static String encodeText(String text) {
        return text == null ? "" : text.replaceAll("\n", "\\\\n");
    }

    /**
     * Загрузка БД из внешний файл.
     * БД обнуляется
     */
    public int importDb(BufferedReader reader) throws IOException {
        // Обнуление БД
        this.clear();

        return importWordsFromFile(reader);
    }

    /**
     * Загрузка БД из внешний файл.
     */
    public int importWordsFromFile(BufferedReader reader) throws IOException {
        // Буфферизация записи карт
        int numCards = 50;
        List<Card> cards = new ArrayList<>(numCards);
        // Минимальное время самой старой карты
        long time = getMinTimeNewCard();
        int count = 0;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank() || line.startsWith("#")) {
                // Skip comments and empty lines
                continue;
            }
            String[] ss = line.split("\\|");
            Card.TYPE type = Card.TYPE.parse(ss[0].charAt(0));
            if (type == null) {
                // Не удалось определить тип карты по умолчанию используем по умолчанию
                type = DEFAULT_CARD_TYPE;
                // Скорее всего в файл вообще нет типа карты, сдвигаем на единицу
                String[] newSS = new String[ss.length + 1];
                System.arraycopy(ss, 0, newSS, 1, ss.length);
                ss = newSS;
            }
            if ( !type.equals(Card.TYPE.IGNORE)) {
                Card card = new Card(0,
                        type,
                        decodeText(ss[2]),
                        decodeText(ss[1]),
                        (ss.length > 4) && !ss[3].isEmpty() ? decodeText(ss[4]) : "",
                        (ss.length > 3) && !ss[4].isEmpty() ? decodeText(ss[3]) : "",
                        (ss.length > 5) && !ss[5].isEmpty() ? Integer.parseInt(ss[5]) : DEFAULT_COUNT_VALUE,
                        (ss.length > 6) && !ss[6].isEmpty() ? Long.parseLong(ss[6]) : --time,
                        (ss.length > 7) && !ss[7].isEmpty() ? Integer.parseInt(ss[7]) : 0,
                        (ss.length > 8) && !ss[8].isEmpty() ? Integer.parseInt(ss[8]) : 0);
                cards.add(card);
                if (cards.size() >= numCards) {
                    count += insertCards(cards);
                    cards.clear();
                }
            }
        }
        // Сохраняем остатки из буфера
        if (!cards.isEmpty()) {
            count += insertCards(cards);
        }

        return count;
    }

    private static String decodeText(String text) {
        return text == null || text.isEmpty() ? "" : text.replaceAll("\\\\n", "\n");
    }
}
