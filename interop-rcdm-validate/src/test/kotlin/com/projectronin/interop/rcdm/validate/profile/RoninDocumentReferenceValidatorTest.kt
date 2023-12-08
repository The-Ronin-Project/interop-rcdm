package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.datatype.Attachment
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.Url
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceContent
import com.projectronin.interop.fhir.r4.resource.DocumentReferenceContext
import com.projectronin.interop.fhir.r4.validate.resource.R4DocumentReferenceValidator
import com.projectronin.interop.fhir.r4.valueset.DocumentReferenceStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.validate.profile.util.requiredIdentifiers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Suppress("ktlint:standard:max-line-length")
class RoninDocumentReferenceValidatorTest {
    private val validator = RoninDocumentReferenceValidator()

    @Test
    fun `returns supported resource`() {
        assertEquals(DocumentReference::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4DocumentReferenceValidator, validator.r4Validator)
    }

    @Test
    fun `returns profile`() {
        assertEquals(RoninProfile.DOCUMENT_REFERENCE, validator.profile)
    }

    @Test
    fun `validate fails if no type extension`() {
        val type =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://loinc.org"), code = Code("34806-0")),
                    ),
            )

        val documentReference =
            DocumentReference(
                id = Id("test-1234"),
                meta = Meta(profile = listOf(RoninProfile.DOCUMENT_REFERENCE.canonical)),
                identifier = requiredIdentifiers,
                status = DocumentReferenceStatus.CURRENT.asCode(),
                type = type,
                content =
                    listOf(
                        DocumentReferenceContent(
                            attachment =
                                Attachment(
                                    url =
                                        Url(
                                            "Binary/test-12345",
                                            extension =
                                                listOf(
                                                    Extension(
                                                        url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                                        value = DynamicValue(DynamicValueType.URL, Url("Binary/12345")),
                                                    ),
                                                ),
                                        ),
                                ),
                        ),
                    ),
                subject = Reference(reference = "Patient/test-1234".asFHIR()),
            )

        val validation = validator.validate(documentReference, LocationContext(DocumentReference::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_DOCREF_001: Tenant source Document Reference extension is missing or invalid @ DocumentReference.extension",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if no type`() {
        val type =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://loinc.org"), code = Code("34806-0")),
                    ),
            )

        val documentReference =
            DocumentReference(
                id = Id("test-1234"),
                meta = Meta(profile = listOf(RoninProfile.DOCUMENT_REFERENCE.canonical)),
                identifier = requiredIdentifiers,
                status = DocumentReferenceStatus.CURRENT.asCode(),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, type),
                        ),
                    ),
                type = null,
                content =
                    listOf(
                        DocumentReferenceContent(
                            attachment =
                                Attachment(
                                    url =
                                        Url(
                                            "Binary/test-12345",
                                            extension =
                                                listOf(
                                                    Extension(
                                                        url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                                        value = DynamicValue(DynamicValueType.URL, Url("Binary/12345")),
                                                    ),
                                                ),
                                        ),
                                ),
                        ),
                    ),
                subject = Reference(reference = "Patient/test-1234".asFHIR()),
            )

        val validation = validator.validate(documentReference, LocationContext(DocumentReference::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: type is a required element @ DocumentReference.type",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if type has no codings`() {
        val type =
            CodeableConcept(
                coding = listOf(),
            )

        val documentReference =
            DocumentReference(
                id = Id("test-1234"),
                meta = Meta(profile = listOf(RoninProfile.DOCUMENT_REFERENCE.canonical)),
                identifier = requiredIdentifiers,
                status = DocumentReferenceStatus.CURRENT.asCode(),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, type),
                        ),
                    ),
                type = type,
                content =
                    listOf(
                        DocumentReferenceContent(
                            attachment =
                                Attachment(
                                    url =
                                        Url(
                                            "Binary/test-12345",
                                            extension =
                                                listOf(
                                                    Extension(
                                                        url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                                        value = DynamicValue(DynamicValueType.URL, Url("Binary/12345")),
                                                    ),
                                                ),
                                        ),
                                ),
                        ),
                    ),
                subject = Reference(reference = "Patient/test-1234".asFHIR()),
            )

        val validation = validator.validate(documentReference, LocationContext(DocumentReference::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_DOCREF_002: One, and only one, coding entry is allowed for type @ DocumentReference.type.coding",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if type has multiple codings`() {
        val type =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://loinc.org"), code = Code("34806-0")),
                        Coding(system = Uri("http://loinc.org"), code = Code("34806-1")),
                    ),
            )

        val documentReference =
            DocumentReference(
                id = Id("test-1234"),
                meta = Meta(profile = listOf(RoninProfile.DOCUMENT_REFERENCE.canonical)),
                identifier = requiredIdentifiers,
                status = DocumentReferenceStatus.CURRENT.asCode(),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, type),
                        ),
                    ),
                type = type,
                content =
                    listOf(
                        DocumentReferenceContent(
                            attachment =
                                Attachment(
                                    url =
                                        Url(
                                            "Binary/test-12345",
                                            extension =
                                                listOf(
                                                    Extension(
                                                        url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                                        value = DynamicValue(DynamicValueType.URL, Url("Binary/12345")),
                                                    ),
                                                ),
                                        ),
                                ),
                        ),
                    ),
                subject = Reference(reference = "Patient/test-1234".asFHIR()),
            )

        val validation = validator.validate(documentReference, LocationContext(DocumentReference::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_DOCREF_002: One, and only one, coding entry is allowed for type @ DocumentReference.type.coding",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if attachment has no url`() {
        val type =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://loinc.org"), code = Code("34806-0")),
                    ),
            )

        val documentReference =
            DocumentReference(
                id = Id("test-1234"),
                meta = Meta(profile = listOf(RoninProfile.DOCUMENT_REFERENCE.canonical)),
                identifier = requiredIdentifiers,
                status = DocumentReferenceStatus.CURRENT.asCode(),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, type),
                        ),
                    ),
                type = type,
                content =
                    listOf(
                        DocumentReferenceContent(
                            attachment =
                                Attachment(
                                    url = null,
                                ),
                        ),
                    ),
                subject = Reference(reference = "Patient/test-1234".asFHIR()),
            )

        val validation = validator.validate(documentReference, LocationContext(DocumentReference::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: url is a required element @ DocumentReference.content[0].attachment.url",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if attachment url has no datalake extension`() {
        val type =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://loinc.org"), code = Code("34806-0")),
                    ),
            )

        val documentReference =
            DocumentReference(
                id = Id("test-1234"),
                meta = Meta(profile = listOf(RoninProfile.DOCUMENT_REFERENCE.canonical)),
                identifier = requiredIdentifiers,
                status = DocumentReferenceStatus.CURRENT.asCode(),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, type),
                        ),
                    ),
                type = type,
                content =
                    listOf(
                        DocumentReferenceContent(
                            attachment =
                                Attachment(
                                    url = Url("Binary/test-12345"),
                                ),
                        ),
                    ),
                subject = Reference(reference = "Patient/test-1234".asFHIR()),
            )

        val validation = validator.validate(documentReference, LocationContext(DocumentReference::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_DOCREF_003: Datalake Attachment URL extension is missing or invalid @ DocumentReference.content[0].attachment.url.extension",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if no subject`() {
        val type =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://loinc.org"), code = Code("34806-0")),
                    ),
            )

        val documentReference =
            DocumentReference(
                id = Id("test-1234"),
                meta = Meta(profile = listOf(RoninProfile.DOCUMENT_REFERENCE.canonical)),
                identifier = requiredIdentifiers,
                status = DocumentReferenceStatus.CURRENT.asCode(),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, type),
                        ),
                    ),
                type = type,
                content =
                    listOf(
                        DocumentReferenceContent(
                            attachment =
                                Attachment(
                                    url =
                                        Url(
                                            "Binary/test-12345",
                                            extension =
                                                listOf(
                                                    Extension(
                                                        url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                                        value = DynamicValue(DynamicValueType.URL, Url("Binary/12345")),
                                                    ),
                                                ),
                                        ),
                                ),
                        ),
                    ),
            )

        val validation = validator.validate(documentReference, LocationContext(DocumentReference::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: subject is a required element @ DocumentReference.subject",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if subject is not a Patient`() {
        val type =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://loinc.org"), code = Code("34806-0")),
                    ),
            )

        val documentReference =
            DocumentReference(
                id = Id("test-1234"),
                meta = Meta(profile = listOf(RoninProfile.DOCUMENT_REFERENCE.canonical)),
                identifier = requiredIdentifiers,
                status = DocumentReferenceStatus.CURRENT.asCode(),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, type),
                        ),
                    ),
                type = type,
                content =
                    listOf(
                        DocumentReferenceContent(
                            attachment =
                                Attachment(
                                    url =
                                        Url(
                                            "Binary/test-12345",
                                            extension =
                                                listOf(
                                                    Extension(
                                                        url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                                        value = DynamicValue(DynamicValueType.URL, Url("Binary/12345")),
                                                    ),
                                                ),
                                        ),
                                ),
                        ),
                    ),
                subject = Reference(reference = "Location/test-1234".asFHIR()),
            )

        val validation = validator.validate(documentReference, LocationContext(DocumentReference::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR INV_REF_TYPE: reference can only be one of the following: Patient @ DocumentReference.subject.reference",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if context has multiple encounters`() {
        val type =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://loinc.org"), code = Code("34806-0")),
                    ),
            )

        val documentReference =
            DocumentReference(
                id = Id("test-1234"),
                meta = Meta(profile = listOf(RoninProfile.DOCUMENT_REFERENCE.canonical)),
                identifier = requiredIdentifiers,
                status = DocumentReferenceStatus.CURRENT.asCode(),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, type),
                        ),
                    ),
                type = type,
                content =
                    listOf(
                        DocumentReferenceContent(
                            attachment =
                                Attachment(
                                    url =
                                        Url(
                                            "Binary/test-12345",
                                            extension =
                                                listOf(
                                                    Extension(
                                                        url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                                        value = DynamicValue(DynamicValueType.URL, Url("Binary/12345")),
                                                    ),
                                                ),
                                        ),
                                ),
                        ),
                    ),
                subject = Reference(reference = "Patient/test-1234".asFHIR()),
                context =
                    DocumentReferenceContext(
                        encounter =
                            listOf(
                                Reference(reference = "Encounter/test-1234".asFHIR()),
                                Reference(reference = "Encounter/test-5678".asFHIR()),
                            ),
                    ),
            )

        val validation = validator.validate(documentReference, LocationContext(DocumentReference::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_DOCREF_004: No more than one encounter is allowed for this type @ DocumentReference.context.encounter",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if context encounter is not an Encounter`() {
        val type =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://loinc.org"), code = Code("34806-0")),
                    ),
            )

        val documentReference =
            DocumentReference(
                id = Id("test-1234"),
                meta = Meta(profile = listOf(RoninProfile.DOCUMENT_REFERENCE.canonical)),
                identifier = requiredIdentifiers,
                status = DocumentReferenceStatus.CURRENT.asCode(),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, type),
                        ),
                    ),
                type = type,
                content =
                    listOf(
                        DocumentReferenceContent(
                            attachment =
                                Attachment(
                                    url =
                                        Url(
                                            "Binary/test-12345",
                                            extension =
                                                listOf(
                                                    Extension(
                                                        url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                                        value = DynamicValue(DynamicValueType.URL, Url("Binary/12345")),
                                                    ),
                                                ),
                                        ),
                                ),
                        ),
                    ),
                subject = Reference(reference = "Patient/test-1234".asFHIR()),
                context =
                    DocumentReferenceContext(
                        encounter =
                            listOf(
                                Reference(reference = "Location/test-1234".asFHIR()),
                            ),
                    ),
            )

        val validation = validator.validate(documentReference, LocationContext(DocumentReference::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR INV_REF_TYPE: reference can only be one of the following: Encounter @ DocumentReference.context.encounter[0].reference",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate succeeds`() {
        val type =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://loinc.org"), code = Code("34806-0")),
                    ),
            )

        val documentReference =
            DocumentReference(
                id = Id("test-1234"),
                meta = Meta(profile = listOf(RoninProfile.DOCUMENT_REFERENCE.canonical)),
                identifier = requiredIdentifiers,
                status = DocumentReferenceStatus.CURRENT.asCode(),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, type),
                        ),
                    ),
                type = type,
                content =
                    listOf(
                        DocumentReferenceContent(
                            attachment =
                                Attachment(
                                    url =
                                        Url(
                                            "Binary/test-12345",
                                            extension =
                                                listOf(
                                                    Extension(
                                                        url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                                        value = DynamicValue(DynamicValueType.URL, Url("Binary/12345")),
                                                    ),
                                                ),
                                        ),
                                ),
                        ),
                    ),
                subject = Reference(reference = "Patient/test-1234".asFHIR()),
            )

        val validation = validator.validate(documentReference, LocationContext(DocumentReference::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate succeeds with context`() {
        val type =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://loinc.org"), code = Code("34806-0")),
                    ),
            )

        val documentReference =
            DocumentReference(
                id = Id("test-1234"),
                meta = Meta(profile = listOf(RoninProfile.DOCUMENT_REFERENCE.canonical)),
                identifier = requiredIdentifiers,
                status = DocumentReferenceStatus.CURRENT.asCode(),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_DOCUMENT_REFERENCE_TYPE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, type),
                        ),
                    ),
                type = type,
                content =
                    listOf(
                        DocumentReferenceContent(
                            attachment =
                                Attachment(
                                    url =
                                        Url(
                                            "Binary/test-12345",
                                            extension =
                                                listOf(
                                                    Extension(
                                                        url = RoninExtension.DATALAKE_DOCUMENT_REFERENCE_ATTACHMENT_URL.uri,
                                                        value = DynamicValue(DynamicValueType.URL, Url("Binary/12345")),
                                                    ),
                                                ),
                                        ),
                                ),
                        ),
                    ),
                subject = Reference(reference = "Patient/test-1234".asFHIR()),
                context =
                    DocumentReferenceContext(
                        encounter =
                            listOf(
                                Reference(reference = "Encounter/test-1234".asFHIR()),
                            ),
                    ),
            )

        val validation = validator.validate(documentReference, LocationContext(DocumentReference::class))
        assertEquals(0, validation.issues().size)
    }
}
