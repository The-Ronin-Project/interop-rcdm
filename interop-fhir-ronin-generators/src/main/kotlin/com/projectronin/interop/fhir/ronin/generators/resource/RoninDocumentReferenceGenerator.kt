package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.fhir.generators.datatypes.attachment
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.generators.resources.DocumentReferenceGenerator
import com.projectronin.interop.fhir.generators.resources.documentReference
import com.projectronin.interop.fhir.generators.resources.documentReferenceContent
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.Url
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceContent
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.generators.resource.observation.subjectReferenceOptions
import com.projectronin.interop.fhir.ronin.generators.util.generateCode
import com.projectronin.interop.fhir.ronin.generators.util.generateCodeableConcept
import com.projectronin.interop.fhir.ronin.generators.util.generateReference
import com.projectronin.interop.fhir.ronin.generators.util.generateRequiredCodeableConceptList
import com.projectronin.interop.fhir.ronin.generators.util.generateUdpId
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile

fun rcdmDocumentReference(
    tenant: String,
    binaryFhirId: String,
    block: DocumentReferenceGenerator.() -> Unit,
): DocumentReference {
    return documentReference {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.DOCUMENT_REFERENCE, tenant) {}
        extension of tenantDocumentReferenceTypeSourceExtension
        generateUdpId(id.generate(), tenant).let {
            id of it
            identifier of rcdmIdentifiers(tenant, identifier, it.value)
        }
        status of generateCode(status.generate(), possibleDocumentReferenceStatusCodes.random())
        type of generateCodeableConcept(type.generate(), possibleDocumentReferenceTypeCodes.random())
        category of
            generateRequiredCodeableConceptList(
                category.generate(),
                possibleDocumentReferenceCategoryCodes.random(),
            )
        subject of generateReference(subject.generate(), subjectReferenceOptions, tenant, "Patient")
        content of generateContent(content.generate(), binaryFhirId)
    }
}

fun Patient.rcdmDocumentReference(
    binaryFhirId: String,
    block: DocumentReferenceGenerator.() -> Unit,
): DocumentReference {
    val data = this.referenceData()
    return rcdmDocumentReference(data.tenantId, binaryFhirId) {
        block.invoke(this)
        subject of
            generateReference(
                subject.generate(),
                subjectReferenceOptions,
                data.tenantId,
                "Patient",
                data.udpId,
            )
    }
}

private fun generateContent(
    generatedContent: List<DocumentReferenceContent>,
    binaryFhirId: String,
): List<DocumentReferenceContent> {
    val contentList = generatedContent.ifEmpty { listOf(documentReferenceContent { }) }

    return contentList.map { content ->
        val baseAttachment = content.attachment ?: attachment { }

        val currentUrl = baseAttachment.url
        val url =
            currentUrl?.copy(extension = currentUrl.extension + datalakeAttachmentUrlExtension)
                ?: Url("Binary/$binaryFhirId", extension = listOf(datalakeAttachmentUrlExtension))

        content.copy(
            attachment = baseAttachment.copy(url = url),
        )
    }
}

private val datalakeAttachmentUrlExtension =
    Extension(
        url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
        value = DynamicValue(DynamicValueType.URL, Url("datalake/attachment/id")),
    )

private val tenantDocumentReferenceTypeSourceExtension =
    listOf(
        Extension(
            url = Uri(RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.value),
            value =
                DynamicValue(
                    DynamicValueType.CODEABLE_CONCEPT,
                    CodeableConcept(
                        text = "Tenant Note".asFHIR(),
                        coding =
                            listOf(
                                Coding(
                                    system = CodeSystem.LOINC.uri,
                                    display = "Invalid Note".asFHIR(),
                                    code = Code("invalid-note"),
                                ),
                            ),
                    ),
                ),
        ),
    )

val possibleDocumentReferenceStatusCodes =
    listOf(
        Code("current"),
        Code("superseded"),
        Code("entered-in-error"),
    )

val possibleDocumentReferenceCategoryCodes =
    listOf(
        // per RCDM: The US Core DocumentReferences Type Value Set is a starter set
        // of categories supported for fetching and storing clinical notes.
        coding {
            system of CodeSystem.DOCUMENT_REFERENCE_CATEGORY.uri
            code of Code("clinical-note")
            display of "Clinical Note"
        },
    )
val docRefLoincCodes =
    listOf(
        Pair(
            "100029-8",
            "Cancer related multigene analysis in Plasma cell-free DNA by Molecular genetics method",
        ),
        Pair(
            "100213-8",
            "Prostate cancer multigene analysis in Blood or Tissue by Molecular genetics method",
        ),
        Pair(
            "100215-3",
            "Episode of care medical records Document Transplant surgery",
        ),
        Pair(
            "100217-9",
            "Surgical oncology synoptic report",
        ),
        Pair(
            "100455-5",
            "Clinical pathology Outpatient Progress note",
        ),
        Pair(
            "100468-8",
            "Gynecologic oncology Outpatient Progress note",
        ),
        Pair(
            "100474-6",
            "Hematology+Medical oncology Outpatient Progress note",
        ),
        Pair(
            "100496-9",
            "Oncology Outpatient Progress note",
        ),
        Pair(
            "100525-5",
            "Radiation oncology Outpatient Progress note",
        ),
        Pair(
            "100526-3",
            "Radiology Outpatient Progress note",
        ),
        Pair(
            "100553-7",
            "Blood banking and transfusion medicine Hospital Progress note",
        ),
        Pair(
            "100563-6",
            "Clinical pathology Hospital Progress note",
        ),
        Pair(
            "100604-8",
            "Oncology Hospital Progress note",
        ),
        Pair(
            "100631-1",
            "Radiation oncology Hospital Progress note",
        ),
        Pair(
            "100719-4",
            "Surgical oncology Discharge summary",
        ),
        Pair(
            "101136-0",
            "Radiation oncology End of treatment letter",
        ),
        Pair("11486-8", "Chemotherapy records"), Pair("18776-5", "Plan of care note"),
    )
        .map {
            coding {
                system of CodeSystem.LOINC.uri
                code of it.first
                display of it.second
            }
        }

val possibleDocumentReferenceTypeCodes =
    listOf(
        // per RCDM: All LOINC values whose SCALE is DOC in the LOINC database
        // and the HL7 v3 Code System NullFlavor concept 'unknown'. Use "UNK" plus
        // a short extract from USCore featuring keywords "cancer", "oncology", etc.
        coding {
            system of CodeSystem.NULL_FLAVOR.uri
            code of Code("UNK")
            display of "Unknown"
        },
    ) + docRefLoincCodes

val authorReferenceOptions =
    listOf(
        "Patient",
        "Practitioner",
        "PractitionerRole",
        "Organization",
    )

val authenticatorReferenceOptions =
    listOf(
        "Practitioner",
        "Organization",
        "PractitionerRole",
    )

val custodianReferenceOptions =
    listOf(
        "Organization",
    )
