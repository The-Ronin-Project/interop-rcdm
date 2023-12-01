package com.projectronin.interop.rcdm.transform.profile.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Annotation
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.ObservationComponent
import com.projectronin.interop.fhir.r4.resource.ObservationReferenceRange
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ValueSetList
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninPulseOximetryTransformerTest {
    private val pulseOximetryCode = Code("59408-5")
    private val pulseOximetryCodingList = listOf(
        Coding(
            system = CodeSystem.LOINC.uri,
            display = "Pulse Oximetry".asFHIR(),
            code = pulseOximetryCode
        )
    )
    private val pulseOximetryConcept = CodeableConcept(
        text = "Pulse Oximetry".asFHIR(),
        coding = pulseOximetryCodingList
    )

    private val registryClient = mockk<NormalizationRegistryClient> {
        every {
            getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)
        } returns ValueSetList(pulseOximetryCodingList, mockk())
    }

    private val transformer = RoninPulseOximetryTransformer(registryClient)

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    private val flowRateCodeableConcept =
        CodeableConcept(
            coding = listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("3151-8"))),
            text = "Flow Rate".asFHIR()
        )
    private val concentrationCodeableConcept =
        CodeableConcept(
            coding = listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("3150-0"))),
            text = "Concentration".asFHIR()
        )

    @Test
    fun `returns correct profile`() {
        assertEquals(RoninProfile.OBSERVATION_PULSE_OXIMETRY, transformer.profile)
    }

    @Test
    fun `returns not default`() {
        assertFalse(transformer.isDefault)
    }

    @Test
    fun `does not qualify when no category`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = listOf(),
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = pulseOximetryConcept
        )

        assertFalse(transformer.qualifies(observation))
    }

    @Test
    fun `does not qualify when no code`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = vitalSignsCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = null
        )

        assertFalse(transformer.qualifies(observation))
    }

    @Test
    fun `does not qualify when no code coding`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = vitalSignsCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(text = "code".asFHIR())
        )

        assertFalse(transformer.qualifies(observation))
    }

    @Test
    fun `does not qualify when code coding is present, but no entries match pulse oximetry`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = vitalSignsCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        code = Code("8867-4")
                    ),
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        code = Code("vital-signs")
                    )
                )
            )
        )

        assertFalse(transformer.qualifies(observation))
    }

    @Test
    fun `does not qualify when wrong system for code coding`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = vitalSignsCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("faulty"),
                        code = pulseOximetryCode
                    )
                )
            )
        )

        assertFalse(transformer.qualifies(observation))
    }

    @Test
    fun `does not qualify when components are good, with bad code coding code`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = vitalSignsCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.LOINC.uri,
                        code = Code("13245")
                    )
                )
            ),
            component = listOf(
                ObservationComponent(
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        assertFalse(transformer.qualifies(observation))
    }

    @Test
    fun `does not qualify when components are good, with bad code coding system`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = vitalSignsCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("faulty"),
                        code = pulseOximetryCode
                    )
                )
            ),
            component = listOf(
                ObservationComponent(
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            )
        )

        assertFalse(transformer.qualifies(observation))
    }

    @Test
    fun `qualifies for profile`() {
        val observation = Observation(
            id = Id("123"),
            status = ObservationStatus.AMENDED.asCode(),
            dataAbsentReason = CodeableConcept(text = "dataAbsent".asFHIR()),
            category = vitalSignsCategoryConceptList,
            subject = Reference(reference = "Patient/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            code = pulseOximetryConcept
        )

        assertTrue(transformer.qualifies(observation))
    }

    @Test
    fun `transform works`() {
        val observation = Observation(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical("https://www.hl7.org/fhir/observation")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(
                status = com.projectronin.interop.fhir.r4.valueset.NarrativeStatus.GENERATED.asCode(),
                div = "div".asFHIR()
            ),
            contained = listOf(Location(id = Id("67890"))),
            extension = listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = listOf(Identifier(value = "id".asFHIR())),
            basedOn = listOf(Reference(reference = "CarePlan/1234".asFHIR())),
            partOf = listOf(Reference(reference = "MedicationStatement/1234".asFHIR())),
            status = ObservationStatus.AMENDED.asCode(),
            category = vitalSignsCategoryConceptList,
            code = pulseOximetryConcept,
            subject = Reference(reference = "Patient/1234".asFHIR()),
            focus = listOf(Reference(display = "focus".asFHIR())),
            encounter = Reference(reference = "Encounter/1234".asFHIR()),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            issued = Instant("2022-01-01T00:00:00Z"),
            performer = listOf(Reference(reference = "Organization/1234".asFHIR())),
            value = DynamicValue(
                type = DynamicValueType.STRING,
                "string"
            ),
            interpretation = listOf(CodeableConcept(text = "interpretation".asFHIR())),
            bodySite = CodeableConcept(text = "bodySite".asFHIR()),
            method = CodeableConcept(text = "method".asFHIR()),
            specimen = Reference(reference = "Specimen/1234".asFHIR()),
            device = Reference(reference = "DeviceMetric/1234".asFHIR()),
            referenceRange = listOf(ObservationReferenceRange(text = "referenceRange".asFHIR())),
            hasMember = listOf(Reference(reference = "Observation/5678".asFHIR())),
            derivedFrom = listOf(Reference(reference = "DocumentReference/1234".asFHIR())),
            component = listOf(
                ObservationComponent(
                    code = flowRateCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 110.0),
                            unit = "L/min".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("L/min")
                        )
                    )
                ),
                ObservationComponent(
                    code = concentrationCodeableConcept,
                    value = DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(value = 70.0),
                            unit = "%".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("%")
                        )
                    )
                )
            ),
            note = listOf(
                Annotation(
                    text = Markdown("text"),
                    author = DynamicValue(type = DynamicValueType.REFERENCE, value = "Practitioner/0001")
                )
            )
        )

        val transformResponse = transformer.transform(observation, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Observation", transformed.resourceType)
        assertEquals(Id("123"), transformed.id)
        assertEquals(
            Meta(
                profile = listOf(Canonical(RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)),
                source = Uri("source")
            ),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(
            Narrative(
                status = com.projectronin.interop.fhir.r4.valueset.NarrativeStatus.GENERATED.asCode(),
                div = "div".asFHIR()
            ),
            transformed.text
        )
        assertEquals(
            listOf(Location(id = Id("67890"))),
            transformed.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            transformed.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            transformed.modifierExtension
        )
        assertEquals(
            listOf(
                Identifier(value = "id".asFHIR()),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "123".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertEquals(listOf(Reference(reference = "CarePlan/1234".asFHIR())), transformed.basedOn)
        assertEquals(listOf(Reference(reference = "MedicationStatement/1234".asFHIR())), transformed.partOf)
        assertEquals(ObservationStatus.AMENDED.asCode(), transformed.status)
        assertEquals(
            vitalSignsCategoryConceptList,
            transformed.category
        )
        assertEquals(
            pulseOximetryConcept,
            transformed.code
        )
        assertEquals(
            Reference(reference = "Patient/1234".asFHIR()),
            transformed.subject
        )
        assertEquals(listOf(Reference(display = "focus".asFHIR())), transformed.focus)
        assertEquals(Reference(reference = "Encounter/1234".asFHIR()), transformed.encounter)
        assertEquals(
            DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z"
            ),
            transformed.effective
        )
        assertEquals(Instant("2022-01-01T00:00:00Z"), transformed.issued)
        assertEquals(listOf(Reference(reference = "Organization/1234".asFHIR())), transformed.performer)
        assertEquals(
            DynamicValue(
                type = DynamicValueType.STRING,
                "string"
            ),
            transformed.value
        )
        Assertions.assertNull(transformed.dataAbsentReason)
        assertEquals(listOf(CodeableConcept(text = "interpretation".asFHIR())), transformed.interpretation)
        assertEquals(CodeableConcept(text = "bodySite".asFHIR()), transformed.bodySite)
        assertEquals(CodeableConcept(text = "method".asFHIR()), transformed.method)
        assertEquals(Reference(reference = "Specimen/1234".asFHIR()), transformed.specimen)
        assertEquals(Reference(reference = "DeviceMetric/1234".asFHIR()), transformed.device)
        assertEquals(listOf(ObservationReferenceRange(text = "referenceRange".asFHIR())), transformed.referenceRange)
        assertEquals(listOf(Reference(reference = "Observation/5678".asFHIR())), transformed.hasMember)
        assertEquals(listOf(Reference(reference = "DocumentReference/1234".asFHIR())), transformed.derivedFrom)
        assertEquals(
            ObservationComponent(
                code = flowRateCodeableConcept,
                value = DynamicValue(
                    DynamicValueType.QUANTITY,
                    Quantity(
                        value = Decimal(value = 110.0),
                        unit = "L/min".asFHIR(),
                        system = CodeSystem.UCUM.uri,
                        code = Code("L/min")
                    )
                )
            ),
            transformed.component[0]
        )
        assertEquals(
            ObservationComponent(
                code = concentrationCodeableConcept,
                value = DynamicValue(
                    DynamicValueType.QUANTITY,
                    Quantity(
                        value = Decimal(value = 70.0),
                        unit = "%".asFHIR(),
                        system = CodeSystem.UCUM.uri,
                        code = Code("%")
                    )
                )
            ),
            transformed.component[1]
        )
        assertEquals(
            listOf(
                Annotation(
                    text = Markdown("text"),
                    author = DynamicValue(type = DynamicValueType.REFERENCE, value = "Practitioner/0001")
                )
            ),
            transformed.note
        )
    }
}
