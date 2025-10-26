package com.hius74.words;

import android.util.Log;

public class Card {

    private static final String TAG = Card.class.getName();

    public static final long DAY = 24 * 60 * 60 * 1000L;

    /**
     * Тип карты
     */
    public enum TYPE {
        /**
         * Карточка для игнорирования, не заргужается в БД
         */
        IGNORE('-'),
        /**
         * Переворачиваем и сверяюсь с тем, что в голове
         */
        SIMPLE('S'),
        /**
         * Требуется ввод текста для проверки. Проверяется полное совпадение текста (без учета регистра и спесимволов).
         */
        ANSWER('A'),
        /**
         * Требуется ввод текста для проверки. Проверяется полное совпадение текста (без учета регистра и спесимволов).
         * Пользователь может подтвердить считать ли ответ правильными или нет.
         * Применяется для предложений
         */
        ANSWER_EX('E');

        final char name;

        TYPE(char name) {
            this.name = name;
        }

        static TYPE parse(char name) {
            for(TYPE type : values()) {
                if (type.name == name) {
                    return type;
                }
            }
            Log.i(TAG, "Unknown type: " + name);
            return null;
        }
    }

    /**
     * Номер строки в БД. Используется для обновления карточек
     */
    final long rowId;

    final TYPE type;

    final String frontText;

    final String backText;

    final String frontSentence;

    final String backSentence;

    /**
     * Текущий счетик повтора слов (служит для расчета времени следующего повтора).
     */
    private final int count;

    private final long nextCheckTime;

    /**
     * Число ошибок за время урока
     */
    transient int errors = 0;

    public Card(long rowId, TYPE type, String frontText, String backText, String frontSentence,
                String backSentence, int count, long nextCheckTime) {
        this.rowId = rowId;
        this.type = type;
        this.frontText = frontText;
        this.backText = backText;
        this.frontSentence = frontSentence;
        this.backSentence = backSentence;
        this.count = count;
        this.nextCheckTime = nextCheckTime;
    }

    public int getCount() {
        return this.count;
    }

    public int checkErrorsAndGetCount() {
        return switch(this.errors) {
            case 0 -> this.count + 1;
            case 1 -> this.count;
            default -> Math.max(this.count - 1, 0);
        };
    }

    /**
     * Запрос ранее сохраненного в БД времени
     */
    public long getNextRepeatPeriod() {
        return this.nextCheckTime;
    }

    /**
     * Расчет даты следующего повртора карт.
     */
    public long getNextRepeatPeriod(long time) {
        long delta = switch(this.errors) {
            case 0 -> 2 * this.count * DAY; // Нет ошибок за урок, увеличиваем время повтора в 2 раза
            case 1 -> DAY; // Одна ошибка, можно повторить завтра
            default -> 0; // Много ошибок, позволяем повторить на следующем уроке
        };
        // Округляем до начала дня
        return (time + delta) / DAY * DAY;
    }
}
