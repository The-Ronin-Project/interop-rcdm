package com.projectronin.interop.rcdm.transform.normalization

import com.projectronin.interop.fhir.r4.element.Element
import com.projectronin.interop.rcdm.transform.introspection.BaseGenericTransformer
import com.projectronin.interop.rcdm.transform.introspection.TransformResult
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

@Component
class Normalizer(elementNormalizers: List<ElementNormalizer<*>>) : BaseGenericTransformer() {
    private val normalizersByElement = elementNormalizers.associateBy { it.elementType }

    override val ignoredFieldNames: Set<String> = setOf("contained")

    /**
     * Normalizes the [element] for the [tenant]
     */
    fun <T : Any> normalize(
        element: T,
        tenant: Tenant,
    ): T {
        val normalizedValues = getTransformedValues(element, tenant)
        return copy(element, normalizedValues)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> transformType(
        element: T,
        tenant: Tenant,
    ): TransformResult<T>? {
        return when (element) {
            is Element<*> -> transformElement(element, tenant) as? TransformResult<T>
            else -> null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <E : Element<E>> transformElement(
        element: Element<E>,
        tenant: Tenant,
    ): TransformResult<E>? {
        val internallyNormalizedElement = (transformOrNull(element, tenant) ?: element) as E

        val normalizer = normalizersByElement[element::class] as? ElementNormalizer<E>
        return normalizer?.normalize(internallyNormalizedElement, tenant)
            ?: if (internallyNormalizedElement == element) {
                null
            } else {
                TransformResult(internallyNormalizedElement)
            }
    }
}
