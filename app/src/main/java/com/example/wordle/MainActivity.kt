package com.example.wordle

import android.net.http.HttpResponseCache
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import java.io.File
import java.net.URL
import java.util.Timer
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.concurrent.schedule


class MainActivity : AppCompatActivity() {
    private fun game(wordsOfLength: List<String>, wordLength: Int, maxGuesses: Int) {
        supportActionBar?.show()
        setContentView(R.layout.game)
        val input = findViewById<TableLayout>(R.id.input)

        var chosenWord = wordsOfLength.random()

        val inputLabelsList = Array(maxGuesses) { Array(wordLength) { TextView(this) } }

        for (rowNum in 0 until maxGuesses) {
            val inputRow = TableRow(this)
            val inputLabels = inputLabelsList[rowNum]

            val labelSize = 1000 / maxOf(wordLength, maxGuesses)
            val padding = labelSize.floorDiv(50)
            for (i in 0 until wordLength) {
                val label = inputLabels[i]
                label.width = (labelSize - padding)
                label.height = (labelSize - padding)
                label.textSize = (labelSize / 3).toFloat()
                label.gravity = Gravity.CENTER
                label.setTextColor(getColorStateList(R.color.textColor))
                label.setBackgroundResource(R.color.grey)

                val params = TableRow.LayoutParams(labelSize, labelSize)
                params.setMargins(padding, padding, padding, padding)
                label.layoutParams = params

                inputRow.addView(label)
            }
            inputRow.layoutParams = TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT
            )
            inputRow.gravity = Gravity.CENTER_HORIZONTAL
            input.addView(inputRow)
        }

        var rowNum = 0

        val inputStrings = Array(maxGuesses) { arrayOfNulls<String>(wordLength) }

        var inputLen = 0
        var inputString = inputStrings[rowNum]
        var inputLabels = inputLabelsList[rowNum]

        val keyboard = findViewById<LinearLayout>(R.id.keyboard)
        val keyToButton = emptyMap<String, Button>().toMutableMap()
        fun makeKeyRow(keys: String) {
            val row = LinearLayout(this)

            row.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            row.gravity = Gravity.CENTER
            row.setPadding(0, 0 ,0 ,0)
            for (character in keys) {
                val key = Button(this)
                key.backgroundTintList = getColorStateList(R.color.grey)
                key.text = character.toString()
                key.setTextColor(getColorStateList(R.color.textColor))
                key.textSize = 34f
                key.textAlignment = Button.TEXT_ALIGNMENT_CENTER
                key.setPadding(0, 0 ,0 ,0)
                key.layoutParams = LinearLayout.LayoutParams(
                    105,
                    160
                )
                row.addView(key)
                keyToButton[character.toString()] = key
            }

            keyboard.addView(row)
        }
        makeKeyRow("qwertyuiop")
        makeKeyRow("asdfghjkl")
        makeKeyRow(">zxcvbnm?")

        fun newGame() {
            for (labels in inputLabelsList) {
                for (label in labels) {
                    label.text = ""
                    label.backgroundTintList = getColorStateList(R.color.grey)
                }
            }
            for (list in inputStrings) {
                for (i in list.indices) {
                    list[i] = null
                }
            }
            for (keyInfo in keyToButton) {
                val button = keyInfo.value
                button.backgroundTintList = getColorStateList(R.color.grey)
            }
            rowNum = 0
            inputLen = 0
            inputString = inputStrings[rowNum]
            inputLabels = inputLabelsList[rowNum]
            chosenWord = wordsOfLength.random()
        }

