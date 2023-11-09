package com.projectronin.interop.rcdm.transform.normalization.element

import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.rcdm.transform.introspection.TransformResult
import com.projectronin.interop.rcdm.transform.normalization.ElementNormalizer
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class ContactPointNormalizer : ElementNormalizer<ContactPoint> {
    override val elementType: KClass<ContactPoint> = ContactPoint::class

    override fun normalize(element: ContactPoint, tenant: Tenant): TransformResult<ContactPoint> {
        return if (element.value == null || element.system == null) {
            TransformResult(null, true)
        } else {
            TransformResult(element)
        }
    }
}
