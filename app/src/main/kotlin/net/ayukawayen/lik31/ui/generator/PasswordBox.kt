package net.ayukawayen.lik31.ui.generator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ayukawayen.lik31.R

/** Shows the generated password; tapping it copies and closes. */
@Composable
fun PasswordBox(
    password: String,
    onCopy: () -> Unit,
) {
    val hasPassword = password.isNotEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(
            onClick = onCopy,
            enabled = hasPassword,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = if (hasPassword) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (hasPassword) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ) {
            Box(
                modifier = Modifier
                    .heightIn(min = 56.dp)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (hasPassword) {
                    Text(
                        text = password,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        // Up to 75 characters: shrink and let long passwords wrap.
                        fontSize = if (password.length <= 20) 20.sp else 16.sp,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.password_placeholder),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.copy_hint),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}
