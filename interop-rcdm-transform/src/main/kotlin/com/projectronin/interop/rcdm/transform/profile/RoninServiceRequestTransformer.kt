package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.resource.ServiceRequest
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.util.getRoninIdentifiers
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class RoninServiceRequestTransformer : ProfileTransformer<ServiceRequest>() {
    override val supportedResource: KClass<ServiceRequest> = ServiceRequest::class
    override val profile: RoninProfile = RoninProfile.SERVICE_REQUEST
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_27_0
    override val profileVersion: Int = 1

    override fun transformInternal(original: ServiceRequest, tenant: Tenant): TransformResponse<ServiceRequest>? {
        val transformed = original.copy(
            identifier = original.getRoninIdentifiers(tenant)
        )
        return TransformResponse(transformed)
    }
}
