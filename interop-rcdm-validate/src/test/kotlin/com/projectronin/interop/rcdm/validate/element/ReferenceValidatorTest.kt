package com.projectronin.interop.rcdm.validate.element

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.dataAuthorityExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReferenceValidatorTest {
    private val validator = ReferenceValidator()

    @Test
    fun `returns supported element`() {
        assertEquals(Reference::class, validator.supportedElement)
    }

    @Test
    fun `validates if no type`() {
        val reference = Reference(
            type = null
        )

        val validation =
            validator.validate(reference, listOf(RoninProfile.PATIENT), LocationContext(Patient::managingOrganization))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validates with type and no reference`() {
        val reference = Reference(
            type = Uri("Organization"),
            reference = null
        )

        val validation =
            validator.validate(reference, listOf(RoninProfile.PATIENT), LocationContext(Patient::managingOrganization))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `fails if no extension with type and reference`() {
        val reference = Reference(
            type = Uri("Organization", extension = listOf()),
            reference = FHIRString("Organization/1234")
        )

        val validation =
            validator.validate(reference, listOf(RoninProfile.PATIENT), LocationContext(Patient::managingOrganization))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_DAUTH_EX_001: Data Authority extension identifier is required for reference @ Patient.managingOrganization.type.extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `fails if extension is not the data authority extension with type and reference`() {
        val reference = Reference(
            type = Uri(
                "Organization",
                extension = listOf(Extension(value = DynamicValue(DynamicValueType.BOOLEAN, FHIRBoolean.TRUE)))
            ),
            reference = FHIRString("Organization/1234")
        )

        val validation =
            validator.validate(reference, listOf(RoninProfile.PATIENT), LocationContext(Patient::managingOrganization))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_DAUTH_EX_001: Data Authority extension identifier is required for reference @ Patient.managingOrganization.type.extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validates with type, reference and data authority extension`() {
        val reference = Reference(
            type = Uri("Organization", extension = dataAuthorityExtension),
            reference = FHIRString("Organization/1234")
        )

        val validation =
            validator.validate(reference, listOf(RoninProfile.PATIENT), LocationContext(Patient::managingOrganization))
        assertEquals(0, validation.issues().size)
    }
}
