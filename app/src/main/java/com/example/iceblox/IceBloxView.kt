package com.example.iceblox

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs

@Composable
fun IceBloxGameScreen() {
    val context = LocalContext.current
    val game = remember { IceBloxGame(context) }
    var lastUpdate by remember { mutableStateOf(0L) }
    val updateInterval = 100_000_000L // 100ms in nanoseconds (10 FPS like original)
    var triggerDraw by remember { mutableStateOf(0L) }

    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                game.pause()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                game.resume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            game.pause() // Ensure paused/released on dispose
        }
    }

    // Game Loop
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time ->
                if (time - lastUpdate > updateInterval) {
                     game.update()
                     lastUpdate = time
                     triggerDraw = time // Force recomposition/redraw
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // Game Rendering Area
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        // Title screen: Tap to start
                        // Game: Tap to stop moving (set dir=0)
                        if (game.gameState >= 7) {
                             game.handleInput(1) // Start
                        } else {
                             game.handleInput(0) // Stop
                        }
                    })
                }
                .pointerInput(Unit) {
                    var accumulatedDrag = Offset.Zero
                    detectDragGestures(
                        onDragEnd = { accumulatedDrag = Offset.Zero },
                        onDragCancel = { accumulatedDrag = Offset.Zero },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedDrag += dragAmount
                            val threshold = 20f 
                            if (accumulatedDrag.getDistance() > threshold) {
                                if (abs(accumulatedDrag.x) > abs(accumulatedDrag.y)) {
                                    if (accumulatedDrag.x > 0) game.handleInput(2) else game.handleInput(1)
                                } else {
                                    if (accumulatedDrag.y > 0) game.handleInput(4) else game.handleInput(3)
                                }
                                accumulatedDrag = Offset.Zero
                            }
                        }
                    )
                }
        ) {
            // We use the triggerDraw to ensure recomposition, but the actual drawing happens here.
            game.draw(drawContext.canvas.nativeCanvas)
            
            val x = triggerDraw 
        }

// GameButton removed
    }
}

