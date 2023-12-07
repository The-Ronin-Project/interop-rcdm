package com.projectronin.interop.rcdm.validate.profile.observation

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
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.ObservationComponent
import com.projectronin.interop.fhir.r4.validate.resource.R4ObservationValidator
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.metadata.ValueSetMetadata
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ValueSetList
import com.projectronin.interop.rcdm.validate.profile.util.requiredIdentifiers
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BaseRoninObservationProfileValidatorTest {
    private val validCoding = Coding(system = Uri("system"), code = Code("code"), display = FHIRString("display"))
    private val metadata = mockk<ValueSetMetadata>()
    private val registryClient = mockk<NormalizationRegistryClient> {
        every { getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION.value, null) } returns
            ValueSetList(listOf(validCoding), metadata)
    }
    private val validator = TestValidator(registryClient)

    @Test
    fun `returns supported resource`() {
        assertEquals(Observation::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4ObservationValidator, validator.r4Validator)
    }

    @Test
    fun `validation fails for un-normalized code`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(text = "code".asFHIR()),
            subject = Reference(reference = FHIRString("Patient/1234"))
        )
        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(2, validation.issues().size)
        assertEquals(
            "ERROR RONIN_NOV_CODING_002: Must contain exactly 1 coding @ Observation.code.coding",
            validation.issues()[0].toString()
        )
        assertEquals(
            "ERROR RONIN_OBS_003: Must match this system|code: system|code @ Observation.code",
            validation.issues()[1].toString()
        )
    }

    @Test
    fun `validation fails for no subject`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = null
        )
        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: subject is a required element @ Observation.subject",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails for category outside supported set`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(CodeableConcept(coding = listOf(Coding())))
        )

        val validCategory = Coding(system = Uri("category-system"), code = Code("category-code"))
        val validation = TestValidator(registryClient, listOf(validCategory)).validate(
            observation,
            LocationContext(Observation::class)
        )
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_OBS_002: Must match this system|code: category-system|category-code @ Observation.category",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails for code outside supported set`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("system"),
                        code = Code("other-code"),
                        display = FHIRString("display")
                    )
                )
            ),
            subject = Reference(reference = FHIRString("Patient/1234"))
        )

        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_OBS_003: Must match this system|code: system|code @ Observation.code",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails for no code extension`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Patient/1234"))
        )

        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_OBS_004: Tenant source observation code extension is missing or invalid @ Observation.extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails for code extension with null value`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = null
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Patient/1234"))
        )

        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_OBS_004: Tenant source observation code extension is missing or invalid @ Observation.extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails for code extension with wrong value type`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.BOOLEAN, FHIRBoolean.TRUE)
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Patient/1234"))
        )

        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_OBS_004: Tenant source observation code extension is missing or invalid @ Observation.extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails for no value extension when value is a codeable concept`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept())
        )

        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_OBS_005: Tenant source observation value extension is missing or invalid @ Observation.extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails for value extension with null value`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                ),
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_VALUE.uri,
                    value = null
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept())
        )

        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_OBS_005: Tenant source observation value extension is missing or invalid @ Observation.extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails for value extension with wrong value type`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                ),
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_VALUE.uri,
                    value = DynamicValue(DynamicValueType.BOOLEAN, FHIRBoolean.FALSE)
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept())
        )

        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_OBS_005: Tenant source observation value extension is missing or invalid @ Observation.extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails for component with no code extension`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            component = listOf(
                ObservationComponent(
                    extension = listOf(),
                    code = CodeableConcept()
                )
            )
        )

        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_OBS_006: Tenant source observation component code extension is missing or invalid @ Observation.component[0].extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails for component with code extension with null value`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            component = listOf(
                ObservationComponent(
                    extension = listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                            value = null
                        )
                    ),
                    code = CodeableConcept()
                )
            )
        )

        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_OBS_006: Tenant source observation component code extension is missing or invalid @ Observation.component[0].extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails for component with code extension with wrong value type`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            component = listOf(
                ObservationComponent(
                    extension = listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                            value = DynamicValue(DynamicValueType.BOOLEAN, FHIRBoolean.TRUE)
                        )
                    ),
                    code = CodeableConcept()
                )
            )
        )

        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_OBS_006: Tenant source observation component code extension is missing or invalid @ Observation.component[0].extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails for component with no value extension when value is a codeable concept`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            component = listOf(
                ObservationComponent(
                    extension = listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept())
                        )
                    ),
                    code = CodeableConcept(),
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept())
                )
            )
        )

        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_OBS_007: Tenant source observation component value extension is missing or invalid @ Observation.component[0].extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails for component with value extension with null value`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            component = listOf(
                ObservationComponent(
                    extension = listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept())
                        ),
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_VALUE.uri,
                            value = null
                        )
                    ),
                    code = CodeableConcept(),
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept())
                )
            )
        )

        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_OBS_007: Tenant source observation component value extension is missing or invalid @ Observation.component[0].extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails for component with value extension with wrong value type`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            component = listOf(
                ObservationComponent(
                    extension = listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept())
                        ),
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_VALUE.uri,
                            value = DynamicValue(DynamicValueType.BOOLEAN, FHIRBoolean.TRUE)
                        )
                    ),
                    code = CodeableConcept(),
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept())
                )
            )
        )

        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_OBS_007: Tenant source observation component value extension is missing or invalid @ Observation.component[0].extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails for wrong subject reference type`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Practitioner/1234"))
        )
        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR INV_REF_TYPE: reference can only be one of the following: Patient @ Observation.subject.reference",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation succeeds`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Patient/1234"))
        )
        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validation succeeds with category`() {
        val validCategory = Coding(system = Uri("category-system"), code = Code("category-code"))

        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(CodeableConcept(coding = listOf(validCategory)))
        )

        val validation = TestValidator(registryClient, listOf(validCategory)).validate(
            observation,
            LocationContext(Observation::class)
        )
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validation succeeds with valueCodeableConcept`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                ),
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_VALUE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept())
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept())
        )

        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validation succeeds with component`() {
        val observation = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(validCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            component = listOf(
                ObservationComponent(
                    extension = listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept())
                        ),
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_VALUE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept())
                        )
                    ),
                    code = CodeableConcept(),
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept())
                )
            )
        )

        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }

    private class TestValidator(
        registryClient: NormalizationRegistryClient,
        private val categories: List<Coding> = emptyList()
    ) : BaseRoninObservationProfileValidator(registryClient) {
        override fun getSupportedCategories(): List<Coding> = categories

        override fun validateSpecificObservation(
            resource: Observation,
            parentContext: LocationContext,
            validation: Validation
        ) {
        }

        override val profile: RoninProfile = RoninProfile.OBSERVATION
        override val rcdmVersion: RCDMVersion = RCDMVersion.V3_26_1
        override val profileVersion: Int = 1
    }
}
