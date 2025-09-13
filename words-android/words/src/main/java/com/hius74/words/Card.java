package com.hius74.words;

import android.util.Log;

public class Card {

    private static final String TAG = Card.class.getName();

    /**
     * Тип карты
     */
    enum TYPE {
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
    int count;

    long nextCheckTime;

    /**
     * Общее число правильных ответов за все время
     */
    int totalOk;

    /**
     * Общее число ошибочных ответов за все время
     */
    int totalError;

    public Card(long rowId, TYPE type, String frontText, String backText, String frontSentence,
                String backSentence, int count, long nextCheckTime, int totalOk, int totalError) {
        this.rowId = rowId;
        this.type = type;
        this.frontText = frontText;
        this.backText = backText;
        this.frontSentence = frontSentence;
        this.backSentence = backSentence;
        this.count = count;
        this.nextCheckTime = nextCheckTime;
        this.totalOk = totalOk;
        this.totalError = totalError;
    }
}
