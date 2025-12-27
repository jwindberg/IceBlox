package com.marsraver.iceblox

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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.abs
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun IceBloxGameScreen(viewModel: IceBloxViewModel = viewModel()) {
    val context = LocalContext.current
    val game = viewModel.game
    var lastUpdate by remember { mutableStateOf(0L) }
    val updateInterval = 100_000_000L // 100ms in nanoseconds
    var triggerDraw by remember { mutableStateOf(0L) }
    
    // Drawer State
    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    
    // Effect to pause/resume game based on drawer state
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            game.pause()
        } else {
            game.resume()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE || event == androidx.lifecycle.Lifecycle.Event.ON_STOP) {
                game.pause()
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                 // Only resume if drawer is NOT open
                 if (!drawerState.isOpen) {
                     game.resume()
                 }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            game.pause()
        }
    }

    // Game Loop
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { time ->
                // Only update game if drawer is closed
                if (!drawerState.isOpen && time - lastUpdate > updateInterval) {
                     game.update()
                     lastUpdate = time
                     triggerDraw = time // Force recomposition
                } else if (drawerState.isOpen) {
                    // Just force redraw to keep UI responsive if needed, or do nothing.
                    // But we likely want to keep the game frozen on screen.
                }
            }
        }
    }

    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            androidx.compose.material3.ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    androidx.compose.material3.Text(
                        text = "IceBlox",
                        style = androidx.compose.material3.MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    androidx.compose.material3.HorizontalDivider()
                    Spacer(modifier = Modifier.padding(8.dp))
                    
                    androidx.compose.material3.Text(
                        text = "About",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                    )
                    // About Text with Links
                    val aboutText = androidx.compose.ui.text.buildAnnotatedString {
                        append("A classic penguin puzzle game. Break ice, find coins, and avoid the flames!\n\n")
                        
                        append("Reimplemented as an Android App, originally a Java Applet.\n\n")
                        
                        pushLink(
                            androidx.compose.ui.text.LinkAnnotation.Url(
                                "http://www.javaonthebrain.com/java/iceblox/",
                                styles = androidx.compose.ui.text.TextLinkStyles(
                                    style = androidx.compose.ui.text.SpanStyle(color = androidx.compose.material3.MaterialTheme.colorScheme.primary, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                                )
                            )
                        )
                        append("Original Java Applet")
                        pop()
                        
                        append("\nby Karl Hörnell.\n\n")
                        
                        pushLink(
                            androidx.compose.ui.text.LinkAnnotation.Url(
                                "https://github.com/jwindberg/IceBlox",
                                styles = androidx.compose.ui.text.TextLinkStyles(
                                    style = androidx.compose.ui.text.SpanStyle(color = androidx.compose.material3.MaterialTheme.colorScheme.primary, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
                                )
                            )
                        )
                        append("Android source code")
                        pop()
                        
                        append("\nby John Windberg")
                    }

                    androidx.compose.material3.Text(
                        text = aboutText,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    
                    androidx.compose.material3.Button(
                        onClick = {
                            // Find Activity and finish
                            // Context usually is the Activity in standard android apps
                             val activity = context as? android.app.Activity
                             activity?.finish()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        androidx.compose.material3.Text("Quit Game")
                    }
                }
            }
        }
    ) {
        // Main Content (Game)
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            // Game Rendering Area
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            if (game.gameState >= 6) {
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
                game.draw(drawContext.canvas.nativeCanvas)
                // Force recomposition when game updates
                val x = triggerDraw 
            }
            
            // Menu Button (Hamburger)
            androidx.compose.material3.IconButton(
                onClick = {
                    scope.launch { drawerState.open() }
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .windowInsetsPadding(WindowInsets.statusBars) // Avoid overlap with status bar logic if any
            ) {
                 androidx.compose.material3.Icon(
                     imageVector = androidx.compose.material.icons.Icons.Filled.Menu,
                     contentDescription = "Menu",
                     tint = Color.White
                 )
            }
        }
    }
}

