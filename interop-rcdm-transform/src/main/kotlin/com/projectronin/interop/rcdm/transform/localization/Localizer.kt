package com.projectronin.interop.rcdm.transform.localization

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.element.Element
import com.projectronin.interop.fhir.validate.Validatable
import com.projectronin.interop.rcdm.common.util.dataAuthorityExtension
import com.projectronin.interop.rcdm.transform.introspection.BaseGenericTransformer
import com.projectronin.interop.rcdm.transform.introspection.TransformResult
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component

/**
 * Localizer is capable of localizing an element. This localization may include system normalization or tenant-level
 * data segregation.
 */
@Component
class Localizer : BaseGenericTransformer() {
    override val ignoredFieldNames: Set<String> = setOf("contained", "versionId")

    /**
     * Localizes the [element] for the [tenant]
     */
    fun <T : Any> localize(
        element: T,
        tenant: Tenant,
    ): T {
        val localizedValues = getTransformedValues(element, tenant)
        return copy(element, localizedValues)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> transformType(
        element: T,
        tenant: Tenant,
    ): TransformResult<T> {
        val transformed =
            when (element) {
                is DynamicValue<*> -> localizeDynamicValue(element as DynamicValue<Any>, tenant)
                is Id -> localizeId(element, tenant)
                is Reference -> localizeReference(element, tenant)
                is Validatable<*> -> transformOrNull(element, tenant)
                else -> null
            }
        return TransformResult(transformed?.let { it as T })
    }

    /**
     * Localizes the [dynamicValue] for the [tenant].
     */
    private fun localizeDynamicValue(
        dynamicValue: DynamicValue<Any>,
        tenant: Tenant,
    ): DynamicValue<Any>? {
        val localizedValue = transformType(dynamicValue.value as Element<*>, tenant)
        return localizedValue.element?.let { DynamicValue(dynamicValue.type, it) }
    }

    /**
     * Localizes the [id] for the [tenant].
     */
    private fun localizeId(
        id: Id,
        tenant: Tenant,
    ): Id = Id(id.value?.localize(tenant), id.id, id.extension)

    /**
     * Localizes the [reference] for the [tenant].
     */
    private fun localizeReference(
        reference: Reference,
        tenant: Tenant,
    ): Reference? {
        val nonReferenceLocalized = transformOrNull(reference, tenant) ?: reference
        return nonReferenceLocalized.localize(tenant)
    }

    /**
     * Localizes the String relative to the [tenant]
     */
    private fun String.localize(tenant: Tenant): String {
        val prefix = "${tenant.mnemonic}-"
        return if (this.startsWith(prefix)) this else "$prefix$this"
    }

    /**
     * Localizes the [reference](http://hl7.org/fhir/R4/references.html) contained by this String relative to the [tenant].
     * If this String does not represent a reference, the original String will be returned. Also returns the reference type.
     */
    private fun Reference.localize(tenant: Tenant): Reference {
        reference?.value?.let {
            val matchResult = Reference.FHIR_RESOURCE_REGEX.matchEntire(it) ?: return this

            // Should we localize if there's a history?
            val (_, _, _, type, fhirId, history) = matchResult.destructured
            return copy(
                reference =
                    FHIRString(
                        "$type/${fhirId.localize(tenant)}$history",
                        reference?.id,
                        reference?.extension ?: listOf(),
                    ),
                type = Uri(type, extension = dataAuthorityExtension),
            )
        } ?: return this
    }
}
