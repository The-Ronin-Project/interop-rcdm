package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.MedicationStatement
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationStatementValidator
import com.projectronin.interop.fhir.r4.valueset.MedicationStatementStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.dataAuthorityExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninMedicationStatementValidatorTest {
    private val validator = RoninMedicationStatementValidator()

    @Test
    fun `returns supported resource`() {
        assertEquals(MedicationStatement::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4MedicationStatementValidator, validator.r4Validator)
    }

    @Test
    fun `returns profile`() {
        assertEquals(RoninProfile.MEDICATION_STATEMENT, validator.profile)
    }

    @Test
    fun `validate succeeds`() {
        val medicationStatement =
            MedicationStatement(
                id = Id("123"),
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.MEDICATION_STATEMENT.value)),
                        source = Uri("source"),
                    ),
                extension =
                    listOf(
                        Extension(
                            url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code("literal reference"),
                                ),
                        ),
                    ),
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
                status = MedicationStatementStatus.ACTIVE.asCode(),
                medication =
                    DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = FHIRString("Medication/1234")),
                    ),
                subject =
                    Reference(
                        reference = "Patient/123".asFHIR(),
                        type = Uri("Patient", extension = dataAuthorityExtension),
                    ),
            )
        val validation = validator.validate(medicationStatement, LocationContext(MedicationStatement::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate passed with more than one extension`() {
        val medicationStatement =
            MedicationStatement(
                id = Id("123"),
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.MEDICATION_STATEMENT.value)),
                        source = Uri("source"),
                    ),
                extension =
                    listOf(
                        Extension(
                            url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code("literal reference"),
                                ),
                        ),
                        Extension(
                            url = Uri("something"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code("literal reference"),
                                ),
                        ),
                        Extension(
                            url = Uri(null),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.REFERENCE,
                                    value = Code(null),
                                ),
                        ),
                    ),
                identifier =
                    listOf(
                        Identifier(value = "id".asFHIR()),
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
                status = MedicationStatementStatus.ACTIVE.asCode(),
                medication =
                    DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = FHIRString("Medication/1234")),
                    ),
                effective =
                    DynamicValue(
                        type = DynamicValueType.DATE_TIME,
                        value = DateTime("1905-08-23"),
                    ),
            )
        val validation = validator.validate(medicationStatement, LocationContext(MedicationStatement::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validation fails with no medication datatype extension url`() {
        val medicationStatement =
            MedicationStatement(
                id = Id("123"),
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.MEDICATION_STATEMENT.value)),
                        source = Uri("source"),
                    ),
                extension =
                    listOf(
                        Extension(
                            url = Uri(value = "incorrect-url"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code("literal reference"),
                                ),
                        ),
                    ),
                identifier =
                    listOf(
                        Identifier(value = "id".asFHIR()),
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
                status = MedicationStatementStatus.ACTIVE.asCode(),
                medication =
                    DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = FHIRString("Medication/1234")),
                    ),
                effective =
                    DynamicValue(
                        type = DynamicValueType.DATE_TIME,
                        value = DateTime("1905-08-23"),
                    ),
            )

        val validation = validator.validate(medicationStatement, LocationContext(MedicationStatement::class))
        assertEquals(1, validation.issues().size)

        assertEquals(
            "ERROR RONIN_MEDDTEXT_001: Extension must contain original Medication Datatype @ MedicationStatement.extension",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails with wrong medication datatype extension value`() {
        val medicationStatement =
            MedicationStatement(
                id = Id("123"),
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.MEDICATION_STATEMENT.value)),
                        source = Uri("source"),
                    ),
                extension =
                    listOf(
                        Extension(
                            url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODE,
                                    value = Code("blah"),
                                ),
                        ),
                    ),
                identifier =
                    listOf(
                        Identifier(value = "id".asFHIR()),
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
                status = MedicationStatementStatus.ACTIVE.asCode(),
                medication =
                    DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = FHIRString("Medication/1234")),
                    ),
                effective =
                    DynamicValue(
                        type = DynamicValueType.DATE_TIME,
                        value = DateTime("1905-08-23"),
                    ),
            )

        val validation = validator.validate(medicationStatement, LocationContext(MedicationStatement::class))
        assertEquals(1, validation.issues().size)

        assertEquals(
            "ERROR RONIN_MEDDTEXT_002: Medication Datatype extension value is invalid @ MedicationStatement.extension",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails with wrong medication datatype extension type`() {
        val medicationStatement =
            MedicationStatement(
                id = Id("123"),
                meta =
                    Meta(
                        profile = listOf(Canonical(RoninProfile.MEDICATION_STATEMENT.value)),
                        source = Uri("source"),
                    ),
                extension =
                    listOf(
                        Extension(
                            url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.STRING,
                                    value = Code("codeable concept"),
                                ),
                        ),
                        Extension(
                            url = Uri(value = null),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.STRING,
                                    value = Code(null),
                                ),
                        ),
                    ),
                identifier =
                    listOf(
                        Identifier(value = "id".asFHIR()),
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
                status = MedicationStatementStatus.ACTIVE.asCode(),
                medication =
                    DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = FHIRString("Medication/1234")),
                    ),
                effective =
                    DynamicValue(
                        type = DynamicValueType.DATE_TIME,
                        value = DateTime("1905-08-23"),
                    ),
            )

        val validation = validator.validate(medicationStatement, LocationContext(MedicationStatement::class))
        assertEquals(1, validation.issues().size)

        assertEquals(
            "ERROR RONIN_MEDDTEXT_003: Medication Datatype extension type is invalid @ MedicationStatement.extension",
            validation.issues().first().toString(),
        )
    }
}
