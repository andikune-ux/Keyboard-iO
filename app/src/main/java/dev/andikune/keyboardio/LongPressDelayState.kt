package dev.andikune.keyboardio

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue

/** Shared Compose state used by the customization screen for the persisted long-press setting. */
var longPressDelay by mutableLongStateOf(300L)
