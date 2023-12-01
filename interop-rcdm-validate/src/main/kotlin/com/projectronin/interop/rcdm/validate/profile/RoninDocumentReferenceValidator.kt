package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.primitive.Url
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceContext
import com.projectronin.interop.fhir.r4.validate.resource.R4DocumentReferenceValidator
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.RequiredFieldError
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

@Component
class RoninDocumentReferenceValidator : ProfileValidator<DocumentReference>() {
    override val supportedResource: KClass<DocumentReference> = DocumentReference::class
    override val r4Validator: R4ProfileValidator<DocumentReference> = R4DocumentReferenceValidator
    override val profile: RoninProfile = RoninProfile.DOCUMENT_REFERENCE
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_25_0
    override val profileVersion: Int = 5

    private val requiredSubjectError = RequiredFieldError(DocumentReference::subject)
    private val requiredTypeError = RequiredFieldError(DocumentReference::type)
    private val requiredUrlError = RequiredFieldError(Attachment::url)

    private val requiredDocumentReferenceTypeExtension = FHIRError(
        code = "RONIN_DOCREF_001",
        description = "Tenant source Document Reference extension is missing or invalid",
        severity = ValidationIssueSeverity.ERROR,
        location = LocationContext(DocumentReference::extension)
    )
    private val requiredCodingSize = FHIRError(
        code = "RONIN_DOCREF_002",
        severity = ValidationIssueSeverity.ERROR,
        description = "One, and only one, coding entry is allowed for type",
        location = LocationContext(CodeableConcept::coding)
    )
    private val requiredDatalakeAttachmentExtension = FHIRError(
        code = "RONIN_DOCREF_003",
        severity = ValidationIssueSeverity.ERROR,
        description = "Datalake Attachment URL extension is missing or invalid",
        location = LocationContext(Url::extension)
    )
    private val requiredEncounter = FHIRError(
        code = "RONIN_DOCREF_004",
        severity = ValidationIssueSeverity.ERROR,
        description = "No more than one encounter is allowed for this type",
        location = LocationContext(DocumentReferenceContext::encounter)
    )

    override fun validate(resource: DocumentReference, validation: Validation, context: LocationContext) {
        validation.apply {
            checkTrue(
                resource.extension.any {
                    it.url == RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.uri &&
                        it.value?.type == DynamicValueType.CODEABLE_CONCEPT
                },
                requiredDocumentReferenceTypeExtension,
                context
            )

            checkNotNull(resource.type, requiredTypeError, context)
            ifNotNull(resource.type) {
                checkTrue(
                    resource.type!!.coding.size == 1, // coding is required
                    requiredCodingSize,
                    context.append(LocationContext(DocumentReference::type))
                )
            }

            resource.content.forEachIndexed { index, content ->
                content.attachment?.let { attachment ->
                    val attachmentContext = context.append(LocationContext("", "content[$index].attachment"))
                    checkNotNull(attachment.url, requiredUrlError, attachmentContext)

                    attachment.url?.let { url ->
                        val urlContext = attachmentContext.append(LocationContext("", "url"))
                        checkTrue(
                            url.extension.any {
                                it.url == RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri &&
                                    it.value?.type == DynamicValueType.URL
                            },
                            requiredDatalakeAttachmentExtension,
                            urlContext
                        )
                    }
                }
            }

            checkNotNull(resource.subject, requiredSubjectError, context)
            validateReferenceType(
                resource.subject,
                listOf(ResourceType.Patient),
                LocationContext(DocumentReference::subject),
                validation
            )

            resource.context?.let { docRefContext ->
                val contextLocationContext = context.append(LocationContext(DocumentReference::context))

                checkTrue(docRefContext.encounter.size < 2, requiredEncounter, contextLocationContext)

                validateReferenceType(
                    docRefContext.encounter.firstOrNull(),
                    listOf(ResourceType.Encounter),
                    contextLocationContext.append(LocationContext("", "encounter[0]")),
                    validation
                )
            }

            // USCore adds an optional code (0.*) of Clinical Note, in the category attribute,
            // but the base binding is still an example binding and there should remain unconstrained.
        }
    }
}
