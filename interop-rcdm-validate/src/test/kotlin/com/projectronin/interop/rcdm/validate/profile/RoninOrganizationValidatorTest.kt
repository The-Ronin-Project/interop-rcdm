package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Organization
import com.projectronin.interop.fhir.r4.validate.resource.R4OrganizationValidator
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.validation
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninOrganizationValidatorTest {
    private val validator = RoninOrganizationValidator()

    @Test
    fun `returns supported resource`() {
        assertEquals(Organization::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4OrganizationValidator, validator.r4Validator)
    }

    @Test
    fun `returns profile`() {
        assertEquals(RoninProfile.ORGANIZATION, validator.profile)
    }

    @Test
    fun `validate is successful`() {
        val organization =
            Organization(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.ORGANIZATION.value)), source = Uri("source")),
                identifier =
                    listOf(
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                name = "Organization name".asFHIR(),
                active = true.asFHIR(),
            )

        val validation = validator.validate(organization, LocationContext(Organization::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate fails with no organization name provided`() {
        val organization =
            Organization(
                meta = Meta(profile = listOf(Canonical(RoninProfile.ORGANIZATION.value)), source = Uri("source")),
                id = Id("12345"),
                identifier =
                    listOf(
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                active = true.asFHIR(),
            )

        val validation = validator.validate(organization, LocationContext(Organization::class))
        assertEquals(1, validation.issues().size)

        assertEquals(
            "ERROR REQ_FIELD: name is a required element @ Organization.name",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails with no organization active provided`() {
        val organization =
            Organization(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.ORGANIZATION.value)), source = Uri("source")),
                identifier =
                    listOf(
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                name = "Organization name".asFHIR(),
            )

        val validation = validator.validate(organization, LocationContext(Organization::class))
        assertEquals(1, validation.issues().size)

        assertEquals(
            "ERROR REQ_FIELD: active is a required element @ Organization.active",
            validation.issues().first().toString(),
        )
    }
}
