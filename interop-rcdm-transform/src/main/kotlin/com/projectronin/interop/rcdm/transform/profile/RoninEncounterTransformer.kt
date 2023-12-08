package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.util.getRoninIdentifiers
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class RoninEncounterTransformer : ProfileTransformer<Encounter>() {
    override val supportedResource: KClass<Encounter> = Encounter::class
    override val profile: RoninProfile = RoninProfile.ENCOUNTER
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_20_0
    override val profileVersion: Int = 4

    override fun transformInternal(
        original: Encounter,
        tenant: Tenant,
    ): TransformResponse<Encounter>? {
        val transformed =
            original.copy(
                identifier = original.getRoninIdentifiers(tenant),
            )
        return TransformResponse(transformed)
    }
}
