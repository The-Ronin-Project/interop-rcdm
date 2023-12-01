package com.projectronin.interop.rcdm.transform.profile.diagnosticreport

import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.springframework.stereotype.Component

@Component
class RoninDiagnosticReportNoteExchangeTransformer : BaseRoninDiagnosticReportProfileTransformer() {
    override val profile: RoninProfile = RoninProfile.DIAGNOSTIC_REPORT_NOTE_EXCHANGE
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_19_0
    override val profileVersion: Int = 2
    override val isDefault: Boolean = true

    override fun qualifies(resource: DiagnosticReport): Boolean = true
}
