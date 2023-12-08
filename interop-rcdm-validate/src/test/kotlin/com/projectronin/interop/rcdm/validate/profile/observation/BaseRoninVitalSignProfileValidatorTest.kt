package com.projectronin.interop.rcdm.validate.profile.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Observation
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
import java.math.BigDecimal

@Suppress("ktlint:standard:max-line-length")
class BaseRoninVitalSignProfileValidatorTest {
    private val validCoding = Coding(system = Uri("system"), code = Code("code"), display = FHIRString("display"))
    private val metadata = mockk<ValueSetMetadata>()
    private val registryClient =
        mockk<NormalizationRegistryClient> {
            every { getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION.value, null) } returns
                ValueSetList(listOf(validCoding), metadata)
        }

    private val validator = TestValidator(registryClient)
    private val valueValidator = TestValueValidator(registryClient)

    @Test
    fun `validate fails if non vital signs category`() {
        val observation =
            Observation(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
                identifier = requiredIdentifiers,
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(text = "code".asFHIR()),
                                ),
                        ),
                    ),
                status = ObservationStatus.FINAL.asCode(),
                code = CodeableConcept(coding = listOf(validCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.OBSERVATION_CATEGORY.uri,
                                        code = Code("not-vital-signs"),
                                    ),
                                ),
                        ),
                    ),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
            )
        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_OBS_002: Must match this system|code: http://terminology.hl7.org/CodeSystem/observation-category|vital-signs @ Observation.category",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails for effective outside supported types`() {
        val observation =
            Observation(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
                identifier = requiredIdentifiers,
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(text = "code".asFHIR()),
                                ),
                        ),
                    ),
                status = ObservationStatus.FINAL.asCode(),
                code = CodeableConcept(coding = listOf(validCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.OBSERVATION_CATEGORY.uri,
                                        code = Code("vital-signs"),
                                    ),
                                ),
                        ),
                    ),
                effective = DynamicValue(DynamicValueType.STRING, FHIRString("2023")),
            )
        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_INV_DYN_VAL: http://projectronin.io/fhir/StructureDefinition/ronin-observation profile restricts effective to one of: DateTime, Period @ Observation.effective",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate succeeds`() {
        val observation =
            Observation(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
                identifier = requiredIdentifiers,
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(text = "code".asFHIR()),
                                ),
                        ),
                    ),
                status = ObservationStatus.FINAL.asCode(),
                code = CodeableConcept(coding = listOf(validCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.OBSERVATION_CATEGORY.uri,
                                        code = Code("vital-signs"),
                                    ),
                                ),
                        ),
                    ),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
            )
        val validation = validator.validate(observation, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validateVitalSignValue fails when no quantity value`() {
        val observation =
            Observation(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
                identifier = requiredIdentifiers,
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(text = "code".asFHIR()),
                                ),
                        ),
                    ),
                status = ObservationStatus.FINAL.asCode(),
                code = CodeableConcept(coding = listOf(validCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.OBSERVATION_CATEGORY.uri,
                                        code = Code("vital-signs"),
                                    ),
                                ),
                        ),
                    ),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                value =
                    DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = null,
                            unit = "mg".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mg"),
                        ),
                    ),
            )
        val validation = valueValidator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: value is a required element @ Observation.valueQuantity.value",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateVitalSignValue fails when no quantity unit`() {
        val observation =
            Observation(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
                identifier = requiredIdentifiers,
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(text = "code".asFHIR()),
                                ),
                        ),
                    ),
                status = ObservationStatus.FINAL.asCode(),
                code = CodeableConcept(coding = listOf(validCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.OBSERVATION_CATEGORY.uri,
                                        code = Code("vital-signs"),
                                    ),
                                ),
                        ),
                    ),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                value =
                    DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(20)),
                            unit = null,
                            system = CodeSystem.UCUM.uri,
                            code = Code("mg"),
                        ),
                    ),
            )
        val validation = valueValidator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: unit is a required element @ Observation.valueQuantity.unit",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateVitalSignValue fails when quantity system is not UCUM`() {
        val observation =
            Observation(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
                identifier = requiredIdentifiers,
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(text = "code".asFHIR()),
                                ),
                        ),
                    ),
                status = ObservationStatus.FINAL.asCode(),
                code = CodeableConcept(coding = listOf(validCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.OBSERVATION_CATEGORY.uri,
                                        code = Code("vital-signs"),
                                    ),
                                ),
                        ),
                    ),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                value =
                    DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(20)),
                            unit = "mg".asFHIR(),
                            system = CodeSystem.SNOMED_CT.uri,
                            code = Code("mg"),
                        ),
                    ),
            )
        val validation = valueValidator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR USCORE_VSOBS_002: Quantity system must be UCUM @ Observation.valueQuantity.system",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateVitalSignValue fails when no quantity code`() {
        val observation =
            Observation(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
                identifier = requiredIdentifiers,
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(text = "code".asFHIR()),
                                ),
                        ),
                    ),
                status = ObservationStatus.FINAL.asCode(),
                code = CodeableConcept(coding = listOf(validCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.OBSERVATION_CATEGORY.uri,
                                        code = Code("vital-signs"),
                                    ),
                                ),
                        ),
                    ),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                value =
                    DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(20)),
                            unit = "mg".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = null,
                        ),
                    ),
            )
        val validation = valueValidator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: code is a required element @ Observation.valueQuantity.code",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateVitalSignValue fails when no quantity code is not in valid unit list`() {
        val observation =
            Observation(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
                identifier = requiredIdentifiers,
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(text = "code".asFHIR()),
                                ),
                        ),
                    ),
                status = ObservationStatus.FINAL.asCode(),
                code = CodeableConcept(coding = listOf(validCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.OBSERVATION_CATEGORY.uri,
                                        code = Code("vital-signs"),
                                    ),
                                ),
                        ),
                    ),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                value =
                    DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(20)),
                            unit = "mg".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("other-mg"),
                        ),
                    ),
            )
        val validation = valueValidator.validate(observation, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR INV_VALUE_SET: 'other-mg' is outside of required value set @ Observation.valueQuantity.code",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateVitalSignValue succeeds when no value`() {
        val observation =
            Observation(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
                identifier = requiredIdentifiers,
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(text = "code".asFHIR()),
                                ),
                        ),
                    ),
                status = ObservationStatus.FINAL.asCode(),
                code = CodeableConcept(coding = listOf(validCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.OBSERVATION_CATEGORY.uri,
                                        code = Code("vital-signs"),
                                    ),
                                ),
                        ),
                    ),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                value = null,
            )
        val validation = valueValidator.validate(observation, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validateVitalSignValue succeeds when value is not quantity`() {
        val observation =
            Observation(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
                identifier = requiredIdentifiers,
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(text = "code".asFHIR()),
                                ),
                        ),
                    ),
                status = ObservationStatus.FINAL.asCode(),
                code = CodeableConcept(coding = listOf(validCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.OBSERVATION_CATEGORY.uri,
                                        code = Code("vital-signs"),
                                    ),
                                ),
                        ),
                    ),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                value = DynamicValue(DynamicValueType.STRING, "20".asFHIR()),
            )
        val validation = valueValidator.validate(observation, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validateVitalSignValue succeeds when valid quantity value`() {
        val observation =
            Observation(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.OBSERVATION.canonical), source = Uri("source")),
                identifier = requiredIdentifiers,
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(text = "code".asFHIR()),
                                ),
                        ),
                    ),
                status = ObservationStatus.FINAL.asCode(),
                code = CodeableConcept(coding = listOf(validCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.OBSERVATION_CATEGORY.uri,
                                        code = Code("vital-signs"),
                                    ),
                                ),
                        ),
                    ),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                value =
                    DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(20)),
                            unit = "mg".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mg"),
                        ),
                    ),
            )
        val validation = valueValidator.validate(observation, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }

    class TestValidator(registryClient: NormalizationRegistryClient) :
        BaseRoninVitalSignProfileValidator(registryClient) {
        override fun validateVitalSign(
            resource: Observation,
            parentContext: LocationContext,
            validation: Validation,
        ) {
        }

        override val profile: RoninProfile = RoninProfile.OBSERVATION
        override val rcdmVersion: RCDMVersion = RCDMVersion.V3_26_1
        override val profileVersion: Int = 1
    }

    class TestValueValidator(registryClient: NormalizationRegistryClient) :
        BaseRoninVitalSignProfileValidator(registryClient) {
        override fun validateVitalSign(
            resource: Observation,
            parentContext: LocationContext,
            validation: Validation,
        ) {
            validateVitalSignValue(
                resource.value,
                listOf("mg"),
                validation,
            )
        }

        override val profile: RoninProfile = RoninProfile.OBSERVATION
        override val rcdmVersion: RCDMVersion = RCDMVersion.V3_26_1
        override val profileVersion: Int = 1
    }
}
