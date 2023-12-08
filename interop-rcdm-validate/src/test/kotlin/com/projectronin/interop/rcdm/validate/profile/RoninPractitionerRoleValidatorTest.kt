package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.fhir.r4.validate.resource.R4PractitionerRoleValidator
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.validate.profile.util.requiredIdentifiers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninPractitionerRoleValidatorTest {
    private val validator = RoninPractitionerRoleValidator()

    @Test
    fun `returns supported resource`() {
        assertEquals(PractitionerRole::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4PractitionerRoleValidator, validator.r4Validator)
    }

    @Test
    fun `returns profile`() {
        assertEquals(RoninProfile.PRACTITIONER_ROLE, validator.profile)
    }

    @Test
    fun `validate succeeds with required`() {
        val practitionerRole =
            PractitionerRole(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.PRACTITIONER_ROLE.value)),
                        source = Uri("source"),
                    ),
                identifier = requiredIdentifiers,
            )

        val validation = validator.validate(practitionerRole, LocationContext(PractitionerRole::class))
        assertEquals(0, validation.issues().size)
    }
}
