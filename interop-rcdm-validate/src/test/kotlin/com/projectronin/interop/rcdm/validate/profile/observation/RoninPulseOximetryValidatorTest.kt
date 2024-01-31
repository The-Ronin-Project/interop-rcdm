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

@Suppress("ktlint:standard:max-line-length")
class RoninPulseOximetryValidatorTest {
    private val pulseOximetryCoding =
        Coding(
            system = CodeSystem.LOINC.uri,
            display = "Pulse Oximetry".asFHIR(),
            code = Code("12345-1"),
        )

    private val flowRateCoding = Coding(system = CodeSystem.LOINC.uri, code = Code("12345-2"))
    private val flowRateCodeableConcept =
        CodeableConcept(
            coding = listOf(flowRateCoding),
            text = "FlowRate".asFHIR(),
        )
    private val sourceFlowRateCodeExtension =
        Extension(
            url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
            value =
                DynamicValue(
                    DynamicValueType.CODEABLE_CONCEPT,
                    flowRateCodeableConcept,
                ),
        )

    private val concentrationCoding = Coding(system = CodeSystem.LOINC.uri, code = Code("12345-3"))
    private val concentrationCodeableConcept =
        CodeableConcept(
            coding = listOf(concentrationCoding),
            text = "Concentration".asFHIR(),
        )
    private val sourceConcentrationCodeExtension =
        Extension(
            url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
            value =
                DynamicValue(
                    DynamicValueType.CODEABLE_CONCEPT,
                    concentrationCodeableConcept,
                ),
        )

    private val vitalSignsCategoryConcept =
        CodeableConcept(
            coding =
                listOf(
                    Coding(
                        system = CodeSystem.OBSERVATION_CATEGORY.uri,
                        code = Code("vital-signs"),
                    ),
                ),
        )
    private val valueSetMetadata =
        ValueSetMetadata(
            registryEntryType = "value_set",
            valueSetName = "test-value-set",
            valueSetUuid = "03d51d53-1a31-49a9-af74-573b456efca5",
            version = "2",
        )

    private val registryClient =
        mockk<NormalizationRegistryClient> {
            every {
                getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)
            } returns ValueSetList(listOf(pulseOximetryCoding), valueSetMetadata)

            every {
                getRequiredValueSet(
                    "Observation.component:FlowRate.code",
                    RoninProfile.OBSERVATION_PULSE_OXIMETRY.value,
                )
            } returns ValueSetList(listOf(flowRateCoding), valueSetMetadata)

