package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Annotation
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Dosage
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
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.dataAuthorityExtension
import com.projectronin.interop.rcdm.transform.extractor.MedicationExtractor
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninMedicationStatementTransformerTest {
    private val medicationExtractor =
        mockk<MedicationExtractor> {
            every { extractMedication(any(), any(), any()) } returns null
        }
    private val transformer = RoninMedicationStatementTransformer(medicationExtractor)

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }

    @Test
    fun `returns supported resource`() {
        assertEquals(MedicationStatement::class, transformer.supportedResource)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(transformer.qualifies(MedicationStatement()))
    }

    @Test
    fun `transform succeeds with all attributes`() {
        val medicationStatement =
            MedicationStatement(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical("http://hl7.org/fhir/R4/medicationstatement.html")),
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
                basedOn = listOf(Reference(display = "reference".asFHIR())),
                partOf = listOf(Reference(display = "partOf".asFHIR())),
                status = com.projectronin.interop.fhir.r4.valueset.MedicationStatementStatus.ACTIVE.asCode(),
                statusReason = listOf(CodeableConcept(text = "statusReason".asFHIR())),
                category = CodeableConcept(text = "category".asFHIR()),
                medication =
                    DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = FHIRString("Medication/1234")),
                    ),
                subject = Reference(reference = FHIRString("Location/1234")),
                context = Reference(display = "context".asFHIR()),
                effective =
                    DynamicValue(
                        type = DynamicValueType.DATE_TIME,
                        value = DateTime("1905-08-23"),
                    ),
                dateAsserted = DateTime("1905-08-23"),
                informationSource = Reference(display = "informationSource".asFHIR()),
                derivedFrom = listOf(Reference(display = "derivedFrom".asFHIR())),
                reasonCode = listOf(CodeableConcept(text = "reasonCode".asFHIR())),
                reasonReference = listOf(Reference(display = "reasonReference".asFHIR())),
                note = listOf(Annotation(text = Markdown("annotation"))),
                dosage = listOf(Dosage(text = "dosage".asFHIR())),
            )

        val transformResponse = transformer.transform(medicationStatement, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            RoninProfile.MEDICATION_STATEMENT.value,
            transformed.meta!!.profile[0].value,
        )
        assertEquals(medicationStatement.implicitRules, transformed.implicitRules)
        assertEquals(medicationStatement.language, transformed.language)
        assertEquals(medicationStatement.text, transformed.text)
        assertEquals(medicationStatement.contained, transformed.contained)
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value"),
                ),
                Extension(
                    url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                    value =
                        DynamicValue(
                            type = DynamicValueType.CODE,
                            value = Code("literal reference"),
                        ),
                ),
            ),
            transformed.extension,
        )
        assertEquals(medicationStatement.modifierExtension, transformed.modifierExtension)
        assertEquals(4, transformed.identifier.size)
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
        assertEquals(medicationStatement.basedOn, transformed.basedOn)
        assertEquals(medicationStatement.partOf, transformed.partOf)
        assertEquals(medicationStatement.status, transformed.status)
        assertEquals(medicationStatement.statusReason, transformed.statusReason)
        assertEquals(medicationStatement.category, transformed.category)
        assertEquals(medicationStatement.medication, transformed.medication)
        assertEquals(medicationStatement.subject, transformed.subject)
        assertEquals(medicationStatement.context, transformed.context)
        assertEquals(medicationStatement.effective, transformed.effective)
        assertEquals(medicationStatement.informationSource, transformed.informationSource)
        assertEquals(medicationStatement.derivedFrom, transformed.derivedFrom)
        assertEquals(medicationStatement.reasonCode, transformed.reasonCode)
        assertEquals(medicationStatement.reasonReference, transformed.reasonReference)
        assertEquals(medicationStatement.note, transformed.note)
        assertEquals(medicationStatement.dosage, transformed.dosage)
    }

    @Test
    fun `transform succeeds with just required attributes`() {
        val medicationStatement =
            MedicationStatement(
                id = Id("12345"),
                meta = Meta(source = Uri("source")),
                status = com.projectronin.interop.fhir.r4.valueset.MedicationStatementStatus.ACTIVE.asCode(),
                medication =
                    DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = FHIRString("Medication/1234")),
                    ),
                subject =
                    Reference(
                        display = "subject".asFHIR(),
                        reference = "Patient/1234".asFHIR(),
                        type = Uri("Patient", extension = dataAuthorityExtension),
                    ),
                effective =
                    DynamicValue(
                        type = DynamicValueType.DATE_TIME,
                        value = DateTime("1905-08-23"),
                    ),
            )

        val transformResponse = transformer.transform(medicationStatement, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(3, transformed.identifier.size)
        assertEquals(
            listOf(
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
        assertEquals(medicationStatement.status, transformed.status)
        assertEquals(medicationStatement.medication, transformed.medication)
        assertEquals(medicationStatement.subject, transformed.subject)
        assertEquals(medicationStatement.effective, transformed.effective)
    }

    @Test
    fun `transform succeeds with extracted medications`() {
        val containedMedication =
            Medication(
                id = Id("67890"),
                code = CodeableConcept(text = "medication".asFHIR()),
            )
        val originalMedicationDynamicValue =
            DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "#67890".asFHIR()))
        val medicationStatement =
            MedicationStatement(
                id = Id("12345"),
                meta = Meta(source = Uri("source")),
                contained = listOf(containedMedication),
                status = com.projectronin.interop.fhir.r4.valueset.MedicationStatementStatus.ACTIVE.asCode(),
                medication = originalMedicationDynamicValue,
                subject =
                    Reference(
                        display = "subject".asFHIR(),
                        reference = "Patient/1234".asFHIR(),
                        type = Uri("Patient", extension = dataAuthorityExtension),
                    ),
                effective =
                    DynamicValue(
                        type = DynamicValueType.DATE_TIME,
                        value = DateTime("1905-08-23"),
                    ),
            )

        val updatedMedicationDynamicValue =
            DynamicValue(
                DynamicValueType.REFERENCE,
                Reference(reference = "Medication/contained-12345-67890".asFHIR()),
            )
        val theExtractedMedication =
            Medication(
                id = Id("contained-12345-67890"),
                code = CodeableConcept(text = "medication".asFHIR()),
            )
        every {
            medicationExtractor.extractMedication(
                originalMedicationDynamicValue,
                listOf(containedMedication),
                medicationStatement,
            )
        } returns
            mockk {
                every { updatedMedication } returns updatedMedicationDynamicValue
                every { updatedContained } returns emptyList()
                every { extractedMedication } returns theExtractedMedication
            }

        val transformResponse = transformer.transform(medicationStatement, tenant)

        transformResponse!!
        assertEquals(listOf(theExtractedMedication), transformResponse.embeddedResources)

        val transformed = transformResponse.resource
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(
            listOf(
                Extension(
                    url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                    value =
                        DynamicValue(
                            type = DynamicValueType.CODE,
                            value = Code("contained reference"),
                        ),
                ),
            ),
            transformed.extension,
        )
        assertEquals(3, transformed.identifier.size)
        assertEquals(
            listOf(
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
        assertEquals(medicationStatement.status, transformed.status)
        assertEquals(updatedMedicationDynamicValue, transformed.medication)
        assertEquals(medicationStatement.subject, transformed.subject)
        assertEquals(medicationStatement.effective, transformed.effective)
    }

    @Test
    fun `transform can handle all dynamic types of effective`() {
        fun testMedication(
            type: DynamicValueType,
            value: Any,
        ) {
            val medicationStatement =
                MedicationStatement(
                    id = Id("12345"),
                    meta = Meta(source = Uri("source")),
                    status = com.projectronin.interop.fhir.r4.valueset.MedicationStatementStatus.ACTIVE.asCode(),
                    medication =
                        DynamicValue(
                            type = DynamicValueType.REFERENCE,
                            value = Reference(reference = FHIRString("Medication/1234")),
                        ),
                    subject =
                        Reference(
                            display = "subject".asFHIR(),
                            reference = "Patient/1234".asFHIR(),
                            type = Uri("Patient", extension = dataAuthorityExtension),
                        ),
                    effective =
                        DynamicValue(
                            type = type,
                            value = value,
                        ),
                )
            val transformResponse = transformer.transform(medicationStatement, tenant)
            assertEquals(medicationStatement.medication, transformResponse!!.resource.medication)
        }

        testMedication(DynamicValueType.DATE_TIME, DateTime("2022-10-14"))
        testMedication(DynamicValueType.PERIOD, Period(start = DateTime("2022-10-14")))
    }
}
