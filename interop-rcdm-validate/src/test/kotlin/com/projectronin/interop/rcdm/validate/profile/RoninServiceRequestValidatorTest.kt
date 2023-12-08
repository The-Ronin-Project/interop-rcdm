package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ServiceRequest
import com.projectronin.interop.fhir.r4.validate.resource.R4ServiceRequestValidator
import com.projectronin.interop.fhir.r4.valueset.RequestIntent
import com.projectronin.interop.fhir.r4.valueset.RequestStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.dataAuthorityExtension
import com.projectronin.interop.rcdm.validate.profile.util.requiredIdentifiers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninServiceRequestValidatorTest {
    private val validator = RoninServiceRequestValidator()

    @Test
    fun `returns supported resource`() {
        assertEquals(ServiceRequest::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4ServiceRequestValidator, validator.r4Validator)
    }

    @Test
    fun `returns profile`() {
        assertEquals(RoninProfile.SERVICE_REQUEST, validator.profile)
    }

    @Test
    fun `validate succeeds`() {
        val serviceRequest = ServiceRequest(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.SERVICE_REQUEST.canonical)),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CODE.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = Coding(code = Code("Code"), system = Uri("CodeSystem"))
                    )
                ),
                Extension(
                    url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CATEGORY.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = Coding(code = Code("Category"), system = Uri("CategorySystem"))
                    )
                )
            ),
            identifier = requiredIdentifiers,
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("CategorySystem"),
                            code = Code(value = "Category"),
                            display = "CategoryDisplay".asFHIR()
                        )
                    )
                )
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("CodeSystem"),
                        code = Code(value = "Code")
                    )
                )
            ),
            intent = RequestIntent.ORDER.asCode(),
            status = RequestStatus.ACTIVE.asCode(),
            subject = Reference(reference = "Patient/888".asFHIR())
        )
        val validation = validator.validate(serviceRequest, LocationContext(ServiceRequest::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validation succeeds with multiple tenant categories, but only one normalized category`() {
        // Update the complete service request to have a second category extension
        val serviceRequest = ServiceRequest(
            id = Id("1234"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.SERVICE_REQUEST.value)),
                source = Uri("source")
            ),
            identifier = requiredIdentifiers,
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CODE.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("Test"),
                                    code = Code(value = "1234")
                                )
                            )
                        )
                    )
                ),
                Extension(
                    url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CATEGORY.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("Category"),
                                    code = Code(value = "9876")
                                )
                            )
                        )
                    )
                ),
                Extension(
                    url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CATEGORY.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("Category"),
                                    code = Code(value = "9998")
                                )
                            )
                        )
                    )
                )
            ),
            intent = RequestIntent.ORDER.asCode(),
            status = RequestStatus.ACTIVE.asCode(),
            subject = Reference(
                id = "888".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension),
                reference = "Patient/888".asFHIR()
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("Test"),
                        code = Code(value = "1234")
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("Category"),
                            code = Code(value = "9876"),
                            display = "CategoryDisplay".asFHIR()
                        )
                    )
                )
            )
        )
        val validation = validator.validate(serviceRequest, LocationContext(ServiceRequest::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validation fails with multiple normalized categories`() {
        // Update the complete service request to have a second category
        val serviceRequest = ServiceRequest(
            id = Id("1234"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.SERVICE_REQUEST.value)),
                source = Uri("source")
            ),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CODE.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("Test"),
                                    code = Code(value = "1234")
                                )
                            )
                        )
                    )
                ),
                Extension(
                    url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CATEGORY.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("Category"),
                                    code = Code(value = "9876")
                                )
                            )
                        )
                    )
                )
            ),
            identifier = requiredIdentifiers,
            intent = RequestIntent.ORDER.asCode(),
            status = RequestStatus.ACTIVE.asCode(),
            subject = Reference(
                id = "888".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension),
                reference = "Patient/888".asFHIR()
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("Test"),
                        code = Code(value = "1234")
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("Category"),
                            code = Code(value = "9876"),
                            display = "CategoryDisplay".asFHIR()
                        )
                    )
                ),
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("Category"),
                            code = Code(value = "9998"),
                            display = "CategoryDisplay".asFHIR()
                        )
                    )
                )
            )
        )

        val validation = validator.validate(serviceRequest, LocationContext(ServiceRequest::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_SERVREQ_004: Service Request requires exactly 1 Category element @ ServiceRequest.category",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails when missing tenant code or category extensions`() {
        val serviceRequest = ServiceRequest(
            id = Id("1234"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.SERVICE_REQUEST.value)),
                source = Uri("source")
            ),
            extension = listOf(),
            identifier = requiredIdentifiers,
            intent = RequestIntent.ORDER.asCode(),
            status = RequestStatus.ACTIVE.asCode(),
            subject = Reference(
                id = "888".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension),
                reference = "Patient/888".asFHIR()
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("Test"),
                        code = Code(value = "1234")
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("Category"),
                            code = Code(value = "9876"),
                            display = "CategoryDisplay".asFHIR()
                        )
                    )
                )
            )
        )

        val validation = validator.validate(serviceRequest, LocationContext(ServiceRequest::class))
        assertEquals(3, validation.issues().size)
        assertEquals(
            "ERROR RONIN_SERVREQ_001: Service Request must have at least two extensions @ ServiceRequest.extension",
            validation.issues().first().toString()
        )
        assertEquals(
            "ERROR RONIN_SERVREQ_002: Service Request extension Tenant Source Service Request Category is invalid @ ServiceRequest.extension",
            validation.issues()[1].toString()
        )
        assertEquals(
            "ERROR RONIN_SERVREQ_003: Service Request extension Tenant Source Service Request Code is invalid @ ServiceRequest.extension",
            validation.issues()[2].toString()
        )
    }

    @Test
    fun `validation fails when subject is wrong type`() {
        val serviceRequest = ServiceRequest(
            id = Id("1234"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.SERVICE_REQUEST.value)),
                source = Uri("source")
            ),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CODE.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("Test"),
                                    code = Code(value = "1234")
                                )
                            )
                        )
                    )
                ),
                Extension(
                    url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CATEGORY.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("Category"),
                                    code = Code(value = "9876")
                                )
                            )
                        )
                    )
                )
            ),
            identifier = requiredIdentifiers,
            intent = RequestIntent.ORDER.asCode(),
            status = RequestStatus.ACTIVE.asCode(),
            subject = Reference(
                id = "888".asFHIR(),
                reference = "blah/888".asFHIR()
            ),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("Test"),
                        code = Code(value = "1234")
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("Category"),
                            code = Code(value = "9876"),
                            display = "CategoryDisplay".asFHIR()
                        )
                    )
                )
            )
        )

        val validation = validator.validate(serviceRequest, LocationContext(ServiceRequest::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR INV_REF_TYPE: reference can only be one of the following: Patient @ ServiceRequest.subject.reference",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails when subject is null`() {
        val serviceRequest = ServiceRequest(
            id = Id("1234"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.SERVICE_REQUEST.value)),
                source = Uri("source")
            ),
            extension = listOf(
                Extension(
                    url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CODE.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("Test"),
                                    code = Code(value = "1234")
                                )
                            )
                        )
                    )
                ),
                Extension(
                    url = RoninExtension.TENANT_SOURCE_SERVICE_REQUEST_CATEGORY.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("Category"),
                                    code = Code(value = "9876")
                                )
                            )
                        )
                    )
                )
            ),
            identifier = requiredIdentifiers,
            intent = RequestIntent.ORDER.asCode(),
            status = RequestStatus.ACTIVE.asCode(),
            subject = null,
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("Test"),
                        code = Code(value = "1234")
                    )
                )
            ),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("Category"),
                            code = Code(value = "9876"),
                            display = "CategoryDisplay".asFHIR()
                        )
                    )
                )
            )
        )
        val validation = validator.validate(serviceRequest, LocationContext(ServiceRequest::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: subject is a required element @ ServiceRequest.subject",
            validation.issues().first().toString()
        )
    }
}
