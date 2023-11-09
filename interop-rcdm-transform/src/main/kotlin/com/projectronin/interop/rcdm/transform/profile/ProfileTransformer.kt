package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.common.reflect.copy
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.tenant.config.model.Tenant
import kotlin.reflect.KClass

/**
 * Base transformer for profiles that simplifies some of the needs for building profile transformers.
 */
abstract class ProfileTransformer<R : Resource<R>> {
    abstract val supportedResource: KClass<R>
    abstract val profile: RoninProfile
    abstract val rcdmVersion: RCDMVersion
    abstract val profileVersion: Int

    open val isDefault: Boolean = true

    /**
     * Performs the internal transformation of the [original]. Note that Meta is guaranteed to be set by this class, so this method does not need to implement that functionality.
     */
    abstract fun transformInternal(original: R, tenant: Tenant): TransformResponse<R>?

    /**
     * Returns true if [resource] qualifies for this particular profile.
     */
    open fun qualifies(resource: R): Boolean = true

    /**
     * Transforms the [original] to this profile for [tenant].
     */
    fun transform(original: R, tenant: Tenant): TransformResponse<R>? {
        val transformedResponse = transformInternal(original, tenant)
        return transformedResponse?.let {
            val transformedMeta = it.resource.meta.transform()
            val resourceWithMeta = copy(it.resource, mapOf("meta" to transformedMeta))

            TransformResponse(
                resourceWithMeta,
                it.embeddedResources
            )
        }
    }

    private fun Meta?.transform(): Meta {
        val existingRoninProfiles =
            this?.profile?.filter { c -> c.value?.let { RoninProfile.forUrl(it) } != null } ?: emptyList()
        val transformedProfiles = existingRoninProfiles + this@ProfileTransformer.profile.canonical
        return this?.copy(profile = transformedProfiles) ?: Meta(profile = transformedProfiles)
    }
}
