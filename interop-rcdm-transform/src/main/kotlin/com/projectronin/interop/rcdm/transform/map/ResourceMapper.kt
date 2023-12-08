package com.projectronin.interop.rcdm.transform.map

import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDateTime
import kotlin.reflect.KClass

interface ResourceMapper<R : Resource<R>> {
    val supportedResource: KClass<R>

    /**
     * Maps the [resource] for the [tenant].
     */
    fun map(
        resource: R,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): MapResponse<R>
}
