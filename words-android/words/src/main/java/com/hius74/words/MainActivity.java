package com.hius74.words;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getName();

    /** Число карт для повторения за раз */
    private static final int NUM_TOTAL_CARDS = 50;

    /** Число новых карт для заучивания за раз */
    private static final int NUM_LEARN_CARDS = 10;

    /** Команда выбора нового словаря файла */
    private static final int ACTION_IMPORT_NEW_WORDS = 1;

    /** Экспорт БД на внешний носитель */
    private static final int ACTION_EXPORT_DB = 2;

    /** Import БД с внешнего носителя */
    private static final int ACTION_IMPORT_DB = 3;

    @SuppressLint("ConstantLocale")
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd hh:mm", Locale.getDefault());

    /** БД для работы с картами */
    private Database database;

    /** Список карт на урок */
    private final List<Card> cards = new ArrayList<>(NUM_TOTAL_CARDS);

    /** Позиция текущей карты */
    private int currentCardIdx;

    /** Указатель на текущую карточку */
    private Card card;

    /** Элементы управления */
    private ProgressBar progressBar;
    private TextView frontCardText;
    private TextView backCardText;
    private Button rollButton;
    private EditText answerEdit;
    private TextView answerText;
    private Button okButton;
    private Button errorButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.database = new Database(getApplicationContext());

        // Элементы управления
        this.progressBar = findViewById(R.id.progressBar);
        this.frontCardText = findViewById(R.id.frontTextView);
        this.backCardText = findViewById(R.id.backTextView);
        // Элементы простой карты (переворачиваю и сверяюсь со своим знанием)
        this.rollButton = findViewById(R.id.rollCardButton);
        // Элементы для карты ANSWER
        this.answerEdit = findViewById(R.id.inputAnswerText);
        this.answerText = findViewById(R.id.answerText);
        // Общие элементы для SIMPLE и ANSWER
        this.okButton = findViewById(R.id.okCardButton);
        this.errorButton = findViewById(R.id.errorCardButton);

        // Устанавливаем листенеры
        this.frontCardText.setOnClickListener(view -> {
            if (this.card != null && (!this.card.frontText.isEmpty()) && (!this.card.frontSentence.isEmpty())) {
                CharSequence text = this.frontCardText.getText();
                if (text != null && text.equals(this.card.frontText)) {
                    this.frontCardText.setText(this.card.frontSentence);
                } else {
                    this.frontCardText.setText(this.card.frontText);
                }
            }
        });
        this.backCardText.setOnClickListener(view -> {
            if (this.card != null && (!this.card.backText.isEmpty()) && (!this.card.backSentence.isEmpty())) {
                CharSequence text = this.backCardText.getText();
                if (text != null && text.equals(this.card.backText)) {
                    this.backCardText.setText(this.card.backSentence);
                } else {
                    this.backCardText.setText(this.card.backText);
                }
            }
        });
        this.rollButton.setOnClickListener(view -> rollCard());
        this.answerEdit.setOnEditorActionListener((view, actionId, event) -> {
            // Hide keyboard
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            // imm.hideSoftInputFromWindow(this.answerEdit.getWindowToken(), InputMethodManager.RESULT_HIDDEN);
            // Check the answer
            checkAnswer();
            return true;
        });
        this.answerText.setOnClickListener(view -> {
            switch(card.type) {
                case ANSWER:
                    processWrongAnswer();
                    break;

                case ANSWER_EX:
                    processWrongAnswerEx();
                    break;

                default:
                    Log.e(TAG, "Unhandled card type: " + card.type);
                    break;
            }
        });
        this.okButton.setOnClickListener(view -> knownAnswer());
        this.errorButton.setOnClickListener(view -> unknownAnswer());
    }

    @Override
    protected void onDestroy() {
        this.database.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Android bug: switch not working
        if (item.getItemId() == R.id.menu_start || item.getItemId() == R.id.menu_repeat_old_words) {
            startRepeatCards();
            return true;
        } else if (item.getItemId() == R.id.menu_learn_new_words) {
            startLearnCards();
            return true;
        } else if (item.getItemId() == R.id.menu_import_new_words) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            startActivityForResult(intent, ACTION_IMPORT_NEW_WORDS);
            return true;
        } else if (item.getItemId() == R.id.menu_db_export) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, "words_export.txt");
            startActivityForResult(intent, ACTION_EXPORT_DB);
            return true;
        } else if (item.getItemId() == R.id.menu_db_import) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TITLE, "words_import.txt");
            startActivityForResult(intent, ACTION_IMPORT_DB);
            return true;
        } else if (item.getItemId() == R.id.menu_db_clear) {
            clearDb();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Обработка результата выбора файлов
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == ACTION_IMPORT_NEW_WORDS && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                try {
                    importNewWords(resultData.getData());
                } catch (IOException e) {
                    Log.e(TAG, "Exception on add new dictionary", e);
                    Toast.makeText(getApplicationContext(), "Exception: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        } else if (requestCode == ACTION_EXPORT_DB && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                try {
                    exportDb(resultData.getData());
                } catch (IOException e) {
                    Log.e(TAG, "Exception on export DB", e);
                    Toast.makeText(getApplicationContext(), "Exception: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        } else if (requestCode == ACTION_IMPORT_DB && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                try {
                    importDb(resultData.getData());
                } catch (IOException e) {
                    Log.e(TAG, "Exception on import DB", e);
                    Toast.makeText(getApplicationContext(), "Exception: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Чтение файла новых слов и добавление его в БД
     * Каждая строка состоит из 2 элементов, разделенных |
     * - передняя сторона карты
     * - задняя сторока карты
     * Чтобы учить прямой и обратный перевод, добавляем карты парами
     */
    private void importNewWords(Uri uri) throws IOException {
        try (InputStream input = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
            int res = this.database.importWordsFromFile(reader);
            Toast.makeText(this, "Добавлено " + res + " новых карточек", Toast.LENGTH_LONG).show();
        }
    }

    private void clearDb() {
        this.database.clear();
        Toast.makeText(this, "БД очищена", Toast.LENGTH_LONG).show();
    }

    private void exportDb(Uri uri) throws IOException {
        int count;
        try (OutputStream outputStream = getContentResolver().openOutputStream(uri);
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            count = this.database.exportDb(writer);
        }
        Toast.makeText(this, "Exported DB " + count + " cards ended successful.", Toast.LENGTH_LONG).show();
    }

    private void importDb(Uri uri) throws IOException {
        int count;
        try (InputStream inputStream = getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            count = this.database.importDb(reader);
        }
        Toast.makeText(this, "Imported DB " + count + " cards ended successful", Toast.LENGTH_LONG).show();
    }

    /**
     * Перемешать карточки.
     * Допускаются, что некоторые карты останутся на своих местах
     */
    private void shuffle() {
        for (int i = 0; i < this.cards.size(); i++) {
            int r = (int)(Math.random() * this.cards.size());
            Card card = this.cards.get(i);
            this.cards.set(i, cards.get(r));
            this.cards.set(r, card);
        }
    }

    /**
     * Повтор самых старых карт.
     * Перемешивание
     */
    private void startRepeatCards() {
        this.cards.clear();
        int total = this.database.getTotalCardsToRepeat();
        this.database.cardsToRepeat(this.cards, NUM_TOTAL_CARDS);

        if (this.cards.isEmpty()) {
            Date time = new Date(this.database.getMinTimeNewCard());
            Toast.makeText(this, "Нет карт: " + DATE_FORMAT.format(time), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Карт для повтора: " + total, Toast.LENGTH_LONG).show();
            shuffle();
            this.progressBar.setMax(this.cards.size());
            this.progressBar.setVisibility(View.VISIBLE);
            this.frontCardText.setVisibility(View.VISIBLE);
            this.currentCardIdx = 0;
            nextCard();
        }
    }

    /**
     * Изучение новых карт.
     * Перемешивание
     */
    private void startLearnCards() {
        this.cards.clear();
        int total = this.database.getTotalCardsToLearn();
        this.database.cardsToLearn(this.cards, NUM_LEARN_CARDS);

        if (this.cards.isEmpty()) {
            Toast.makeText(this, "Нет карт для изучения", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Карт для изучения: " + total, Toast.LENGTH_LONG).show();
            shuffle();
            this.progressBar.setMax(this.cards.size());
            this.progressBar.setVisibility(View.VISIBLE);
            this.frontCardText.setVisibility(View.VISIBLE);
            this.currentCardIdx = 0;
            nextCard();
        }
    }

    /**
     * Показ лицевой стороны карты
     */
    private void nextCard() {
        if (this.currentCardIdx >= this.cards.size()) {
            // Окончание карточек
            // Для отладки - проверяем что все счетчики правильные
            for (Card c : this.cards) {
                if (c.count <= 0) {
                    Toast.makeText(this, "Ошибка в карте: " + card.frontText, Toast.LENGTH_SHORT).show();
                }
            }
            // Сохраняем в БД обновленный статус числа повторов
            this.database.updateCards(this.cards);
            this.progressBar.setVisibility(View.GONE);
            this.frontCardText.setVisibility(View.GONE);
            Toast.makeText(this, "Урок окончен", Toast.LENGTH_LONG).show();
        } else {
            this.progressBar.setProgress(this.currentCardIdx + 1);
            this.card = this.cards.get(this.currentCardIdx);
            this.frontCardText.setText(this.card.frontText);
            switch (this.card.type) {
                case SIMPLE:
                    this.rollButton.setVisibility(View.VISIBLE);
                    break;

                case ANSWER:
                case ANSWER_EX:
                    if (card.count < 0) {
                        // Карта показывается первый раз (делаем как Simple)
                        this.rollButton.setVisibility(View.VISIBLE);
                    } else {
                        // Последующие показы
                        this.answerEdit.getText().clear();
                        this.answerEdit.setVisibility(View.VISIBLE);
                        this.answerEdit.requestFocus();
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(this.answerEdit, InputMethodManager.SHOW_IMPLICIT);
                    }
                    break;

                default:
                    Log.w(TAG, "Unknown card type: " + this.card.type);
                    break;
            }
        }
    }

    /**
     * Переворачивание простой карты
     */
    private void rollCard() {
        this.backCardText.setVisibility(View.VISIBLE);
        this.backCardText.setText(this.card.backText);
        this.rollButton.setVisibility(View.GONE);
        this.okButton.setVisibility(View.VISIBLE);
        this.errorButton.setVisibility(View.VISIBLE);
    }

    /**
     * Проверка ввода карты типа ANSWER и ANSWER_EX
     */
    private void checkAnswer() {
        String answer = this.answerEdit.getText().toString();
        if (checkAnswerText(this.card.backText, answer)) {
            // Ответ правильный
            this.answerEdit.setVisibility(View.GONE);
            ++this.card.count;
            ++this.card.totalOk;
            // Если карта показывалась первый раз, то помещаем ее в конец
            if (this.card.count <= 0) {
                // Помещаем ее в конец
                Card card = this.cards.remove(this.currentCardIdx);
                this.cards.add(card);
            } else {
                // Переходим к следующей карте
                ++this.currentCardIdx;
            }
            nextCard();
        } else {
            // Ответ не правильный, показываем ответ и предложение утвердить правильный ли ответ или нет
            this.backCardText.setVisibility(View.VISIBLE);
            this.backCardText.setText(this.card.backText);
            this.answerEdit.setVisibility(View.GONE);
            this.answerText.setText(answer);
            this.answerText.setVisibility(View.VISIBLE);
            if (this.card.type == Card.TYPE.ANSWER_EX) {
                this.okButton.setVisibility(View.VISIBLE);
                this.errorButton.setVisibility(View.VISIBLE);
            }
        }
    }

    private void processWrongAnswer() {
        this.backCardText.setVisibility(View.GONE);
        this.answerText.setVisibility(View.GONE);
        // Уменьшаем счетчик
        if (this.card.count >= 0) {
            --this.card.count;
        }
        ++this.card.totalError;
        // и помещаем карту в конец
        Card card = this.cards.remove(this.currentCardIdx);
        this.cards.add(card);
        nextCard();
    }

    /**
     * Подтверждение что ответ правильный или нет Для карт типа ANSWER_EX
     */
    private void processWrongAnswerEx() {
        this.backCardText.setVisibility(View.VISIBLE);
        this.backCardText.setText(this.card.backText);
        this.answerEdit.setVisibility(View.GONE);
        this.okButton.setVisibility(View.VISIBLE);
        this.errorButton.setVisibility(View.VISIBLE);
    }

    /**
     * Проверка текста на совпадение (без учета пробелов и регистра).
     * Простыми средствами сделать это не удалось. Все равно просачиваются
     * UTF-8 символы. Поэтому привожу строки к ASCII и сверяю без
     * учета ? (непечатный символ при конвертации к ASCII)
     */
    protected static boolean checkAnswerText(String expectedText, String inputText) {
        String s1 = expectedText.replaceAll("[^\\p{IsAlphabetic}]", "");
        String s2 = inputText.replaceAll("[^\\p{IsAlphabetic}]", "");

        byte[] b1 = s1.toLowerCase().getBytes(StandardCharsets.ISO_8859_1);
        byte[] b2 = s2.toLowerCase().getBytes(StandardCharsets.ISO_8859_1);

        if (b1.length != b2.length) return false;

        for(int i = 0; i < b1.length; i++) {
            byte c1 = b1[i];
            if( c1 == '?') continue;
            byte c2 = b2[i];
            if( c2 == '?') continue;
            if (c1 != c2) return false;
        }

        return true;
    }

    /**
     * Соглашаюсь что ответ знаю
     * Если карта изучается в первый раз, то она помещается в конец.
     */
    private void knownAnswer() {
        if (this.card.type == Card.TYPE.ANSWER_EX) {
            this.answerText.setVisibility(View.GONE);
        }
        this.backCardText.setVisibility(View.GONE);
        this.okButton.setVisibility(View.GONE);
        this.errorButton.setVisibility(View.GONE);
        ++this.card.count;
        ++this.card.totalOk;
        if (this.card.count <= 0) {
            // Хоть и ответ правильный, но данная карта изучется в первый раз
            Card card = this.cards.remove(this.currentCardIdx);
            this.cards.add(card);
        } else {
            // Переходим к следующей карте (карта уже была ранее изучена)
            ++this.currentCardIdx;
        }
        nextCard();
    }

    /**
     * Соглашаюсь что ответ НЕ знаю
     */
    private void unknownAnswer() {
        if (this.card.type == Card.TYPE.ANSWER_EX) {
            this.answerText.setVisibility(View.GONE);
        }
        this.backCardText.setVisibility(View.GONE);
        this.okButton.setVisibility(View.GONE);
        this.errorButton.setVisibility(View.GONE);
        // уменьшаю счетчик счетчик правильных ответов
        if (this.card.count >= 0) {
            --this.card.count;
        }
        ++this.card.totalError;
        // и помещаем карту в конец
        Card card = this.cards.remove(this.currentCardIdx);
        this.cards.add(card);
        nextCard();
    }
}