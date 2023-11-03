package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.tenant.config.model.Tenant
import kotlin.reflect.KClass

/**
 * Defines the interface for a transformer capable of converting [R] resources into the supported profile.
 */
interface ProfileTransformer<R : Resource<R>> {
    val supportedResource: KClass<R>
    val profile: RoninProfile
    val rcdmVersion: RCDMVersion
    val profileVersion: Int
    val isDefault: Boolean

    /**
     * Returns true if [resource] qualifies for this particular profile.
     */
    fun qualifies(resource: R): Boolean

    /**
     * Transforms the [original] to this profile for [tenant].
     */
    fun transform(original: R, tenant: Tenant): TransformResponse<R>?
}
