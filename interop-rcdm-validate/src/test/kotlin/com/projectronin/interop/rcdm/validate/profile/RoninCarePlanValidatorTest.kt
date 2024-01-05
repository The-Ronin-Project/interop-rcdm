package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.fhir.r4.validate.resource.R4CarePlanValidator
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.validate.profile.util.requiredIdentifiers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Suppress("ktlint:standard:max-line-length")
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

    @Test
    fun `validate succeeds with category extension matching size`() {
        val carePlan =
            CarePlan(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.CARE_PLAN.canonical)),
                identifier = requiredIdentifiers,
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = Uri("category-system"),
                                        code = Code("category-code"),
                                    ),
                                ),
                            extension =
                                listOf(
                                    Extension(
                                        url = RoninExtension.TENANT_SOURCE_CARE_PLAN_CATEGORY.uri,
                                        value =
                                            DynamicValue(
                                                DynamicValueType.CODEABLE_CONCEPT,
                                                CodeableConcept(
                                                    coding =
                                                        listOf(
                                                            Coding(
                                                                system = Uri("category-system"),
                                                                code = Code("category-code"),
                                                            ),
                                                        ),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        val validation = validator.validate(carePlan, LocationContext(CarePlan::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate succeeds with extension filter on category extensions`() {
        val carePlan =
            CarePlan(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.CARE_PLAN.canonical)),
                identifier = requiredIdentifiers,
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = Uri("category-system"),
                                        code = Code("category-code"),
                                    ),
                                ),
                            extension =
                                listOf(
                                    Extension(
                                        url = RoninExtension.TENANT_SOURCE_CARE_PLAN_CATEGORY.uri,
                                        value =
                                            DynamicValue(
                                                DynamicValueType.CODEABLE_CONCEPT,
                                                CodeableConcept(
                                                    coding =
                                                        listOf(
                                                            Coding(
                                                                system = Uri("category-system"),
                                                                code = Code("category-code"),
                                                            ),
                                                        ),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        val validation = validator.validate(carePlan, LocationContext(CarePlan::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate fails with extension missing on category entry`() {
        val carePlan =
            CarePlan(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.CARE_PLAN.canonical)),
                identifier = requiredIdentifiers,
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = Uri("category-system"),
                                        code = Code("category-code"),
                                    ),
                                ),
                            extension =
                                listOf(
                                    Extension(
                                        url = RoninExtension.TENANT_SOURCE_CARE_PLAN_CATEGORY.uri,
                                        value =
                                            DynamicValue(
                                                DynamicValueType.CODEABLE_CONCEPT,
                                                CodeableConcept(
                                                    coding =
                                                        listOf(
                                                            Coding(
                                                                system = Uri("category-system"),
                                                                code = Code("category-code"),
                                                            ),
                                                        ),
                                                ),
                                            ),
                                    ),
                                ),
                        ),
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = Uri("category-system"),
                                        code = Code("category-code"),
                                    ),
                                ),
                        ),
                    ),
            )

        val validation = validator.validate(carePlan, LocationContext(CarePlan::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_CAREPLAN_001: CarePlan category entries must each contain the tenantSourceCarePlanCategory extension @ CarePlan.category",
            validation.issues().first().toString(),
        )
    }
}
