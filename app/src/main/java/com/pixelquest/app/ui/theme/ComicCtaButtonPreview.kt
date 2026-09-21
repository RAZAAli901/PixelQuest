package com.pixelquest.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.ComicButton
import com.pixelquest.app.ui.components.ComicButtonVariant

/**
 * Step 32: Validates the coral-red CTA button styling (black border, black shadow, bold text)
 * against the Nitnode reference's "Book a 15-min teardown" button specifically.
 */
@Preview(name = "Nitnode Reference CTA Button", showBackground = true, backgroundColor = 0xFFFAF8F5)
@Composable
fun ComicCtaButtonPreview() {
    var clickCount by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ComicTokens.PaperBackground)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NITNODE CTA BUTTON FIDELITY",
            style = ComicTypography.headlineMedium,
            color = ComicTokens.CoralRed
        )

        Text(
            text = "Validating coral-red (#FF5A4E) fill, solid 2.5dp black border, 4dp flat offset shadow, and heavy bold text",
            style = ComicTypography.bodySmall,
            color = ComicTokens.TextSecondary
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Reference: "Book a 15-min teardown" primary coral-red CTA button (full-width)
        ComicButton(
            text = "Book a 15-min teardown",
            onClick = { clickCount++ },
            variant = ComicButtonVariant.PRIMARY,
            modifier = Modifier.fillMaxWidth()
        )

        // Reference: "Book a 15-min teardown" primary coral-red CTA button (wrap content)
        ComicButton(
            text = "Book a 15-min teardown",
            onClick = { clickCount++ },
            variant = ComicButtonVariant.PRIMARY
        )

        if (clickCount > 0) {
            Text(
                text = "Tactile press registered! ($clickCount)",
                style = ComicTypography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = ComicTokens.TextPrimary
            )
        }
    }
}
