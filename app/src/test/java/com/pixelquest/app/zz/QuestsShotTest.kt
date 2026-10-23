package com.pixelquest.app.zz

import com.pixelquest.app.ui.components.QuestCategory
import com.pixelquest.app.ui.components.QuestUi
import com.pixelquest.app.ui.screens.QuestGroup
import com.pixelquest.app.ui.screens.QuestTab
import com.pixelquest.app.ui.screens.QuestsContent
import com.pixelquest.app.ui.theme.PqSkin
import org.junit.Test

class QuestsShotTest : ScratchShot() {
    private val groups = listOf(
        QuestGroup("m", "Morning · 2 done", listOf(
            QuestUi(1, "20 Push-ups", "08:00", QuestCategory.Fitness, "Daily", done = true),
            QuestUi(2, "Ring_Test", "11:18", QuestCategory.Fitness, done = true))),
        QuestGroup("e", "Evening · 2 left", listOf(
            QuestUi(3, "Go to Gym", "17:30", QuestCategory.Fitness, "Daily", overdue = true),
            QuestUi(4, "Read 15 Pages", "21:00", QuestCategory.Learning, "Daily"))),
    )
    @Test fun comic() = shot("quests", PqSkin.Comic) { QuestsContent(QuestTab.Today, {}, 4, groups, {}, {}, {}, canSkip = { !it.done }) }
    @Test fun pixel() = shot("quests", PqSkin.Pixel) { QuestsContent(QuestTab.Today, {}, 4, groups, {}, {}, {}, query = "") }
}
