#!/bin/sh
./gradlew :quest:assembleEcbisDebugAndroidTest --stacktrace && \
/Users/runner/Library/Android/sdk/platform-tools/adb install quest/build/outputs/apk/androidTest/ecbis/debug/quest-ecbis-debug-androidTest.apk && \
./gradlew :quest:assembleEcbisDebug --stacktrace && \
/Users/runner/Library/Android/sdk/platform-tools/adb install quest/build/outputs/apk/ecbis/debug/quest-ecbis-debug.apk && \
/Users/runner/Library/Android/sdk/platform-tools/adb shell am instrument -w -e package org.smartregister.fhircore.quest.performance -e "androidx.benchmark.suppressErrors" ACTIVITY-MISSING,CODE-COVERAGE,DEBUGGABLE,UNLOCKED,EMULATOR -e additionalTestOutputDir "/sdcard/Download/" org.smartregister.opensrp.ecbis.test/org.smartregister.fhircore.quest.QuestTestRunner && \
/Users/runner/Library/Android/sdk/platform-tools/adb shell ls /sdcard/Download/ && \
/Users/runner/Library/Android/sdk/platform-tools/adb pull /sdcard/Download/org.smartregister.opensrp.ecbis-benchmarkData.json quest/ && \
./gradlew :quest:assembleOpensrpDebug --stacktrace