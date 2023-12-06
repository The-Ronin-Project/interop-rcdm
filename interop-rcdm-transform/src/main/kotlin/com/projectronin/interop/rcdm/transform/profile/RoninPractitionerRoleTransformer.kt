package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.util.getRoninIdentifiers
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class RoninPractitionerRoleTransformer : ProfileTransformer<PractitionerRole>() {
    override val supportedResource: KClass<PractitionerRole> = PractitionerRole::class
    override val profile: RoninProfile = RoninProfile.PRACTITIONER_ROLE
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_19_0
    override val profileVersion: Int = 2

    override fun transformInternal(original: PractitionerRole, tenant: Tenant): TransformResponse<PractitionerRole>? {
        val transformed = original.copy(
            identifier = original.getRoninIdentifiers(tenant)
        )
        return TransformResponse(transformed)
    }
}
