package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.validate.resource.R4PatientValidator
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.NameUse
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.dataAbsentReasonExtension
import com.projectronin.interop.rcdm.validate.profile.util.requiredIdentifiers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninPatientValidatorTest {
    private val validator = RoninPatientValidator()

    private val mrnIdentifier = Identifier(
        type = CodeableConcepts.RONIN_MRN,
        system = CodeSystem.RONIN_MRN.uri,
        value = FHIRString("mrn")
    )

    @Test
    fun `returns supported resource`() {
        assertEquals(Patient::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4PatientValidator, validator.r4Validator)
    }

    @Test
    fun `returns profile`() {
        assertEquals(RoninProfile.PATIENT, validator.profile)
    }

    @Test
    fun `validate fails if no MRN identifier`() {
        val patient = Patient(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            identifier = requiredIdentifiers,
            birthDate = Date("1987-05-19"),
            name = listOf(
                HumanName(
                    family = FHIRString("Doe"),
                    use = NameUse.OFFICIAL.asCode()
                )
            ),
            gender = AdministrativeGender.MALE.asCode()
        )

        val validation = validator.validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_PAT_001: MRN identifier is required @ Patient.identifier",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails if MRN identifier has wrong type`() {
        val patient = Patient(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            identifier = requiredIdentifiers +
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_MRN.uri,
                    value = FHIRString("mrn")
                ),
            birthDate = Date("1987-05-19"),
            name = listOf(
                HumanName(
                    family = FHIRString("Doe"),
                    use = NameUse.OFFICIAL.asCode()
                )
            ),
            gender = AdministrativeGender.MALE.asCode()
        )

        val validation = validator.validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_PAT_002: MRN identifier type defined without proper CodeableConcept @ Patient.identifier",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails if identifier has null system and no data absent reason`() {
        val patient = Patient(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            identifier = listOf(
                Identifier(
                    system = null,
                    value = FHIRString("value")
                )
            ) + requiredIdentifiers + mrnIdentifier,
            birthDate = Date("1987-05-19"),
            name = listOf(
                HumanName(
                    family = FHIRString("Doe"),
                    use = NameUse.OFFICIAL.asCode()
                )
            ),
            gender = AdministrativeGender.MALE.asCode()
        )

        val validation = validator.validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_PAT_006: Identifier system or data absent reason is required @ Patient.identifier[0].system",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails if identifier has system with null value and no data absent reason`() {
        val patient = Patient(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            identifier = listOf(
                Identifier(
                    system = Uri(null),
                    value = FHIRString("value")
                )
            ) + requiredIdentifiers + mrnIdentifier,
            birthDate = Date("1987-05-19"),
            name = listOf(
                HumanName(
                    family = FHIRString("Doe"),
                    use = NameUse.OFFICIAL.asCode()
                )
            ),
            gender = AdministrativeGender.MALE.asCode()
        )

        val validation = validator.validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_PAT_006: Identifier system or data absent reason is required @ Patient.identifier[0].system",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails if identifier has null value`() {
        val patient = Patient(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            identifier = listOf(
                Identifier(
                    system = CodeSystem.CONDITION_CATEGORY.uri,
                    value = null
                )
            ) + requiredIdentifiers + mrnIdentifier,
            birthDate = Date("1987-05-19"),
            name = listOf(
                HumanName(
                    family = FHIRString("Doe"),
                    use = NameUse.OFFICIAL.asCode()
                )
            ),
            gender = AdministrativeGender.MALE.asCode()
        )

        val validation = validator.validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: value is a required element @ Patient.identifier[0].value",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails if birthDate is null`() {
        val patient = Patient(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            identifier = requiredIdentifiers + mrnIdentifier,
            birthDate = null,
            name = listOf(
                HumanName(
                    family = FHIRString("Doe"),
                    use = NameUse.OFFICIAL.asCode()
                )
            ),
            gender = AdministrativeGender.MALE.asCode()
        )

        val validation = validator.validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: birthDate is a required element @ Patient.birthDate",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails if birthDate has value without 10 characters`() {
        val patient = Patient(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            identifier = requiredIdentifiers + mrnIdentifier,
            birthDate = Date("19870519"),
            name = listOf(
                HumanName(
                    family = FHIRString("Doe"),
                    use = NameUse.OFFICIAL.asCode()
                )
            ),
            gender = AdministrativeGender.MALE.asCode()
        )

        val validation = validator.validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_PAT_004: Birth date is invalid @ Patient.birthDate",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails if no names provided`() {
        val patient = Patient(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            identifier = requiredIdentifiers + mrnIdentifier,
            birthDate = Date("1987-05-19"),
            name = listOf(),
            gender = AdministrativeGender.MALE.asCode()
        )

        val validation = validator.validate(patient, LocationContext(Patient::class))
        assertEquals(2, validation.issues().size)
        assertEquals(
            "ERROR RONIN_PAT_007: At least one name must be provided @ Patient.name",
            validation.issues()[0].toString()
        )
        assertEquals(
            "ERROR RONIN_PAT_005: A name for official use must be present @ Patient.name",
            validation.issues()[1].toString()
        )
    }

    @Test
    fun `validate fails if no official name`() {
        val patient = Patient(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            identifier = requiredIdentifiers + mrnIdentifier,
            birthDate = Date("1987-05-19"),
            name = listOf(
                HumanName(
                    family = FHIRString("Doe"),
                    use = NameUse.NICKNAME.asCode()
                )
            ),
            gender = AdministrativeGender.MALE.asCode()
        )

        val validation = validator.validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_PAT_005: A name for official use must be present @ Patient.name",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails if name has null family, no given and no data absent reason`() {
        val patient = Patient(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            identifier = requiredIdentifiers + mrnIdentifier,
            birthDate = Date("1987-05-19"),
            name = listOf(
                HumanName(
                    family = null,
                    given = listOf(),
                    use = NameUse.OFFICIAL.asCode()
                )
            ),
            gender = AdministrativeGender.MALE.asCode()
        )

        val validation = validator.validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_PAT_008: Either Patient.name.given and/or Patient.name.family SHALL be present or a Data Absent Reason Extension SHALL be present @ Patient.name[0]",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails if name has family and data absent reason`() {
        val patient = Patient(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            identifier = requiredIdentifiers + mrnIdentifier,
            birthDate = Date("1987-05-19"),
            name = listOf(
                HumanName(
                    extension = dataAbsentReasonExtension,
                    family = FHIRString("Doe"),
                    use = NameUse.OFFICIAL.asCode()
                )
            ),
            gender = AdministrativeGender.MALE.asCode()
        )

        val validation = validator.validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_PAT_008: Either Patient.name.given and/or Patient.name.family SHALL be present or a Data Absent Reason Extension SHALL be present @ Patient.name[0]",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails if name has given and data absent reason`() {
        val patient = Patient(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            identifier = requiredIdentifiers + mrnIdentifier,
            birthDate = Date("1987-05-19"),
            name = listOf(
                HumanName(
                    extension = dataAbsentReasonExtension,
                    given = listOf(FHIRString("John")),
                    use = NameUse.OFFICIAL.asCode()
                )
            ),
            gender = AdministrativeGender.MALE.asCode()
        )

        val validation = validator.validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_PAT_008: Either Patient.name.given and/or Patient.name.family SHALL be present or a Data Absent Reason Extension SHALL be present @ Patient.name[0]",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails if gender is null`() {
        val patient = Patient(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            identifier = requiredIdentifiers + mrnIdentifier,
            birthDate = Date("1987-05-19"),
            name = listOf(
                HumanName(
                    family = FHIRString("Doe"),
                    use = NameUse.OFFICIAL.asCode()
                )
            ),
            gender = null
        )

        val validation = validator.validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: gender is a required element @ Patient.gender",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate succeeds`() {
        val patient = Patient(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            identifier = requiredIdentifiers + mrnIdentifier,
            birthDate = Date("1987-05-19"),
            name = listOf(
                HumanName(
                    family = FHIRString("Doe"),
                    use = NameUse.OFFICIAL.asCode()
                )
            ),
            gender = AdministrativeGender.MALE.asCode()
        )

        val validation = validator.validate(patient, LocationContext(Patient::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate succeeds with identifier with data absent reason system`() {
        val patient = Patient(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            identifier = requiredIdentifiers + mrnIdentifier +
                Identifier(
                    system = Uri(value = null, extension = dataAbsentReasonExtension),
                    value = FHIRString("value")
                ),
            birthDate = Date("1987-05-19"),
            name = listOf(
                HumanName(
                    family = FHIRString("Doe"),
                    given = listOf(FHIRString("John")),
                    use = NameUse.OFFICIAL.asCode()
                )
            ),
            gender = AdministrativeGender.MALE.asCode()
        )

        val validation = validator.validate(patient, LocationContext(Patient::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate succeeds with birthDate with null value`() {
        val patient = Patient(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            identifier = requiredIdentifiers + mrnIdentifier,
            birthDate = Date(null),
            name = listOf(
                HumanName(
                    family = FHIRString("Doe"),
                    use = NameUse.OFFICIAL.asCode()
                )
            ),
            gender = AdministrativeGender.MALE.asCode()
        )

        val validation = validator.validate(patient, LocationContext(Patient::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate succeeds with name with data absent reason`() {
        val patient = Patient(
            id = Id("1234"),
            meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            identifier = requiredIdentifiers + mrnIdentifier,
            birthDate = Date("1987-05-19"),
            name = listOf(
                HumanName(
                    use = NameUse.OFFICIAL.asCode(),
                    extension = dataAbsentReasonExtension
                )
            ),
            gender = AdministrativeGender.MALE.asCode()
        )

        val validation = validator.validate(patient, LocationContext(Patient::class))
        assertEquals(0, validation.issues().size)
    }
}
