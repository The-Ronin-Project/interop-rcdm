package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ServiceRequest
import com.projectronin.interop.fhir.r4.valueset.RequestIntent
import com.projectronin.interop.fhir.r4.valueset.RequestStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.util.dataAuthorityExtension
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninServiceRequestTransformerTest {
    private val transformer = RoninServiceRequestTransformer()

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }

    @Test
    fun `returns supported resource`() {
        assertEquals(ServiceRequest::class, transformer.supportedResource)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(
            transformer.qualifies(
                ServiceRequest(
                    intent = RequestIntent.ORDER.asCode(),
                    status = RequestStatus.ACTIVE.asCode(),
                    subject =
                        Reference(
                            reference = "Patient/Patient#1".asFHIR(),
                            type = Uri("Patient", extension = dataAuthorityExtension),
                        ),
                ),
            ),
        )
    }

    @Test
    fun `transform succeeds`() {
        val serviceRequest =
            ServiceRequest(
                id = Id("ServiceRequest1"),
                meta =
                    Meta(
                        profile = listOf(Canonical("ServiceRequest")),
                        source = Uri("source"),
                    ),
                identifier = listOf(),
                intent = RequestIntent.ORDER.asCode(),
                status = RequestStatus.ACTIVE.asCode(),
                subject =
                    Reference(
                        reference = "Patient/Patient#1".asFHIR(),
                        type = Uri("Patient", extension = dataAuthorityExtension),
                    ),
                code =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("CodeSystem"),
                                    code = Code(value = "Code"),
                                ),
                            ),
                    ),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = Uri("CategorySystem"),
                                        code = Code(value = "CategoryCode"),
                                    ),
                                ),
                        ),
                    ),
            )

        val transformResponse = transformer.transform(serviceRequest, tenant)
        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)
        val transformed = transformResponse.resource
        assertEquals("ServiceRequest1", transformed.id?.value)
        assertEquals(
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            system = Uri("CodeSystem"),
                            code = Code(value = "Code"),
                        ),
                    ),
            ),
            transformed.code,
        )
        assertEquals(1, transformed.category.size)
        assertEquals(
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            system = Uri("CategorySystem"),
                            code = Code(value = "CategoryCode"),
                        ),
                    ),
            ),
            transformed.category.first(),
        )
    }
}
