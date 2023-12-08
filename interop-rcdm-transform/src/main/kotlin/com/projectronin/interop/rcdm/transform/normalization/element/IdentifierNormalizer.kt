package com.projectronin.interop.rcdm.transform.normalization.element

import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.rcdm.transform.introspection.TransformResult
import com.projectronin.interop.rcdm.transform.normalization.ElementNormalizer
import com.projectronin.interop.rcdm.transform.normalization.normalizeIdentifier
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class IdentifierNormalizer : ElementNormalizer<Identifier> {
    override val elementType: KClass<Identifier> = Identifier::class

    override fun normalize(
        element: Identifier,
        tenant: Tenant,
    ): TransformResult<Identifier> {
        val normalizedSystem = element.system?.normalizeIdentifier()
        val normalizedIdentifier =
            if (normalizedSystem == element.system) {
                element
            } else {
                element.copy(system = normalizedSystem)
            }
        return TransformResult(normalizedIdentifier)
    }
}
