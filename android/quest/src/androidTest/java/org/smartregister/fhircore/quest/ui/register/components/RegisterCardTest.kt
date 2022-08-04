package org.smartregister.fhircore.quest.ui.register.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.hl7.fhir.r4.model.Patient
import org.junit.Rule
import org.junit.Test
import org.smartregister.fhircore.engine.configuration.view.*
import org.smartregister.fhircore.engine.domain.model.ResourceData
import org.smartregister.fhircore.engine.domain.model.ServiceStatus
import org.smartregister.fhircore.engine.domain.model.ViewType
import org.smartregister.fhircore.engine.ui.theme.AppTheme

class RegisterCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun registerCardShouldRenderViewGroups() {
        val registerCardViewProperties = listOf<RegisterCardViewProperties>(
            ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children = listOf(
                    ViewGroupProperties(
                        viewType = ViewType.ROW,
                    ),
                    ViewGroupProperties(
                        viewType = ViewType.COLUMN,
                    )
                )
            )
        )

        composeTestRule.setContent {
            AppTheme {
                RegisterCard(
                    registerCardViewProperties = registerCardViewProperties,
                    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
                    onCardClick = {}
                )
            }
        }

        composeTestRule.onAllNodesWithTag(TEST_TAG_VIEW_GROUP_ROW).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(TEST_TAG_VIEW_GROUP_COLUMN).assertCountEquals(1)
    }

    @Test
    fun registerCardShouldRenderCompoundTextPrimaryText() {
        val registerCardViewProperties = listOf<RegisterCardViewProperties>(
            ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children = listOf(
                    ViewGroupProperties(
                        viewType = ViewType.ROW,
                        children = listOf(
                                CompoundTextProperties(
                                    viewType = ViewType.COMPOUND_TEXT,
                                    primaryText = "Amalia Rizky",
                                    primaryTextColor = "#000000",
                                )
                            )
                        )
                    )
            )
        )

        composeTestRule.setContent {
            AppTheme {
                RegisterCard(
                    registerCardViewProperties = registerCardViewProperties,
                    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
                    onCardClick = {}
                )
            }
        }

        composeTestRule.onAllNodesWithTag(TEST_TAG_COMPOUND_TEXT).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(TEST_TAG_COMPOUND_TEXT_PRIMARY).assertCountEquals(1)
        composeTestRule.onNodeWithTag(TEST_TAG_COMPOUND_TEXT_PRIMARY).assert(hasText("Amalia Rizky"))
    }

    @Test
    fun registerCardShouldRenderCompoundTextPrimaryAndSecondaryTextWithSeparator() {
        val registerCardViewProperties = listOf<RegisterCardViewProperties>(
            ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children = listOf(
                    ViewGroupProperties(
                        viewType = ViewType.ROW,
                        children = listOf(
                                CompoundTextProperties(
                                    viewType = ViewType.COMPOUND_TEXT,
                                    primaryText = "Amalia Rizky",
                                    primaryTextColor = "#000000",
                                    secondaryText = "Female",
                                    secondaryTextColor = "#555AAA",
                                    separator = "/"
                                )
                            )
                        )
                    )
            )
        )

        composeTestRule.setContent {
            AppTheme {
                RegisterCard(
                    registerCardViewProperties = registerCardViewProperties,
                    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
                    onCardClick = {}
                )
            }
        }

        composeTestRule.onAllNodesWithTag(TEST_TAG_COMPOUND_TEXT).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(TEST_TAG_COMPOUND_TEXT_PRIMARY).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(TEST_TAG_COMPOUND_TEXT_SECONDARY).assertCountEquals(1)
        composeTestRule.onNodeWithTag(TEST_TAG_COMPOUND_TEXT_PRIMARY).assert(hasText("Amalia Rizky"))
        composeTestRule.onNodeWithTag(TEST_TAG_COMPOUND_TEXT_SECONDARY).assert(hasText("Female"))
        composeTestRule.onNodeWithTag(TEST_TAG_COMPOUND_TEXT_SEPARATOR).assert(hasText("/"))
    }

    @Test
    fun registerCardShouldRenderCompoundTextPrimaryAndSecondaryTextWithDefaultSeparator() {
        val registerCardViewProperties = listOf<RegisterCardViewProperties>(
            ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children = listOf(
                    ViewGroupProperties(
                        viewType = ViewType.ROW,
                        children = listOf(
                                CompoundTextProperties(
                                    primaryText = "Amalia Rizky",
                                    primaryTextColor = "#000000",
                                    secondaryText = "Female",
                                    secondaryTextColor = "#555AAA",
                                )
                            )
                        )
                    )
            )
        )

        composeTestRule.setContent {
            AppTheme {
                RegisterCard(
                    registerCardViewProperties = registerCardViewProperties,
                    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
                    onCardClick = {}
                )
            }
        }

        composeTestRule.onAllNodesWithTag(TEST_TAG_COMPOUND_TEXT).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(TEST_TAG_COMPOUND_TEXT_PRIMARY).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(TEST_TAG_COMPOUND_TEXT_SECONDARY).assertCountEquals(1)
        composeTestRule.onNodeWithTag(TEST_TAG_COMPOUND_TEXT_PRIMARY).assert(hasText("Amalia Rizky"))
        composeTestRule.onNodeWithTag(TEST_TAG_COMPOUND_TEXT_SECONDARY).assert(hasText("Female"))
        composeTestRule.onNodeWithTag(TEST_TAG_COMPOUND_TEXT_SEPARATOR).assert(hasText("-"))
    }

    @Test
    fun registerCardShouldRenderServiceCardDetailsWithNoVerticalDivider() {
        val registerCardViewProperties = listOf<RegisterCardViewProperties>(
            ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children =
                listOf(
                    ServiceCardProperties(
                        viewType = ViewType.SERVICE_CARD,
                        details =
                        listOf(
                            CompoundTextProperties(
                                viewType = ViewType.COMPOUND_TEXT,
                                primaryText = "Amalia Rizky",
                                primaryTextColor = "#000000"
                            ),
                            CompoundTextProperties(
                                viewType = ViewType.COMPOUND_TEXT,
                                primaryText = "Female",
                                primaryTextColor = "#5A5A5A"
                            )
                        ),
                        showVerticalDivider = false,
                    )
                )
            )
        )

        composeTestRule.setContent {
            AppTheme {
                RegisterCard(
                    registerCardViewProperties = registerCardViewProperties,
                    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
                    onCardClick = {}
                )
            }
        }

        composeTestRule.onAllNodesWithTag(TEST_TAG_SERVICE_CARD).assertCountEquals(1)
        composeTestRule.onAllNodesWithTag(TEST_TAG_COMPOUND_TEXT, useUnmergedTree = true).assertCountEquals(2)
        composeTestRule.onAllNodesWithTag(TEST_TAG_COMPOUND_TEXT_PRIMARY, useUnmergedTree = true).assertCountEquals(2)
        composeTestRule.onAllNodesWithTag(TEST_TAG_COMPOUND_TEXT_PRIMARY, useUnmergedTree = true).assertAny(hasText("Amalia Rizky"))
        composeTestRule.onAllNodesWithTag(TEST_TAG_COMPOUND_TEXT_PRIMARY, useUnmergedTree = true).assertAny(hasText("Female"))
        composeTestRule.onAllNodesWithTag(TEST_TAG_COMPOUND_TEXT_SEPARATOR).assertAll(hasText("-"))
        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_VERTICAL_DIVIDER).assertDoesNotExist()
    }

    @Test
    fun registerCardShouldNotRenderNullServiceButton() {
        val registerCardViewProperties = listOf<RegisterCardViewProperties>(
            ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children =
                listOf(
                    ServiceCardProperties(
                        serviceButton = null
                    )
                    )
                )
            )


        composeTestRule.setContent {
            AppTheme {
                RegisterCard(
                    registerCardViewProperties = registerCardViewProperties,
                    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
                    onCardClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_SMALL_BUTTON).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_BIG_BUTTON).assertDoesNotExist()
    }

    @Test
    fun registerCardShouldNotRenderNonVisibleServiceButton() {
        val registerCardViewProperties = listOf<RegisterCardViewProperties>(
            ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children =
                listOf(
                    ServiceCardProperties(
                        serviceButton = ServiceButton(
                            visible = false,
                            status = ""
                        )
                    )
                    )
                )
            )


        composeTestRule.setContent {
            AppTheme {
                RegisterCard(
                    registerCardViewProperties = registerCardViewProperties,
                    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
                    onCardClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_SMALL_BUTTON).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_BIG_BUTTON).assertDoesNotExist()
    }

    @Test
    fun registerCardShouldRenderSmallServiceButtonWithDefaultText() {
        val registerCardViewProperties = listOf<RegisterCardViewProperties>(
            ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children =
                listOf(
                    ServiceCardProperties(
                        serviceButton = ServiceButton(
                            visible = true,
                            status = ServiceStatus.DUE.name,
                            smallSized = true
                        )
                    )
                    )
                )
            )


        composeTestRule.setContent {
            AppTheme {
                RegisterCard(
                    registerCardViewProperties = registerCardViewProperties,
                    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
                    onCardClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_SMALL_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_BUTTON_TEXT, useUnmergedTree = true).assert(hasText(""))
    }

    @Test
    fun registerCardShouldRenderSmallServiceButtonWithDueStatus() {
        val registerCardViewProperties = listOf<RegisterCardViewProperties>(
            ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children =
                listOf(
                    ServiceCardProperties(
                        serviceButton = ServiceButton(
                            visible = true,
                            text = "1",
                            status = ServiceStatus.DUE.name,
                            smallSized = true
                        )
                    )
                    )
                )
            )


        composeTestRule.setContent {
            AppTheme {
                RegisterCard(
                    registerCardViewProperties = registerCardViewProperties,
                    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
                    onCardClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_SMALL_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_BUTTON_TEXT, useUnmergedTree = true).assert(hasText("1"))
    }

    @Test
    fun registerCardShouldRenderSmallServiceButtonWithOverdueStatus() {
        val registerCardViewProperties = listOf<RegisterCardViewProperties>(
            ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children =
                listOf(
                    ServiceCardProperties(
                        serviceButton = ServiceButton(
                            visible = true,
                            text = "1",
                            status = ServiceStatus.OVERDUE.name,
                            smallSized = true
                        )
                    )
                    )
                )
            )


        composeTestRule.setContent {
            AppTheme {
                RegisterCard(
                    registerCardViewProperties = registerCardViewProperties,
                    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
                    onCardClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_SMALL_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_BUTTON_TEXT, useUnmergedTree = true).assert(
            hasText("1")
        )
    }

    @Test
    fun registerCardShouldRenderSmallServiceButtonWithUpcomingStatus() {
        val registerCardViewProperties = listOf<RegisterCardViewProperties>(
            ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children =
                listOf(
                    ServiceCardProperties(
                        serviceButton = ServiceButton(
                            visible = true,
                            text = "1",
                            status = ServiceStatus.UPCOMING.name,
                            smallSized = true
                        )
                    )
                    )
                )
            )


        composeTestRule.setContent {
            AppTheme {
                RegisterCard(
                    registerCardViewProperties = registerCardViewProperties,
                    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
                    onCardClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_SMALL_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_BUTTON_TEXT, useUnmergedTree = true).assert(
            hasText("1")
        )
    }

    @Test
    fun registerCardShouldRenderSmallServiceButtonWithCompletedStatus() {
        val registerCardViewProperties = listOf<RegisterCardViewProperties>(
            ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children =
                listOf(
                    ServiceCardProperties(
                        serviceButton = ServiceButton(
                            visible = true,
                            text = "1",
                            status = ServiceStatus.COMPLETED.name,
                            smallSized = true
                        )
                    )
                    )
                )
            )


        composeTestRule.setContent {
            AppTheme {
                RegisterCard(
                    registerCardViewProperties = registerCardViewProperties,
                    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
                    onCardClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_SMALL_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_BUTTON_TEXT, useUnmergedTree = true).assert(
            hasText("1")
        )
    }

    @Test
    fun registerCardShouldRenderBigServiceButtonWithDueStatus() {
        val registerCardViewProperties = listOf<RegisterCardViewProperties>(
            ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children =
                listOf(
                    ServiceCardProperties(
                        serviceButton = ServiceButton(
                            visible = true,
                            text = "1",
                            status = ServiceStatus.DUE.name,
                            smallSized = false
                        )
                    )
                    )
                )
            )


        composeTestRule.setContent {
            AppTheme {
                RegisterCard(
                    registerCardViewProperties = registerCardViewProperties,
                    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
                    onCardClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_BIG_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_BUTTON_TEXT, useUnmergedTree = true).assert(hasText("1"))
    }

    @Test
    fun registerCardShouldRenderBigServiceButtonWithOverdueStatus() {
        val registerCardViewProperties = listOf<RegisterCardViewProperties>(
            ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children =
                listOf(
                    ServiceCardProperties(
                        serviceButton = ServiceButton(
                            visible = true,
                            text = "1",
                            status = ServiceStatus.OVERDUE.name,
                            smallSized = false
                        )
                    )
                    )
                )
            )


        composeTestRule.setContent {
            AppTheme {
                RegisterCard(
                    registerCardViewProperties = registerCardViewProperties,
                    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
                    onCardClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_BIG_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_BUTTON_TEXT, useUnmergedTree = true).assert(
            hasText("1")
        )
    }

    @Test
    fun registerCardShouldRenderBigServiceButtonWithUpcomingStatus() {
        val registerCardViewProperties = listOf<RegisterCardViewProperties>(
            ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children =
                listOf(
                    ServiceCardProperties(
                        serviceButton = ServiceButton(
                            visible = true,
                            text = "1",
                            status = ServiceStatus.UPCOMING.name,
                            smallSized = false
                        )
                    )
                    )
                )
            )


        composeTestRule.setContent {
            AppTheme {
                RegisterCard(
                    registerCardViewProperties = registerCardViewProperties,
                    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
                    onCardClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_BIG_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_BUTTON_TEXT, useUnmergedTree = true).assert(
            hasText("1")
        )
    }

    @Test
    fun registerCardShouldRenderBigServiceButtonWithCompletedStatus() {
        val registerCardViewProperties = listOf<RegisterCardViewProperties>(
            ViewGroupProperties(
                viewType = ViewType.COLUMN,
                children =
                listOf(
                    ServiceCardProperties(
                        serviceButton = ServiceButton(
                            visible = true,
                            text = "1",
                            status = ServiceStatus.COMPLETED.name,
                            smallSized = false
                        )
                    )
                    )
                )
            )


        composeTestRule.setContent {
            AppTheme {
                RegisterCard(
                    registerCardViewProperties = registerCardViewProperties,
                    resourceData = ResourceData(Patient(), emptyMap(), emptyMap()),
                    onCardClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_BIG_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(TEST_TAG_SERVICE_BUTTON_TEXT, useUnmergedTree = true).assert(
            hasText("1")
        )
    }

}