package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.fhir.r4.validate.resource.R4CarePlanValidator
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.validate.profile.util.requiredIdentifiers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninCarePlanValidatorTest {
    private val validator = RoninCarePlanValidator()

    @Test
    fun `returns supported resource`() {
        assertEquals(CarePlan::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4CarePlanValidator, validator.r4Validator)
    }

    @Test
    fun `returns profile`() {
        assertEquals(RoninProfile.CARE_PLAN, validator.profile)
    }

    @Test
    fun `validate succeeds`() {
        val carePlan =
            CarePlan(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.CARE_PLAN.canonical)),
                identifier = requiredIdentifiers,
            )

        val validation = validator.validate(carePlan, LocationContext(CarePlan::class))
        assertEquals(0, validation.issues().size)
    }
}
