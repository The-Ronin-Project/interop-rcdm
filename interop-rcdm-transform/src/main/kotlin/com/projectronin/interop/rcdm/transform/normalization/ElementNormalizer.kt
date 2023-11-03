package com.projectronin.interop.rcdm.transform.normalization

import com.projectronin.interop.fhir.r4.element.Element
import com.projectronin.interop.rcdm.transform.introspection.TransformResult
import com.projectronin.interop.tenant.config.model.Tenant
import kotlin.reflect.KClass

interface ElementNormalizer<E : Element<E>> {
    val elementType: KClass<E>

    fun normalize(element: E, tenant: Tenant): TransformResult<E>
}
