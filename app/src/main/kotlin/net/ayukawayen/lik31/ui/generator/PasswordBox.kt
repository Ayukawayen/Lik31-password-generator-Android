package net.ayukawayen.lik31.ui.generator

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.ayukawayen.lik31.R

/** Press this long before the "release to copy" hint starts alternating with the password. */
private const val HINT_DELAY_MS = 500L

/** How long the hint and the password are each shown while the press continues. */
private const val HINT_PERIOD_MS = 1000L

private val BoxPaddingH = 12.dp
private val BoxPaddingV = 8.dp

/**
 * The generated password.
 *
 * - Always at least two lines tall, with or without a password, so the card
 *   does not change height while typing.
 * - Lines are broken by [PasswordLines] (first line as full as possible,
 *   shrinking from 20sp to 14sp before a third line is allowed).
 * - A tap copies and closes immediately. A long press shows "release to copy
 *   & close" alternating with the password; releasing copies and closes,
 *   sliding off the box before releasing cancels.
 */
@Composable
fun PasswordBox(
    password: String,
    onCopy: () -> Unit,
) {
    val hasPassword = password.isNotEmpty()
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val currentOnCopy by rememberUpdatedState(onCopy)

    var pressing by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    LaunchedEffect(pressing) {
        showHint = false
        if (!pressing) return@LaunchedEffect
        delay(HINT_DELAY_MS)
        while (true) {
            showHint = true
            delay(HINT_PERIOD_MS)
            showHint = false
            delay(HINT_PERIOD_MS)
        }
    }

    // Two lines of the largest password text. Converted line by line, because
    // with Android 14+ non-linear font scaling 2 * x.sp is not (2x).sp.
    val lineHeightSp = PasswordLines.MAX_FONT_SP * PasswordLines.LINE_HEIGHT_FACTOR
    val minHeight = with(density) { lineHeightSp.sp.toDp() } * PasswordLines.TARGET_LINES + BoxPaddingV * 2

    val baseStyle = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
    )
    val copyLabel = stringResource(R.string.copy_and_close)
    val containerColor = when {
        !hasPassword -> MaterialTheme.colorScheme.surfaceVariant
        pressing -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val contentColor = if (hasPassword) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .then(
                if (hasPassword) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    pressing = true
                                    // false when the finger slides off or the gesture is cancelled
                                    tryAwaitRelease()
                                    pressing = false
                                },
                                // Called only for a release inside the box, however long the press.
                                onTap = { currentOnCopy() },
                            )
                        }
                        .semantics(mergeDescendants = true) {
                            role = Role.Button
                            onClick(label = copyLabel) {
                                currentOnCopy()
                                true
                            }
                        }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = BoxPaddingH, vertical = BoxPaddingV),
        contentAlignment = Alignment.Center,
    ) {
        if (!hasPassword) {
            Text(
                text = stringResource(R.string.password_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        } else {
            // Leave half a dp so rounding never pushes a line past the edge.
            val availablePx = constraints.maxWidth - with(density) { 0.5.dp.toPx() }
            val layout = remember(password, availablePx, density) {
                val cache = HashMap<Pair<Char, Int>, Float>()
                PasswordLines.fit(password, availablePx) { c, sp ->
                    cache.getOrPut(c to sp) {
                        // Measure ten copies to average out per-glyph pixel rounding.
                        measurer.measure(c.toString().repeat(10), baseStyle.copy(fontSize = sp.sp)).size.width / 10f
                    }
                }
            }

            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = layout.lines.joinToString("\n"),
                    modifier = Modifier.alpha(if (showHint) 0f else 1f),
                    style = baseStyle.copy(
                        fontSize = layout.fontSizeSp.sp,
                        lineHeight = (layout.fontSizeSp * PasswordLines.LINE_HEIGHT_FACTOR).sp,
                    ),
                    color = contentColor,
                    textAlign = TextAlign.Center,
                    // Lines are already broken; never let the text engine break again.
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )
                if (showHint) {
                    Text(
                        text = stringResource(R.string.release_to_copy),
                        style = MaterialTheme.typography.titleMedium,
                        color = contentColor,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
