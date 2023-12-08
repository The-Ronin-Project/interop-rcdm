package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.fhir.r4.validate.resource.R4PractitionerValidator
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninPractitionerValidatorTest {
    private val validator = RoninPractitionerValidator()

    @Test
    fun `returns supported resource`() {
        assertEquals(Practitioner::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4PractitionerValidator, validator.r4Validator)
    }

    @Test
    fun `returns profile`() {
        assertEquals(RoninProfile.PRACTITIONER, validator.profile)
    }

    @Test
    fun `validate fails if no name`() {
        val practitioner =
            Practitioner(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER.value)), source = Uri("source")),
                identifier =
                    listOf(
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                name = listOf(),
            )

        val validation = validator.validate(practitioner, LocationContext(Practitioner::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: name is a required element @ Practitioner.name",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if no family name`() {
        val practitioner =
            Practitioner(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER.value)), source = Uri("source")),
                identifier =
                    listOf(
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                name = listOf(HumanName(given = listOf("George".asFHIR()))),
            )

        val validation = validator.validate(practitioner, LocationContext(Practitioner::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: family is a required element @ Practitioner.name[0].family",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails for multiple names with no family name`() {
        val practitioner =
            Practitioner(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER.value)), source = Uri("source")),
                identifier =
                    listOf(
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                name =
                    listOf(
                        HumanName(given = listOf("George").asFHIR()),
                        HumanName(family = "Smith".asFHIR()),
                        HumanName(given = listOf("John").asFHIR()),
                    ),
            )

        val validation = validator.validate(practitioner, LocationContext(Practitioner::class))
        assertEquals(2, validation.issues().size)
        assertEquals(
            "[ERROR REQ_FIELD: family is a required element @ Practitioner.name[0].family, " +
                "ERROR REQ_FIELD: family is a required element @ Practitioner.name[2].family]",
            validation.issues().toString(),
        )
    }

    @Test
    fun `validate succeeds`() {
        val practitioner =
            Practitioner(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER.value)), source = Uri("source")),
                identifier =
                    listOf(
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = "test".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = "12345".asFHIR(),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                            value = "EHR Data Authority".asFHIR(),
                        ),
                    ),
                name = listOf(HumanName(family = "Doe".asFHIR())),
            )

        val validation = validator.validate(practitioner, LocationContext(Practitioner::class))
        assertEquals(0, validation.issues().size)
    }
}
