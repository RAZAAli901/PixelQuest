package com.pixelquest.app.darood.ui

import java.text.NumberFormat

/** A count as the phone's language writes it: 2,450 in English, 2.450 in German. */
fun formatCount(count: Long): String = NumberFormat.getIntegerInstance().format(count)

fun formatCount(count: Int): String = formatCount(count.toLong())