        var stopInputs = false
        for ((key, button) in keyToButton)
            button.setOnClickListener {
                if (stopInputs) {
                    return@setOnClickListener
                }
                if (inputLen == wordLength) {
                    if (key == "?") {
                        // not valid word
                        if (!wordsOfLength.contains(inputString.joinToString(separator = ""))) {
                            Toast.makeText(this, "that's not a word, idiot", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        for (i in 0 until rowNum) {
                            if (inputStrings[i].contentEquals(inputString)) {
                                // Already entered
                                Toast.makeText(
                                    this,
                                    "you already said that",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@setOnClickListener
                            }
                        }

                        var allCorrect = true
                        for (i in 0 until wordLength) {
                            val guess = inputString[i].toString()
                            val correctCharacter = chosenWord[i].toString()

                            if (guess == correctCharacter) {
                                inputLabels[i].backgroundTintList =
                                    getColorStateList(R.color.green)
                                keyToButton[guess]?.backgroundTintList = getColorStateList(R.color.green)
                            } else {
                                allCorrect = false
                                if (chosenWord.contains(guess)) {
                                    inputLabels[i].backgroundTintList =
                                        getColorStateList(R.color.orange)
                                    if (keyToButton[guess]?.backgroundTintList != getColorStateList(R.color.green)) {
                                        keyToButton[guess]?.backgroundTintList = getColorStateList(R.color.orange)
                                    }
                                } else {
                                    if (keyToButton[guess]?.backgroundTintList == getColorStateList(R.color.grey)) {
                                        keyToButton[guess]?.backgroundTintList = getColorStateList(R.color.black)
                                    }
                                }
                            }
                        }

                        if (allCorrect) {
                            // Won
                            stopInputs = true
                            Toast.makeText(this, "wow", Toast.LENGTH_LONG).show()
                            Timer().schedule(5000) {
                                newGame()
                                stopInputs = false
                            }
                            return@setOnClickListener
                        }

                        if (rowNum + 1 == maxGuesses) {
                            // Out of guesses
                            stopInputs = true
                            Toast.makeText(
                                this,
                                "you're so bad, it was $chosenWord",
                                Toast.LENGTH_LONG
                            ).show()
                            Timer().schedule(5000) {
                                newGame()
                                stopInputs = false
                            }
                            return@setOnClickListener
                        }

                        // New row
                        rowNum += 1
                        inputString = inputStrings[rowNum]
                        inputLen = 0
                        inputLabels = inputLabelsList[rowNum]
                    }
                } else {
                    if (key != "?" && key != ">") {
                        inputLabels[inputLen].text = key.uppercase()
                        inputString[inputLen] = key
                        inputLen += 1
                    }
                }
                if (key == ">" && inputLen != 0) {
                    inputLen -= 1
                    inputLabels[inputLen].text = ""
                    inputString[inputLen] = null
                }
            }
    }
    private fun menu(words: List<String>) {
        supportActionBar?.hide()
        setContentView(R.layout.menu)

        val wordLengthBar = findViewById<Slider>(R.id.wordLengthBar)
        val textLengthLabel = findViewById<TextView>(R.id.wordLength)

        textLengthLabel.text = wordLengthBar.value.toInt().toString()
        wordLengthBar.addOnChangeListener{ _, value, _ ->
            textLengthLabel.text = value.toInt().toString()
        }

        val maxGuessesBar = findViewById<Slider>(R.id.maxGuessesBar)
        val maxGuessesLabel = findViewById<TextView>(R.id.maxGuesses)
        maxGuessesLabel.text = maxGuessesBar.value.toInt().toString()
        maxGuessesBar.addOnChangeListener{ _, value, _ ->
            maxGuessesLabel.text = value.toInt().toString()
        }

        val playButton = findViewById<Button>(R.id.play)
        playButton.setOnClickListener {
            val wordLength = wordLengthBar.value.toInt()
            val maxGuesses = maxGuessesBar.value.toInt()
            game(words.filter { it.length == wordLength }, wordLength, maxGuesses)
        }
    }

    private lateinit var words: List<String>

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.menu -> {
            menu(words)
            true
        }
        else -> {
            super.onOptionsItemSelected(item)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        HttpResponseCache.install(
            File(applicationContext.filesDir, "http"),
            8 * 1024 * 1024
        ) // 8 MiB cache

        // download string of line-break-separated English words
        words = Executors.newSingleThreadExecutor().submit(Callable {
            URL("https://raw.githubusercontent.com/dwyl/english-words/master/words_alpha.txt")
                .openStream()
                .bufferedReader()
                .readLines()
        }).get()
        menu(words)
    }
}