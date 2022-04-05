package org.smartregister.fhircore.quest.tests

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(*[LaunchActivityTest::class,
    RegisterClientTest::class])
class ESP_start