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
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.UnsignedInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.DispenseRequest
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.resource.Substitution
import com.projectronin.interop.fhir.r4.valueset.MedicationRequestIntent
import com.projectronin.interop.fhir.r4.valueset.MedicationRequestStatus
import com.projectronin.interop.fhir.r4.valueset.RequestPriority
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.dataAuthorityExtension
import com.projectronin.interop.rcdm.transform.extractor.MedicationExtractor
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninMedicationRequestTransformerTest {
    private val medicationExtractor = mockk<MedicationExtractor> {
        every { extractMedication(any(), any(), any()) } returns null
    }
    private val transformer = RoninMedicationRequestTransformer(medicationExtractor)

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `returns supported resource`() {
        assertEquals(MedicationRequest::class, transformer.supportedResource)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(
            transformer.qualifies(
                MedicationRequest(
                    status = MedicationRequestStatus.COMPLETED.asCode(),
                    intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
                    medication = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(text = "medication".asFHIR())
                    ),
                    subject = Reference(reference = "Patient/1234".asFHIR()),
                    requester = Reference(reference = "Practitioner/1234".asFHIR())
                )
            )
        )
    }

    @Test
    fun `transforms medication request with only required attributes`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            status = MedicationRequestStatus.CANCELLED.asCode(),
            intent = MedicationRequestIntent.PROPOSAL.asCode(),
            medication = DynamicValue(
                type = DynamicValueType.REFERENCE,
                value = Reference(reference = FHIRString("Medication/1234"))
            ),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            requester = Reference(reference = "Practitioner/1234".asFHIR())
        )

        val transformResponse = transformer.transform(medicationRequest, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("MedicationRequest", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(
            listOf(
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
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
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
        assertEquals(MedicationRequestStatus.CANCELLED.asCode(), transformed.status)
        assertNull(transformed.statusReason)
        assertEquals(MedicationRequestIntent.PROPOSAL.asCode(), transformed.intent)
        assertEquals(listOf<CodeableConcept>(), transformed.category)
        assertNull(transformed.priority)
        assertNull(transformed.doNotPerform)
        assertNull(transformed.reported)
        assertEquals(
            DynamicValue(
                type = DynamicValueType.REFERENCE,
                value = Reference(reference = FHIRString("Medication/1234"))
            ),
            transformed.medication
        )
        assertEquals(
            Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            transformed.subject
        )
        assertNull(transformed.encounter)
        assertEquals(listOf<Reference>(), transformed.supportingInformation)
        assertNull(transformed.authoredOn)
        assertEquals(Reference(reference = "Practitioner/1234".asFHIR()), transformed.requester)
        assertNull(transformed.performer)
        assertNull(transformed.performerType)
        assertNull(transformed.recorder)
        assertEquals(listOf<CodeableConcept>(), transformed.reasonCode)
        assertEquals(listOf<Reference>(), transformed.reasonReference)
        assertEquals(listOf<Canonical>(), transformed.instantiatesCanonical)
        assertEquals(listOf<Uri>(), transformed.instantiatesUri)
        assertEquals(listOf<Reference>(), transformed.basedOn)
        assertNull(transformed.groupIdentifier)
        assertNull(transformed.courseOfTherapyType)
        assertEquals(listOf<Reference>(), transformed.insurance)
        assertEquals(listOf<Annotation>(), transformed.note)
        assertEquals(listOf<Dosage>(), transformed.dosageInstruction)
        assertNull(transformed.dispenseRequest)
        assertNull(transformed.substitution)
        assertNull(transformed.priorPrescription)
        assertEquals(listOf<Reference>(), transformed.detectedIssue)
        assertEquals(listOf<Reference>(), transformed.eventHistory)
    }

    @Test
    fun `transforms medication request with all attributes`() {
        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("MedicationRequest")),
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
                    value = DynamicValue(DynamicValueType.STRING, "value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = listOf(Identifier(value = "id".asFHIR())),
            status = MedicationRequestStatus.CANCELLED.asCode(),
            statusReason = CodeableConcept(text = "statusReason".asFHIR()),
            intent = MedicationRequestIntent.PROPOSAL.asCode(),
            category = listOf(CodeableConcept(text = "category".asFHIR())),
            priority = RequestPriority.ASAP.asCode(),
            doNotPerform = FHIRBoolean.FALSE,
            reported = DynamicValue(DynamicValueType.BOOLEAN, true),
            medication = DynamicValue(
                type = DynamicValueType.REFERENCE,
                value = Reference(reference = FHIRString("Medication/1234"))
            ),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            encounter = Reference(reference = "Encounter/1234".asFHIR()),
            supportingInformation = listOf(Reference(reference = "Condition/1234".asFHIR())),
            authoredOn = DateTime("2022-11-03"),
            requester = Reference(reference = "Practitioner/1234".asFHIR()),
            performer = Reference(reference = "Practitioner/5678".asFHIR()),
            performerType = CodeableConcept(text = "performer type".asFHIR()),
            recorder = Reference(reference = "Practitioner/3456".asFHIR()),
            reasonCode = listOf(CodeableConcept(text = "reason".asFHIR())),
            reasonReference = listOf(Reference(reference = "Condition/5678".asFHIR())),
            instantiatesCanonical = listOf(Canonical("canonical")),
            instantiatesUri = listOf(Uri("uri")),
            basedOn = listOf(Reference(reference = "CarePlan/1234".asFHIR())),
            groupIdentifier = Identifier(value = "group".asFHIR()),
            courseOfTherapyType = CodeableConcept(text = "therapy".asFHIR()),
            insurance = listOf(Reference(reference = "Coverage/1234".asFHIR())),
            note = listOf(Annotation(text = Markdown("note"))),
            dosageInstruction = listOf(Dosage(text = "dosage".asFHIR())),
            dispenseRequest = DispenseRequest(numberOfRepeatsAllowed = UnsignedInt(2)),
            substitution = Substitution(allowed = DynamicValue(DynamicValueType.BOOLEAN, true)),
            priorPrescription = Reference(reference = "MedicationRequest/1234".asFHIR()),
            detectedIssue = listOf(Reference(reference = "DetectedIssue/1234".asFHIR())),
            eventHistory = listOf(Reference(reference = "Provenance/1234".asFHIR()))
        )

        val transformResponse = transformer.transform(medicationRequest, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("MedicationRequest", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = com.projectronin.interop.fhir.r4.valueset.NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()), transformed.text)
        assertEquals(
            listOf(Location(id = Id("67890"))),
            transformed.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
                ),
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
        assertEquals(MedicationRequestStatus.CANCELLED.asCode(), transformed.status)
        assertEquals(CodeableConcept(text = "statusReason".asFHIR()), transformed.statusReason)
        assertEquals(MedicationRequestIntent.PROPOSAL.asCode(), transformed.intent)
        assertEquals(listOf(CodeableConcept(text = "category".asFHIR())), transformed.category)
        assertEquals(RequestPriority.ASAP.asCode(), transformed.priority)
        assertEquals(FHIRBoolean.FALSE, transformed.doNotPerform)
        assertEquals(DynamicValue(DynamicValueType.BOOLEAN, true), transformed.reported)
        assertEquals(
            DynamicValue(
                type = DynamicValueType.REFERENCE,
                value = Reference(reference = FHIRString("Medication/1234"))
            ),
            transformed.medication
        )
        assertEquals(
            Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            transformed.subject
        )
        assertEquals(Reference(reference = "Encounter/1234".asFHIR()), transformed.encounter)
        assertEquals(listOf(Reference(reference = "Condition/1234".asFHIR())), transformed.supportingInformation)
        assertEquals(DateTime("2022-11-03"), transformed.authoredOn)
        assertEquals(Reference(reference = "Practitioner/1234".asFHIR()), transformed.requester)
        assertEquals(Reference(reference = "Practitioner/5678".asFHIR()), transformed.performer)
        assertEquals(CodeableConcept(text = "performer type".asFHIR()), transformed.performerType)
        assertEquals(Reference(reference = "Practitioner/3456".asFHIR()), transformed.recorder)
        assertEquals(listOf(CodeableConcept(text = "reason".asFHIR())), transformed.reasonCode)
        assertEquals(listOf(Reference(reference = "Condition/5678".asFHIR())), transformed.reasonReference)
        assertEquals(listOf(Canonical("canonical")), transformed.instantiatesCanonical)
        assertEquals(listOf(Uri("uri")), transformed.instantiatesUri)
        assertEquals(listOf(Reference(reference = "CarePlan/1234".asFHIR())), transformed.basedOn)
        assertEquals(Identifier(value = "group".asFHIR()), transformed.groupIdentifier)
        assertEquals(CodeableConcept(text = "therapy".asFHIR()), transformed.courseOfTherapyType)
        assertEquals(listOf(Reference(reference = "Coverage/1234".asFHIR())), transformed.insurance)
        assertEquals(listOf(Annotation(text = Markdown("note"))), transformed.note)
        assertEquals(listOf(Dosage(text = "dosage".asFHIR())), transformed.dosageInstruction)
        assertEquals(DispenseRequest(numberOfRepeatsAllowed = UnsignedInt(2)), transformed.dispenseRequest)
        assertEquals(Substitution(allowed = DynamicValue(DynamicValueType.BOOLEAN, true)), transformed.substitution)
        assertEquals(Reference(reference = "MedicationRequest/1234".asFHIR()), transformed.priorPrescription)
        assertEquals(listOf(Reference(reference = "DetectedIssue/1234".asFHIR())), transformed.detectedIssue)
        assertEquals(listOf(Reference(reference = "Provenance/1234".asFHIR())), transformed.eventHistory)
    }

    @Test
    fun `transforms medication request with extracted medications`() {
        val containedMedication = Medication(
            id = Id("67890"),
            code = CodeableConcept(text = "medication".asFHIR())
        )
        val originalMedicationDynamicValue =
            DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "#67890".asFHIR()))

        val medicationRequest = MedicationRequest(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            contained = listOf(containedMedication),
            status = MedicationRequestStatus.CANCELLED.asCode(),
            intent = MedicationRequestIntent.PROPOSAL.asCode(),
            medication = originalMedicationDynamicValue,
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            requester = Reference(reference = "Practitioner/1234".asFHIR())
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
                medicationRequest
            )
        } returns mockk {
            every { updatedMedication } returns updatedMedicationDynamicValue
            every { updatedContained } returns emptyList()
            every { extractedMedication } returns theExtractedMedication
        }

        val transformResponse = transformer.transform(medicationRequest, tenant)

        transformResponse!!
        assertEquals(listOf(theExtractedMedication), transformResponse.embeddedResources)

        val transformed = transformResponse.resource
        assertEquals("MedicationRequest", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
            transformed.meta
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(
            listOf(
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
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
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
        assertEquals(MedicationRequestStatus.CANCELLED.asCode(), transformed.status)
        assertNull(transformed.statusReason)
        assertEquals(MedicationRequestIntent.PROPOSAL.asCode(), transformed.intent)
        assertEquals(listOf<CodeableConcept>(), transformed.category)
        assertNull(transformed.priority)
        assertNull(transformed.doNotPerform)
        assertNull(transformed.reported)
        assertEquals(updatedMedicationDynamicValue, transformed.medication)
        assertEquals(
            Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            transformed.subject
        )
        assertNull(transformed.encounter)
        assertEquals(listOf<Reference>(), transformed.supportingInformation)
        assertNull(transformed.authoredOn)
        assertEquals(Reference(reference = "Practitioner/1234".asFHIR()), transformed.requester)
        assertNull(transformed.performer)
        assertNull(transformed.performerType)
        assertNull(transformed.recorder)
        assertEquals(listOf<CodeableConcept>(), transformed.reasonCode)
        assertEquals(listOf<Reference>(), transformed.reasonReference)
        assertEquals(listOf<Canonical>(), transformed.instantiatesCanonical)
        assertEquals(listOf<Uri>(), transformed.instantiatesUri)
        assertEquals(listOf<Reference>(), transformed.basedOn)
        assertNull(transformed.groupIdentifier)
        assertNull(transformed.courseOfTherapyType)
        assertEquals(listOf<Reference>(), transformed.insurance)
        assertEquals(listOf<Annotation>(), transformed.note)
        assertEquals(listOf<Dosage>(), transformed.dosageInstruction)
        assertNull(transformed.dispenseRequest)
        assertNull(transformed.substitution)
        assertNull(transformed.priorPrescription)
        assertEquals(listOf<Reference>(), transformed.detectedIssue)
        assertEquals(listOf<Reference>(), transformed.eventHistory)
    }
}
