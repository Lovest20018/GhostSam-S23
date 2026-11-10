package com.snothin.ghostsam.data.device

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class S23TargetTest {
    @Test
    fun acceptsAndroid16OnTheTargetModel() {
        assertTrue(S23Target.matches("SM-S9110", 36))
    }

    @Test
    fun acceptsFutureAndroidVersionsWithoutAnUpperSdkLimit() {
        assertTrue(S23Target.matches("SM-S9110", 37))
        assertTrue(S23Target.matches("SM-S9110", 38))
    }

    @Test
    fun rejectsOtherModelsAndOlderAndroidVersions() {
        assertFalse(S23Target.matches("SM-S9160", 36))
        assertFalse(S23Target.matches("SM-S911B", 36))
        assertFalse(S23Target.matches("", 36))
        assertFalse(S23Target.matches("SM-S9110", 35))
        assertFalse(S23Target.matches("SM-S9110", 0))
    }
}
