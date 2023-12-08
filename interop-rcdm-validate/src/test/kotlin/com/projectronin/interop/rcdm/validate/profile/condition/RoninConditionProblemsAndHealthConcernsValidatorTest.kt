package com.projectronin.interop.rcdm.validate.profile.condition

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.validate.profile.util.requiredIdentifiers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Suppress("ktlint:standard:max-line-length")
class RoninConditionProblemsAndHealthConcernsValidatorTest {
    private val validator = RoninConditionProblemsAndHealthConcernsValidator()

    @Test
    fun `returns proper profile`() {
        assertEquals(RoninProfile.CONDITION_PROBLEMS_CONCERNS, validator.profile)
    }

    @Test
    fun `validate fails if category outside qualifying categories`() {
        val condition =
            Condition(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                subject = Reference(reference = FHIRString("Patient/1234")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(system = CodeSystem.CONDITION_CATEGORY.uri, code = Code("not-qualified")),
                                ),
                        ),
                    ),
                code =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("system"),
                                    code = Code("code"),
                                    display = FHIRString("display"),
                                ),
                            ),
                    ),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_CONDITION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(text = FHIRString("tenant code")),
                                ),
                        ),
                    ),
            )

        val validation = validator.validate(condition, LocationContext(Condition::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_CND_001: Must match this system|code: http://terminology.hl7.org/CodeSystem/condition-category|problem-list-item, http://hl7.org/fhir/us/core/CodeSystem/condition-category|health-concern @ Condition.category",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate works for problem-list-item`() {
        val condition =
            Condition(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                subject = Reference(reference = FHIRString("Patient/1234")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(system = CodeSystem.CONDITION_CATEGORY.uri, code = Code("problem-list-item")),
                                ),
                        ),
                    ),
                code =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("system"),
                                    code = Code("code"),
                                    display = FHIRString("display"),
                                ),
                            ),
                    ),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_CONDITION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(text = FHIRString("tenant code")),
                                ),
                        ),
                    ),
            )

        val validation = validator.validate(condition, LocationContext(Condition::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate works for health-concern`() {
        val condition =
            Condition(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                subject = Reference(reference = FHIRString("Patient/1234")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                                        code = Code("health-concern"),
                                    ),
                                ),
                        ),
                    ),
                code =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("system"),
                                    code = Code("code"),
                                    display = FHIRString("display"),
                                ),
                            ),
                    ),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_CONDITION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(text = FHIRString("tenant code")),
                                ),
                        ),
                    ),
            )

        val validation = validator.validate(condition, LocationContext(Condition::class))
        assertEquals(0, validation.issues().size)
    }
}
