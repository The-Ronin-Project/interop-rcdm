package com.projectronin.interop.rcdm.validate.profile.diagnosticreport

import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.springframework.stereotype.Component

@Component
class RoninDiagnosticReportNoteExchangeValidator : BaseRoninDiagnosticReportProfileValidator() {
    override val profile: RoninProfile = RoninProfile.DIAGNOSTIC_REPORT_NOTE_EXCHANGE
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_19_0
    override val profileVersion: Int = 2

    override fun validateProfile(
        resource: DiagnosticReport,
        validation: Validation,
        context: LocationContext,
    ) {}
}
