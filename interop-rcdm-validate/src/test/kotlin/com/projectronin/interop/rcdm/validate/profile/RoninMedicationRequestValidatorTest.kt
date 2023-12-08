package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.validate.resource.R4MedicationRequestValidator
import com.projectronin.interop.fhir.r4.valueset.MedicationRequestIntent
import com.projectronin.interop.fhir.r4.valueset.MedicationRequestStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.dataAuthorityExtension
import com.projectronin.interop.rcdm.validate.profile.util.requiredIdentifiers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninMedicationRequestValidatorTest {
    private val validator = RoninMedicationRequestValidator()

    @Test
    fun `returns supported resource`() {
        assertEquals(MedicationRequest::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4MedicationRequestValidator, validator.r4Validator)
    }

    @Test
    fun `returns profile`() {
        assertEquals(RoninProfile.MEDICATION_REQUEST, validator.profile)
    }

    @Test
    fun `validate succeeds`() {
        val medicationRequest =
            MedicationRequest(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
                identifier = requiredIdentifiers,
                status = MedicationRequestStatus.COMPLETED.asCode(),
                intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
                medication =
                    DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = FHIRString("Medication/test-1234")),
                    ),
                subject =
                    Reference(
                        reference = "Patient/1234".asFHIR(),
                        type = Uri("Patient", extension = dataAuthorityExtension),
                    ),
                requester = Reference(reference = "Practitioner/1234".asFHIR()),
            )
        val validation = validator.validate(medicationRequest, LocationContext(MedicationRequest::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate fails when requester is not provided`() {
        val medicationRequest =
            MedicationRequest(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
                identifier = requiredIdentifiers,
                status = MedicationRequestStatus.COMPLETED.asCode(),
                intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
                requester = null,
                medication =
                    DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = FHIRString("Medication/test-1234")),
                    ),
                subject =
                    Reference(
                        reference = "Patient/1234".asFHIR(),
                        type = Uri("Patient", extension = dataAuthorityExtension),
                    ),
            )

        val validation = validator.validate(medicationRequest, LocationContext(MedicationRequest::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: requester is a required element @ MedicationRequest.requester",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails with no medication datatype extension`() {
        val medicationRequest =
            MedicationRequest(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
                identifier = requiredIdentifiers,
                status = MedicationRequestStatus.COMPLETED.asCode(),
                intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
                medication =
                    DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = FHIRString("Medication/test-1234")),
                    ),
                subject =
                    Reference(
                        reference = "Patient/1234".asFHIR(),
                        type = Uri("Patient", extension = dataAuthorityExtension),
                    ),
                requester = Reference(reference = "Practitioner/1234".asFHIR()),
            )

        val validation = validator.validate(medicationRequest, LocationContext(MedicationRequest::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_MEDDTEXT_001: Extension must contain original Medication Datatype @ MedicationRequest.extension",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails with wrong medication datatype extension value`() {
        val medicationRequest =
            MedicationRequest(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
                identifier = requiredIdentifiers,
                status = MedicationRequestStatus.COMPLETED.asCode(),
                intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
                medication =
                    DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = FHIRString("Medication/test-1234")),
                    ),
                subject =
                    Reference(
                        reference = "Patient/1234".asFHIR(),
                        type = Uri("Patient", extension = dataAuthorityExtension),
                    ),
                requester = Reference(reference = "Practitioner/1234".asFHIR()),
            )

        val validation = validator.validate(medicationRequest, LocationContext(MedicationRequest::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_MEDDTEXT_002: Medication Datatype extension value is invalid @ MedicationRequest.extension",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails with wrong medication datatype extension type`() {
        val medicationRequest =
            MedicationRequest(
                id = Id("12345"),
                meta = Meta(profile = listOf(Canonical(RoninProfile.MEDICATION_REQUEST.value)), source = Uri("source")),
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
                    ),
                identifier = requiredIdentifiers,
                status = MedicationRequestStatus.COMPLETED.asCode(),
                intent = MedicationRequestIntent.FILLER_ORDER.asCode(),
                medication =
                    DynamicValue(
                        type = DynamicValueType.REFERENCE,
                        value = Reference(reference = FHIRString("Medication/test-1234")),
                    ),
                subject =
                    Reference(
                        reference = "Patient/1234".asFHIR(),
                        type = Uri("Patient", extension = dataAuthorityExtension),
                    ),
                requester = Reference(reference = "Practitioner/1234".asFHIR()),
            )

        val validation = validator.validate(medicationRequest, LocationContext(MedicationRequest::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_MEDDTEXT_003: Medication Datatype extension type is invalid @ MedicationRequest.extension",
            validation.issues().first().toString(),
        )
    }
}
