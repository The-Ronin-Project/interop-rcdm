package com.projectronin.interop.rcdm.transform.map

import com.projectronin.interop.common.reflect.copy
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.primitive.Primitive
import com.projectronin.interop.fhir.r4.element.Element
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import kotlin.reflect.KProperty1
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

/**
 * Service that manages the mapping workflow for resources.
 */
@Component
class MappingService(
    resourceMappers: List<ResourceMapper<*>>,
    elementMappers: List<ElementMapper<*>>
) {
    private val mappersByResource = resourceMappers.associateBy { it.supportedResource }
    private val mappersByElement = elementMappers.associateBy { it.supportedElement }

    /**
     * Maps the [resource] for the [tenant].
     */
    fun <R : Resource<R>> map(resource: R, tenant: Tenant, forceCacheReloadTS: LocalDateTime?): MapResponse<R> {
        // First we map the resource itself. This relies on a specific ResourceMapper for the resource type.
        val (resourceMapped, validation) = mapResource(resource, tenant, forceCacheReloadTS)

        // Now we ensure the properties for the resource are also mapped as appropriate per defined ElementMappers.
        val propertyMapped = resourceMapped?.let { mapResourceProperties(it, tenant, validation, forceCacheReloadTS) }
        return MapResponse(propertyMapped, validation)
    }

    /**
     * Maps the [resource] for the [tenant].
     */
    @Suppress("UNCHECKED_CAST")
    private fun <R : Resource<R>> mapResource(
        resource: R,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): MapResponse<R> {
        val resourceMapper = mappersByResource[resource::class] as? ResourceMapper<R>
            ?: throw IllegalStateException("No ResourceMapper defined for ${resource.resourceType}")
        return resourceMapper.map(resource, tenant, forceCacheReloadTS)
    }

    /**
     * Maps the properties for the [resource], returning an updated value containing any mapped elements.
     */
    private fun <R : Resource<R>> mapResourceProperties(
        resource: R,
        tenant: Tenant,
        validation: Validation,
        forceCacheReloadTS: LocalDateTime?
    ): R {
        val resourceContext = LocationContext(resource::class)

        var mappedResource = resource
        resource.javaClass.kotlin.memberProperties.forEach { property ->
            // We never map the "contained" values of a resource.
            if (property.name == "contained") return@forEach

            handleProperty(
                property,
                resource,
                resource,
                tenant,
                validation,
                resourceContext,
                forceCacheReloadTS
            )?.let { newValue ->
                mappedResource = copy(mappedResource, mapOf(property.name to newValue))
            }
        }

        return mappedResource
    }

    /**
     * Maps the [element] for the [resource] based off any defined [ElementMapper]s.
     */
    @Suppress("UNCHECKED_CAST")
    private fun <E : Element<E>, R : Resource<R>> mapElement(
        element: Element<E>,
        resource: R,
        tenant: Tenant,
        elementContext: LocationContext,
        validation: Validation,
        forceCacheReloadTS: LocalDateTime?
    ): Element<E>? {
        val elementMapper = mappersByElement[element::class] as? ElementMapper<E>
        val currentElement = element as E

        // Map the element itself against any ElementMapper configured for this element type.
        val mappedElement = if (elementMapper == null) {
            currentElement
        } else {
            elementMapper.map(currentElement, resource, tenant, elementContext, validation, forceCacheReloadTS)
        }

        return mappedElement?.let {
            // If the element did not fail mapping, we need to attempt to map its properties as well to handle any nested values.
            mapElementProperties(mappedElement, resource, tenant, elementContext, validation, forceCacheReloadTS)
        }
    }

    /**
     * Maps the properties for the [element] in [resource], returning an updated value containing any mapped elements.
     */
    private fun <E : Element<E>, R : Resource<R>> mapElementProperties(
        element: E,
        resource: R,
        tenant: Tenant,
        parentContext: LocationContext,
        validation: Validation,
        forceCacheReloadTS: LocalDateTime?
    ): Element<E> {
        var mappedElement = element
        element.javaClass.kotlin.memberProperties.forEach { property ->
            handleProperty(
                property,
                element,
                resource,
                tenant,
                validation,
                parentContext,
                forceCacheReloadTS
            )?.let { newValue ->
                mappedElement = copy(mappedElement, mapOf(property.name to newValue))
            }
        }

        return mappedElement
    }

    /**
     * Handles mapping for the supplied [property] on [element] for the [resource], returning the new mapped value or null.
     */
    private fun <T : Any, R : Resource<R>> handleProperty(
        property: KProperty1<T, *>,
        element: T,
        resource: R,
        tenant: Tenant,
        validation: Validation,
        parentContext: LocationContext,
        forceCacheReloadTS: LocalDateTime?
    ): Any? {
        val kotlinType = property.returnType.jvmErasure

        return if (kotlinType.isSubclassOf(Collection::class)) {
            val collection = property.get(element) as? Collection<*>
            collection?.let {
                val mappedCollection = collection.mapIndexed { index, item ->
                    val indexedContext = parentContext.append(LocationContext("", "${property.name}[$index]"))
                    item?.let {
                        handleValue(item, resource, tenant, validation, indexedContext, forceCacheReloadTS) ?: item
                    }
                }
                if (mappedCollection == collection) null else mappedCollection
            }
        } else {
            property.get(element)?.let {
                val currentContext = parentContext.append(LocationContext("", property.name))
                handleValue(it, resource, tenant, validation, currentContext, forceCacheReloadTS)
            }
        }
    }

    /**
     * Handles mapping for a specific [value] from [resource], returning either the new mapped value or null.
     */
    private fun <T : Any, R : Resource<R>> handleValue(
        value: T,
        resource: R,
        tenant: Tenant,
        validation: Validation,
        valueContext: LocationContext,
        forceCacheReloadTS: LocalDateTime?
    ): Any? {
        val kotlinType = value::class

        return if (kotlinType.isSubclassOf(Primitive::class)) {
            // Primitives are also Elements, so tracking these separately from them.
            null
        } else if (kotlinType.isSubclassOf(Element::class)) {
            val element = value as? Element<*>
            element?.let { mapElement(element, resource, tenant, valueContext, validation, forceCacheReloadTS) }
        } else if (kotlinType.isSubclassOf(DynamicValue::class)) {
            val dynamicValue = value as? DynamicValue<*>
            dynamicValue?.let {
                val dynamicPrimitive = dynamicValue.value as? Primitive<*, *>
                if (dynamicPrimitive == null) {
                    val dynamicElement = dynamicValue.value as? Element<*>
                    dynamicElement?.let {
                        mapElement(
                            dynamicElement,
                            resource,
                            tenant,
                            valueContext,
                            validation,
                            forceCacheReloadTS
                        )?.let { element ->
                            DynamicValue(dynamicValue.type, element)
                        }
                    }
                } else {
                    null
                }
            }
        } else {
            null
        }
    }
}
