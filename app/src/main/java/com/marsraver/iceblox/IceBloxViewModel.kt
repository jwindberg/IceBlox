package com.marsraver.iceblox

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class IceBloxViewModel(application: Application) : AndroidViewModel(application) {
    // The game instance holds all the state.
    // By keeping it in the ViewModel, it survives configuration changes (rotation).
    val game: IceBloxGame = IceBloxGame(application.applicationContext)
    
    override fun onCleared() {
        super.onCleared()
        // Pause the game when the ViewModel is cleared (e.g. app closed)
        game.pause()
    }
}