            every {
                getRequiredValueSet(
                    "Observation.component:Concentration.code",
                    RoninProfile.OBSERVATION_PULSE_OXIMETRY.value,
                )
            } returns ValueSetList(listOf(concentrationCoding), valueSetMetadata)
        }
    private val validator = RoninPulseOximetryValidator(registryClient)

    @Test
    fun `returns proper profile`() {
        assertEquals(RoninProfile.OBSERVATION_PULSE_OXIMETRY, validator.profile)
    }

    @Test
    fun `validation fails if invalid value type`() {
        val pulseOximetry =
            Observation(
                id = Id("1234"),
                meta =
                    Meta(
                        profile = listOf(RoninProfile.OBSERVATION_PULSE_OXIMETRY.canonical),
                        source = Uri("source"),
                    ),
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
                code = CodeableConcept(coding = listOf(pulseOximetryCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category = listOf(vitalSignsCategoryConcept),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                dataAbsentReason = CodeableConcept(text = "Unknown".asFHIR()),
                value =
                    DynamicValue(
                        DynamicValueType.STRING,
                        FHIRString("value"),
                    ),
            )
        val validation = validator.validate(pulseOximetry, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_INV_DYN_VAL: http://projectronin.io/fhir/StructureDefinition/ronin-observationPulseOximetry profile restricts value to one of: Quantity @ Observation.value",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validation fails if invalid value`() {
        val pulseOximetry =
            Observation(
                id = Id("1234"),
                meta =
                    Meta(
                        profile = listOf(RoninProfile.OBSERVATION_PULSE_OXIMETRY.canonical),
                        source = Uri("source"),
                    ),
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
                code = CodeableConcept(coding = listOf(pulseOximetryCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category = listOf(vitalSignsCategoryConcept),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                dataAbsentReason = CodeableConcept(text = "Unknown".asFHIR()),
                value =
                    DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(70)),
                            unit = "/100".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("/100"),
                        ),
                    ),
            )
        val validation = validator.validate(pulseOximetry, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR INV_VALUE_SET: '/100' is outside of required value set @ Observation.valueQuantity.code",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validation fails if multiple flowRate components`() {
        val pulseOximetry =
            Observation(
                id = Id("1234"),
                meta =
                    Meta(
                        profile = listOf(RoninProfile.OBSERVATION_PULSE_OXIMETRY.canonical),
                        source = Uri("source"),
                    ),
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
                code = CodeableConcept(coding = listOf(pulseOximetryCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category = listOf(vitalSignsCategoryConcept),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                component =
                    listOf(
                        ObservationComponent(
                            extension = listOf(sourceFlowRateCodeExtension),
                            code = flowRateCodeableConcept,
                            value =
                                DynamicValue(
                                    DynamicValueType.QUANTITY,
                                    Quantity(
                                        value = Decimal(BigDecimal.valueOf(110)),
                                        unit = "L/min".asFHIR(),
                                        system = CodeSystem.UCUM.uri,
                                        code = Code("L/min"),
                                    ),
                                ),
                        ),
                        ObservationComponent(
                            extension = listOf(sourceFlowRateCodeExtension),
                            code = flowRateCodeableConcept,
                            value =
                                DynamicValue(
                                    DynamicValueType.QUANTITY,
                                    Quantity(
                                        value = Decimal(BigDecimal.valueOf(110)),
                                        unit = "L/min".asFHIR(),
                                        system = CodeSystem.UCUM.uri,
                                        code = Code("L/min"),
                                    ),
                                ),
                        ),
                        ObservationComponent(
                            extension = listOf(sourceConcentrationCodeExtension),
                            code = concentrationCodeableConcept,
                            value =
                                DynamicValue(
                                    DynamicValueType.QUANTITY,
                                    Quantity(
                                        value = Decimal(BigDecimal.valueOf(65)),
                                        unit = "%".asFHIR(),
                                        system = CodeSystem.UCUM.uri,
                                        code = Code("%"),
                                    ),
                                ),
                        ),
                    ),
            )
        val validation = validator.validate(pulseOximetry, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR USCORE_PXOBS_005: Only 1 entry is allowed for pulse oximetry flow rate @ Observation.component:FlowRate.code",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validation fails if invalid flowRate value`() {
        val pulseOximetry =
            Observation(
                id = Id("1234"),
                meta =
                    Meta(
                        profile = listOf(RoninProfile.OBSERVATION_PULSE_OXIMETRY.canonical),
                        source = Uri("source"),
                    ),
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
                code = CodeableConcept(coding = listOf(pulseOximetryCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category = listOf(vitalSignsCategoryConcept),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                component =
                    listOf(
                        ObservationComponent(
                            extension = listOf(sourceFlowRateCodeExtension),
                            code = flowRateCodeableConcept,
                            value =
                                DynamicValue(
                                    DynamicValueType.QUANTITY,
                                    Quantity(
                                        value = Decimal(BigDecimal.valueOf(110)),
                                        unit = "L/min".asFHIR(),
                                        system = CodeSystem.UCUM.uri,
                                        code = Code("L/sec"),
                                    ),
                                ),
                        ),
                        ObservationComponent(
                            extension = listOf(sourceConcentrationCodeExtension),
                            code = concentrationCodeableConcept,
                            value =
                                DynamicValue(
                                    DynamicValueType.QUANTITY,
                                    Quantity(
                                        value = Decimal(BigDecimal.valueOf(65)),
                                        unit = "%".asFHIR(),
                                        system = CodeSystem.UCUM.uri,
                                        code = Code("%"),
                                    ),
                                ),
                        ),
                    ),
            )
        val validation = validator.validate(pulseOximetry, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR INV_VALUE_SET: 'L/sec' is outside of required value set @ Observation.component:FlowRate.valueQuantity.code",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validation fails if multiple concentration components`() {
        val pulseOximetry =
            Observation(
                id = Id("1234"),
                meta =
                    Meta(
                        profile = listOf(RoninProfile.OBSERVATION_PULSE_OXIMETRY.canonical),
                        source = Uri("source"),
                    ),
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
                code = CodeableConcept(coding = listOf(pulseOximetryCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category = listOf(vitalSignsCategoryConcept),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                component =
                    listOf(
                        ObservationComponent(
                            extension = listOf(sourceFlowRateCodeExtension),
                            code = flowRateCodeableConcept,
                            value =
                                DynamicValue(
                                    DynamicValueType.QUANTITY,
                                    Quantity(
                                        value = Decimal(BigDecimal.valueOf(110)),
                                        unit = "L/min".asFHIR(),
                                        system = CodeSystem.UCUM.uri,
                                        code = Code("L/min"),
                                    ),
                                ),
                        ),
                        ObservationComponent(
                            extension = listOf(sourceConcentrationCodeExtension),
                            code = concentrationCodeableConcept,
                            value =
                                DynamicValue(
                                    DynamicValueType.QUANTITY,
                                    Quantity(
                                        value = Decimal(BigDecimal.valueOf(65)),
                                        unit = "%".asFHIR(),
                                        system = CodeSystem.UCUM.uri,
                                        code = Code("%"),
                                    ),
                                ),
                        ),
                        ObservationComponent(
                            extension = listOf(sourceConcentrationCodeExtension),
                            code = concentrationCodeableConcept,
                            value =
                                DynamicValue(
                                    DynamicValueType.QUANTITY,
                                    Quantity(
                                        value = Decimal(BigDecimal.valueOf(65)),
                                        unit = "%".asFHIR(),
                                        system = CodeSystem.UCUM.uri,
                                        code = Code("%"),
                                    ),
                                ),
                        ),
                    ),
            )
        val validation = validator.validate(pulseOximetry, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR USCORE_PXOBS_006: Only 1 entry is allowed for pulse oximetry oxygen concentration @ Observation.component:Concentration.code",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validation fails if invalid concentration value`() {
        val pulseOximetry =
            Observation(
                id = Id("1234"),
                meta =
                    Meta(
                        profile = listOf(RoninProfile.OBSERVATION_PULSE_OXIMETRY.canonical),
                        source = Uri("source"),
                    ),
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
                code = CodeableConcept(coding = listOf(pulseOximetryCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category = listOf(vitalSignsCategoryConcept),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                component =
                    listOf(
                        ObservationComponent(
                            extension = listOf(sourceFlowRateCodeExtension),
                            code = flowRateCodeableConcept,
                            value =
                                DynamicValue(
                                    DynamicValueType.QUANTITY,
                                    Quantity(
                                        value = Decimal(BigDecimal.valueOf(110)),
                                        unit = "L/min".asFHIR(),
                                        system = CodeSystem.UCUM.uri,
                                        code = Code("L/min"),
                                    ),
                                ),
                        ),
                        ObservationComponent(
                            extension = listOf(sourceConcentrationCodeExtension),
                            code = concentrationCodeableConcept,
                            value =
                                DynamicValue(
                                    DynamicValueType.QUANTITY,
                                    Quantity(
                                        value = Decimal(BigDecimal.valueOf(65)),
                                        unit = "%".asFHIR(),
                                        system = CodeSystem.UCUM.uri,
                                        code = Code("/100"),
                                    ),
                                ),
                        ),
                    ),
            )
        val validation = validator.validate(pulseOximetry, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR INV_VALUE_SET: '/100' is outside of required value set @ Observation.component:Concentration.valueQuantity.code",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validation fails with a generic component`() {
        val pulseOximetry =
            Observation(
                id = Id("1234"),
                meta =
                    Meta(
                        profile = listOf(RoninProfile.OBSERVATION_PULSE_OXIMETRY.canonical),
                        source = Uri("source"),
                    ),
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
                code = CodeableConcept(coding = listOf(pulseOximetryCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category = listOf(vitalSignsCategoryConcept),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                component =
                    listOf(
                        ObservationComponent(
                            extension =
                                listOf(
                                    Extension(
                                        url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                                        value =
                                            DynamicValue(
                                                DynamicValueType.CODEABLE_CONCEPT,
                                                concentrationCodeableConcept,
                                            ),
                                    ),
                                ),
                            code = CodeableConcept("code".asFHIR()),
                            value =
                                DynamicValue(
                                    DynamicValueType.STRING,
                                    "test",
                                ),
                        ),
                    ),
            )
        val validation = validator.validate(pulseOximetry, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_PXOBS_007: Pulse Oximetry components must be either a Flow Rate or Concentration @ Observation.component",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validation succeeds with components`() {
        val pulseOximetry =
            Observation(
                id = Id("1234"),
                meta =
                    Meta(
                        profile = listOf(RoninProfile.OBSERVATION_PULSE_OXIMETRY.canonical),
                        source = Uri("source"),
                    ),
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
                code = CodeableConcept(coding = listOf(pulseOximetryCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category = listOf(vitalSignsCategoryConcept),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                component =
                    listOf(
                        ObservationComponent(
                            extension = listOf(sourceFlowRateCodeExtension),
                            code = flowRateCodeableConcept,
                            value =
                                DynamicValue(
                                    DynamicValueType.QUANTITY,
                                    Quantity(
                                        value = Decimal(BigDecimal.valueOf(110)),
                                        unit = "L/min".asFHIR(),
                                        system = CodeSystem.UCUM.uri,
                                        code = Code("L/min"),
                                    ),
                                ),
                        ),
                        ObservationComponent(
                            extension = listOf(sourceConcentrationCodeExtension),
                            code = concentrationCodeableConcept,
                            value =
                                DynamicValue(
                                    DynamicValueType.QUANTITY,
                                    Quantity(
                                        value = Decimal(BigDecimal.valueOf(65)),
                                        unit = "%".asFHIR(),
                                        system = CodeSystem.UCUM.uri,
                                        code = Code("%"),
                                    ),
                                ),
                        ),
                    ),
            )
        val validation = validator.validate(pulseOximetry, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validation succeeds without components`() {
        val pulseOximetry =
            Observation(
                id = Id("1234"),
                meta =
                    Meta(
                        profile = listOf(RoninProfile.OBSERVATION_PULSE_OXIMETRY.canonical),
                        source = Uri("source"),
                    ),
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
                code = CodeableConcept(coding = listOf(pulseOximetryCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category = listOf(vitalSignsCategoryConcept),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                component = emptyList(),
            )
        val validation = validator.validate(pulseOximetry, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validation succeeds with data absent reason`() {
        val pulseOximetry =
            Observation(
                id = Id("1234"),
                meta =
                    Meta(
                        profile = listOf(RoninProfile.OBSERVATION_PULSE_OXIMETRY.canonical),
                        source = Uri("source"),
                    ),
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
                code = CodeableConcept(coding = listOf(pulseOximetryCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category = listOf(vitalSignsCategoryConcept),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                dataAbsentReason = CodeableConcept(text = "Unknown".asFHIR()),
            )
        val validation = validator.validate(pulseOximetry, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validation succeeds if no flowRate components`() {
        val pulseOximetry =
            Observation(
                id = Id("1234"),
                meta =
                    Meta(
                        profile = listOf(RoninProfile.OBSERVATION_PULSE_OXIMETRY.canonical),
                        source = Uri("source"),
                    ),
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
                code = CodeableConcept(coding = listOf(pulseOximetryCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category = listOf(vitalSignsCategoryConcept),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                component =
                    listOf(
                        ObservationComponent(
                            extension = listOf(sourceConcentrationCodeExtension),
                            code = concentrationCodeableConcept,
                            value =
                                DynamicValue(
                                    DynamicValueType.QUANTITY,
                                    Quantity(
                                        value = Decimal(BigDecimal.valueOf(65)),
                                        unit = "%".asFHIR(),
                                        system = CodeSystem.UCUM.uri,
                                        code = Code("%"),
                                    ),
                                ),
                        ),
                    ),
            )
        val validation = validator.validate(pulseOximetry, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validation succeeds if no concentration components`() {
        val pulseOximetry =
            Observation(
                id = Id("1234"),
                meta =
                    Meta(
                        profile = listOf(RoninProfile.OBSERVATION_PULSE_OXIMETRY.canonical),
                        source = Uri("source"),
                    ),
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
                code = CodeableConcept(coding = listOf(pulseOximetryCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category = listOf(vitalSignsCategoryConcept),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                component =
                    listOf(
                        ObservationComponent(
                            extension = listOf(sourceFlowRateCodeExtension),
                            code = flowRateCodeableConcept,
                            value =
                                DynamicValue(
                                    DynamicValueType.QUANTITY,
                                    Quantity(
                                        value = Decimal(BigDecimal.valueOf(110)),
                                        unit = "L/min".asFHIR(),
                                        system = CodeSystem.UCUM.uri,
                                        code = Code("L/min"),
                                    ),
                                ),
                        ),
                    ),
            )
        val validation = validator.validate(pulseOximetry, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }
}
