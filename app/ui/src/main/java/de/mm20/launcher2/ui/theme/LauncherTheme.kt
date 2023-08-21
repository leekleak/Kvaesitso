package de.mm20.launcher2.ui.theme

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.preferences.LauncherDataStore
import de.mm20.launcher2.preferences.Settings
import de.mm20.launcher2.preferences.Settings.AppearanceSettings
import de.mm20.launcher2.themes.DefaultThemeId
import de.mm20.launcher2.themes.Theme
import de.mm20.launcher2.ui.locals.LocalDarkTheme
import de.mm20.launcher2.ui.theme.colorscheme.*
import de.mm20.launcher2.ui.theme.typography.DefaultTypography
import de.mm20.launcher2.ui.theme.typography.getDeviceDefaultTypography
import kotlinx.coroutines.flow.map
import org.koin.androidx.compose.inject


@Composable
fun LauncherTheme(
    content: @Composable () -> Unit
) {

    val context = LocalContext.current
    val dataStore: LauncherDataStore by inject()

    val colorSchemePreference by remember {
        dataStore.data.map {
            if (it.easterEgg) Settings.AppearanceSettings.ColorScheme.EasterEgg
            else it.appearance.colorScheme
        }
    }.collectAsState(
        AppearanceSettings.ColorScheme.Default
    )

    val themePreference by remember { dataStore.data.map { it.appearance.theme } }.collectAsState(
        AppearanceSettings.Theme.System
    )
    val darkTheme =
        themePreference == AppearanceSettings.Theme.Dark || themePreference == AppearanceSettings.Theme.System && isSystemInDarkTheme()

    val cornerRadius by remember {
        dataStore.data.map { it.cards.radius.dp }
    }.collectAsState(8.dp)

    val baseShape by remember {
        dataStore.data.map {
            when (it.cards.shape) {
                Settings.CardSettings.Shape.Cut -> CutCornerShape(0f)
                else -> RoundedCornerShape(0f)
            }
        }
    }.collectAsState(RoundedCornerShape(0f))

    val colorScheme by colorSchemeAsState(colorSchemePreference, darkTheme)

    val font by remember { dataStore.data.map { it.appearance.font } }.collectAsState(
        AppearanceSettings.Font.Outfit
    )

    val typography = remember(font) {
        getTypography(context, font)
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = Shapes(
                extraSmall = baseShape.copy(CornerSize(cornerRadius / 3f)),
                small = baseShape.copy(CornerSize(cornerRadius / 3f * 2f)),
                medium = baseShape.copy(CornerSize(cornerRadius)),
                large = baseShape.copy(CornerSize((cornerRadius / 3f * 4f).coerceAtMost(16.dp))),
                extraLarge = baseShape.copy(CornerSize((cornerRadius / 3f * 7f).coerceAtMost(28.dp))),
            ),
            content = content
        )
    }
}

@Composable
fun colorSchemeAsState(
    colorScheme: AppearanceSettings.ColorScheme,
    darkTheme: Boolean
): MutableState<ColorScheme> {
    val dataStore: LauncherDataStore by inject()

    when (colorScheme) {
        AppearanceSettings.ColorScheme.BlackAndWhite -> {
            return remember(darkTheme) {
                mutableStateOf(
                    if (darkTheme) DarkBlackAndWhiteColorScheme else LightBlackAndWhiteColorScheme
                )
            }
        }
        AppearanceSettings.ColorScheme.EasterEgg -> {
            return remember(darkTheme) {
                mutableStateOf(
                    if (darkTheme) DarkEasterEggColorScheme else LightEasterEggColorScheme
                )
            }
        }
        AppearanceSettings.ColorScheme.Custom -> {
            val colors by remember(darkTheme) {
                dataStore.data.map { if (darkTheme) it.appearance.customColors.darkScheme else it.appearance.customColors.lightScheme }
            }.collectAsState(null)
            val state = remember(colors, darkTheme) {
                mutableStateOf(
                    colors?.let { CustomColorScheme(it) }
                        ?: if (darkTheme) DarkDefaultColorScheme else LightDefaultColorScheme
                )
            }
            return state
        }
        else -> {
            val scheme = if (darkTheme) {
                darkColorSchemeOf(Theme(DefaultThemeId, name = ""))
            } else {
                lightColorSchemeOf(Theme(DefaultThemeId, name = ""))
            }
            return remember(scheme, darkTheme) {
                mutableStateOf(scheme)
            }
        }
    }

}

fun getTypography(context: Context, font: AppearanceSettings.Font?): Typography {
    return when (font) {
        AppearanceSettings.Font.SystemDefault -> getDeviceDefaultTypography(context)
        else -> DefaultTypography
    }
}