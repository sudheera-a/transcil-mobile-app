package com.example.transcilmobileapp.kyc

import com.example.transcilmobileapp.core.KycStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KycProgressStatusRedirectTest {

    @Test
    fun verified_redirectsToApproved() {
        assertEquals(
            KycStatus.APPROVED,
            KycProgressViewModel.statusRedirectFor(
                verified = true,
                allComplete = true,
                documentsOverall = "approved",
            ),
        )
    }

    @Test
    fun allCompleteInProgress_redirectsToPending() {
        assertEquals(
            KycStatus.PENDING,
            KycProgressViewModel.statusRedirectFor(
                verified = false,
                allComplete = true,
                documentsOverall = "in_progress",
            ),
        )
    }

    @Test
    fun incomplete_staysOnProgress() {
        assertNull(
            KycProgressViewModel.statusRedirectFor(
                verified = false,
                allComplete = false,
                documentsOverall = null,
            ),
        )
    }
}
