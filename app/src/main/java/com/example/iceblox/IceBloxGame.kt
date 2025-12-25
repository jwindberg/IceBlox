package com.example.iceblox

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.abs

class IceBloxGame(private val context: Context) {

    // Constants
    private val playX = 390
    private val playY = 330
    private val mainX = 390
    private val mainY = 348
    private val smalls = 48
    private val blockX = 13
    private val blockY = 11

    // Arrays
    private val animP = intArrayOf(7, 8, 9, 8, 10, 11, 12, 11, 4, 5, 6, 5, 1, 2, 3, 2)
    private val animF = intArrayOf(32, 33, 34, 35, 36, 35, 34, 33)
    private val levFlame = intArrayOf(2, 3, 4, 2, 3, 4)
    private val levRock = intArrayOf(5, 6, 7, 8, 9, 10)
    private val levSpeed = intArrayOf(3, 3, 3, 5, 5, 5)
    private val levIce = intArrayOf(35, 33, 31, 29, 27, 25)
    private val effMax = 5

    // State
    private var playArea = IntArray((blockX + 2) * (blockY + 3))
    var gameState = 0
    private var counter = 0
    var dir = 0
    private var inFront = 0
    private var inFront2 = 0
    private var level = 0
    private var coins = 0
    private var effLevel = 0
    private var lives = 3
    private var actors = 0
    private var flames = 0
    private var score: Long = 0

    // Actor arrays
    // Actor arrays - Increased to 100 to prevent crashes
    private val x = IntArray(100)
    private val y = IntArray(100)
    private val dx = IntArray(100)
    private val dy = IntArray(100)
    private val motion = IntArray(100)
    private val look = IntArray(100)
    private val creature = IntArray(100)
    private val ccount = IntArray(100)

    private val sideIX = intArrayOf(0, -1, 1, -15, 15)
    private val coorDx = intArrayOf(0, -30, 30, 0, 0)
    private val coorDy = intArrayOf(0, 0, 0, -30, 30)

    // Graphics
    private lateinit var small: Array<Bitmap>
    private lateinit var title: Bitmap
    private val paint = Paint()
    
    // Scale factor for drawing on different screen sizes
    var scaleFactor = 1.0f
    var offsetX = 0f
    var offsetY = 0f

    // Sounds - Music (MediaPlayer)
    private var sndTitle: android.media.MediaPlayer? = null
    private var sndMainGame: android.media.MediaPlayer? = null
    
    // Sounds - SFX (SoundPool)
    private var soundPool: android.media.SoundPool? = null
    private var sfxGameStart: Int = 0
    private var sfxLevelClear: Int = 0
    private var sfxPlayerDown: Int = 0
    private var sfxGameOver: Int = 0
    
    // Removed Legacy MediaPlayer vars
    private var sndGameStart: android.media.MediaPlayer? = null 
    private var sndLevelClear: android.media.MediaPlayer? = null
    private var sndPlayerDown: android.media.MediaPlayer? = null
    private var sndGameOver: android.media.MediaPlayer? = null

    init {
        initGame()
    }

    private fun initGame() {
        // Load resources
        val options = BitmapFactory.Options().apply { inScaled = false }
        val collection = BitmapFactory.decodeResource(context.resources, R.drawable.iceblox, options)
        
        small = Array(smalls) { Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888) }
        
        var k = 0
        var i = 0
        var j = 0
        while (k < smalls) {
            small[k] = Bitmap.createBitmap(collection, j * 30, i * 30, 30, 30)
            k++
            j++
            if (j == 8) {
                j = 0
                i++
            }
        }
        
        title = Bitmap.createBitmap(collection, 0, 180, 224, 64)

        // Init Sounds
        try {
            // SoundPool for SFX
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_GAME)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            
            soundPool = android.media.SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(audioAttributes)
                .build()
                
            sfxGameStart = soundPool?.load(context, R.raw.game_start, 1) ?: 0
            sfxLevelClear = soundPool?.load(context, R.raw.level_clear, 1) ?: 0
            sfxPlayerDown = soundPool?.load(context, R.raw.player_down, 1) ?: 0
            sfxGameOver = soundPool?.load(context, R.raw.game_over, 1) ?: 0
        
