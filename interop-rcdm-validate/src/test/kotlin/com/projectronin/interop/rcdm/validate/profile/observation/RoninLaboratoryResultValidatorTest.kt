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
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.ObservationComponent
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ValueSetList
import com.projectronin.interop.rcdm.validate.profile.util.requiredIdentifiers
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RoninLaboratoryResultValidatorTest {
    private val laboratoryResultCoding =
        Coding(
            system = CodeSystem.LOINC.uri,
            display = "Laboratory Result".asFHIR(),
            code = Code("1234-5")
        )
    private val nonLoincCoding =
        Coding(
            system = CodeSystem.SNOMED_CT.uri,
            display = "Laboratory Result".asFHIR(),
            code = Code("1234-5")
        )

    private val laboratoryCategoryConcept = CodeableConcept(
        coding = listOf(
            Coding(
                system = CodeSystem.OBSERVATION_CATEGORY.uri,
                code = Code("laboratory")
            )
        )
    )

    private val registryClient = mockk<NormalizationRegistryClient> {
        every {
            getRequiredValueSet(
                "Observation.code",
                RoninProfile.OBSERVATION_LABORATORY_RESULT.value
            )
        } returns ValueSetList(listOf(laboratoryResultCoding, nonLoincCoding), mockk())
    }
    private val validator = RoninLaboratoryResultValidator(registryClient)

    @Test
    fun `returns proper profile`() {
        assertEquals(RoninProfile.OBSERVATION_LABORATORY_RESULT, validator.profile)
    }

    @Test
    fun `validate fails when code has non-LOINC coding`() {
        val respiratoryRate = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_LABORATORY_RESULT.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(nonLoincCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(laboratoryCategoryConcept),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(BigDecimal.valueOf(70)),
                    unit = "/min".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("/min")
                )
            )
        )
        val validation = validator.validate(respiratoryRate, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_LABOBS_001: Code system must be LOINC @ Observation.code",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails when no category`() {
        val respiratoryRate = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_LABORATORY_RESULT.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(laboratoryResultCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(BigDecimal.valueOf(70)),
                    unit = "/min".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("/min")
                )
            )
        )
        val validation = validator.validate(respiratoryRate, LocationContext(Observation::class))
        assertEquals(2, validation.issues().size)
        assertEquals(
            "ERROR RONIN_OBS_002: Must match this system|code: http://terminology.hl7.org/CodeSystem/observation-category|laboratory @ Observation.category",
            validation.issues()[0].toString()
        )
        assertEquals(
            "ERROR REQ_FIELD: category is a required element @ Observation.category",
            validation.issues()[1].toString()
        )
    }

    @Test
    fun `validate fails when value is a Quantity with a non-UCUM system`() {
        val respiratoryRate = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_LABORATORY_RESULT.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(laboratoryResultCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(laboratoryCategoryConcept),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(BigDecimal.valueOf(70)),
                    unit = "/min".asFHIR(),
                    system = CodeSystem.SNOMED_CT.uri,
                    code = Code("/min")
                )
            )
        )
        val validation = validator.validate(respiratoryRate, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR USCORE_LABOBS_002: Quantity system must be UCUM @ Observation.valueQuantity.system",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails when value is a CodeableConcept with a non-SNOMED system`() {
        val respiratoryRate = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_LABORATORY_RESULT.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                ),
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_VALUE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "value".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(laboratoryResultCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(laboratoryCategoryConcept),
            value = DynamicValue(
                DynamicValueType.CODEABLE_CONCEPT,
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.LOINC.uri,
                            code = Code("1234-5")
                        )
                    )
                )
            )
        )
        val validation = validator.validate(respiratoryRate, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_LABOBS_003: Value code system must be SNOMED CT @ Observation.value",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails when no component, no member, no value and no data absent reason`() {
        val respiratoryRate = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_LABORATORY_RESULT.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(laboratoryResultCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(laboratoryCategoryConcept)
        )
        val validation = validator.validate(respiratoryRate, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR USCORE_LABOBS_003: If there is no component or hasMember element then either a value[x] or a data absent reason must be present @ Observation",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails if effectiveDateTime is not in day format`() {
        val respiratoryRate = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_LABORATORY_RESULT.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(laboratoryResultCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(laboratoryCategoryConcept),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(BigDecimal.valueOf(70)),
                    unit = "/min".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("/min")
                )
            ),
            effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023"))
        )
        val validation = validator.validate(respiratoryRate, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR USCORE_LABOBS_004: Datetime must be at least to day @ Observation.effective",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails if effective is not an accepted type`() {
        val respiratoryRate = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_LABORATORY_RESULT.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(laboratoryResultCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(laboratoryCategoryConcept),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(BigDecimal.valueOf(70)),
                    unit = "/min".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("/min")
                )
            ),
            effective = DynamicValue(DynamicValueType.BOOLEAN, FHIRBoolean.TRUE)
        )
        val validation = validator.validate(respiratoryRate, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_INV_DYN_VAL: http://projectronin.io/fhir/StructureDefinition/ronin-observationLaboratoryResult profile restricts effective to one of: DateTime, Period @ Observation.effective",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate succeeds with valueQuantity`() {
        val respiratoryRate = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_LABORATORY_RESULT.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(laboratoryResultCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(laboratoryCategoryConcept),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(BigDecimal.valueOf(70)),
                    unit = "/min".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("/min")
                )
            )
        )
        val validation = validator.validate(respiratoryRate, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate succeeds with component`() {
        val respiratoryRate = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_LABORATORY_RESULT.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(laboratoryResultCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(laboratoryCategoryConcept),
            component = listOf(
                ObservationComponent(
                    extension = listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                            value = DynamicValue(
                                DynamicValueType.CODEABLE_CONCEPT,
                                CodeableConcept(text = "code".asFHIR())
                            )
                        )
                    ),
                    code = CodeableConcept(coding = listOf(laboratoryResultCoding))
                )
            )
        )
        val validation = validator.validate(respiratoryRate, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate succeeds with hasMember`() {
        val respiratoryRate = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_LABORATORY_RESULT.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(laboratoryResultCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(laboratoryCategoryConcept),
            hasMember = listOf(Reference(reference = "Observation/other".asFHIR()))
        )
        val validation = validator.validate(respiratoryRate, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate succeeds with dataAbsentReason`() {
        val respiratoryRate = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_LABORATORY_RESULT.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(laboratoryResultCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(laboratoryCategoryConcept),
            dataAbsentReason = CodeableConcept()
        )
        val validation = validator.validate(respiratoryRate, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate succeeds with effective`() {
        val respiratoryRate = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_LABORATORY_RESULT.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(laboratoryResultCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(laboratoryCategoryConcept),
            value = DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(BigDecimal.valueOf(70)),
                    unit = "/min".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("/min")
                )
            ),
            effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023-12-05"))
        )
        val validation = validator.validate(respiratoryRate, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }
}
