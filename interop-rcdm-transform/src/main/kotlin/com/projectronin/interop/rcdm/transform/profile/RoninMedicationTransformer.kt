package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.util.getRoninIdentifiers
import com.projectronin.interop.tenant.config.model.Tenant
import kotlin.reflect.KClass

class RoninMedicationTransformer : ProfileTransformer<Medication>() {
    override val supportedResource: KClass<Medication> = Medication::class
    override val profile: RoninProfile = RoninProfile.MEDICATION
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_19_0
    override val profileVersion: Int = 2
    override fun transformInternal(original: Medication, tenant: Tenant): TransformResponse<Medication>? {
        val transformed = original.copy(
            identifier = original.getRoninIdentifiers(tenant)
        )
        return TransformResponse(transformed)
    }
}
