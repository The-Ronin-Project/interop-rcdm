package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.util.getRoninIdentifiers
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class RoninLocationTransformer : ProfileTransformer<Location>() {
    override val supportedResource: KClass<Location> = Location::class
    override val profile: RoninProfile = RoninProfile.LOCATION
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_19_0
    override val profileVersion: Int = 2

    private val DEFAULT_NAME = "Unnamed Location"
    override fun transformInternal(original: Location, tenant: Tenant): TransformResponse<Location>? {
        val originalName = original.name
        val name = if (originalName == null) {
            FHIRString(DEFAULT_NAME)
        } else if (originalName.value.isNullOrEmpty()) {
            FHIRString(DEFAULT_NAME, originalName.id, originalName.extension)
        } else {
            original.name
        }
        val transformed = original.copy(
            identifier = original.getRoninIdentifiers(tenant),
            name = name
        )
        return TransformResponse(transformed)
    }
}
