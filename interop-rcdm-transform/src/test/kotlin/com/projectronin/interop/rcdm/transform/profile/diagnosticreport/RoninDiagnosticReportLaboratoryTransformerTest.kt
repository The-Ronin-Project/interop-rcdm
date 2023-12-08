package com.projectronin.interop.rcdm.transform.profile.diagnosticreport

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
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.fhir.r4.resource.DiagnosticReportMedia
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.valueset.DiagnosticReportStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninDiagnosticReportLaboratoryTransformerTest {
    private val transformer = RoninDiagnosticReportLaboratoryTransformer()

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }

    @Test
    fun `returns supported resource`() {
        assertEquals(DiagnosticReport::class, transformer.supportedResource)
    }

    @Test
    fun `returns not default`() {
        assertFalse(transformer.isDefault)
    }

    @Test
    fun `returns proper profile`() {
        assertEquals(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY, transformer.profile)
    }

    @Test
    fun `qualifies for lab category`() {
        val diagnosticReport =
            DiagnosticReport(
                code = CodeableConcept(text = "code".asFHIR()),
                status = DiagnosticReportStatus.FINAL.asCode(),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.DIAGNOSTIC_REPORT_LABORATORY.uri,
                                        code = Code("LAB"),
                                    ),
                                ),
                        ),
                    ),
            )
        assertTrue(transformer.qualifies(diagnosticReport))
    }

    @Test
    fun `does not qualify for other category`() {
        val diagnosticReport =
            DiagnosticReport(
                code = CodeableConcept(text = "code".asFHIR()),
                status = DiagnosticReportStatus.FINAL.asCode(),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.DIAGNOSTIC_REPORT_LABORATORY.uri,
                                        code = Code("NOT-SUPPORTED"),
                                    ),
                                ),
                        ),
                    ),
            )
        assertFalse(transformer.qualifies(diagnosticReport))
    }

    @Test
    fun `transform works`() {
        val dxReport =
            DiagnosticReport(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical("http://hl7.org/fhir/R4/diagnosticreport.html")),
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
                identifier = listOf(Identifier(value = "id".asFHIR())),
                basedOn =
                    listOf(
                        Reference(id = "basedOnId".asFHIR(), display = "basedOnDisplay".asFHIR()),
                    ),
                status = Code("registered"),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074"),
                                        code = Code("LAB"),
                                    ),
                                ),
                        ),
                    ),
                code = CodeableConcept(text = "dx report".asFHIR()),
                subject = Reference(display = "reference".asFHIR()),
                encounter = Reference(id = "encounterReference".asFHIR(), display = "encounterDisplay".asFHIR()),
                effective =
                    DynamicValue(
                        type = DynamicValueType.DATE_TIME,
                        "2022-01-01T00:00:00Z",
                    ),
                issued = Instant("2018-02-02T00:00:00Z"),
                performer =
                    listOf(
                        Reference(id = "performerId".asFHIR(), display = "performerDisplay".asFHIR()),
                    ),
                resultsInterpreter =
                    listOf(
                        Reference(id = "resultsInterpreter".asFHIR(), display = "resultsInterpreterDisplay".asFHIR()),
                    ),
                specimen =
                    listOf(
                        Reference(id = "specimenId".asFHIR(), display = "specimenDisplay".asFHIR()),
                    ),
                result =
                    listOf(
                        Reference(id = "resultId".asFHIR(), display = "resultDisplay".asFHIR()),
                    ),
                imagingStudy =
                    listOf(
                        Reference(id = "imagingStudyId".asFHIR(), display = "imagingStudyDisplay".asFHIR()),
                    ),
                media =
                    listOf(
                        DiagnosticReportMedia(
                            id = "mediaId".asFHIR(),
                            link = Reference(id = "linkId".asFHIR(), display = "linkDisplay".asFHIR()),
                        ),
                    ),
                conclusion = "conclusionFhirString".asFHIR(),
                conclusionCode =
                    listOf(
                        CodeableConcept(text = "conclusionCode".asFHIR()),
                    ),
                presentedForm =
                    listOf(
                        Attachment(id = "attachmentId".asFHIR()),
                    ),
            )

        val transformResponse = transformer.transform(dxReport, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource

        assertEquals("DiagnosticReport", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.value)), source = Uri("source")),
            transformed.meta,
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(
            Narrative(
                status = com.projectronin.interop.fhir.r4.valueset.NarrativeStatus.GENERATED.asCode(),
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
        assertEquals(
            listOf(
                Reference(id = "basedOnId".asFHIR(), display = "basedOnDisplay".asFHIR()),
            ),
            transformed.basedOn,
        )
        assertEquals(Code("registered"), transformed.status)
        assertEquals(
            listOf(
                CodeableConcept(
                    coding =
                        listOf(
                            Coding(
                                system = Uri("http://terminology.hl7.org/CodeSystem/v2-0074"),
                                code = Code("LAB"),
                            ),
                        ),
                ),
            ),
            transformed.category,
        )
        assertEquals(
            CodeableConcept(
                text = "dx report".asFHIR(),
            ),
            transformed.code,
        )
        assertEquals(Reference(display = "reference".asFHIR()), transformed.subject)
        assertEquals(
            Reference(
                id = "encounterReference".asFHIR(),
                display = "encounterDisplay".asFHIR(),
            ),
            transformed.encounter,
        )
        assertEquals(
            DynamicValue(
                type = DynamicValueType.DATE_TIME,
                "2022-01-01T00:00:00Z",
            ),
            transformed.effective,
        )
        assertEquals(
            Instant("2018-02-02T00:00:00Z"),
            transformed.issued,
        )
        assertEquals(
            listOf(
                Reference(id = "performerId".asFHIR(), display = "performerDisplay".asFHIR()),
            ),
            transformed.performer,
        )
        assertEquals(
            listOf(
                Reference(id = "resultsInterpreter".asFHIR(), display = "resultsInterpreterDisplay".asFHIR()),
            ),
            transformed.resultsInterpreter,
        )
        assertEquals(
            listOf(
                Reference(id = "specimenId".asFHIR(), display = "specimenDisplay".asFHIR()),
            ),
            transformed.specimen,
        )
        assertEquals(
            listOf(
                Reference(id = "resultId".asFHIR(), display = "resultDisplay".asFHIR()),
            ),
            transformed.result,
        )
        assertEquals(
            listOf(
                Reference(id = "imagingStudyId".asFHIR(), display = "imagingStudyDisplay".asFHIR()),
            ),
            transformed.imagingStudy,
        )
        assertEquals(
            listOf(
                DiagnosticReportMedia(
                    id = "mediaId".asFHIR(),
                    link = Reference(id = "linkId".asFHIR(), display = "linkDisplay".asFHIR()),
                ),
            ),
            transformed.media,
        )
        assertEquals("conclusionFhirString".asFHIR(), transformed.conclusion)
        assertEquals(
            listOf(
                CodeableConcept(text = "conclusionCode".asFHIR()),
            ),
            transformed.conclusionCode,
        )
        assertEquals(
            listOf(
                Attachment(id = "attachmentId".asFHIR()),
            ),
            transformed.presentedForm,
        )
    }
}
