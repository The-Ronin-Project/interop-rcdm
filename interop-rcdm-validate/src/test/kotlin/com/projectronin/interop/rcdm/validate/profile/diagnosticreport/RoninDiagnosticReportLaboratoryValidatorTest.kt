package com.projectronin.interop.rcdm.validate.profile.diagnosticreport

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.fhir.r4.validate.resource.R4DiagnosticReportValidator
import com.projectronin.interop.fhir.r4.valueset.DiagnosticReportStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.validate.profile.util.requiredIdentifiers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@Suppress("ktlint:standard:max-line-length")
class RoninDiagnosticReportLaboratoryValidatorTest {
    private val validator = RoninDiagnosticReportLaboratoryValidator()

    @Test
    fun `returns supported resource`() {
        assertEquals(DiagnosticReport::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4DiagnosticReportValidator, validator.r4Validator)
    }

    @Test
    fun `returns profile`() {
        assertEquals(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY, validator.profile)
    }

    @Test
    fun `validate fails if no category`() {
        val diagnosticReport =
            DiagnosticReport(
                id = Id("test-1234"),
                meta = Meta(profile = listOf(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.canonical)),
                identifier = requiredIdentifiers,
                code = CodeableConcept(text = "code".asFHIR()),
                status = DiagnosticReportStatus.FINAL.asCode(),
                category = listOf(),
                subject = Reference(reference = "Patient/test-1234".asFHIR()),
            )

        val validation = validator.validate(diagnosticReport, LocationContext(DiagnosticReport::class))
        assertEquals(2, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: category is a required element @ DiagnosticReport.category",
            validation.issues()[0].toString(),
        )
        assertEquals(
            "ERROR USCORE_DX_RPT_001: Must match this system|code: http://terminology.hl7.org/CodeSystem/v2-0074|LAB @ DiagnosticReport.category",
            validation.issues()[1].toString(),
        )
    }

    @Test
    fun `validate fails if no subject`() {
        val diagnosticReport =
            DiagnosticReport(
                id = Id("test-1234"),
                meta = Meta(profile = listOf(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.canonical)),
                identifier = requiredIdentifiers,
                code = CodeableConcept(text = "code".asFHIR()),
                status = DiagnosticReportStatus.FINAL.asCode(),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.DIAGNOSTIC_REPORT_LABORATORY.uri,
                                        code = Code("LAB"),
                                    ),
                                ),
                        ),
                    ),
                subject = null,
            )

        val validation = validator.validate(diagnosticReport, LocationContext(DiagnosticReport::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: subject is a required element @ DiagnosticReport.subject",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if subject is not a patient`() {
        val diagnosticReport =
            DiagnosticReport(
                id = Id("test-1234"),
                meta = Meta(profile = listOf(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.canonical)),
                identifier = requiredIdentifiers,
                code = CodeableConcept(text = "code".asFHIR()),
                status = DiagnosticReportStatus.FINAL.asCode(),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.DIAGNOSTIC_REPORT_LABORATORY.uri,
                                        code = Code("LAB"),
                                    ),
                                ),
                        ),
                    ),
                subject = Reference(reference = "Group/test-1234".asFHIR()),
            )

        val validation = validator.validate(diagnosticReport, LocationContext(DiagnosticReport::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR INV_REF_TYPE: reference can only be one of the following: Patient @ DiagnosticReport.subject.reference",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if category is not lab`() {
        val diagnosticReport =
            DiagnosticReport(
                id = Id("test-1234"),
                meta = Meta(profile = listOf(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.canonical)),
                identifier = requiredIdentifiers,
                code = CodeableConcept(text = "code".asFHIR()),
                status = DiagnosticReportStatus.FINAL.asCode(),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.DIAGNOSTIC_REPORT_LABORATORY.uri,
                                        code = Code("NOT-LAB"),
                                    ),
                                ),
                        ),
                    ),
                subject = Reference(reference = "Patient/test-1234".asFHIR()),
            )

        val validation = validator.validate(diagnosticReport, LocationContext(DiagnosticReport::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR USCORE_DX_RPT_001: Must match this system|code: http://terminology.hl7.org/CodeSystem/v2-0074|LAB @ DiagnosticReport.category",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate succeeds`() {
        val diagnosticReport =
            DiagnosticReport(
                id = Id("test-1234"),
                meta = Meta(profile = listOf(RoninProfile.DIAGNOSTIC_REPORT_LABORATORY.canonical)),
                identifier = requiredIdentifiers,
                code = CodeableConcept(text = "code".asFHIR()),
                status = DiagnosticReportStatus.FINAL.asCode(),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.DIAGNOSTIC_REPORT_LABORATORY.uri,
                                        code = Code("LAB"),
                                    ),
                                ),
                        ),
                    ),
                subject = Reference(reference = "Patient/test-1234".asFHIR()),
            )

        val validation = validator.validate(diagnosticReport, LocationContext(DiagnosticReport::class))
        assertEquals(0, validation.issues().size)
    }
}
