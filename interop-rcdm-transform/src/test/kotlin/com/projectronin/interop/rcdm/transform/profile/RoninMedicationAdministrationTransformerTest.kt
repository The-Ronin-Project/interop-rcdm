package com.projectronin.interop.rcdm.transform.profile

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
import com.projectronin.interop.fhir.r4.datatype.SimpleQuantity
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.resource.MedicationAdministration
import com.projectronin.interop.fhir.r4.resource.MedicationAdministrationDosage
import com.projectronin.interop.fhir.r4.resource.MedicationAdministrationPerformer
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

class RoninMedicationAdministrationTransformerTest {
    private val medicationExtractor = mockk<MedicationExtractor> {
        every { extractMedication(any(), any(), any()) } returns null
    }
    private val transformer = RoninMedicationAdministrationTransformer(medicationExtractor)

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `returns supported resource`() {
        assertEquals(MedicationAdministration::class, transformer.supportedResource)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(transformer.qualifies(MedicationAdministration()))
    }

    @Test
    fun `transform succeeds with all required attributes`() {
        val medAdmin = MedicationAdministration(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://projectronin.io/fhir/StructureDefinition/ronin-medicationAdministration")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            extension = listOf(statusCodingExtension("in-progress")),
            language = Code("en-US"),
            text = Narrative(
                status = NarrativeStatus.GENERATED.asCode(),
                div = "div".asFHIR()
            ),
            status = Code("in-progress"),
            effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
            medication = DynamicValue(
                DynamicValueType.REFERENCE,
                value = Reference(
                    reference = "Medication/something".asFHIR(),
                    identifier = null,
                    type = Uri("Medication", extension = dataAuthorityExtension)
                )
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = dataAuthorityExtension
                )
            )
        )

