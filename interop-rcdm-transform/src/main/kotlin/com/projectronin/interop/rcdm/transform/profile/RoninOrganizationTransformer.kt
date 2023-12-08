package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.resource.Organization
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.util.getRoninIdentifiers
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class RoninOrganizationTransformer : ProfileTransformer<Organization>() {
    override val supportedResource: KClass<Organization> = Organization::class
    override val profile: RoninProfile = RoninProfile.ORGANIZATION
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_19_0
    override val profileVersion: Int = 2

    override fun transformInternal(
        original: Organization,
        tenant: Tenant,
    ): TransformResponse<Organization>? {
        val transformed =
            original.copy(
                identifier = original.getRoninIdentifiers(tenant),
            )
        return TransformResponse(transformed)
    }
}