            // MediaPlayer for Music
            // ONLY create Title initially. Others created on demand.
            sndTitle = android.media.MediaPlayer.create(context, R.raw.title).apply { isLooping = true }
            // sndMainGame and sndMazeForming will be created in playSound
            
            // Legacy nulls (just to match var definitions if kept)
            // sndGameStart = android.media.MediaPlayer.create(context, R.raw.game_start)
            // sndLevelClear = android.media.MediaPlayer.create(context, R.raw.level_clear)
            // sndPlayerDown = android.media.MediaPlayer.create(context, R.raw.player_down)
            // sndGameOver = android.media.MediaPlayer.create(context, R.raw.game_over)
        } catch (e: Exception) {
            android.util.Log.e("IceBlox", "Error initializing sounds: " + e.message)
            e.printStackTrace()
        }
        
        // Initial State
        gameState = 7 // Intro 1
        playSound(7)
        counter = 0
    }

    fun pause() {
        android.util.Log.d("IceBlox", "Game Paused")
        if (sndTitle?.isPlaying == true) sndTitle?.pause()
        if (sndMainGame?.isPlaying == true) sndMainGame?.pause()
        soundPool?.autoPause()
    }

    fun resume() {
        android.util.Log.d("IceBlox", "Game Resumed. global state: $gameState")
        if (gameState >= 7 && gameState <= 12 && sndTitle?.isPlaying == false) sndTitle?.start()
        if (gameState == 2 && sndMainGame?.isPlaying == false) sndMainGame?.start()
        soundPool?.autoResume()
    }

    private fun playSound(state: Int) {
        android.util.Log.d("IceBlox", "playSound called for state: $state")
        try {
            when (state) {
                7 -> { // Intro
                   // Clean up
                   sndMainGame?.pause()
                   sndMainGame?.seekTo(0)
                   
                   // Ensure Title is ready
                   if (sndTitle == null) {
                        try { sndTitle = android.media.MediaPlayer.create(context, R.raw.title).apply { isLooping = true } } catch(e:Exception){}
                   }
                   if (sndTitle?.isPlaying == false) sndTitle?.start()
                }
                0 -> { // Prepare (Start Game)
                    sndTitle?.pause()
                    sndTitle?.seekTo(0)
                    // moved to state 1
                }
                1 -> { // Show Field (Maze forming)
                    // Release MainGame if it was playing (e.g. from previous level?)
                    try { sndMainGame?.release() } catch(e:Exception){}
                    sndMainGame = null
                    
                    // Play Game Start Sound Once
                    soundPool?.play(sfxGameStart, 1.0f, 1.0f, 0, 0, 1.0f)
                }
                2 -> { // Playing
                    // Release Title just in case
                    try { sndTitle?.release() } catch(e:Exception){}
                    sndTitle = null

                    // Create Main Game Music (Fresh)
                    if (sndMainGame == null) {
                         sndMainGame = android.media.MediaPlayer.create(context, R.raw.main_game).apply { isLooping = true }
                    }
                    
                    try {
                        // FORCE START
                        sndMainGame?.setVolume(1.0f, 1.0f)
                        sndMainGame?.seekTo(0)
                        sndMainGame?.start()
                        android.util.Log.d("IceBlox", "MainGame fresh start.")
                    } catch (e: Exception) {
                        android.util.Log.e("IceBlox", "Start MainGame failed ($e). Hard Resetting...")
                        // Fallback mechanisms
                        sndMainGame?.reset()
                        val afd = context.resources.openRawResourceFd(R.raw.main_game)
                        if (afd != null) {
                            sndMainGame?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            afd.close()
                            sndMainGame?.isLooping = true
                            sndMainGame?.prepare()
                            sndMainGame?.start()
                        }
                    }
                }
                3 -> { // Happy (Level Clear)
                    // Release Music
                    try { sndMainGame?.release() } catch(e:Exception){}
                    sndMainGame = null
                    
                    soundPool?.play(sfxLevelClear, 1.0f, 1.0f, 0, 0, 1.0f)
                }
                5 -> { // Death
                    android.util.Log.d("IceBlox", "Playing Death Sound")
                    try { sndMainGame?.release() } catch(e:Exception){}
                    sndMainGame = null
                    
                    soundPool?.play(sfxPlayerDown, 1.0f, 1.0f, 0, 0, 1.0f)
                }
                6 -> { // Game Over
                   android.util.Log.d("IceBlox", "Playing Game Over Sound")
                   try { sndMainGame?.release() } catch(e:Exception){}
                   sndMainGame = null
                   soundPool?.play(sfxGameOver, 1.0f, 1.0f, 0, 0, 1.0f)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("IceBlox", "Error playing sound for state $state: " + e.message)
            e.printStackTrace()
        }
    }

    fun update() {
        counter = (counter + 1) and 255
        // Sound transitions handled here if state changed externally, but we mostly handle changes in methods
        // EXCEPT for implicit transitions like counter > X
        
        when (gameState) {
            0 -> { prepareField(); playSound(1) } // Transition 0->1 happens inside prepareField
            1 -> showField()
            2 -> gameLoop()
            3 -> happyPenguin()
            4 -> clearField()
            5 -> fixDeath()
            6 -> gameOver()
            7 -> drawIntro1() 
            8 -> waitIntro1()
            9 -> drawIntro2()
            10 -> waitIntro2()
            11 -> drawIntro3()
            12 -> waitIntro3()
            13 -> waitStartGame() // New wait state
        }
    }
    
    // -- Game Logic Methods --
    
    private fun waitStartGame() {
        // Wait for ~3 seconds (30 frames at 100ms) for start sound to finish
        if (counter > 30) {
            gameState = 0
            counter = 0
        }
    }

    private fun prepareField() {
        if (level > effMax) effLevel = effMax else effLevel = level
        
        buildMap()
        
        gameState = 1
        playSound(1) // Start Maze forming sound
        counter = 0
        motion[0] = 0
        actors = 1
        coins = 0
        flames = 0
        look[0] = 0
        x[0] = 30
        y[0] = 30
        dx[0] = 6
        dy[0] = 6
        look[0] = 2
        creature[0] = 1
        for (i in 0 until 100) ccount[i] = 0
    }

    private fun buildMap() {
        var notDone = true
        var i: Int
        var j: Int
        var p: Int
        var q: Int
        val stack = IntArray(blockX * blockY)
        
        for (idx in playArea.indices) playArea[idx] = 255
        
        while (notDone) {
            for (currY in 1..blockY) {
                for (currX in 1..blockX) {
                    playArea[currY * (blockX + 2) + currX] = 0
                }
            }
            playArea[blockX + 3] = -1 // Make room for start square

            i = 0
            j = 5 + levIce[effLevel] + levRock[effLevel]
            while (i < j) {
                p = 1 + (Math.random() * blockX).toInt()
                q = 1 + (Math.random() * blockY).toInt()
                if (playArea[q * (blockX + 2) + p] == 0) {
                    if (i < 5)
                        playArea[q * (blockX + 2) + p] = 10 // Frozen coin
                    else if (i < levIce[effLevel] + 5)
                        playArea[q * (blockX + 2) + p] = 2 // Ice cube
                    else
                        playArea[q * (blockX + 2) + p] = 1 // Rock
                    i++
                }
            }
            playArea[blockX + 3] = 0
            p = 0
            q = 1
            i = 0
            stack[0] = blockX + 3
            while (p < q) {
                j = stack[p++]
                if ((playArea[j - blockX - 2] and 17) == 0) {
                    stack[q] = j - blockX - 2
                    if (playArea[stack[q]] == 10) i++
                    playArea[stack[q++]] = playArea[stack[q-1]] or 16
                }
                if ((playArea[j + blockX + 2] and 17) == 0) {
                    stack[q] = j + blockX + 2
                    if (playArea[stack[q]] == 10) i++
                    playArea[stack[q++]] = playArea[stack[q-1]] or 16
                }
                if ((playArea[j - 1] and 17) == 0) {
                    stack[q] = j - 1
                    if (playArea[stack[q]] == 10) i++
                    playArea[stack[q++]] = playArea[stack[q-1]] or 16
                }
                if ((playArea[j + 1] and 17) == 0) {
                    stack[q] = j + 1
                    if (playArea[stack[q]] == 10) i++
                    playArea[stack[q++]] = playArea[stack[q-1]] or 16
                }
            }
            notDone = i < 5
        }
        
        // Clean up map for play
        for (row in 0 until blockY) {
             for (col in 0 until blockX) {
                 p = (row + 1) * (blockX + 2) + col + 1
                 playArea[p] = playArea[p] and 15
             }
        }
    }

    private fun showField() {
        if (counter >= 30) {
            gameState = 2
            playSound(2)
            // Note: Game loop continues using counter, no need to reset, 
            // but since we handle counter overflow in update(), it's fine.
        }
    }

    private fun gameLoop() {
        var j: Int
        // Spawn flames
        if (flames < levFlame[effLevel]) {
            if (x[0] < (playX / 2)) x[actors] = playX + 30 else x[actors] = 0
            y[actors] = 30 * (1 + (Math.random() * blockY).toInt())
            j = (y[actors] / 30) * (blockX + 2) + x[actors] / 30
            motion[actors] = 0
            dx[actors] = levSpeed[effLevel]
            dy[actors] = levSpeed[effLevel]
            creature[actors] = 4
            if (playArea[j + 1] == 0 || playArea[j - 1] == 0) {
                actors++
                flames++
            }
        }

        // Move actors
        for (i in 0 until actors) {
            ccount[i]++
            when (motion[i]) {
                1 -> x[i] -= dx[i]
                2 -> x[i] += dx[i]
                3 -> y[i] -= dy[i]
                4 -> y[i] += dy[i]
            }
            
            j = (y[i] / 30) * (blockX + 2) + x[i] / 30
            
            when (creature[i]) {
                1 -> updatePenguin(i, j)
                2 -> updateMovingIceBlock(i, j)
                3 -> updateMovingFrozenCoin(i, j)
                4 -> updateFlame(i, j)
                5 -> updateFlashing50(i)
                7 -> updateSkeleton(i)
            }
        }
        
        if (coins > 4) {
            gameState = 3
            playSound(3)
            updateScore(1000)
            counter = 0
            coins = 0
        }

        // Music Watchdog: DISABLED for debugging
        /*
        if (gameState == 2 && counter % 60 == 0) { 
             try {
                 if (sndMainGame == null) {
                      // Init if missing
                      sndMainGame = android.media.MediaPlayer.create(context, R.raw.main_game).apply { isLooping = true }
                 }
                 
                 if (sndMainGame?.isPlaying == false) {
                     android.util.Log.w("IceBlox", "Watchdog: Music stopped. Restarting.")
                     try {
                         sndMainGame?.start()
                     } catch(e: Exception) {
                         // State error? Reset.
                         sndMainGame?.reset()
                         val afd = context.resources.openRawResourceFd(R.raw.main_game)
                         if (afd != null) {
                             sndMainGame?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                             afd.close()
                             sndMainGame?.isLooping = true
                             sndMainGame?.prepare()
                             sndMainGame?.start()
                         }
                     }
                 }
             } catch (e: Exception) {
                 android.util.Log.e("IceBlox", "Watchdog Error: " + e.message)
             }
        }
        */
    }

    private fun updatePenguin(i: Int, j: Int) {
        if (x[i] % 30 == 0 && y[i] % 30 == 0) motion[i] = 0
        
        if (motion[i] == 0) {
            inFront = playArea[j + sideIX[dir]]
            inFront2 = if ((j + 2 * sideIX[dir]) < 0) 1 else playArea[j + 2 * sideIX[dir]]
            
            if (inFront == 0) {
                motion[i] = dir
            } else {
                if (inFront2 == 0 && (inFront == 2 || inFront == 10)) {
                    // Push
                    creature[actors] = if (inFront == 2) 2 else 3
                    look[actors] = if (inFront == 2) 16 else 24
                    
                    x[actors] = x[i] + coorDx[dir]
                    y[actors] = y[i] + coorDy[dir]
                    dx[actors] = 15
                    dy[actors] = 15
                    motion[actors] = dir
                    actors++
                    playArea[j + sideIX[dir]] = 0
                } else if (inFront > 1 && inFront < 18) {
                    // Crack
                    // Fix: Check status BEFORE incrementing to avoid race condition drawing the next "stage" (which might be sprite 10 or 18)
                    if (inFront == 9) { // All cracked
                        playArea[j + sideIX[dir]] = 0
                        playGraphicsFillRect(x[i] + coorDx[dir] - 30f, y[i] + coorDy[dir] - 30f, 30f, 30f)
                        updateScore(5)
                    } else if (inFront == 17) {
                         playArea[j + sideIX[dir]] = 0
                         playGraphicsFillRect(x[i] + coorDx[dir] - 30f, y[i] + coorDy[dir] - 30f, 30f, 30f)
                         updateScore(100)
                         coins++
                    } else {
                         playArea[j + sideIX[dir]]++
                         // Just cracking, handled by sprite change in playArea but we might want visual feedback?
                         // Original just changed the tile.
                    }
                }
            }
        }
        
        if (motion[i] != 0) {
            look[i] = animP[(motion[i] - 1) * 4 + counter % 4]
        }
        
        // Collision with enemies
        for (k in 1 until actors) {
            if (creature[k] == 4) {
                 if (abs(x[k] - x[i]) < 20 && abs(y[k] - y[i]) < 20) {
                     creature[k] = 6 // dummy
                     x[k] = 0
                     y[k] = 0
                     motion[k] = 0
                     ccount[i] = 0
                     dx[i] = 0
                     dy[i] = 0
                     creature[i] = 7 // Skeleton
                     playSound(5) // Play death sound immediately on collision
                 }
            }
        }
    }

    private fun updateMovingIceBlock(i: Int, j: Int) {
        if (x[i] % 30 == 0 && y[i] % 30 == 0 && playArea[j + sideIX[motion[i]]] != 0) {
            playArea[j] = 2
            removeActor(i)
        }
    }

    private fun updateMovingFrozenCoin(i: Int, j: Int) {
        if (x[i] % 30 == 0 && y[i] % 30 == 0 && playArea[j + sideIX[motion[i]]] != 0) {
             playArea[j] = 10
             removeActor(i)
        }
    }
    
    private fun updateFlame(i: Int, j: Int) {
        look[i] = animF[counter % 8]
        if (motion[i] == 0) motion[i] = 1 + (Math.random() * 4).toInt()
        
        if (x[i] % 30 == 0 && y[i] % 30 == 0) {
             // Track penguin
             if (abs(x[i] - x[0]) < 3) {
                 motion[i] = if (y[i] > y[0]) 3 else 4
             } else if (abs(y[i] - y[0]) < 3) {
                 motion[i] = if (x[i] > x[0]) 1 else 2
             }
             if (playArea[j + sideIX[motion[i]]] != 0) motion[i] = 0
        }
        
        // Collide with moving block?
        for (k in 1 until actors) {
            if ((creature[k] and 254) == 2) {
                if (abs(x[k] - x[i]) < 30 && abs(y[k] - y[i]) < 30) {
                    creature[i] = 5
                    look[i] = 37
                    motion[i] = 0
                    ccount[i] = 0
                    updateScore(50)
                }
            }
        }
    }
    
    private fun updateFlashing50(i: Int) {
        look[i] = 37 + (counter and 1)
        if (ccount[i] > 20) {
            flames--
            removeActor(i)
        }
    }

    private fun updateSkeleton(i: Int) {
        if (ccount[i] < 8) look[i] = 39 + ccount[i]
        else if (ccount[i] < 30) look[i] = 47
        else {
            lives--
            if (lives < 0) {
                gameState = 5
                android.util.Log.d("IceBlox", "Skeleton killed player. Lives < 0. State=5")
                // playSound(5) removed - already played in updatePenguin
                counter = 0 
            } else {
                actors = 1
                flames = 0
                counter = 0
                dx[i] = 6
                dy[i] = 6
                creature[0] = 1
                look[0] = 2
                
                // Music Restart (Required because playSound(5) released it)
                playSound(2)
            }
        }
    }

    private fun updateDebris(i: Int) {
       // Removed
    }
    
    private fun spawnDebris(pos: Int, spriteIndex: Int) {
        // Removed
    }

    private fun playGraphicsFillRect(left: Float, top: Float, w: Float, h: Float) {
        // Helper to simulate 'playGraphics.fillRect' from original. 
        // Since we redraw everything every frame in draw(), we don't strictly *need* this for persistence,
        // but the original might have used it for immediate feedback or dirty rects.
        // In this port, the state change (playArea=0) handles the erasure on next frame.
    }

    private fun happyPenguin() {
        if (counter > 35) {
            level++
            gameState = 4
            counter = 0
        }
    }

    private fun clearField() {
        if (counter > 14) gameState = 0
    }

    private fun fixDeath() {
        if (counter == 1) playSound(6) // Play immediately
        if (counter > 10) { // Reduced Wait
             gameState = 6
             // playSound(6) moved up
             counter = 0 // CRITICAL FIX: Reset counter so 'gameOver' waits properly
        }
    }

    private fun gameOver() {
        if (counter > 80) {
            gameState = 7
            playSound(7)
        }
    }

    private fun drawIntro1() {
        level = 0
        score = 0
        lives = 3
        counter = 0
        // Transition to wait
        gameState = 8
    }

    private fun waitIntro1() {
        if (counter > 70) gameState = 9
    }

    private fun drawIntro2() {
        gameState = 10
        counter = 0
    }
    
    private fun waitIntro2() {
        if (counter > 80) gameState = 11
    }
    
    private fun drawIntro3() {
        gameState = 12
        counter = 0
    }
    
    private fun waitIntro3() {
        if (counter > 70) gameState = 7
    }

    private fun removeActor(i: Int) {
        for (j in i until actors - 1) {
            x[j] = x[j+1]
            y[j] = y[j+1]
            dx[j] = dx[j+1]
            dy[j] = dy[j+1]
            look[j] = look[j+1]
            motion[j] = motion[j+1]
            creature[j] = creature[j+1]
        }
        actors--
    }

    private fun updateScore(points: Long) {
        score += points
    }

    // -- Drawing --
    
    fun draw(canvas: Canvas) {
        // Calculate scaling to fit screen
        val targetWidth = playX.toFloat() 
        val targetHeight = mainY.toFloat()
        
        val scale = minOf(canvas.width / targetWidth, canvas.height / targetHeight)
        scaleFactor = scale
        
        // Center it
        offsetX = (canvas.width - targetWidth * scale) / 2
        offsetY = (canvas.height - targetHeight * scale) / 2
        
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)
        
        // Clip to main area
        canvas.clipRect(0, 0, mainX, mainY)
        
        // Draw Background
        canvas.drawColor(Color.BLACK)
        
        when (gameState) {
             1 -> drawShowField(canvas)
             2 -> drawPlaying(canvas)
             3 -> drawHappyPenguin(canvas)
             4 -> drawClearField(canvas)
             5 -> drawDeath(canvas)
             6 -> drawGameOver(canvas)
             7 -> {} // Logic handled in update
             8 -> drawIntro1Screen(canvas)
             9 -> {}
             10 -> drawIntro2Screen(canvas)
             11 -> {}
             12 -> drawIntro3Screen(canvas)
        }
        
        // Draw Top Bar (Score, Level) - if in game
        if (gameState >= 0 && gameState <= 6) {
             drawHUD(canvas)
        }

        canvas.restore()
    }

    private fun drawHUD(canvas: Canvas) {
        paint.color = Color.LTGRAY
        canvas.drawRect(0f, mainY - playY - 4f, mainX.toFloat(), mainY - playY.toFloat(), paint)
        
        paint.color = Color.WHITE
        paint.textSize = 12f
        canvas.drawText("SCORE:", 2f, 12f, paint)
        canvas.drawText(score.toString(), 50f, 12f, paint)
        
        canvas.drawText("LEVEL:  ${level + 1}", 125f, 12f, paint)
        canvas.drawText("SPARE LIVES:", 220f, 12f, paint)
        
        for (i in 0 until lives) {
            canvas.drawBitmap(small[13], 300f + i * 15, -16f, null)
        }
    }
    
    private fun drawPlaying(canvas: Canvas) {
        drawMap(canvas)
        
        for (k in 0 until actors) {
            canvas.drawBitmap(small[look[k]], x[k] - 30f, y[k] - 30f + mainY - playY, null)
        }
    }
    
    private fun drawHappyPenguin(canvas: Canvas) {
        drawMap(canvas)
        canvas.drawBitmap(small[39 * (counter and 1)], x[0] - 30f, y[0] - 30f + mainY - playY, null)
    }

    private fun drawMap(canvas: Canvas) {
         for (i in 0 until blockY) {
            for (j in 0 until blockX) {
                val p = (i + 1) * (blockX + 2) + j + 1
                val type = playArea[p] // and 15 removed to prevent 17->1 collision
                val px = j * 30f
                val py = i * 30f + mainY - playY
                 
                if (type == 1) {
                    canvas.drawBitmap(small[14], px, py, null)
                } else if (type > 1 && type < 18) {
                    canvas.drawBitmap(small[14 + type], px, py, null)
                }
            }
         }
    }

    private fun drawShowField(canvas: Canvas) {
        // Curtains effect
        // offGraphics.clipRect(playX/2-(counter*playX/2/30), ... )
        // Adapting this is tricky without offscreen buffer, simpler to just draw black bars
        
        drawPlaying(canvas) // Draw underlying field
        
        paint.color = Color.BLACK
        val cx = playX / 2f
        val cy = (mainY - playY) + playY / 2f
        
        val w = counter * playX / 30f
        val h = counter * playY / 30f
        
        // This logic in original was expanding the VIEW, so we obscure the REST?
        // Original: clipRect(center - expanding_size). 
        // So we should draw black rectangles AROUND the center hole.
        
        // Actually, let's just implement the 'opening' effect by drawing 4 black rectangles
        val halfW = w / 2
        val halfH = h / 2
        
        // Left
        canvas.drawRect(0f, mainY - playY.toFloat(), cx - halfW, mainY.toFloat(), paint)
        // Right
        canvas.drawRect(cx + halfW, mainY - playY.toFloat(), playX.toFloat(), mainY.toFloat(), paint)
        // Top
        canvas.drawRect(0f, mainY - playY.toFloat(), playX.toFloat(), cy - halfH, paint)
        // Bottom
        canvas.drawRect(0f, cy + halfH, playX.toFloat(), mainY.toFloat(), paint)
    }

    private fun drawClearField(canvas: Canvas) {
         // Contract effect
         paint.color = Color.BLACK
         val w = playX * counter / 15f
         val h = playY * counter / 15f
         val cx = playX / 2f
         val cy = (mainY - playY) + playY / 2f
         
         canvas.drawRect(cx - w/2, cy - h/2, cx + w/2, cy + h/2, paint)
    }
    
    private fun drawDeath(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        paint.color = Color.WHITE
        paint.textSize = 20f
        canvas.drawText("GAME OVER", 175f, 100f, paint)
        canvas.drawText("You scored $score", 160f, 130f, paint)
        canvas.drawBitmap(small[2], 190f, 150f, null)
    }
    
    private fun drawGameOver(canvas: Canvas) {
        drawDeath(canvas)
    }

    private fun drawIntro1Screen(canvas: Canvas) {
        paint.color = Color.WHITE
        canvas.drawBitmap(title, (mainX - 224f) / 2, 10f, null)
        
        paint.textSize = 14f
        canvas.drawText("ACTORS AND OBJECTS", 145f, 97f, paint)
        canvas.drawBitmap(small[2], 140f, 110f, null)
        canvas.drawText("Pixel Pete, the penguin", 180f, 130f, paint)
        
        canvas.drawBitmap(small[34], 120f, 150f, null)
        canvas.drawBitmap(small[32], 140f, 150f, null)
        canvas.drawText("Evil flames", 180f, 170f, paint)
        // Animating flames
        canvas.drawBitmap(small[animF[(counter + 2) and 7]], 120f, 150f, null)
        canvas.drawBitmap(small[animF[counter and 7]], 140f, 150f, null)

        canvas.drawBitmap(small[16], 140f, 190f, null)
        canvas.drawText("Ice cube", 180f, 210f, paint)
        
        canvas.drawBitmap(small[14], 140f, 230f, null)
        canvas.drawText("Solid rock", 180f, 250f, paint)
        
        canvas.drawBitmap(small[24], 140f, 270f, null)
        canvas.drawText("Frozen gold coin", 180f, 290f, paint)
        
        canvas.drawText("Tap to start", 138f, 330f, paint)
    }
    
    private fun drawIntro2Screen(canvas: Canvas) {
        paint.color = Color.BLACK
        canvas.drawRect(0f, 75f, mainX.toFloat(), 305f, paint) // Clear area
        
        paint.color = Color.WHITE
        canvas.drawText("HOW TO PLAY", 165f, 97f, paint)
        
        canvas.drawBitmap(small[2], 140f, 110f, null)
        
        // Original text: "Move up, down, left and right / with the K, M, A and D keys"
        // Adapted text for swipe:
        canvas.drawText("Swipe up, down, left and right", 180f, 122f, paint)
        canvas.drawText("to move the penguin", 180f, 137f, paint)
        
        canvas.drawBitmap(small[10], 70f, 150f, null)
        canvas.drawBitmap(small[16], 140f, 150f, null)
        
        canvas.drawText("Walk against ice cubes", 180f, 162f, paint)
        canvas.drawText("to move them out of the way", 180f, 177f, paint)
        
        paint.strokeWidth = 1f
        canvas.drawLine(110f, 160f, 136f, 160f, paint)
        canvas.drawLine(116f, 169f, 136f, 169f, paint)
        
        canvas.drawBitmap(small[10], 80f, 190f, null)
        canvas.drawBitmap(small[18], 110f, 190f, null)
        canvas.drawBitmap(small[16], 140f, 190f, null)
        
        canvas.drawText("Walk against blocked", 180f, 202f, paint)
        canvas.drawText("ice cubes to crack them", 180f, 217f, paint)
        
        canvas.drawBitmap(small[28], 110f, 230f, null)
        canvas.drawBitmap(small[9], 140f, 230f, null)
        
        canvas.drawText("Free the gold coins by", 180f, 242f, paint)
        canvas.drawText("crushing the ice around them", 180f, 257f, paint)
        
        canvas.drawBitmap(small[9], 80f, 270f, null)
        canvas.drawBitmap(small[32], 140f, 270f, null)
        
        canvas.drawLine(110f, 280f, 126f, 280f, paint)
        canvas.drawLine(110f, 289f, 130f, 289f, paint)
        
        canvas.drawText("And watch out", 180f, 282f, paint)
        canvas.drawText("for the flames", 180f, 297f, paint)
    }
    
    private fun drawIntro3Screen(canvas: Canvas) {
        paint.color = Color.BLACK
        canvas.drawRect(0f, 75f, mainX.toFloat(), 305f, paint)

        paint.color = Color.WHITE
        canvas.drawText("SCORING", 180f, 97f, paint)
        
        canvas.drawBitmap(small[10], 110f, 110f, null)
        canvas.drawBitmap(small[18], 140f, 110f, null)
        
        canvas.drawText("Breaking ice,", 180f, 122f, paint)
        canvas.drawText("5 points", 180f, 137f, paint)
        
        canvas.drawBitmap(small[33], 60f, 150f, null)
        canvas.drawBitmap(small[16], 80f, 150f, null)
        canvas.drawBitmap(small[9], 140f, 150f, null)
        
        canvas.drawLine(112f, 160f, 126f, 160f, paint)
        canvas.drawLine(112f, 169f, 130f, 169f, paint)
        
        canvas.drawText("Putting out flame", 180f, 162f, paint)
        canvas.drawText("with ice, 50 points", 180f, 177f, paint)
        
        canvas.drawBitmap(small[10], 110f, 190f, null)
        canvas.drawBitmap(small[27], 140f, 190f, null)
        
        canvas.drawText("Freeing coin,", 180f, 202f, paint)
        canvas.drawText("100 points", 180f, 217f, paint)
        
        for (j in 0 until 5) {
             canvas.drawBitmap(small[15], 100f - 9 * j, 230f, null)
        }
        canvas.drawBitmap(small[39], 140f, 230f, null)
        
        canvas.drawText("Taking all coins and advancing", 180f, 242f, paint)
        canvas.drawText("to next level, 1000 points", 180f, 257f, paint)
    }

    fun handleInput(newDir: Int) {
        // 0=None, 1=Left, 2=Right, 3=Up, 4=Down
        if (gameState >= 7) {
            // Tap to start
            if (newDir != 0) { // Any press
                 gameState = 0 // SKIP Wait State (13) -> Go directly to Prepare (0)
                 playSound(0)   // Play Start Sound
                 counter = 0
            }
        } else {
            dir = newDir
        }
    }
}
