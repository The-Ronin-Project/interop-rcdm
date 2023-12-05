package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.validate.resource.R4LocationValidator
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.validate.profile.util.requiredIdentifiers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninLocationValidatorTest {
    private val validator = RoninLocationValidator()

    @Test
    fun `returns supported resource`() {
        assertEquals(Location::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4LocationValidator, validator.r4Validator)
    }

    @Test
    fun `returns profile`() {
        assertEquals(RoninProfile.LOCATION, validator.profile)
    }

    @Test
    fun `validate succeeds`() {
        val location = Location(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.LOCATION.canonical)),
            identifier = requiredIdentifiers
        )

        val validation = validator.validate(location, LocationContext(Location::class))
        assertEquals(0, validation.issues().size)
    }
}
