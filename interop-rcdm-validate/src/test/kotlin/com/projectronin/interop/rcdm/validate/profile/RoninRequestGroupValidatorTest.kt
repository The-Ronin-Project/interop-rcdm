package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.RequestGroup
import com.projectronin.interop.fhir.r4.validate.resource.R4RequestGroupValidator
import com.projectronin.interop.fhir.r4.valueset.RequestGroupIntent
import com.projectronin.interop.fhir.r4.valueset.RequestGroupStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.dataAuthorityExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninRequestGroupValidatorTest {
    private val validator = RoninRequestGroupValidator()

    @Test
    fun `returns supported resource`() {
        assertEquals(RequestGroup::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4RequestGroupValidator, validator.r4Validator)
    }

    @Test
    fun `returns profile`() {
        assertEquals(RoninProfile.REQUEST_GROUP, validator.profile)
    }

    @Test
    fun `validation fails without subject`() {
        val requestGroup = RequestGroup(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.REQUEST_GROUP.value)), source = Uri("source")),
            identifier = listOf(
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
            status = RequestGroupStatus.DRAFT.asCode(),
            intent = RequestGroupIntent.OPTION.asCode()
        )

        val validation = validator.validate(requestGroup, LocationContext(RequestGroup::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: subject is a required element @ RequestGroup.subject",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate profile - succeeds`() {
        val requestGroup = RequestGroup(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.REQUEST_GROUP.value)), source = Uri("source")),
            identifier = listOf(
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
                    value = "Data Authority Identifier.asFHIR".asFHIR()
                )
            ),
            status = RequestGroupStatus.DRAFT.asCode(),
            intent = RequestGroupIntent.OPTION.asCode(),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            )
        )
        val validation = validator.validate(requestGroup, LocationContext(RequestGroup::class))
        assertEquals(0, validation.issues().size)
    }
}
