package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.IdentifierGenerator
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.TENANT_MNEMONIC
import com.projectronin.test.data.generator.collection.ListDataGenerator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class RoninIdentifiersUtilTest {
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

    private val dataAuthorityIdentifier =
        Identifier(
            value = "EHR Data Authority".asFHIR(),
            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
        )

    @Test
    fun `generates ronin Identifiers if no ronin identifiers`() {
        val testList = ListDataGenerator(3, IdentifierGenerator())
        val identifiers = rcdmIdentifiers(TENANT_MNEMONIC, testList)
        assertNotNull(identifiers)
        assertEquals(6, identifiers.size)
        assertEquals(CodeSystem.RONIN_FHIR_ID.uri, identifiers[3].system)
        assertEquals(CodeSystem.RONIN_TENANT.uri, identifiers[4].system)
        assertEquals(CodeSystem.RONIN_DATA_AUTHORITY.uri, identifiers[5].system)
    }

    @Test
    fun `generates ronin tenant ID if no tenant ID`() {
        val testList = ListDataGenerator(0, IdentifierGenerator()).plus(roninFhir).plus(dataAuthorityIdentifier)
        val identifiers = rcdmIdentifiers(TENANT_MNEMONIC, testList)
        assertNotNull(identifiers)
        assertEquals(3, identifiers.size)
        assertEquals(CodeSystem.RONIN_TENANT.uri, identifiers.last().system)
    }

    @Test
    fun `generates data authority ID if no data authority ID`() {
        val testList = ListDataGenerator(0, IdentifierGenerator()).plus(roninTenant).plus(roninFhir)
        val identifiers = rcdmIdentifiers(TENANT_MNEMONIC, testList)
        assertNotNull(identifiers)
        assertEquals(3, identifiers.size)
        assertEquals(CodeSystem.RONIN_DATA_AUTHORITY.uri, identifiers.last().system)
    }

    @Test
    fun `generates ronin FHIR ID if no FHIR ID`() {
        val testList = ListDataGenerator(1, IdentifierGenerator()).plus(roninTenant).plus(dataAuthorityIdentifier)
        val identifiers = rcdmIdentifiers(TENANT_MNEMONIC, testList)
        assertNotNull(identifiers)
        assertEquals(4, identifiers.size)
        assertEquals(CodeSystem.RONIN_FHIR_ID.uri, identifiers.last().system)
    }

    @Test
    fun `generates no IDs if all present`() {
        val testList = ListDataGenerator(0, IdentifierGenerator()).plus(roninTenant).plus(dataAuthorityIdentifier).plus(roninFhir)
        val identifiers = rcdmIdentifiers(TENANT_MNEMONIC, testList)
        assertEquals(3, identifiers.size)
    }

    @Test
    fun `generates ronin FHIR ID if empty FHIR ID`() {
        val testList = ListDataGenerator(0, IdentifierGenerator()).plus(roninTenant).plus(dataAuthorityIdentifier)
        val identifiers = rcdmIdentifiers(TENANT_MNEMONIC, testList, "")
        assertNotNull(identifiers)
        assertEquals(3, identifiers.size)
        assertEquals(CodeSystem.RONIN_FHIR_ID.uri, identifiers.last().system)
    }

    @Test
    fun `generates ronin FHIR ID if null FHIR ID`() {
        val testList = ListDataGenerator(1, IdentifierGenerator()).plus(roninTenant).plus(dataAuthorityIdentifier)
        val identifiers = rcdmIdentifiers(TENANT_MNEMONIC, testList, null)
        assertNotNull(identifiers)
        assertEquals(4, identifiers.size)
        assertEquals(CodeSystem.RONIN_FHIR_ID.uri, identifiers.last().system)
    }
}
