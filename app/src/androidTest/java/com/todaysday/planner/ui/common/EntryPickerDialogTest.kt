package com.todaysday.planner.ui.common

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.todaysday.planner.domain.model.Goal
import com.todaysday.planner.domain.picker.PickerModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests the picker pop-up on the phone: open a goal, then choose a subgoal inside it. */
@RunWith(AndroidJUnit4::class)
class EntryPickerDialogTest {

    // The "rule" starts a tiny app around the composable we want to test.
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun click_your_way_down_to_a_subgoal_and_choose_it() {
        val entries = listOf(
            Goal(id = 1, name = "Learn Serbian"),
            Goal(id = 2, parentId = 1, name = "Book")
        )
        var chosenId: Long? = null

        composeRule.setContent {
            EntryPickerDialog(
                title = "Part of",
                model = PickerModel(entries),
                selectedIds = emptySet(),
                multiSelect = false,
                onSelect = { chosenId = it },
                onDismiss = {}
            )
        }

        // At the top level only "Learn Serbian" is shown. Open it with its arrow.
        composeRule.onNodeWithText("Learn Serbian").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open").performClick()

        // Now the subgoal is shown. Tap it to choose it.
        composeRule.onNodeWithText("Book").performClick()

        assertEquals(2L, chosenId)
    }
}