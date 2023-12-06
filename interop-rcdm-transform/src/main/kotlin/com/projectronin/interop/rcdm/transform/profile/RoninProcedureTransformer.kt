package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.resource.Procedure
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.util.getRoninIdentifiers
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class RoninProcedureTransformer : ProfileTransformer<Procedure>() {
    override val supportedResource: KClass<Procedure> = Procedure::class
    override val profile = RoninProfile.PROCEDURE
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_28_0
    override val profileVersion: Int = 1

    override fun transformInternal(original: Procedure, tenant: Tenant): TransformResponse<Procedure>? {
        val transformed = original.copy(
            identifier = original.getRoninIdentifiers(tenant)
        )
        return TransformResponse(transformed)
    }
}