        val transformResponse = transformer.transform(medAdmin, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            RoninProfile.MEDICATION_ADMINISTRATION.value,
            transformed.meta!!.profile[0].value
        )
        assertEquals(medAdmin.implicitRules, transformed.implicitRules)
        assertEquals(medAdmin.language, transformed.language)
        assertEquals(medAdmin.text, transformed.text)
        assertEquals(
            listOf(
                statusCodingExtension("in-progress"),
                Extension(
                    url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                    value = DynamicValue(
                        type = DynamicValueType.CODE,
                        value = Code("literal reference")
                    )
                )
            ),
            transformed.extension
        )
        assertEquals(3, transformed.identifier.size)
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
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
        assertEquals(medAdmin.instantiates, transformed.instantiates)
        assertEquals(medAdmin.partOf, transformed.partOf)
        assertEquals(medAdmin.status, transformed.status)
        assertEquals(medAdmin.statusReason, transformed.statusReason)
        assertEquals(medAdmin.category, transformed.category)
        assertEquals(medAdmin.medication, transformed.medication)
        assertEquals(medAdmin.subject, transformed.subject)
        assertEquals(medAdmin.context, transformed.context)
        assertEquals(medAdmin.supportingInformation, transformed.supportingInformation)
        assertEquals(medAdmin.effective, transformed.effective)
        assertEquals(medAdmin.performer, transformed.performer)
        assertEquals(medAdmin.note, transformed.note)
        assertEquals(medAdmin.dosage, transformed.dosage)
        assertEquals(medAdmin.eventHistory, transformed.eventHistory)
    }

    @Test
    fun `transform succeeds and returns correct extension with all required attributes`() {
        val medAdmin = MedicationAdministration(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://projectronin.io/fhir/StructureDefinition/ronin-medicationAdministration")),
                source = Uri("source")
            ),
            extension = listOf(
                statusCodingExtension("in-progress")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(
                status = NarrativeStatus.GENERATED.asCode(),
                div = "div".asFHIR()
            ),
            status = Code("in-progress"),
            effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
            medication = DynamicValue(
                DynamicValueType.REFERENCE,
                value = Reference(
                    reference = "Medication/something".asFHIR(),
                    identifier = Identifier(id = "12345678".asFHIR()),
                    type = Uri("Medication", extension = dataAuthorityExtension)
                )
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = dataAuthorityExtension
                )
            )
        )

        val transformResponse = transformer.transform(medAdmin, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            RoninProfile.MEDICATION_ADMINISTRATION.value,
            transformed.meta!!.profile[0].value
        )
        assertEquals(medAdmin.implicitRules, transformed.implicitRules)
        assertEquals(medAdmin.language, transformed.language)
        assertEquals(medAdmin.text, transformed.text)
        assertEquals(
            listOf(
                statusCodingExtension("in-progress"),
                Extension(
                    url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                    value = DynamicValue(
                        type = DynamicValueType.CODE,
                        value = Code("logical reference")
                    )
                )
            ),
            transformed.extension
        )
        assertEquals(3, transformed.identifier.size)
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
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
        assertEquals(medAdmin.instantiates, transformed.instantiates)
        assertEquals(medAdmin.partOf, transformed.partOf)
        assertEquals(medAdmin.status, transformed.status)
        assertEquals(medAdmin.statusReason, transformed.statusReason)
        assertEquals(medAdmin.category, transformed.category)
        assertEquals(medAdmin.medication, transformed.medication)
        assertEquals(medAdmin.subject, transformed.subject)
        assertEquals(medAdmin.context, transformed.context)
        assertEquals(medAdmin.supportingInformation, transformed.supportingInformation)
        assertEquals(medAdmin.effective, transformed.effective)
        assertEquals(medAdmin.performer, transformed.performer)
        assertEquals(medAdmin.note, transformed.note)
        assertEquals(medAdmin.dosage, transformed.dosage)
        assertEquals(medAdmin.eventHistory, transformed.eventHistory)
    }

    @Test
    fun `transform succeeds with all attributes`() {
        val medAdmin = MedicationAdministration(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://projectronin.io/fhir/StructureDefinition/ronin-medicationAdministration")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(
                status = NarrativeStatus.GENERATED.asCode(),
                div = "div".asFHIR()
            ),
            extension = listOf(statusCodingExtension("in-progress")),
            contained = listOf(Location(id = Id("67890"))),
            identifier = listOf(Identifier(value = "id".asFHIR())),
            instantiates = listOf(Uri("something-here")),
            partOf = listOf(Reference(display = "partOf".asFHIR())),
            status = Code("in-progress"),
            statusReason = listOf(CodeableConcept(text = "statusReason".asFHIR())),
            category = CodeableConcept(coding = listOf(Coding(code = Code("code"))), text = "category".asFHIR()),
            medication = DynamicValue(
                DynamicValueType.REFERENCE,
                value = Reference(
                    reference = "#something".asFHIR(),
                    identifier = null,
                    type = Uri("Medication", extension = dataAuthorityExtension)
                )
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = dataAuthorityExtension
                )
            ),
            context = Reference(reference = "Encounter/12345678".asFHIR(), display = "context".asFHIR()),
            supportingInformation = listOf(Reference(display = "supportingInformation".asFHIR())),
            effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
            performer = listOf(
                MedicationAdministrationPerformer(
                    actor = Reference(reference = "Patient".asFHIR()),
                    id = "12345678".asFHIR()
                )
            ),
            note = listOf(Annotation(text = Markdown("annotation"))),
            dosage = MedicationAdministrationDosage(
                rate = DynamicValue(
                    type = DynamicValueType.QUANTITY,
                    SimpleQuantity(value = Decimal(1))
                )
            ),
            eventHistory = listOf(Reference(display = "eventHistory".asFHIR()))
        )

        val transformResponse = transformer.transform(medAdmin, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            RoninProfile.MEDICATION_ADMINISTRATION.value,
            transformed.meta!!.profile[0].value
        )
        assertEquals(medAdmin.implicitRules, transformed.implicitRules)
        assertEquals(medAdmin.language, transformed.language)
        assertEquals(medAdmin.text, transformed.text)
        assertEquals(transformed.contained, listOf(Location(id = Id("67890"))))
        assertEquals(
            listOf(
                statusCodingExtension("in-progress"),
                Extension(
                    url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                    value = DynamicValue(
                        type = DynamicValueType.CODE,
                        value = Code("contained reference")
                    )
                )
            ),
            transformed.extension
        )
        assertEquals(4, transformed.identifier.size)
        assertEquals(
            listOf(
                Identifier(value = "id".asFHIR()),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
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
        assertEquals(transformed.instantiates, listOf(Uri("something-here")))
        assertEquals(transformed.partOf, listOf(Reference(display = "partOf".asFHIR())))
        assertEquals(transformed.status, Code("in-progress"))
        assertEquals(transformed.statusReason, listOf(CodeableConcept(text = "statusReason".asFHIR())))
        assertEquals(
            transformed.category,
            CodeableConcept(coding = listOf(Coding(code = Code("code"))), text = "category".asFHIR())
        )
        assertEquals(
            transformed.medication,
            DynamicValue(
                DynamicValueType.REFERENCE,
                value = Reference(
                    reference = "#something".asFHIR(),
                    type = Uri("Medication", extension = dataAuthorityExtension)
                )
            )
        )
        assertEquals(
            transformed.subject,
            Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = dataAuthorityExtension
                )
            )
        )
        assertEquals(
            transformed.context,
            Reference(reference = "Encounter/12345678".asFHIR(), display = "context".asFHIR())
        )
        assertEquals(
            transformed.supportingInformation,
            listOf(Reference(display = "supportingInformation".asFHIR()))
        )
        assertEquals(transformed.effective, DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"))
        assertEquals(
            transformed.performer,
            listOf(
                MedicationAdministrationPerformer(
                    actor = Reference(reference = "Patient".asFHIR()),
                    id = "12345678".asFHIR()
                )
            )
        )
        assertEquals(transformed.note, listOf(Annotation(text = Markdown("annotation"))))
        assertEquals(
            transformed.dosage,
            MedicationAdministrationDosage(
                rate = DynamicValue(
                    type = DynamicValueType.QUANTITY,
                    SimpleQuantity(value = Decimal(1))
                )
            )
        )
        assertEquals(transformed.eventHistory, listOf(Reference(display = "eventHistory".asFHIR())))
    }

    @Test
    fun `transform succeeds with extracted medications`() {
        val containedMedication = Medication(
            id = Id("67890"),
            code = CodeableConcept(text = "medication".asFHIR())
        )
        val originalMedicationDynamicValue =
            DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "#67890".asFHIR()))
        val medAdmin = MedicationAdministration(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://projectronin.io/fhir/StructureDefinition/ronin-medicationAdministration")),
                source = Uri("source")
            ),
            contained = listOf(containedMedication),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(
                status = NarrativeStatus.GENERATED.asCode(),
                div = "div".asFHIR()
            ),
            extension = listOf(statusCodingExtension("in-progress")),
            status = Code("in-progress"),
            effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
            medication = originalMedicationDynamicValue,
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = dataAuthorityExtension
                )
            )
        )

        val updatedMedicationDynamicValue = DynamicValue(
            DynamicValueType.REFERENCE,
            Reference(reference = "Medication/contained-12345-67890".asFHIR())
        )
        val theExtractedMedication = Medication(
            id = Id("contained-12345-67890"),
            code = CodeableConcept(text = "medication".asFHIR())
        )
        every {
            medicationExtractor.extractMedication(
                originalMedicationDynamicValue,
                listOf(containedMedication),
                any()
            )
        } returns mockk {
            every { updatedMedication } returns updatedMedicationDynamicValue
            every { updatedContained } returns emptyList()
            every { extractedMedication } returns theExtractedMedication
        }

        val transformResponse = transformer.transform(medAdmin, tenant)

        transformResponse!!
        assertEquals(listOf(theExtractedMedication), transformResponse.embeddedResources)

        val transformed = transformResponse.resource
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            RoninProfile.MEDICATION_ADMINISTRATION.value,
            transformed.meta!!.profile[0].value
        )
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(medAdmin.implicitRules, transformed.implicitRules)
        assertEquals(medAdmin.language, transformed.language)
        assertEquals(medAdmin.text, transformed.text)
        assertEquals(
            listOf(
                statusCodingExtension("in-progress"),
                Extension(
                    url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                    value = DynamicValue(
                        type = DynamicValueType.CODE,
                        value = Code("contained reference")
                    )
                )
            ),
            transformed.extension
        )
        assertEquals(3, transformed.identifier.size)
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
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
        assertEquals(medAdmin.instantiates, transformed.instantiates)
        assertEquals(medAdmin.partOf, transformed.partOf)
        assertEquals(medAdmin.status, transformed.status)
        assertEquals(medAdmin.statusReason, transformed.statusReason)
        assertEquals(medAdmin.category, transformed.category)
        assertEquals(updatedMedicationDynamicValue, transformed.medication)
        assertEquals(medAdmin.subject, transformed.subject)
        assertEquals(medAdmin.context, transformed.context)
        assertEquals(medAdmin.supportingInformation, transformed.supportingInformation)
        assertEquals(medAdmin.effective, transformed.effective)
        assertEquals(medAdmin.performer, transformed.performer)
        assertEquals(medAdmin.note, transformed.note)
        assertEquals(medAdmin.dosage, transformed.dosage)
        assertEquals(medAdmin.eventHistory, transformed.eventHistory)
    }

    private fun statusCoding(value: String) = Coding(
        system = Uri("http://projectronin.io/fhir/CodeSystem/test/MedicationAdministrationStatus"),
        code = Code(value = value)
    )

    private fun statusCodingExtension(value: String) = Extension(
        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceMedicationAdministrationStatus"),
        value = DynamicValue(
            type = DynamicValueType.CODING,
            value = Coding(
                system = Uri("http://projectronin.io/fhir/CodeSystem/test/MedicationAdministrationStatus"),
                code = Code(value = value)
            )
        )
    )
}
