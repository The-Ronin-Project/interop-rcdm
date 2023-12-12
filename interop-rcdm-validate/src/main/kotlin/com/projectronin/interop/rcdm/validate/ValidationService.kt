package com.projectronin.interop.rcdm.validate

import com.projectronin.interop.common.logmarkers.LogMarkers
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.element.Element
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.validation.ValidationClient
import com.projectronin.interop.rcdm.validate.element.ElementValidator
import com.projectronin.interop.rcdm.validate.profile.ProfileValidator
import mu.KotlinLogging
import org.springframework.stereotype.Service
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.jvmErasure

@Service
class ValidationService(
    private val validationClient: ValidationClient,
    profileValidators: List<ProfileValidator<*>>,
    elementValidators: List<ElementValidator<*>>,
) {
    private val logger = KotlinLogging.logger { }

    private val validatorsByResource = profileValidators.groupBy { it.supportedResource }
    private val validatorsByElement = elementValidators.associateBy { it.supportedElement }

    fun <R : Resource<R>> validate(
        resource: R,
        tenantMnemonic: String,
    ): ValidationResponse {
        val qualifiedValidators = getQualifiedValidators(resource)
        if (qualifiedValidators.isEmpty()) {
            throw IllegalStateException(
                "No qualified validators found for ${resource.resourceType} with id ${resource.id?.value} and meta ${resource.meta}",
            )
        }

        val locationContext = LocationContext(resource::class)

        val validation = Validation()

        // Validate the resource
        qualifiedValidators.forEach {
            validation.merge(it.validate(resource, locationContext))
        }

        val profiles = qualifiedValidators.map { it.profile }

        // Validate the properties on the resource
        validateProperties(resource, profiles, locationContext, validation)

        // Validate against R4
        qualifiedValidators.map { it.r4Validator }.toSet().forEach {
            validation.merge(it.validate(resource, locationContext))
        }

        if (validation.hasIssues()) {
            logger.warn(LogMarkers.VALIDATION_ISSUE) { "Failed to validate ${resource.resourceType}" }
            validation.issues()
                .forEach { logger.warn(LogMarkers.VALIDATION_ISSUE) { it } } // makes mirth debugging much easier
            validationClient.reportIssues(validation, resource, tenantMnemonic)
        }

        return if (validation.hasErrors()) {
            FailedValidation(validation.getErrorString()!!)
        } else {
            PassedValidation
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <R : Resource<R>> getQualifiedValidators(resource: R): List<ProfileValidator<R>> {
        val validators =
            validatorsByResource[resource::class] as? List<ProfileValidator<R>>
                ?: throw IllegalStateException("No Validators found for ${resource.resourceType}")
        return validators.filter { it.qualifies(resource) }
    }

    private fun <T : Any> validateProperties(
        element: T,
        profiles: List<RoninProfile>,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        val elementName = element.javaClass.simpleName

        element.javaClass.kotlin.memberProperties.forEach { property ->
            val kotlinType = property.returnType.jvmErasure

            val currentContext = parentContext.append(LocationContext(elementName, property.name))

            if (kotlinType.isSubclassOf(Element::class)) {
                val datatype = property.get(element) as? Element<*>
                datatype?.let {
                    validateElement(it, profiles, currentContext, validation)
                }
            } else if (kotlinType.isSubclassOf(DynamicValue::class)) {
                val dynamicValue = property.get(element) as? DynamicValue<*>
                dynamicValue?.let {
                    validateDynamicValue(dynamicValue, profiles, currentContext, validation)
                }
            } else if (kotlinType.isSubclassOf(Collection::class)) {
                val collection = property.get(element) as? Collection<*>
                collection?.let {
                    collection.filterIsInstance<Element<*>>()
                        .forEachIndexed { index, t ->
                            val indexedContext =
                                parentContext.append(LocationContext(elementName, "${property.name}[$index]"))

                            validateElement(t, profiles, indexedContext, validation)
                        }
                    collection.filterIsInstance<DynamicValue<*>>()
                        .forEachIndexed { index, t ->
                            val indexedContext =
                                parentContext.append(LocationContext(elementName, "${property.name}[$index]"))

                            validateDynamicValue(t, profiles, indexedContext, validation)
                        }
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <E : Element<E>> validateElement(
        element: Element<E>,
        profiles: List<RoninProfile>,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        val validator = validatorsByElement[element::class] as? ElementValidator<E>
        validator?.let {
            validation.merge(it.validate(element as E, profiles, parentContext))
        }

        validateProperties(element, profiles, parentContext, validation)
    }

    private fun validateDynamicValue(
        dynamicValue: DynamicValue<*>,
        profiles: List<RoninProfile>,
        parentContext: LocationContext,
        validation: Validation,
    ) {
        val element = dynamicValue.value as? Element<*>
        element?.let {
            validateElement(element, profiles, parentContext, validation)
        }
    }
}
