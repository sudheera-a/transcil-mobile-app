/**
 * Unit tests for mapping API document type strings to onboarding slot types.
 */
package com.transcil.rider.kyc

import com.transcil.rider.repository.KycDocumentRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OtherDocsSideTypesTest {

    @Test
    fun apiDocType_matchesOnboardingSingleSlot() {
        // Spec: one file → one doc_type → one S3 object.
        assertEquals("driving_license", KycDocumentRepository.apiDocType("Driving License"))
        assertEquals("pan", KycDocumentRepository.apiDocType("PAN Card"))
        assertEquals("voter_id", KycDocumentRepository.apiDocType("Voter ID Card"))
    }

    @Test
    fun unknown_returnsNull() {
        assertNull(KycDocumentRepository.apiDocType("Passport"))
    }
}
