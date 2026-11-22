package com.example.obstacleracer

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : AppCompatActivity() {

    private lateinit var roadGrid: GridLayout
    private lateinit var livesLabel: TextView
    private lateinit var leftButton: Button
    private lateinit var rightButton: Button
    private lateinit var gameOverLayout: FrameLayout

    private val rows = 5
    private val lanes = 3

    private var playerLane = 1
    private var lives = 3

    private var isGameOver = false
    private var isGameReady = false
    private var isGameRunning = false
    private var isActivityResumed = false

    private val obstacles = mutableListOf<Pair<Int, Int>>()

    private val cells = Array(rows) {
        arrayOfNulls<ImageView>(lanes)
    }

    private val handler = Handler(Looper.getMainLooper())
    private val gameSpeed = 700L

    // Main game loop
    private val gameLoop = object : Runnable {
        override fun run() {
            if (!isGameRunning || isGameOver) return

            updateGame()

            if (isGameRunning && !isGameOver) {
                handler.postDelayed(this, gameSpeed)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContentView(R.layout.activity_main)

        // Handle Android system bars
        ViewCompat.setOnApplyWindowInsetsListener(
            findViewById(R.id.main)
        ) { view, insets ->

            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
            )

            view.setPadding(
                bars.left,
                bars.top,
                bars.right,
                bars.bottom
            )

            insets
        }

        // Connect views
        roadGrid = findViewById(R.id.main_GRID_road)
        livesLabel = findViewById(R.id.main_LBL_lives)
        leftButton = findViewById(R.id.main_BTN_left)
        rightButton = findViewById(R.id.main_BTN_right)
        gameOverLayout = findViewById(R.id.main_LAYOUT_gameOver)

        // Movement buttons
        leftButton.setOnClickListener {
            movePlayer(-1)
        }

        rightButton.setOnClickListener {
            movePlayer(1)
        }

        // Restart button
        findViewById<Button>(
            R.id.main_BTN_playAgain
        ).setOnClickListener {
            resetGame()
        }

        // Create the road after layout measurements
        roadGrid.post {
            createRoad()
            updateLives()
            drawGame()

            isGameReady = true
            startGameLoop()
        }
    }

    // Create the 5x3 game board
    private fun createRoad() {
        roadGrid.removeAllViews()

        val cellWidth = roadGrid.width / lanes
        val cellHeight = roadGrid.height / rows

        for (row in 0 until rows) {
            for (lane in 0 until lanes) {

                val image = ImageView(this)

                val params = GridLayout.LayoutParams(
                    GridLayout.spec(row),
                    GridLayout.spec(lane)
                )

                params.width = cellWidth
                params.height = cellHeight
                params.setGravity(Gravity.CENTER)

                image.layoutParams = params
                image.scaleType = ImageView.ScaleType.FIT_CENTER

                image.setPadding(
                    dp(12),
                    dp(12),
                    dp(12),
                    dp(12)
                )

                cells[row][lane] = image
                roadGrid.addView(image)
            }
        }
    }

    // Move the player between lanes
    private fun movePlayer(direction: Int) {
        if (!isGameReady || isGameOver || !isGameRunning) {
            return
        }

        val newLane = playerLane + direction

        if (newLane in 0 until lanes) {
            playerLane = newLane

            checkCollision()
            drawGame()
        }
    }

    // Move obstacles and generate new ones
    private fun updateGame() {
        if (isGameOver) return

        val updatedObstacles = mutableListOf<Pair<Int, Int>>()

        for ((row, lane) in obstacles) {
            val newRow = row + 1

            if (newRow < rows) {
                updatedObstacles.add(
                    Pair(newRow, lane)
                )
            }
        }

        obstacles.clear()
        obstacles.addAll(updatedObstacles)

        // 50% chance to generate a new fighter jet
        if ((0..1).random() == 1) {
            val randomLane = (0 until lanes).random()

            obstacles.add(
                Pair(0, randomLane)
            )
        }

        checkCollision()
        drawGame()
    }

    // Detect collisions between the player and fighter jets
    private fun checkCollision() {
        if (isGameOver) return

        val collision = obstacles.any {
            it.first == rows - 1 &&
                    it.second == playerLane
        }

        if (!collision) return

        // Remove the collided obstacle
        obstacles.removeAll {
            it.first == rows - 1 &&
                    it.second == playerLane
        }

        lives--
        updateLives()
        vibrate()

        if (lives <= 0) {
            gameOver()
        } else {
            Toast.makeText(
                this,
                "Crash! Lives left: $lives",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Draw the current game state
    private fun drawGame() {

        // Clear all cells
        for (row in 0 until rows) {
            for (lane in 0 until lanes) {
                cells[row][lane]?.setImageDrawable(null)
            }
        }

        // Draw fighter jets
        for ((row, lane) in obstacles) {
            cells[row][lane]?.setImageResource(
                R.drawable.fighter_jet
            )
        }

        // Draw Mojtaba
        cells[rows - 1][playerLane]?.setImageResource(
            R.drawable.mojtaba_obstacle
        )
    }

    // Update the hearts display
    private fun updateLives() {
        livesLabel.text = "❤️ ".repeat(lives)
    }

    // Show the Game Over overlay
    private fun gameOver() {
        if (isGameOver) return

        isGameOver = true
        stopGameLoop()

        gameOverLayout.visibility = View.VISIBLE
    }

    // Restart the game
    private fun resetGame() {
        lives = 3
        playerLane = 1
        obstacles.clear()

        updateLives()
        drawGame()

        gameOverLayout.visibility = View.GONE
        isGameOver = false

        startGameLoop()
    }

    // Start the game loop
    private fun startGameLoop() {
        if (!isGameReady ||
            isGameOver ||
            isGameRunning ||
            !isActivityResumed
        ) {
            return
        }

        isGameRunning = true

        handler.removeCallbacks(gameLoop)
        handler.postDelayed(gameLoop, gameSpeed)
    }

    // Stop the game loop
    private fun stopGameLoop() {
        isGameRunning = false
        handler.removeCallbacks(gameLoop)
    }

    // Vibrate when a collision occurs
    private fun vibrate() {
        val vibrator = getSystemService(
            Context.VIBRATOR_SERVICE
        ) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(
                    200,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }

    // Convert dp to pixels
    private fun dp(value: Int): Int {
        return (
                value * resources.displayMetrics.density
                ).toInt()
    }

    // Resume the game when the app returns to foreground
    override fun onResume() {
        super.onResume()

        isActivityResumed = true
        startGameLoop()
    }

    // Pause the game when the app goes to background
    override fun onPause() {
        isActivityResumed = false
        stopGameLoop()

        super.onPause()
    }

    // Clean up callbacks when the Activity is destroyed
    override fun onDestroy() {
        stopGameLoop()
        super.onDestroy()
    }
}