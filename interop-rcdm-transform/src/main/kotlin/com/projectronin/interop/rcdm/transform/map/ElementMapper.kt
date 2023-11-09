package com.projectronin.interop.rcdm.transform.map

import com.projectronin.interop.fhir.r4.element.Element
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDateTime
import kotlin.reflect.KClass

interface ElementMapper<E : Element<E>> {
    val supportedElement: KClass<E>

    fun <R : Resource<R>> map(
        element: E,
        resource: R,
        tenant: Tenant,
        parentContext: LocationContext,
        validation: Validation,
        forceCacheReloadTS: LocalDateTime?
    ): E?
}
