package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Attachment
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
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.Url
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceContent
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceContext
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceRelatesTo
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.valueset.CompositionStatus
import com.projectronin.interop.fhir.r4.valueset.DocumentReferenceStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninDocumentReferenceTransformerTest {
    private val transformer = RoninDocumentReferenceTransformer()

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }

    @Test
    fun `returns supported resource`() {
        assertEquals(DocumentReference::class, transformer.supportedResource)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(transformer.qualifies(DocumentReference(status = DocumentReferenceStatus.CURRENT.asCode())))
    }

    @Test
    fun `transforms with all attributes`() {
        val documentReference =
            DocumentReference(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical("http://hl7.org/fhir/R4/DocumentReference.html")),
                        source = Uri("source"),
                    ),
                implicitRules = Uri("implicit-rules"),
                language = Code("en-US"),
                text =
                    Narrative(
                        status = com.projectronin.interop.fhir.r4.valueset.NarrativeStatus.GENERATED.asCode(),
                        div = "div".asFHIR(),
                    ),
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
                identifier = listOf(Identifier(value = "67890".asFHIR())),
                docStatus = CompositionStatus.FINAL.asCode(),
                date = Instant("2003-04-03T00:00:00Z"),
                author = listOf(Reference(reference = "Practitioner/456".asFHIR())),
                authenticator = Reference(reference = "Practitioner/123".asFHIR()),
                custodian = Reference(reference = "Organization/123".asFHIR()),
                relatesTo =
                    listOf(
                        DocumentReferenceRelatesTo(
                            code = com.projectronin.interop.fhir.r4.valueset.DocumentRelationshipType.SIGNS.asCode(),
                            target = Reference(reference = "DocumentReference/ABC".asFHIR()),
                        ),
                    ),
                description = "everywhere".asFHIR(),
                securityLabel =
                    listOf(
                        CodeableConcept(
                            coding = listOf(Coding(code = Code("a"), system = Uri("b"), display = "c".asFHIR())),
                            text = "d".asFHIR(),
                        ),
                    ),
                context =
                    DocumentReferenceContext(
                        encounter = listOf(Reference(reference = "Encounter/ABC".asFHIR())),
                        related = listOf(Reference(reference = "DocumentReference/XYZ".asFHIR())),
                    ),
                type =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(system = Uri("http://loinc.org"), code = Code("34806-0")),
                            ),
                    ),
                status = DocumentReferenceStatus.CURRENT.asCode(),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = Uri("http://hl7.org/fhir/us/core/CodeSystem/us-core-documentreference-category"),
                                        code = Code("clinical-note"),
                                    ),
                                ),
                        ),
                    ),
                content =
                    listOf(
                        DocumentReferenceContent(
                            attachment =
                                Attachment(
                                    url =
                                        Url(
                                            "Binary/1234",
                                            extension =
                                                listOf(
                                                    Extension(
                                                        url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                                        value = DynamicValue(DynamicValueType.URL, Url("datalakeLocation/1234")),
                                                    ),
                                                ),
                                        ),
                                ),
                        ),
                    ),
                subject = Reference(reference = "Patient/123".asFHIR()),
            )

        val transformResponse = transformer.transform(documentReference, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.DOCUMENT_REFERENCE.value)), source = Uri("source")),
            transformed.meta,
        )
        assertEquals(documentReference.implicitRules, transformed.implicitRules)
        assertEquals(documentReference.language, transformed.language)
        assertEquals(documentReference.text, transformed.text)
        assertEquals(documentReference.contained, transformed.contained)
        assertEquals(documentReference.extension, transformed.extension)
        assertEquals(documentReference.modifierExtension, transformed.modifierExtension)
        assertEquals(4, transformed.identifier.size)
        assertEquals(
            listOf(
                Identifier(value = "67890".asFHIR()),
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
        assertEquals(documentReference.type, transformed.type)
        assertEquals(documentReference.status, transformed.status)
        assertEquals(documentReference.docStatus, transformed.docStatus)
        assertEquals(documentReference.date, transformed.date)
        assertEquals(documentReference.author, transformed.author)
        assertEquals(documentReference.authenticator, transformed.authenticator)
        assertEquals(documentReference.custodian, transformed.custodian)
        assertEquals(documentReference.relatesTo, transformed.relatesTo)
        assertEquals(documentReference.description, transformed.description)
        assertEquals(documentReference.securityLabel, transformed.securityLabel)
        assertEquals(documentReference.context, transformed.context)
    }
}
