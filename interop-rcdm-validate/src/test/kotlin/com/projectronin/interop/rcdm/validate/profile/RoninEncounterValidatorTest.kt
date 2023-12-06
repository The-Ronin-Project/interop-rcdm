package com.projectronin.interop.rcdm.validate.profile

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
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.fhir.r4.validate.resource.R4EncounterValidator
import com.projectronin.interop.fhir.r4.valueset.EncounterStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.validate.profile.util.requiredIdentifiers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninEncounterValidatorTest {
    private val validator = RoninEncounterValidator()

    @Test
    fun `returns supported resource`() {
        assertEquals(Encounter::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4EncounterValidator, validator.r4Validator)
    }

    @Test
    fun `returns profile`() {
        assertEquals(RoninProfile.ENCOUNTER, validator.profile)
    }

    @Test
    fun `validate succeeds`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceEncounterClass"),
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v3-ActCode"),
                            code = Code("AMB"),
                            display = "ambulatory".asFHIR()
                        )
                    )
                )
            ),
            identifier = requiredIdentifiers,
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = listOf(
                        Extension(
                            url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/ronin-dataAuthorityIdentifier"),
                            value = DynamicValue(
                                type = DynamicValueType.IDENTIFIER,
                                Identifier(
                                    value = "EHR Data Authority".asFHIR(),
                                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID
                                )
                            )
                        )
                    )
                )
            )
        )

        val validation = validator.validate(encounter, LocationContext(Encounter::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate fails with bad extension uri`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = Uri("bad.url"),
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v3-ActCode"),
                            code = Code("AMB"),
                            display = "ambulatory".asFHIR()
                        )
                    )
                )
            ),
            identifier = requiredIdentifiers,
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = listOf(
                        Extension(
                            url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/ronin-dataAuthorityIdentifier"),
                            value = DynamicValue(
                                type = DynamicValueType.IDENTIFIER,
                                Identifier(
                                    value = "EHR Data Authority".asFHIR(),
                                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID
                                )
                            )
                        )
                    )
                )
            )
        )

        val validation = validator.validate(encounter, LocationContext(Encounter::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_ENC_001: Tenant source encounter class extension is missing or invalid @ Encounter.extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails with bad extension value`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceEncounterClass"),
                    value = DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference("Garbo".asFHIR())
                    )
                )
            ),
            identifier = requiredIdentifiers,
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = listOf(
                        Extension(
                            url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/ronin-dataAuthorityIdentifier"),
                            value = DynamicValue(
                                type = DynamicValueType.IDENTIFIER,
                                Identifier(
                                    value = "EHR Data Authority".asFHIR(),
                                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID
                                )
                            )
                        )
                    )
                )
            )
        )

        val validation = validator.validate(encounter, LocationContext(Encounter::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_ENC_001: Tenant source encounter class extension is missing or invalid @ Encounter.extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails with missing extension value`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceEncounterClass"),
                    value = null
                )
            ),
            identifier = requiredIdentifiers,
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = listOf(
                        Extension(
                            url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/ronin-dataAuthorityIdentifier"),
                            value = DynamicValue(
                                type = DynamicValueType.IDENTIFIER,
                                Identifier(
                                    value = "EHR Data Authority".asFHIR(),
                                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID
                                )
                            )
                        )
                    )
                )
            )
        )

        val validation = validator.validate(encounter, LocationContext(Encounter::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_ENC_001: Tenant source encounter class extension is missing or invalid @ Encounter.extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails with missing extension uri`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = null,
                    value = null
                )
            ),
            identifier = requiredIdentifiers,
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = listOf(
                        Extension(
                            url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/ronin-dataAuthorityIdentifier"),
                            value = DynamicValue(
                                type = DynamicValueType.IDENTIFIER,
                                Identifier(
                                    value = "EHR Data Authority".asFHIR(),
                                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID
                                )
                            )
                        )
                    )
                )
            )
        )

        val validation = validator.validate(encounter, LocationContext(Encounter::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_ENC_001: Tenant source encounter class extension is missing or invalid @ Encounter.extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails with missing extension`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            identifier = requiredIdentifiers,
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = listOf(
                        Extension(
                            url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/ronin-dataAuthorityIdentifier"),
                            value = DynamicValue(
                                type = DynamicValueType.IDENTIFIER,
                                Identifier(
                                    value = "EHR Data Authority".asFHIR(),
                                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID
                                )
                            )
                        )
                    )
                )
            )
        )

        val validation = validator.validate(encounter, LocationContext(Encounter::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_ENC_001: Tenant source encounter class extension is missing or invalid @ Encounter.extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails with missing type`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceEncounterClass"),
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v3-ActCode"),
                            code = Code("AMB"),
                            display = "ambulatory".asFHIR()
                        )
                    )
                )
            ),
            identifier = requiredIdentifiers,
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = listOf(
                        Extension(
                            url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/ronin-dataAuthorityIdentifier"),
                            value = DynamicValue(
                                type = DynamicValueType.IDENTIFIER,
                                Identifier(
                                    value = "EHR Data Authority".asFHIR(),
                                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID
                                )
                            )
                        )
                    )
                )
            )
        )

        val validation = validator.validate(encounter, LocationContext(Encounter::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: type is a required element @ Encounter.type",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails with missing subject`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceEncounterClass"),
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v3-ActCode"),
                            code = Code("AMB"),
                            display = "ambulatory".asFHIR()
                        )
                    )
                )
            ),
            identifier = requiredIdentifiers,
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code")))))
        )

        val validation = validator.validate(encounter, LocationContext(Encounter::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: subject is a required element @ Encounter.subject",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails with missing identifiers`() {
        val encounter = Encounter(
            id = Id("12345"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.ENCOUNTER.value)), source = Uri("source")),
            extension = listOf(
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceEncounterClass"),
                    value = DynamicValue(
                        type = DynamicValueType.CODING,
                        value = Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/v3-ActCode"),
                            code = Code("AMB"),
                            display = "ambulatory".asFHIR()
                        )
                    )
                )
            ),
            status = EncounterStatus.CANCELLED.asCode(),
            `class` = Coding(code = Code("OBSENC")),
            type = listOf(CodeableConcept(coding = listOf(Coding(code = Code("code"))))),
            subject = Reference(
                reference = "Patient/1234".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = listOf(
                        Extension(
                            url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/ronin-dataAuthorityIdentifier"),
                            value = DynamicValue(
                                type = DynamicValueType.IDENTIFIER,
                                Identifier(
                                    value = "EHR Data Authority".asFHIR(),
                                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID
                                )
                            )
                        )
                    )
                )
            )
        )

        val validation = validator.validate(encounter, LocationContext(Encounter::class))
        assertEquals(3, validation.issues().size)
        assertEquals(
            "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Encounter.identifier",
            validation.issues().first().toString()
        )
        assertEquals(
            "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Encounter.identifier",
            validation.issues()[1].toString()
        )
        assertEquals(
            "ERROR RONIN_DAUTH_ID_001: Data Authority identifier is required @ Encounter.identifier",
            validation.issues()[2].toString()
        )
    }
}
