package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.resource.RequestGroup
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.util.getRoninIdentifiers
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class RoninRequestGroupTransformer : ProfileTransformer<RequestGroup>() {
    override val supportedResource: KClass<RequestGroup> = RequestGroup::class
    override val profile: RoninProfile = RoninProfile.REQUEST_GROUP
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2

    override fun transformInternal(
        original: RequestGroup,
        tenant: Tenant,
    ): TransformResponse<RequestGroup>? {
        val transformed =
            original.copy(
                identifier = original.getRoninIdentifiers(tenant),
            )
        return TransformResponse(transformed)
    }
}
