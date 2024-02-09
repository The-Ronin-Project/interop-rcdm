package com.projectronin.interop.rcdm.registry.dependson

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.resource.ConceptMapDependsOn
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import mu.KotlinLogging
import java.util.Locale
import kotlin.reflect.KClass

/**
 * Base [DependsOnEvaluator] to simplify implementations.
 */
abstract class BaseDependsOnEvaluator<T : Resource<T>>(override val resourceType: KClass<T>) : DependsOnEvaluator<T> {
    private val logger = KotlinLogging.logger { }

    /**
     * Determines if the [resource] meets the [dependsOnExtension] or [dependsOnValue] for the [normalizedProperty].
     */
    protected abstract fun meetsDependsOn(
        resource: T,
        normalizedProperty: String,
        dependsOnValue: String?,
        dependsOnExtensionValue: DynamicValue<*>?,
    ): Boolean

    override fun meetsDependsOn(
        resource: T,
        dependsOn: List<ConceptMapDependsOn>,
    ): Boolean {
        return dependsOn.all {
            val property =
                it.property?.value?.lowercase(Locale.getDefault())
                    ?: throw IllegalStateException("Null property found for DependsOn: $it")
            val value = it.value ?: throw IllegalStateException("Null value found for DependsOn: $it")

            val sourceExtension =
                value.extension.singleOrNull { it.url == RoninExtension.CANONICAL_SOURCE_DATA_EXTENSION.uri }?.value
            if (sourceExtension == null && value.value == null) {
                throw IllegalStateException("DependsOn has a value with null data and no canonical source data extension: $it")
            }

            try {
                meetsDependsOn(resource, property, value.value, sourceExtension)
            } catch (e: Exception) {
                logger.warn(e) { "Exception processing $dependsOn" }
                false
            }
        }
    }
}
