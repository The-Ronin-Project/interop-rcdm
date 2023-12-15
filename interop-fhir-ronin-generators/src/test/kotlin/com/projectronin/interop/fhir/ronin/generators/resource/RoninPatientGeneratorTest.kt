package com.projectronin.interop.fhir.ronin.generators.resource

import com.projectronin.interop.fhir.generators.datatypes.IdentifierGenerator
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.test.data.generator.collection.ListDataGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class RoninPatientGeneratorTest : BaseGeneratorSpringTest() {
    private val goodRoninMrn =
        Identifier(
            type = CodeableConcepts.RONIN_MRN,
            system = CodeSystem.RONIN_MRN.uri,
            value = "An MRN".asFHIR(),
        )
    private val otherMrn =
        Identifier(
            system = Uri("testsystem"),
            value = "tomato".asFHIR(),
        )
    private val roninFhir =
        Identifier(
            system = CodeSystem.RONIN_FHIR_ID.uri,
            value = "fhirId".asFHIR(),
            type = CodeableConcepts.RONIN_FHIR_ID,
        )
    private val roninTenant =
        Identifier(
            system = CodeSystem.RONIN_TENANT.uri,
            value = "tenantId".asFHIR(),
            type = CodeableConcepts.RONIN_TENANT,
        )

    private val identifierList =
        listOf(
            Identifier(
                type =
                    CodeableConcept(
                        text = "MRN".asFHIR(),
                    ),
                system = Uri("mrnSystem"),
                value = "An MRN".asFHIR(),
            ),
        )

    @Test
    fun `generates valid basic RoninPatient`() {
        val roninPatient1 = rcdmPatient(TENANT_MNEMONIC) {}
        val validation = service.validate(roninPatient1, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
        assertNotNull(roninPatient1.meta)
        assertNotNull(roninPatient1.identifier)
        assertEquals(4, roninPatient1.identifier.size)
        assertNotNull(roninPatient1.name)
        assertNotNull(roninPatient1.telecom)
        assertNotNull(roninPatient1.id)
        val patientFHIRId = roninPatient1.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant = roninPatient1.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninPatient1.id?.value.toString())
        assertEquals(TENANT_MNEMONIC, tenant)
    }

    @Test
    fun `rcdmPatient with fhir id input - validate succeeds`() {
        val roninPatient1 =
            rcdmPatient(TENANT_MNEMONIC) {
                id of Id("99")
            }
        val validation = service.validate(roninPatient1, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
        assertEquals(4, roninPatient1.identifier.size)
        val values = roninPatient1.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 4)
        assertTrue(values.contains("99".asFHIR()))
        assertTrue(values.contains(TENANT_MNEMONIC.asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-99", roninPatient1.id?.value)
    }

    @Test
    fun `generates valid RoninPatient with existing correct MRN`() {
        val roninPatient1 =
            rcdmPatient(TENANT_MNEMONIC) {
                identifier of listOf(goodRoninMrn)
            }
        val validation = service.validate(roninPatient1, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
        val mrn = roninPatient1.identifier.find { it.system == CodeSystem.RONIN_MRN.uri }
        assertEquals("An MRN".asFHIR(), mrn!!.value)
    }

    @Test
    fun `generates valid RoninPatient with existing list of MRNs`() {
        val roninPatient1 =
            rcdmPatient(TENANT_MNEMONIC) {
                identifier of listOf(goodRoninMrn, otherMrn)
            }
        val validation = service.validate(roninPatient1, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
        assertEquals(5, roninPatient1.identifier.size)
        val mrn1 = roninPatient1.identifier.find { it.system == CodeSystem.RONIN_MRN.uri }
        assertEquals("An MRN".asFHIR(), mrn1!!.value)
        val mrn2 = roninPatient1.identifier.find { it.system == Uri("testsystem") }
        assertEquals("tomato".asFHIR(), mrn2!!.value)
    }

    @Test
    fun `generates valid RoninPatient with MRN and Ronin Tenant Id`() {
        val roninPatient1 =
            rcdmPatient(TENANT_MNEMONIC) {
                identifier of listOf(otherMrn, roninTenant)
            }
        val validation = service.validate(roninPatient1, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
        assertEquals(5, roninPatient1.identifier.size)
        val tenant = roninPatient1.identifier.find { it.system == CodeSystem.RONIN_TENANT.uri }
        assertEquals("tenantId".asFHIR(), tenant!!.value)
    }

    @Test
    fun `generates valid RoninPatient with MRN and Ronin Fhir Id`() {
        val roninPatient1 =
            rcdmPatient(TENANT_MNEMONIC) {
                identifier of listOf(otherMrn, roninFhir)
            }
        val validation = service.validate(roninPatient1, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
        assertEquals(5, roninPatient1.identifier.size)
        val fhir = roninPatient1.identifier.find { it.system == CodeSystem.RONIN_FHIR_ID.uri }
        assertEquals("fhirId".asFHIR(), fhir!!.value)
    }

    @Test
    fun `generates valid RoninPatient with other MRNs`() {
        val roninPatient1 =
            rcdmPatient(TENANT_MNEMONIC) {
                identifier of listOf(otherMrn)
            }
        val validation = service.validate(roninPatient1, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
        assertEquals(5, roninPatient1.identifier.size)
        val mrn = roninPatient1.identifier.find { it.system == Uri("testsystem") }
        assertEquals("tomato".asFHIR(), mrn!!.value)
    }

    @Test
    fun `generates valid RoninPatient with tenant and bad contact point drops contact point`() {
        val contactPoint =
            ContactPoint(
                value = "123-456-7890".asFHIR(),
            )
        val contactPoint2 =
            ContactPoint(
                value = "123-456-7890".asFHIR(),
                system = Code(ContactPointSystem.PHONE.code),
                use = Code(ContactPointUse.HOME.code),
            )
        val roninPatient1 =
            rcdmPatient(TENANT_MNEMONIC) {
                telecom of listOf(contactPoint, contactPoint2)
                gender of Code("female")
                maritalStatus of CodeableConcept(text = "single".asFHIR())
            }
        val validation = service.validate(roninPatient1, TENANT_MNEMONIC).succeeded

        assertTrue(validation)
        assertEquals(roninPatient1.gender!!.value, "female")
        assertEquals(roninPatient1.maritalStatus!!.text!!.value, "single")
        assertEquals(1, roninPatient1.telecom.size)
        assertEquals("123-456-7890", roninPatient1.telecom[0].value?.value)
    }

    @Test
    fun `generates valid RoninPatient with other names`() {
        val testName =
            HumanName(
                family = "family".asFHIR(),
                given = listOf("given".asFHIR()),
            )
        val roninPatient1 =
            rcdmPatient(TENANT_MNEMONIC) {
                name of listOf(testName)
            }
        val validation = service.validate(roninPatient1, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("official", roninPatient1.name[1].use?.value.toString())
    }

    @Test
    fun `MRN generator with partial MRN, will cause another MRN to be generated`() {
        val testMrn =
            Identifier(
                type = CodeableConcepts.RONIN_MRN,
                system = Uri("testsystem"),
                value = "An MRN".asFHIR(),
            )
        val testMrn2 =
            Identifier(
                type = CodeableConcept(text = "test".asFHIR()),
                system = CodeSystem.RONIN_MRN.uri,
                value = "An MRN".asFHIR(),
            )
        val mrnList = ListDataGenerator(0, IdentifierGenerator()).plus(testMrn).plus(testMrn2)
        val roninMrn = rcdmMrn(mrnList)
        assertEquals(1, roninMrn.size)
        assertEquals(CodeSystem.RONIN_MRN.uri, roninMrn[0].system)
        assertEquals(CodeableConcepts.RONIN_MRN, roninMrn[0].type)
    }

    @Test
    fun `getReferenceData succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) { id of "99" }
        val data = rcdmPatient.referenceData()
        assertEquals(TENANT_MNEMONIC, data.tenantId)
        assertEquals("test-99", data.udpId)
    }

    @Test
    fun `getReferenceData fails when no tenant`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val badPatient = rcdmPatient.copy(identifier = emptyList())
        val exception =
            assertThrows<IllegalArgumentException> {
                badPatient.referenceData()
            }
        assertEquals("Patient is missing some required data", exception.message)
    }

    @Test
    fun `getReferenceData fails when empty tenant`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val fhir = rcdmPatient.identifier.find { it.system == CodeSystem.RONIN_FHIR_ID.uri }!!
        val mrn = rcdmPatient.identifier.firstOrNull { it.system == CodeSystem.RONIN_MRN.uri }!!
        val tenant = rcdmPatient.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }!!
        val badTenant = tenant.copy(value = "".asFHIR())
        val badPatient = rcdmPatient.copy(identifier = emptyList<Identifier>() + fhir + mrn + badTenant)
        val exception =
            assertThrows<IllegalArgumentException> {
                badPatient.referenceData()
            }
        assertEquals("Patient is missing some required data", exception.message)
    }

    @Test
    fun `getReferenceData fails when null tenant value`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val fhir = rcdmPatient.identifier.find { it.system == CodeSystem.RONIN_FHIR_ID.uri }!!
        val mrn = rcdmPatient.identifier.firstOrNull { it.system == CodeSystem.RONIN_MRN.uri }!!
        val tenant = rcdmPatient.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }!!
        val badTenant = tenant.copy(value = null)
        val badPatient = rcdmPatient.copy(identifier = emptyList<Identifier>() + fhir + mrn + badTenant)
        val exception =
            assertThrows<IllegalArgumentException> {
                badPatient.referenceData()
            }
        assertEquals("Patient is missing some required data", exception.message)
    }

    @Test
    fun `getReferenceData fails when no id`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val badPatient = rcdmPatient.copy(id = null)
        val exception =
            assertThrows<IllegalArgumentException> {
                badPatient.referenceData()
            }
        assertEquals("Patient is missing some required data", exception.message)
    }

    @Test
    fun `getReferenceData fails when empty id`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val badPatient = rcdmPatient.copy(id = Id(""))
        val exception =
            assertThrows<IllegalArgumentException> {
                badPatient.referenceData()
            }
        assertEquals("Patient is missing some required data", exception.message)
    }

    @Test
    fun `getReferenceData fails when null id value`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val badPatient = rcdmPatient.copy(id = Id(value = null))
        val exception =
            assertThrows<IllegalArgumentException> {
                badPatient.referenceData()
            }
        assertEquals("Patient is missing some required data", exception.message)
    }
}
