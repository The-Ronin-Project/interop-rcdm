package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.util.getRoninIdentifiers
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class RoninCarePlanTransformer : ProfileTransformer<CarePlan>() {
    override val supportedResource: KClass<CarePlan> = CarePlan::class
    override val profile: RoninProfile = RoninProfile.CARE_PLAN
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_27_0
    override val profileVersion: Int = 7

    override fun transformInternal(
        original: CarePlan,
        tenant: Tenant,
    ): TransformResponse<CarePlan>? {
        val transformed =
            original.copy(
                identifier = original.getRoninIdentifiers(tenant),
            )
        return TransformResponse(transformed)
    }
}
