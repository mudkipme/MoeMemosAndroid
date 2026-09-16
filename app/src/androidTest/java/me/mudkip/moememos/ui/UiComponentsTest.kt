package me.mudkip.moememos.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import me.mudkip.moememos.R
import me.mudkip.moememos.ui.component.ActionIconButton
import me.mudkip.moememos.ui.page.memos.MemoListDetailLayout
import me.mudkip.moememos.ui.page.memos.MemosNavigationDrawer
import me.mudkip.moememos.ui.page.memos.MemoSearchBar
import me.mudkip.moememos.ui.page.memos.PagingStatus
import me.mudkip.moememos.ui.page.settings.SettingSwitchItem
import me.mudkip.moememos.ui.util.edgeToEdgeContentPadding
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UiComponentsTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun drawerHandlesSystemBackWithoutACustomHandler() {
        var drawer: DrawerState? = null
        compose.setContent {
            MaterialTheme {
                val state = rememberDrawerState(DrawerValue.Open)
                drawer = state
                ModalNavigationDrawer(
                    drawerState = state,
                    drawerContent = { ModalDrawerSheet(drawerState = state) { Text("Drawer") } },
                ) { Text("Main content") }
            }
        }
        compose.onNodeWithText("Drawer").assertIsDisplayed()
        Espresso.pressBack()
        compose.runOnIdle { assertEquals(DrawerValue.Closed, requireNotNull(drawer).currentValue) }
    }

    @Test fun settingRowExposesOneSwitchAndTogglesFromItsLabel() {
        compose.setContent {
            MaterialTheme {
                var checked by remember { mutableStateOf(false) }
                SettingSwitchItem(Icons.Outlined.Save, "Autosave", checked = checked) { checked = it }
            }
        }
        compose.onAllNodesWithText("Autosave").assertCountEquals(1)
        compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsOff().performClick().assertIsOn()
    }

    @Test fun disabledSettingCannotBeToggled() {
        compose.setContent {
            MaterialTheme {
                SettingSwitchItem(Icons.Outlined.Save, "Autosave", checked = false, enabled = false) {
                    error("A disabled setting must not be changed")
                }
            }
        }
        compose.onNodeWithText("Autosave").assertIsNotEnabled().assertIsOff()
    }

    @Test fun iconOnlyActionHasAnAccessibleLabel() {
        var clicks = 0
        compose.setContent {
            MaterialTheme {
                ActionIconButton("More options", onClick = { clicks++ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null)
                }
            }
        }
        compose.onNodeWithContentDescription("More options").performClick()
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test fun searchRestoresItsQueryAndClearActionRemovesIt() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MaterialTheme { MemoSearchBar(rememberTextFieldState(), onBack = {}) }
        }
        compose.onNode(hasSetTextAction()).performTextInput("offline notes")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasSetTextAction()).assertTextEquals("offline notes")
        compose.onNodeWithContentDescription(context.getString(R.string.clear_search)).performClick()
        compose.onNode(hasSetTextAction()).assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
        )
    }

    @Test fun rtlPaddingPreservesPhysicalInsetsAndAddsBottomSpace() {
        var result: PaddingValues? = null
        compose.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                result = edgeToEdgeContentPadding(
                    PaddingValues.Absolute(left = 12.dp, right = 28.dp, bottom = 16.dp),
                    additionalBottomPadding = 20.dp,
                )
            }
        }
        compose.runOnIdle {
            val padding = requireNotNull(result)
            assertEquals(12.dp, padding.calculateLeftPadding(LayoutDirection.Rtl))
            assertEquals(28.dp, padding.calculateRightPadding(LayoutDirection.Rtl))
            assertEquals(36.dp, padding.calculateBottomPadding())
        }
    }

    @Test fun pagingFailureExposesWorkingRetryAction() {
        var retries = 0
        compose.setContent {
            MaterialTheme { PagingStatus(LoadState.Error(IllegalStateException("offline"))) { retries++ } }
        }
        compose.onNodeWithText(context.getString(R.string.failed_to_load_memos)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.retry)).performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun compactMemoSelectionRestoresAndBackReturnsToList() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            MaterialTheme {
                // Constrain logical width to a phone-sized pane on any test device.
                CompositionLocalProvider(LocalDensity provides Density(4f)) {
                    MemoListDetailLayout(
                        listPane = { select -> Button(onClick = { select("memo-one") }) { Text("Open memo") } },
                        detailPane = { id, back -> Button(onClick = back) { Text("Close $id") } },
                    )
                }
            }
        }
        compose.onNodeWithText("Open memo").performClick()
        compose.onNodeWithText("Close memo-one").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Close memo-one").assertIsDisplayed().performClick()
        compose.onNodeWithText("Open memo").assertIsDisplayed()
    }

    @Test fun selectionSurvivesChangingBetweenOneAndTwoPanes() {
        var wide by mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalDensity provides Density(if (wide) 0.5f else 4f)) {
                    MemosNavigationDrawer(permanent = wide, drawerContent = { Text("Navigation") }) {
                        MemoListDetailLayout(
                            listPane = { select -> Button(onClick = { select("memo-two") }) { Text("List") } },
                            detailPane = { id, _ -> Text(id) },
                        )
                    }
                }
            }
        }
        compose.onNodeWithText("List").performClick()
        compose.onNodeWithText("List").assertIsDisplayed()
        compose.onNodeWithText("memo-two").assertIsDisplayed()
        compose.runOnIdle { wide = false }
        compose.onNodeWithText("memo-two").assertIsDisplayed()
        compose.runOnIdle { wide = true }
        compose.onNodeWithText("List").assertIsDisplayed()
        compose.onNodeWithText("memo-two").assertIsDisplayed()
    }
}
