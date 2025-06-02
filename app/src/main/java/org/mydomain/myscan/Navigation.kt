package org.mydomain.myscan

sealed class Screen {
    object Camera : Screen()
    object FinalizeDocument : Screen()
}
