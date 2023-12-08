package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Procedure
import com.projectronin.interop.fhir.r4.valueset.EventStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninProcedureTransformerTest {
    private val transformer = RoninProcedureTransformer()

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `returns supported resource`() {
        assertEquals(Procedure::class, transformer.supportedResource)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(
            transformer.qualifies(
                Procedure(
                    status = Code(EventStatus.UNKNOWN.code),
                    subject = Reference(reference = "Patient".asFHIR())
                )
            )
        )
    }

    @Test
    fun `transform succeeds`() {
        val procedure = Procedure(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/Procedure.html")),
                source = Uri("source")
            ),
            extension = listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
                )
            ),
            identifier = listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "6789".asFHIR()
                )
            ),
            status = EventStatus.ON_HOLD.asCode(),
            subject = Reference(reference = FHIRString("Patient/123123")),
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("CodeSystem"),
                        code = Code(value = "Code")
                    )
                )
            ),
            category = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("CategorySystem"),
                        code = Code(value = "CategoryCode")
                    )
                )
            )
        )

        val transformResponse = transformer.transform(procedure, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(Id("12345"), transformed.id)
        assertEquals(4, transformed.identifier.size)
        assertEquals(
            Meta(
                source = Uri("source"),
                profile = listOf(Canonical(RoninProfile.PROCEDURE.value))
            ),
            transformed.meta
        )
        assertEquals(procedure.extension, transformed.extension)
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_MRN,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = "6789".asFHIR()
                ),
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
        assertEquals(procedure.subject, transformed.subject)
        assertEquals(procedure.status, transformed.status)
        assertEquals(
            CodeableConcept(
                coding = listOf(
                    Coding(
                        system = Uri("CodeSystem"),
                        code = Code(value = "Code")
                    )
                )
            ),
            transformed.code
        )
    }
}
