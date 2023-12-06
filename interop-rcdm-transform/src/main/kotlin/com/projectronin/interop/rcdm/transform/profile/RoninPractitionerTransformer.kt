package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.resource.Practitioner
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.util.getRoninIdentifiers
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class RoninPractitionerTransformer : ProfileTransformer<Practitioner>() {
    override val supportedResource: KClass<Practitioner> = Practitioner::class
    override val profile: RoninProfile = RoninProfile.PRACTITIONER
    override val rcdmVersion = RCDMVersion.V3_19_0
    override val profileVersion = 2
    override fun transformInternal(original: Practitioner, tenant: Tenant): TransformResponse<Practitioner>? {
        val transformed = original.copy(
            identifier = original.getRoninIdentifiers(tenant)
        )
        return TransformResponse(transformed)
    }
}
