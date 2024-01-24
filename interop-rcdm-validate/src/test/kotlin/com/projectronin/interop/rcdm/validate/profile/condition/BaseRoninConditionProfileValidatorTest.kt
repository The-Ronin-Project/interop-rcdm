package com.projectronin.interop.rcdm.validate.profile.condition

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.validate.resource.R4ConditionValidator
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.validate.profile.util.requiredIdentifiers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class BaseRoninConditionProfileValidatorTest {
    class TestValidator : BaseRoninConditionProfileValidator() {
        override fun getQualifyingCategories(): List<Coding> = listOf(Coding(system = Uri("system"), code = Code("qualified")))

        override val profile: RoninProfile = RoninProfile.CONDITION_ENCOUNTER_DIAGNOSIS
        override val rcdmVersion: RCDMVersion = RCDMVersion.V3_19_0
        override val profileVersion: Int = 1
    }

    private val validator = TestValidator()

    @Test
    fun `returns proper supported resource`() {
        assertEquals(Condition::class, validator.supportedResource)
    }

    @Test
    fun `returns proper R4 validator`() {
        assertEquals(R4ConditionValidator, validator.r4Validator)
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
                                    Coding(system = Uri("system"), code = Code("not-qualified")),
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
            "ERROR RONIN_CND_001: Must match this system|code: system|qualified @ Condition.category",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if no code`() {
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
                                    Coding(system = Uri("system"), code = Code("qualified")),
                                ),
                        ),
                    ),
                code = null,
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
            "ERROR REQ_FIELD: code is a required element @ Condition.code",
            validation.issues().first().toString(),
        )
    }

    @Disabled("Not currently enabled due to some tenants not mapping Conditions")
    @Test
    fun `validate fails if invalid ronin normalized code`() {
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
                                    Coding(system = Uri("system"), code = Code("qualified")),
                                ),
                        ),
                    ),
                code = CodeableConcept(coding = listOf()),
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
            "ERROR RONIN_NOV_CODING_002: Must contain exactly 1 coding @ Condition.code.coding",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if no tenant source condition code extension`() {
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
                                    Coding(system = Uri("system"), code = Code("qualified")),
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
                            url = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.uri,
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
            "ERROR RONIN_CND_001: Tenant source condition code extension is missing or invalid @ Condition.extension",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if no value on tenant source condition code extension`() {
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
                                    Coding(system = Uri("system"), code = Code("qualified")),
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
                            value = null,
                        ),
                    ),
            )

        val validation = validator.validate(condition, LocationContext(Condition::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_CND_001: Tenant source condition code extension is missing or invalid @ Condition.extension",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if invalid value type on tenant source condition code extension`() {
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
                                    Coding(system = Uri("system"), code = Code("qualified")),
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
                                    DynamicValueType.BOOLEAN,
                                    FHIRBoolean.FALSE,
                                ),
                        ),
                    ),
            )

        val validation = validator.validate(condition, LocationContext(Condition::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_CND_001: Tenant source condition code extension is missing or invalid @ Condition.extension",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate succeeds`() {
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
                                    Coding(system = Uri("system"), code = Code("qualified")),
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
