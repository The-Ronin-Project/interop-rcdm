package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.common.reflect.copy
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.tenant.config.model.Tenant

/**
 * Base transformer for profiles that simplifies some of the needs for building profile transformers.
 */
abstract class BaseProfileTransformer<R : Resource<R>> : ProfileTransformer<R> {
    override val isDefault: Boolean = true

    /**
     * Performs the internal transformation of the [original]. Note that Meta is guaranteed to be set by this class, so this method does not need to implement that functionality.
     */
    abstract fun transformInternal(original: R, tenant: Tenant): TransformResponse<R>?

    override fun qualifies(resource: R): Boolean = true

    final override fun transform(original: R, tenant: Tenant): TransformResponse<R>? {
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
        val transformedProfiles = existingRoninProfiles + this@BaseProfileTransformer.profile.canonical
        return this?.copy(profile = transformedProfiles) ?: Meta(profile = transformedProfiles)
    }
}
