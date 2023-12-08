package com.projectronin.interop.rcdm.transform.profile.diagnosticreport

import com.projectronin.interop.fhir.r4.resource.DiagnosticReport
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.profile.ProfileTransformer
import com.projectronin.interop.rcdm.transform.util.getRoninIdentifiers
import com.projectronin.interop.tenant.config.model.Tenant
import kotlin.reflect.KClass

abstract class BaseRoninDiagnosticReportProfileTransformer : ProfileTransformer<DiagnosticReport>() {
    override val supportedResource: KClass<DiagnosticReport> = DiagnosticReport::class

    override fun transformInternal(
        original: DiagnosticReport,
        tenant: Tenant,
    ): TransformResponse<DiagnosticReport>? {
        val transformed =
            original.copy(
                identifier = original.getRoninIdentifiers(tenant),
            )
        return TransformResponse(transformed)
    }
}
