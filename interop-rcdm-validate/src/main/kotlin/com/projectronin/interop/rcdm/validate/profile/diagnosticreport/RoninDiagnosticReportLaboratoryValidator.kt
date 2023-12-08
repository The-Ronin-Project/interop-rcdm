package com.projectronin.interop.rcdm.validate.profile.diagnosticreport

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.qualifiesForValueSet
import org.springframework.stereotype.Component

@Component
class RoninDiagnosticReportLaboratoryValidator : BaseRoninDiagnosticReportProfileValidator() {
    override val profile: RoninProfile = RoninProfile.DIAGNOSTIC_REPORT_LABORATORY
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_19_0
    override val profileVersion: Int = 2

    private val qualifyingCategories =
        listOf(Coding(system = CodeSystem.DIAGNOSTIC_REPORT_LABORATORY.uri, code = Code("LAB")))

    override fun validateProfile(
        resource: DiagnosticReport,
        validation: Validation,
        context: LocationContext,
    ) {
        validation.apply {
            checkTrue(
                resource.category.qualifiesForValueSet(qualifyingCategories),
                FHIRError(
                    code = "USCORE_DX_RPT_001",
                    severity = ValidationIssueSeverity.ERROR,
                    description = "Must match this system|code: ${
                        qualifyingCategories.joinToString(", ") { "${it.system?.value}|${it.code?.value}" }
                    }",
                    location = LocationContext(DiagnosticReport::category),
                ),
                context,
            )
        }
    }
}
