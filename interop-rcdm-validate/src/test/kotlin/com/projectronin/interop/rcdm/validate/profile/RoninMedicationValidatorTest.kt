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
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationValidator
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninMedicationValidatorTest {
    private val validator = RoninMedicationValidator()

    // re-used codes to make the tests cleaner
    private val vitaminDCode = Code("11253")
    private val medicationCodingList = listOf(
        Coding(system = CodeSystem.RXNORM.uri, code = vitaminDCode, display = "Vitamin D".asFHIR())
    )
    private val tenantSourceCodeExtensionB = listOf(
        Extension(
            url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_CODE.value),
            value = DynamicValue(
                DynamicValueType.CODEABLE_CONCEPT,
                CodeableConcept(
                    text = "b".asFHIR(),
                    coding = medicationCodingList
                )
            )
        )
    )

    @Test
    fun `returns supported resource`() {
        assertEquals(Medication::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4MedicationValidator, validator.r4Validator)
    }

    @Test
    fun `returns profile`() {
        assertEquals(RoninProfile.MEDICATION, validator.profile)
    }

    @Test
    fun `validate succeeds`() {
        val medication = Medication(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
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
            extension = tenantSourceCodeExtensionB,
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = medicationCodingList
            )
        )

        val validation = validator.validate(medication, LocationContext(Medication::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate succeeds -  with any code value`() {
        // except for the test case details,
        // all attributes are correct

        val medication = Medication(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
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
            extension = tenantSourceCodeExtensionB,
            code = CodeableConcept(
                coding = listOf(
                    Coding(
                        system = CodeSystem.RXNORM.uri,
                        code = Code("b"),
                        version = "1.0.0".asFHIR(),
                        display = "b".asFHIR()
                    )
                ),
                text = "test".asFHIR()
            )
        )

        val validation = validator.validate(medication, LocationContext(Medication::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate fails - if missing identifiers`() {
        val medication = Medication(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
            extension = tenantSourceCodeExtensionB,
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = medicationCodingList
            )
        )

        val validation = validator.validate(medication, LocationContext(Medication::class))
        assertEquals(3, validation.issues().size)
        assertEquals(
            "[ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Medication.identifier, " +
                "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Medication.identifier, " +
                "ERROR RONIN_DAUTH_ID_001: Data Authority identifier is required @ Medication.identifier]",
            validation.issues().toString()
        )
    }

    @Test
    fun `validate fails - if missing required code attribute`() {
        val medication = Medication(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
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
            extension = tenantSourceCodeExtensionB
        )

        val validation = validator.validate(medication, LocationContext(Medication::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "[ERROR REQ_FIELD: code is a required element @ Medication.code]",
            validation.issues().toString()
        )
    }

    @Test
    fun `validate fails - if missing required source code extension`() {
        val medication = Medication(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
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
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = medicationCodingList
            )
        )

        val validation = validator.validate(medication, LocationContext(Medication::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "[ERROR RONIN_MED_001: Tenant source medication code extension is missing or invalid @ Medication.extension]",
            validation.issues().toString()
        )
    }

    @Test
    fun `validate fails - if source code extension has wrong url`() {
        val medication = Medication(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
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
            extension = listOf(
                Extension(
                    url = Uri(RoninExtension.TENANT_SOURCE_CONDITION_CODE.value),
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            text = "b".asFHIR(),
                            coding = medicationCodingList
                        )
                    )
                )
            ),
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = medicationCodingList
            )
        )

        val validation = validator.validate(medication, LocationContext(Medication::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "[ERROR RONIN_MED_001: Tenant source medication code extension is missing or invalid @ Medication.extension]",
            validation.issues().toString()
        )
    }

    @Test
    fun `validate fails - if source code extension has wrong datatype`() {
        val medication = Medication(
            id = Id("123"),
            meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION.value)), source = Uri("source")),
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
            extension = listOf(
                Extension(
                    url = Uri(RoninExtension.TENANT_SOURCE_MEDICATION_CODE.value),
                    value = DynamicValue(
                        DynamicValueType.CODING,
                        medicationCodingList.first()
                    )
                )
            ),
            code = CodeableConcept(
                text = "b".asFHIR(),
                coding = medicationCodingList
            )
        )

        val validation = validator.validate(medication, LocationContext(Medication::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "[ERROR RONIN_MED_001: Tenant source medication code extension is missing or invalid @ Medication.extension]",
            validation.issues().toString()
        )
    }
}
