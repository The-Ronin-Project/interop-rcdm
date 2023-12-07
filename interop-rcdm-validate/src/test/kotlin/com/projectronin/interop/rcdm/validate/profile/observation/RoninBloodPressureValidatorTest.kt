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
import com.projectronin.interop.fhir.r4.resource.ObservationComponent
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
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

class RoninBloodPressureValidatorTest {
    private val bloodPressureCoding = Coding(
        system = CodeSystem.LOINC.uri,
        display = "Blood Pressure".asFHIR(),
        code = Code("85354-9")
    )

    private val systolicCoding = Coding(system = CodeSystem.LOINC.uri, code = Code("8480-6"))
    private val systolicCodeableConcept = CodeableConcept(
        coding = listOf(systolicCoding),
        text = "Systolic".asFHIR()
    )
    private val sourceSystolicCodeExtension = Extension(
        url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
        value = DynamicValue(
            DynamicValueType.CODEABLE_CONCEPT,
            systolicCodeableConcept
        )
    )

    private val diastolicCoding = Coding(system = CodeSystem.LOINC.uri, code = Code("8462-4"))
    private val diastolicCodeableConcept = CodeableConcept(
        coding = listOf(diastolicCoding),
        text = "Diastolic".asFHIR()
    )
    private val sourceDiastolicCodeExtension = Extension(
        url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
        value = DynamicValue(
            DynamicValueType.CODEABLE_CONCEPT,
            diastolicCodeableConcept
        )
    )

    private val vitalSignsCategoryConcept = CodeableConcept(
        coding = listOf(
            Coding(
                system = CodeSystem.OBSERVATION_CATEGORY.uri,
                code = Code("vital-signs")
            )
        )
    )
    private val valueSetMetadata = ValueSetMetadata(
        registryEntryType = "value_set",
        valueSetName = "test-value-set",
        valueSetUuid = "03d51d53-1a31-49a9-af74-573b456efca5",
        version = "2"
    )

    private val registryClient = mockk<NormalizationRegistryClient> {
        every {
            getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)
        } returns ValueSetList(listOf(bloodPressureCoding), valueSetMetadata)

        every {
            getRequiredValueSet(
                "Observation.component:systolic.code",
                RoninProfile.OBSERVATION_BLOOD_PRESSURE.value
            )
        } returns ValueSetList(listOf(systolicCoding), valueSetMetadata)

        every {
            getRequiredValueSet(
                "Observation.component:diastolic.code",
                RoninProfile.OBSERVATION_BLOOD_PRESSURE.value
            )
        } returns ValueSetList(listOf(diastolicCoding), valueSetMetadata)
    }
    private val validator = RoninBloodPressureValidator(registryClient)

    @Test
    fun `returns proper profile`() {
        assertEquals(RoninProfile.OBSERVATION_BLOOD_PRESSURE, validator.profile)
    }

    @Test
    fun `validation fails if no systolic components`() {
        val bloodPressure = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_BLOOD_PRESSURE.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(bloodPressureCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(vitalSignsCategoryConcept),
            effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
            component = listOf(
                ObservationComponent(
                    extension = listOf(sourceDiastolicCodeExtension),
                    code = diastolicCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(65)),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )
        val validation = validator.validate(bloodPressure, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR USCORE_BPOBS_001: Must match this system|code: http://loinc.org|8480-6 @ Observation.component:systolic.code",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails if multiple systolic components`() {
        val bloodPressure = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_BLOOD_PRESSURE.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(bloodPressureCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(vitalSignsCategoryConcept),
            effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
            component = listOf(
                ObservationComponent(
                    extension = listOf(sourceSystolicCodeExtension),
                    code = systolicCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(110)),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(sourceSystolicCodeExtension),
                    code = systolicCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(110)),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(sourceDiastolicCodeExtension),
                    code = diastolicCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(65)),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )
        val validation = validator.validate(bloodPressure, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR USCORE_BPOBS_004: Only 1 entry is allowed for systolic blood pressure @ Observation.component:systolic.code",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails if invalid systolic value`() {
        val bloodPressure = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_BLOOD_PRESSURE.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(bloodPressureCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(vitalSignsCategoryConcept),
            effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
            component = listOf(
                ObservationComponent(
                    extension = listOf(sourceSystolicCodeExtension),
                    code = systolicCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(110)),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mmHg")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(sourceDiastolicCodeExtension),
                    code = diastolicCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(65)),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )
        val validation = validator.validate(bloodPressure, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR INV_VALUE_SET: 'mmHg' is outside of required value set @ Observation.component:systolic.valueQuantity.code",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails if no diastolic components`() {
        val bloodPressure = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_BLOOD_PRESSURE.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(bloodPressureCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(vitalSignsCategoryConcept),
            effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
            component = listOf(
                ObservationComponent(
                    extension = listOf(sourceSystolicCodeExtension),
                    code = systolicCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(110)),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )
        val validation = validator.validate(bloodPressure, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR USCORE_BPOBS_002: Must match this system|code: http://loinc.org|8462-4 @ Observation.component:diastolic.code",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails if multiple diastolic components`() {
        val bloodPressure = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_BLOOD_PRESSURE.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(bloodPressureCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(vitalSignsCategoryConcept),
            effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
            component = listOf(
                ObservationComponent(
                    extension = listOf(sourceSystolicCodeExtension),
                    code = systolicCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(110)),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(sourceDiastolicCodeExtension),
                    code = diastolicCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(65)),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(sourceDiastolicCodeExtension),
                    code = diastolicCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(65)),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )
        val validation = validator.validate(bloodPressure, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR USCORE_BPOBS_005: Only 1 entry is allowed for diastolic blood pressure @ Observation.component:diastolic.code",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails if invalid diastolic value`() {
        val bloodPressure = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_BLOOD_PRESSURE.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(bloodPressureCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(vitalSignsCategoryConcept),
            effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
            component = listOf(
                ObservationComponent(
                    extension = listOf(sourceSystolicCodeExtension),
                    code = systolicCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(110)),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(sourceDiastolicCodeExtension),
                    code = diastolicCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(65)),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mmHg")
                        )
                    )
                )
            )
        )
        val validation = validator.validate(bloodPressure, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR INV_VALUE_SET: 'mmHg' is outside of required value set @ Observation.component:diastolic.valueQuantity.code",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation succeeds with components`() {
        val bloodPressure = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_BLOOD_PRESSURE.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(bloodPressureCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(vitalSignsCategoryConcept),
            effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
            component = listOf(
                ObservationComponent(
                    extension = listOf(sourceSystolicCodeExtension),
                    code = systolicCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(110)),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                ),
                ObservationComponent(
                    extension = listOf(sourceDiastolicCodeExtension),
                    code = diastolicCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(65)),
                            unit = "mm[Hg]".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("mm[Hg]")
                        )
                    )
                )
            )
        )
        val validation = validator.validate(bloodPressure, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validation succeeds with data absent reason`() {
        val bloodPressure = Observation(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.OBSERVATION_BLOOD_PRESSURE.canonical), source = Uri("source")),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR()))
                )
            ),
            status = ObservationStatus.FINAL.asCode(),
            code = CodeableConcept(coding = listOf(bloodPressureCoding)),
            subject = Reference(reference = FHIRString("Patient/1234")),
            category = listOf(vitalSignsCategoryConcept),
            effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
            dataAbsentReason = CodeableConcept(text = "Unknown".asFHIR())
        )
        val validation = validator.validate(bloodPressure, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }
}
