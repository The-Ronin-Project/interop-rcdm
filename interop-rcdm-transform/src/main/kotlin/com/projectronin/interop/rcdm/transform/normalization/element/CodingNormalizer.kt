package com.projectronin.interop.rcdm.transform.normalization.element

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.rcdm.transform.introspection.TransformResult
import com.projectronin.interop.rcdm.transform.normalization.ElementNormalizer
import com.projectronin.interop.rcdm.transform.normalization.normalizeCoding
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class CodingNormalizer : ElementNormalizer<Coding> {
    override val elementType: KClass<Coding> = Coding::class

    override fun normalize(element: Coding, tenant: Tenant): TransformResult<Coding> {
        val normalizedSystem = element.system?.normalizeCoding()
        val normalizedCoding = if (normalizedSystem == element.system) {
            element
        } else {
            element.copy(system = normalizedSystem)
        }
        return TransformResult(normalizedCoding)
    }
}
