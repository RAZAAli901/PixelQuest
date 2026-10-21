package com.pixelquest.app.chess.ui

import androidx.annotation.StringRes
import com.pixelquest.app.R
import com.pixelquest.app.chess.domain.Tier

/** A tier's name. */
@StringRes
fun tierName(tier: Tier): Int = when (tier) {
    Tier.PAWN -> R.string.chess_tier_pawn
    Tier.KNIGHT -> R.string.chess_tier_knight
    Tier.BISHOP -> R.string.chess_tier_bishop
    Tier.ROOK -> R.string.chess_tier_rook
    Tier.QUEEN -> R.string.chess_tier_queen
    Tier.KING -> R.string.chess_tier_king
}
