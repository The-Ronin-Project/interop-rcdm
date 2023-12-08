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
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Instant
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.ObservationComponent
import com.projectronin.interop.fhir.r4.resource.ObservationReferenceRange
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.dataAuthorityExtension
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ValueSetList
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninStagingRelatedTransformerTest {
    private val stagingRelatedCode = Code("some-code")
    private val stagingRelatedCodingList =
        listOf(
            Coding(
                system = Uri("some-system-uri"),
                code = stagingRelatedCode,
                display = "some-display".asFHIR(),
            ),
        )
    private val stagingRelatedConcept =
        CodeableConcept(
            text = "Staging Related".asFHIR(),
            coding = stagingRelatedCodingList,
        )

    private val registryClient =
        mockk<NormalizationRegistryClient> {
            every {
                getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_STAGING_RELATED.value)
            } returns ValueSetList(stagingRelatedCodingList, mockk())
        }

    private val transformer = RoninStagingRelatedTransformer(registryClient)

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }

    @Test
    fun `returns correct profile`() {
        assertEquals(RoninProfile.OBSERVATION_STAGING_RELATED, transformer.profile)
    }

    @Test
    fun `returns not default`() {
        assertFalse(transformer.isDefault)
    }

    @Test
    fun `does not qualify when no category`() {
        val observation =
            Observation(
                id = Id("123"),
                status = ObservationStatus.AMENDED.asCode(),
                category = listOf(),
                subject =
                    Reference(
                        reference = "Patient/1234".asFHIR(),
                        type = Uri("Patient", extension = dataAuthorityExtension),
                    ),
                code = stagingRelatedConcept,
            )

        assertFalse(transformer.qualifies(observation))
    }

    @Test
    fun `does not qualify when no category coding`() {
        val observation =
            Observation(
                id = Id("123"),
                status = ObservationStatus.AMENDED.asCode(),
                category = listOf(CodeableConcept(coding = listOf())),
                subject =
                    Reference(
                        reference = "Patient/1234".asFHIR(),
                        type = Uri("Patient", extension = dataAuthorityExtension),
                    ),
                code = stagingRelatedConcept,
            )

        assertFalse(transformer.qualifies(observation))
    }

    @Test
    fun `does not qualify when no code coding`() {
        val observation =
            Observation(
                id = Id("123"),
                status = ObservationStatus.AMENDED.asCode(),
                category = listOf(stagingRelatedConcept),
                subject =
                    Reference(
                        reference = "Patient/1234".asFHIR(),
                        type = Uri("Patient", extension = dataAuthorityExtension),
                    ),
                code = CodeableConcept(text = "fake staging code".asFHIR()),
            )

        assertFalse(transformer.qualifies(observation))
    }

    @Test
    fun qualifies() {
        val observation =
            Observation(
                id = Id("123"),
                status = ObservationStatus.AMENDED.asCode(),
                category = listOf(stagingRelatedConcept),
                subject =
                    Reference(
                        reference = "Patient/1234".asFHIR(),
                        type = Uri("Patient", extension = dataAuthorityExtension),
                    ),
                code = stagingRelatedConcept,
            )

        assertTrue(transformer.qualifies(observation))
    }

    @Test
    fun `transform works`() {
        val observation =
            Observation(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical("https://www.hl7.org/fhir/observation")),
                        source = Uri("source"),
                    ),
                implicitRules = Uri("implicit-rules"),
                language = Code("en-US"),
                text =
                    Narrative(
                        status = NarrativeStatus.GENERATED.asCode(),
                        div = "div".asFHIR(),
                    ),
                contained = listOf(Location(id = Id("67890"))),
                extension =
                    listOf(
                        Extension(
                            url = Uri("http://localhost/extension"),
                            value = DynamicValue(DynamicValueType.STRING, "Value"),
                        ),
                    ),
                modifierExtension =
                    listOf(
                        Extension(
                            url = Uri("http://localhost/modifier-extension"),
                            value = DynamicValue(DynamicValueType.STRING, "Value"),
                        ),
                    ),
                identifier =
                    listOf(
                        Identifier(value = "id".asFHIR()),
                    ),
                basedOn = listOf(Reference(reference = "ServiceRequest/1234".asFHIR())),
                partOf = listOf(Reference(reference = "ImagingStudy/1234".asFHIR())),
                status = ObservationStatus.AMENDED.asCode(),
                category = listOf(stagingRelatedConcept),
                code = stagingRelatedConcept,
                subject = Reference(reference = "Patient/1234".asFHIR()),
                focus = listOf(Reference(display = "focus".asFHIR())),
                encounter = Reference(reference = "Encounter/1234".asFHIR()),
                effective =
                    DynamicValue(
                        type = DynamicValueType.DATE_TIME,
                        "2022-01-01T00:00:00Z",
                    ),
                issued = Instant("2022-01-01T00:00:00Z"),
                performer = listOf(Reference(reference = "RelatedPerson/1234".asFHIR())),
                value =
                    DynamicValue(
                        type = DynamicValueType.STRING,
                        "string",
                    ),
                interpretation = listOf(CodeableConcept(text = "interpretation".asFHIR())),
                note =
                    listOf(
                        Annotation(
                            text = Markdown("note"),
                            author = DynamicValue(type = DynamicValueType.STRING, value = "THE NOTE"),
                        ),
                    ),
                bodySite = CodeableConcept(text = "bodySite".asFHIR()),
                method = CodeableConcept(text = "method".asFHIR()),
                specimen = Reference(reference = "Specimen/1234".asFHIR()),
                device = Reference(reference = "Device/1234".asFHIR()),
                referenceRange = listOf(ObservationReferenceRange(text = "referenceRange".asFHIR())),
                hasMember = listOf(Reference(reference = "Observation/2345".asFHIR())),
                derivedFrom = listOf(Reference(reference = "Observation/3456".asFHIR())),
                component =
                    listOf(
                        ObservationComponent(
                            code = CodeableConcept(text = "code2".asFHIR()),
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    stagingRelatedConcept,
                                ),
                            extension =
                                listOf(
                                    Extension(
                                        url = Uri("http://localhost/valueExtension"),
                                        value = DynamicValue(DynamicValueType.STRING, "Value"),
                                    ),
                                ),
                        ),
                    ),
            )

        val transformResponse = transformer.transform(observation, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Observation", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.OBSERVATION_STAGING_RELATED.value)), source = Uri("source")),
            transformed.meta,
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(
            Narrative(
                status = NarrativeStatus.GENERATED.asCode(),
                div = "div".asFHIR(),
            ),
            transformed.text,
        )
        assertEquals(
            listOf(Location(id = Id("67890"))),
            transformed.contained,
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value"),
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
                Identifier(value = "id".asFHIR()),
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
        assertEquals(listOf(Reference(reference = "ServiceRequest/1234".asFHIR())), transformed.basedOn)
        assertEquals(listOf(Reference(reference = "ImagingStudy/1234".asFHIR())), transformed.partOf)
        assertEquals(ObservationStatus.AMENDED.asCode(), transformed.status)
        assertEquals(
            listOf(stagingRelatedConcept),
            transformed.category,
        )
        assertEquals(
            stagingRelatedConcept,
            transformed.code,
        )
        assertEquals(
            Reference(reference = "Patient/1234".asFHIR()),
            transformed.subject,
        )
        assertEquals(listOf(Reference(display = "focus".asFHIR())), transformed.focus)
        assertEquals(Reference(reference = "Encounter/1234".asFHIR()), transformed.encounter)
        assertEquals(
            DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z",
            ),
            transformed.effective,
        )
        assertEquals(Instant("2022-01-01T00:00:00Z"), transformed.issued)
        assertEquals(listOf(Reference(reference = "RelatedPerson/1234".asFHIR())), transformed.performer)
        assertEquals(
            DynamicValue(
                type = DynamicValueType.STRING,
                "string",
            ),
            transformed.value,
        )
        assertNull(transformed.dataAbsentReason)
        assertEquals(listOf(CodeableConcept(text = "interpretation".asFHIR())), transformed.interpretation)
        assertEquals(CodeableConcept(text = "bodySite".asFHIR()), transformed.bodySite)
        assertEquals(CodeableConcept(text = "method".asFHIR()), transformed.method)
        assertEquals(Reference(reference = "Specimen/1234".asFHIR()), transformed.specimen)
        assertEquals(Reference(reference = "Device/1234".asFHIR()), transformed.device)
        assertEquals(listOf(ObservationReferenceRange(text = "referenceRange".asFHIR())), transformed.referenceRange)
        assertEquals(listOf(Reference(reference = "Observation/2345".asFHIR())), transformed.hasMember)
        assertEquals(listOf(Reference(reference = "Observation/3456".asFHIR())), transformed.derivedFrom)
        assertEquals(
            listOf(
                ObservationComponent(
                    code = CodeableConcept(text = "code2".asFHIR()),
                    value =
                        DynamicValue(
                            DynamicValueType.CODEABLE_CONCEPT,
                            stagingRelatedConcept,
                        ),
                    extension =
                        listOf(
                            Extension(
                                url = Uri("http://localhost/valueExtension"),
                                value = DynamicValue(DynamicValueType.STRING, "Value"),
                            ),
                        ),
                ),
            ),
            transformed.component,
        )
        assertEquals(
            listOf(
                Annotation(
                    text = Markdown("note"),
                    author = DynamicValue(type = DynamicValueType.STRING, value = "THE NOTE"),
                ),
            ),
            transformed.note,
        )
    }
}
