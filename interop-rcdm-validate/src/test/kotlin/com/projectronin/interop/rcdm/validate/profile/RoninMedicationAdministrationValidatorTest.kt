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
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.MedicationAdministration
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationAdministrationValidator
import com.projectronin.interop.fhir.r4.valueset.MedicationAdministrationStatus
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.dataAuthorityExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninMedicationAdministrationValidatorTest {
    private val validator = RoninMedicationAdministrationValidator()

    @Test
    fun `returns supported resource`() {
        assertEquals(MedicationAdministration::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4MedicationAdministrationValidator, validator.r4Validator)
    }

    @Test
    fun `returns profile`() {
        assertEquals(RoninProfile.MEDICATION_ADMINISTRATION, validator.profile)
    }

    @Test
    fun `validate succeeds with required attributes`() {
        val medAdmin = MedicationAdministration(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                source = Uri("source")
            ),
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
                statusCodingExtension("mapped"),
                Extension(
                    url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                    value = DynamicValue(
                        type = DynamicValueType.CODE,
                        value = Code("literal reference")
                    )
                )
            ),
            status = Code("in-progress"),
            category = CodeableConcept(
                coding = listOf(
                    Coding(id = "something".asFHIR())
                )
            ),
            effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
            medication = DynamicValue(
                DynamicValueType.REFERENCE,
                value = Reference(
                    reference = "Medication/something".asFHIR(),
                    identifier = null,
                    type = Uri("Medication", extension = dataAuthorityExtension)
                )
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = dataAuthorityExtension
                )
            )
        )
        val validation = validator.validate(medAdmin, LocationContext(MedicationAdministration::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate passed with more than one extension`() {
        val medAdmin = MedicationAdministration(
            id = Id("123"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                source = Uri("source")
            ),
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
                statusCodingExtension("mapped"),
                Extension(
                    url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                    value = DynamicValue(
                        type = DynamicValueType.CODE,
                        value = Code("literal reference")
                    )
                ),
                Extension(
                    url = Uri("something"),
                    value = DynamicValue(
                        type = DynamicValueType.CODE,
                        value = Code("literal reference")
                    )
                ),
                Extension(
                    url = Uri(null),
                    value = DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Code(null)
                    )
                )
            ),
            status = Code("in-progress"),
            effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
            medication = DynamicValue(
                DynamicValueType.REFERENCE,
                value = Reference(
                    reference = "Medication/something".asFHIR(),
                    identifier = null,
                    type = Uri("Medication", extension = emptyList())
                )
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = dataAuthorityExtension
                )
            )
        )
        val validation = validator.validate(medAdmin, LocationContext(MedicationAdministration::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validation fails with more than one category`() {
        val medAdmin = MedicationAdministration(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
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
                statusCodingExtension("mapped"),
                Extension(
                    url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                    value = DynamicValue(
                        type = DynamicValueType.CODE,
                        value = Code("literal reference")
                    )
                )
            ),
            category = CodeableConcept(
                coding = listOf(
                    Coding(id = "something".asFHIR()),
                    Coding(id = "something-else".asFHIR())
                )
            ),
            status = Code("in-progress"),
            effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
            medication = DynamicValue(
                type = DynamicValueType.REFERENCE,
                value = Reference(
                    reference = "Medication".asFHIR(),
                    identifier = null,
                    type = Uri("Medication", extension = dataAuthorityExtension)
                )
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = dataAuthorityExtension
                )
            )
        )
        val validation = validator.validate(medAdmin, LocationContext(MedicationAdministration::class))
        assertEquals(1, validation.issues().size)

        assertEquals(
            "ERROR RONIN_MEDADMIN_001: More than one category cannot be present if category is not null @ MedicationAdministration.category",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validation fails with no medication datatype extension url`() {
        val medAdmin = MedicationAdministration(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
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
            category = CodeableConcept(
                coding = listOf(
                    Coding(id = "something".asFHIR())
                )
            ),
            extension = listOf(
                statusCodingExtension("mapped"),
                Extension(
                    url = Uri(value = "incorrect-url"),
                    value = DynamicValue(
                        type = DynamicValueType.CODE,
                        value = Code("literal reference")
                    )
                )
            ),
            status = Code("in-progress"),
            effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
            medication = DynamicValue(
                type = DynamicValueType.REFERENCE,
                value = Reference(
                    reference = "Medication".asFHIR(),
                    identifier = null,
                    type = Uri("Medication", extension = dataAuthorityExtension)
                )
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = dataAuthorityExtension
                )
            )
        )

        val validation = validator.validate(medAdmin, LocationContext(MedicationAdministration::class))
        assertEquals(1, validation.issues().size)

        assertEquals(
            "ERROR RONIN_MEDDTEXT_001: Extension must contain original Medication Datatype @ MedicationAdministration.extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails with wrong medication datatype extension value`() {
        val medAdmin = MedicationAdministration(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                source = Uri("source")
            ),
            identifier = listOf(
                Identifier(value = "id".asFHIR()),
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
                statusCodingExtension("mapped"),
                Extension(
                    url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                    value = DynamicValue(
                        type = DynamicValueType.CODE,
                        value = Code("blah")
                    )
                )
            ),
            status = MedicationAdministrationStatus.IN_PROGRESS.asCode(),
            medication = DynamicValue(
                type = DynamicValueType.REFERENCE,
                value = Reference(reference = FHIRString("Medication/test-1234"))
            ),
            subject = Reference(
                display = "subject".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                value = DateTime("1905-08-23")
            )
        )

        val validation = validator.validate(medAdmin, LocationContext(MedicationAdministration::class))
        assertEquals(1, validation.issues().size)

        assertEquals(
            "ERROR RONIN_MEDDTEXT_002: Medication Datatype extension value is invalid @ MedicationAdministration.extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails with wrong medication datatype extension type`() {
        val medAdmin = MedicationAdministration(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                source = Uri("source")
            ),
            identifier = listOf(
                Identifier(value = "id".asFHIR()),
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
                statusCodingExtension("mapped"),
                Extension(
                    url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                    value = DynamicValue(
                        type = DynamicValueType.STRING,
                        value = Code("codeable concept")
                    )
                ),
                Extension(
                    url = Uri(value = null),
                    value = DynamicValue(
                        type = DynamicValueType.STRING,
                        value = Code(null)
                    )
                )
            ),
            status = MedicationAdministrationStatus.IN_PROGRESS.asCode(),
            medication = DynamicValue(
                type = DynamicValueType.REFERENCE,
                value = Reference(reference = FHIRString("Medication/test-1234"))
            ),
            subject = Reference(
                display = "subject".asFHIR(),
                type = Uri("Patient", extension = dataAuthorityExtension)
            ),
            effective = DynamicValue(
                type = DynamicValueType.DATE_TIME,
                value = DateTime("1905-08-23")
            )
        )

        val validation = validator.validate(medAdmin, LocationContext(MedicationAdministration::class))
        assertEquals(1, validation.issues().size)

        assertEquals(
            "ERROR RONIN_MEDDTEXT_003: Medication Datatype extension type is invalid @ MedicationAdministration.extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails for wrong URL in status source extension`() {
        val medAdmin = MedicationAdministration(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                source = Uri("source")
            ),
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
                    url = Uri("http://example.org/other-extension"),
                    value = DynamicValue(
                        type = DynamicValueType.CODE,
                        value = Code(value = "mapped")
                    )
                ),
                Extension(
                    url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                    value = DynamicValue(
                        type = DynamicValueType.CODE,
                        value = Code("literal reference")
                    )
                )
            ),
            status = Code("in-progress"),
            category = CodeableConcept(
                coding = listOf(
                    Coding(id = "something".asFHIR())
                )
            ),
            effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
            medication = DynamicValue(
                DynamicValueType.REFERENCE,
                value = Reference(
                    reference = "Medication/something".asFHIR(),
                    identifier = null,
                    type = Uri("Medication", extension = dataAuthorityExtension)
                )
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = dataAuthorityExtension
                )
            )
        )

        val validation = validator.validate(medAdmin, LocationContext(MedicationAdministration::class))
        assertEquals(1, validation.issues().size)

        assertEquals(
            "ERROR RONIN_MEDADMIN_003: Tenant source medication administration status extension is missing or invalid @ MedicationAdministration.extension",
            validation.issues().first().toString()
        )
    }

    @Test
    fun `validate fails for wrong data type in status source extension`() {
        val medAdmin = MedicationAdministration(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical(RoninProfile.MEDICATION_ADMINISTRATION.value)),
                source = Uri("source")
            ),
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
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceMedicationAdministrationStatus"),
                    value = DynamicValue(
                        type = DynamicValueType.BOOLEAN,
                        value = FHIRBoolean.FALSE
                    )
                ),
                Extension(
                    url = Uri(value = RoninExtension.ORIGINAL_MEDICATION_DATATYPE.uri.value),
                    value = DynamicValue(
                        type = DynamicValueType.CODE,
                        value = Code("literal reference")
                    )
                )
            ),
            status = Code("in-progress"),
            category = CodeableConcept(
                coding = listOf(
                    Coding(id = "something".asFHIR())
                )
            ),
            effective = DynamicValue(DynamicValueType.DATE_TIME, "00:00:00"),
            medication = DynamicValue(
                DynamicValueType.REFERENCE,
                value = Reference(
                    reference = "Medication/something".asFHIR(),
                    identifier = null,
                    type = Uri("Medication", extension = dataAuthorityExtension)
                )
            ),
            subject = Reference(
                reference = "Patient/123".asFHIR(),
                type = Uri(
                    "Patient",
                    extension = dataAuthorityExtension
                )
            )
        )
        val validation = validator.validate(medAdmin, LocationContext(MedicationAdministration::class))
        assertEquals(1, validation.issues().size)

        assertEquals(
            "ERROR RONIN_MEDADMIN_003: Tenant source medication administration status extension is missing or invalid @ MedicationAdministration.extension",
            validation.issues().first().toString()
        )
    }

    private fun statusCoding(value: String) = Coding(
        system = Uri("http://projectronin.io/fhir/CodeSystem/test/MedicationAdministrationStatus"),
        code = Code(value = value)
    )

    private fun statusCodingExtension(value: String) = Extension(
        url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceMedicationAdministrationStatus"),
        value = DynamicValue(
            type = DynamicValueType.CODING,
            value = Coding(
                system = Uri("http://projectronin.io/fhir/CodeSystem/test/MedicationAdministrationStatus"),
                code = Code(value = value)
            )
        )
    )
}
