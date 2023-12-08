package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Duration
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.fhir.r4.resource.EncounterClassHistory
import com.projectronin.interop.fhir.r4.resource.EncounterDiagnosis
import com.projectronin.interop.fhir.r4.resource.EncounterHospitalization
import com.projectronin.interop.fhir.r4.resource.EncounterLocation
import com.projectronin.interop.fhir.r4.resource.EncounterParticipant
import com.projectronin.interop.fhir.r4.resource.EncounterStatusHistory
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.valueset.EncounterStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RoninEncounterTransformerTest {
    private val transformer = RoninEncounterTransformer()

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }

    @Test
    fun `returns supported resource`() {
        assertEquals(Encounter::class, transformer.supportedResource)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(
            transformer.qualifies(
                Encounter(
                    status = EncounterStatus.CANCELLED.asCode(),
                    `class` = Coding(code = Code("OBSENC")),
                ),
            ),
        )
    }

    @Test
    fun `transforms encounter with all attributes`() {
        val encounter =
            Encounter(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical("http://hl7.org/fhir/R4/encounter.html")),
                        source = Uri("source"),
                    ),
                implicitRules = Uri("implicit-rules"),
                language = Code("en-US"),
                text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
                contained = listOf(Location(id = Id("67890"))),
                extension =
                    listOf(
                        Extension(
                            url = Uri("http://hl7.org/extension-1"),
                            value = DynamicValue(DynamicValueType.STRING, "value"),
                        ),
                    ),
                modifierExtension =
                    listOf(
                        Extension(
                            url = Uri("http://localhost/modifier-extension"),
                            value = DynamicValue(DynamicValueType.STRING, "Value"),
                        ),
                    ),
                identifier = listOf(Identifier(value = "id".asFHIR(), system = Uri("urn:oid:1:2:3"))),
                status = EncounterStatus.CANCELLED.asCode(),
                statusHistory =
                    listOf(
                        EncounterStatusHistory(
                            status = EncounterStatus.PLANNED.asCode(),
                            period =
                                Period(
                                    start = DateTime(value = "2021-11-16"),
                                    end = DateTime(value = "2021-11-17T08:00:00Z"),
                                ),
                        ),
                        EncounterStatusHistory(
                            status = EncounterStatus.ARRIVED.asCode(),
                            period =
                                Period(
                                    start = DateTime(value = "2021-11-17T08:00:00Z"),
                                    end = DateTime(value = "2021-11-17T09:00:00Z"),
                                ),
                        ),
                        EncounterStatusHistory(
                            status = EncounterStatus.IN_PROGRESS.asCode(),
                            period =
                                Period(
                                    start = DateTime(value = "2021-11-17T09:00:00Z"),
                                    end = DateTime(value = "2021-11-17T10:00:00Z"),
                                ),
                        ),
                        EncounterStatusHistory(
                            status = EncounterStatus.FINISHED.asCode(),
                            period =
                                Period(
                                    start = DateTime(value = "2021-11-17T10:00:00Z"),
                                ),
                        ),
                    ),
                `class` =
                    Coding(
                        system = Uri("http://terminology.hl7.org/CodeSystem/v3-ActCode"),
                        code = Code("AMB"),
                        display = "ambulatory".asFHIR(),
                    ),
                classHistory =
                    listOf(
                        EncounterClassHistory(
                            `class` = Code("AMB"),
                            period =
                                Period(
                                    start = DateTime(value = "2021-11-16"),
                                ),
                        ),
                    ),
                type =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.SNOMED_CT.uri,
                                        code = Code("270427003"),
                                        display = "Patient-initiated encounter".asFHIR(),
                                    ),
                                ),
                        ),
                    ),
                serviceType = null,
                priority =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = CodeSystem.SNOMED_CT.uri,
                                    code = Code("103391001"),
                                    display = "Non-urgent ear, nose and throat admission".asFHIR(),
                                ),
                            ),
                    ),
                subject = Reference(reference = "Patient/test-1234".asFHIR()),
                episodeOfCare = emptyList(),
                basedOn = emptyList(),
                participant =
                    listOf(
                        EncounterParticipant(
                            individual =
                                Reference(
                                    reference = "Practitioner/f001".asFHIR(),
                                    display = "E.M. van den Broek".asFHIR(),
                                ),
                        ),
                    ),
                appointment = emptyList(),
                period = null,
                length =
                    Duration(
                        value = Decimal(BigDecimal(90.0)),
                        unit = "min".asFHIR(),
                        system = CodeSystem.UCUM.uri,
                        code = Code("min"),
                    ),
                reasonCode =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.SNOMED_CT.uri,
                                        code = Code("18099001"),
                                        display = "Retropharyngeal abscess".asFHIR(),
                                    ),
                                ),
                        ),
                    ),
                reasonReference =
                    listOf(
                        Reference(
                            reference = "Condition/f001".asFHIR(),
                            display = "Test Condition".asFHIR(),
                        ),
                    ),
                diagnosis =
                    listOf(
                        EncounterDiagnosis(
                            condition = Reference(reference = "Condition/stroke".asFHIR()),
                            use =
                                CodeableConcept(
                                    coding =
                                        listOf(
                                            Coding(
                                                system = Uri("http://terminology.hl7.org/CodeSystem/diagnosis-role"),
                                                code = Code("AD"),
                                                display = "Admission diagnosis".asFHIR(),
                                            ),
                                        ),
                                ),
                            rank = PositiveInt(1),
                        ),
                        EncounterDiagnosis(
                            condition = Reference(reference = "Condition/f201".asFHIR()),
                            use =
                                CodeableConcept(
                                    coding =
                                        listOf(
                                            Coding(
                                                system = Uri("http://terminology.hl7.org/CodeSystem/diagnosis-role"),
                                                code = Code("DD"),
                                                display = "Discharge diagnosis".asFHIR(),
                                            ),
                                        ),
                                ),
                        ),
                    ),
                account = listOf(Reference(reference = "Account/f001".asFHIR())),
                hospitalization =
                    EncounterHospitalization(
                        preAdmissionIdentifier =
                            Identifier(
                                use = Code("official"),
                                system = Uri("http://www.bmc.nl/zorgportal/identifiers/pre-admissions"),
                                value = "93042".asFHIR(),
                            ),
                        origin = null,
                        admitSource =
                            CodeableConcept(
                                coding =
                                    listOf(
                                        Coding(
                                            system = CodeSystem.SNOMED_CT.uri,
                                            code = Code("305956004"),
                                            display = "Referral by physician".asFHIR(),
                                        ),
                                    ),
                            ),
                        reAdmission = null,
                        dietPreference =
                            listOf(
                                CodeableConcept(
                                    coding =
                                        listOf(
                                            Coding(
                                                system = Uri("https://www.hl7.org/fhir/R4/valueset-encounter-diet.html"),
                                                code = Code("vegetarian"),
                                                display = "vegetarian".asFHIR(),
                                            ),
                                        ),
                                ),
                                CodeableConcept(
                                    coding =
                                        listOf(
                                            Coding(
                                                system = Uri("https://www.hl7.org/fhir/R4/valueset-encounter-diet.html"),
                                                code = Code("kosher"),
                                                display = "kosher".asFHIR(),
                                            ),
                                        ),
                                ),
                            ),
                        specialCourtesy = emptyList(),
                        specialArrangement = emptyList(),
                        destination = Reference(reference = "Location/place".asFHIR()),
                        dischargeDisposition =
                            CodeableConcept(
                                coding =
                                    listOf(
                                        Coding(
                                            system = CodeSystem.SNOMED_CT.uri,
                                            code = Code("306689006"),
                                            display = "Discharge to home".asFHIR(),
                                        ),
                                    ),
                            ),
                    ),
                location =
                    listOf(
                        EncounterLocation(
                            location = Reference(reference = "Location/f001".asFHIR()),
                            status = com.projectronin.interop.fhir.r4.valueset.EncounterLocationStatus.RESERVED.asCode(),
                            physicalType =
                                CodeableConcept(
                                    coding =
                                        listOf(
                                            Coding(
                                                system = Uri("http://terminology.hl7.org/CodeSystem/location-physical-type"),
                                                code = Code("area"),
                                                display = "Area".asFHIR(),
                                            ),
                                        ),
                                ),
                        ),
                    ),
                serviceProvider =
                    Reference(
                        reference = "Organization/f001".asFHIR(),
                        display = "Community Hospital".asFHIR(),
                    ),
                partOf = Reference(reference = "Encounter/super".asFHIR()),
            )

        val transformResponse = transformer.transform(encounter, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource

        assertEquals("Encounter", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            transformed.meta,
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()), transformed.text)
        assertEquals(
            listOf(Location(id = Id("67890"))),
            transformed.contained,
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value"),
                ),
            ),
            transformed.extension,
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value"),
                ),
            ),
            transformed.modifierExtension,
        )
        assertEquals(
            listOf(
                Identifier(system = Uri("urn:oid:1:2:3"), value = "id".asFHIR()),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR(),
                ),
            ),
            transformed.identifier,
        )
        assertEquals(EncounterStatus.CANCELLED.asCode(), transformed.status)
        assertEquals(
            listOf(
                EncounterStatusHistory(
                    status = EncounterStatus.PLANNED.asCode(),
                    period =
                        Period(
                            start = DateTime(value = "2021-11-16"),
                            end = DateTime(value = "2021-11-17T08:00:00Z"),
                        ),
                ),
                EncounterStatusHistory(
                    status = EncounterStatus.ARRIVED.asCode(),
                    period =
                        Period(
                            start = DateTime(value = "2021-11-17T08:00:00Z"),
                            end = DateTime(value = "2021-11-17T09:00:00Z"),
                        ),
                ),
                EncounterStatusHistory(
                    status = EncounterStatus.IN_PROGRESS.asCode(),
                    period =
                        Period(
                            start = DateTime(value = "2021-11-17T09:00:00Z"),
                            end = DateTime(value = "2021-11-17T10:00:00Z"),
                        ),
                ),
                EncounterStatusHistory(
                    status = EncounterStatus.FINISHED.asCode(),
                    period =
                        Period(
                            start = DateTime(value = "2021-11-17T10:00:00Z"),
                        ),
                ),
            ),
            transformed.statusHistory,
        )
        assertEquals(
            Coding(
                system = Uri("http://terminology.hl7.org/CodeSystem/v3-ActCode"),
                code = Code("AMB"),
                display = "ambulatory".asFHIR(),
            ),
            transformed.`class`,
        )
        assertEquals(
            listOf(
                EncounterClassHistory(
                    `class` = Code("AMB"),
                    period =
                        Period(
                            start = DateTime(value = "2021-11-16"),
                        ),
                ),
            ),
            transformed.classHistory,
        )
        assertEquals(
            listOf(
                CodeableConcept(
                    coding =
                        listOf(
                            Coding(
                                system = CodeSystem.SNOMED_CT.uri,
                                code = Code("270427003"),
                                display = "Patient-initiated encounter".asFHIR(),
                            ),
                        ),
                ),
            ),
            transformed.type,
        )
        Assertions.assertNull(transformed.serviceType)
        assertEquals(
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            system = CodeSystem.SNOMED_CT.uri,
                            code = Code("103391001"),
                            display = "Non-urgent ear, nose and throat admission".asFHIR(),
                        ),
                    ),
            ),
            transformed.priority,
        )
        assertEquals(
            Reference(reference = "Patient/test-1234".asFHIR()),
            transformed.subject,
        )
        assertEquals(listOf<Reference>(), transformed.episodeOfCare)
        assertEquals(listOf<Reference>(), transformed.basedOn)
        assertEquals(
            listOf(
                EncounterParticipant(
                    individual =
                        Reference(
                            reference = "Practitioner/f001".asFHIR(),
                            display = "E.M. van den Broek".asFHIR(),
                        ),
                ),
            ),
            transformed.participant,
        )
        assertEquals(listOf<Reference>(), transformed.appointment)
        Assertions.assertNull(transformed.period)
        assertEquals(
            Duration(
                value = Decimal(BigDecimal(90.0)),
                unit = "min".asFHIR(),
                system = CodeSystem.UCUM.uri,
                code = Code("min"),
            ),
            transformed.length,
        )
        assertEquals(
            listOf(
                CodeableConcept(
                    coding =
                        listOf(
                            Coding(
                                system = CodeSystem.SNOMED_CT.uri,
                                code = Code("18099001"),
                                display = "Retropharyngeal abscess".asFHIR(),
                            ),
                        ),
                ),
            ),
            transformed.reasonCode,
        )
        assertEquals(
            listOf(
                Reference(
                    reference = "Condition/f001".asFHIR(),
                    display = "Test Condition".asFHIR(),
                ),
            ),
            transformed.reasonReference,
        )
        assertEquals(
            listOf(
                EncounterDiagnosis(
                    condition = Reference(reference = "Condition/stroke".asFHIR()),
                    use =
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = Uri("http://terminology.hl7.org/CodeSystem/diagnosis-role"),
                                        code = Code("AD"),
                                        display = "Admission diagnosis".asFHIR(),
                                    ),
                                ),
                        ),
                    rank = PositiveInt(1),
                ),
                EncounterDiagnosis(
                    condition = Reference(reference = "Condition/f201".asFHIR()),
                    use =
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = Uri("http://terminology.hl7.org/CodeSystem/diagnosis-role"),
                                        code = Code("DD"),
                                        display = "Discharge diagnosis".asFHIR(),
                                    ),
                                ),
                        ),
                ),
            ),
            transformed.diagnosis,
        )
        assertEquals(listOf(Reference(reference = "Account/f001".asFHIR())), transformed.account)
        assertEquals(
            EncounterHospitalization(
                preAdmissionIdentifier =
                    Identifier(
                        use = Code("official"),
                        system = Uri("http://www.bmc.nl/zorgportal/identifiers/pre-admissions"),
                        value = "93042".asFHIR(),
                    ),
                origin = null,
                admitSource =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = CodeSystem.SNOMED_CT.uri,
                                    code = Code("305956004"),
                                    display = "Referral by physician".asFHIR(),
                                ),
                            ),
                    ),
                reAdmission = null,
                dietPreference =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = Uri("https://www.hl7.org/fhir/R4/valueset-encounter-diet.html"),
                                        code = Code("vegetarian"),
                                        display = "vegetarian".asFHIR(),
                                    ),
                                ),
                        ),
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = Uri("https://www.hl7.org/fhir/R4/valueset-encounter-diet.html"),
                                        code = Code("kosher"),
                                        display = "kosher".asFHIR(),
                                    ),
                                ),
                        ),
                    ),
                specialCourtesy = emptyList(),
                specialArrangement = emptyList(),
                destination = Reference(reference = "Location/place".asFHIR()),
                dischargeDisposition =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = CodeSystem.SNOMED_CT.uri,
                                    code = Code("306689006"),
                                    display = "Discharge to home".asFHIR(),
                                ),
                            ),
                    ),
            ),
            transformed.hospitalization,
        )
        assertEquals(
            listOf(
                EncounterLocation(
                    location = Reference(reference = "Location/f001".asFHIR()),
                    status = com.projectronin.interop.fhir.r4.valueset.EncounterLocationStatus.RESERVED.asCode(),
                    physicalType =
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = Uri("http://terminology.hl7.org/CodeSystem/location-physical-type"),
                                        code = Code("area"),
                                        display = "Area".asFHIR(),
                                    ),
                                ),
                        ),
                ),
            ),
            transformed.location,
        )
        assertEquals(
            Reference(
                reference = "Organization/f001".asFHIR(),
                display = "Community Hospital".asFHIR(),
            ),
            transformed.serviceProvider,
        )
        assertEquals(Reference(reference = "Encounter/super".asFHIR()), transformed.partOf)
    }
}
