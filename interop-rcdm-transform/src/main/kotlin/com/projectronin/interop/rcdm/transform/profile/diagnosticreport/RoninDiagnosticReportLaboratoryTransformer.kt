package com.projectronin.interop.rcdm.transform.profile.diagnosticreport

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.qualifiesForValueSet
import org.springframework.stereotype.Component

@Component
class RoninDiagnosticReportLaboratoryTransformer : BaseRoninDiagnosticReportProfileTransformer() {
    override val profile: RoninProfile = RoninProfile.DIAGNOSTIC_REPORT_LABORATORY
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_19_0
    override val profileVersion: Int = 2
    override val isDefault: Boolean = false

    private val qualifyingCategories =
        listOf(Coding(system = CodeSystem.DIAGNOSTIC_REPORT_LABORATORY.uri, code = Code("LAB")))

    override fun qualifies(resource: DiagnosticReport): Boolean = resource.category.qualifiesForValueSet(qualifyingCategories)
}
