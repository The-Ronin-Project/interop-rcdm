package com.projectronin.interop.rcdm.registry.dependson

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.rcdm.registry.normalized
import org.springframework.stereotype.Component

/**
 * [DependsOnEvaluator] for [Observation]s.
 */
@Component
class ObservationDependsOnEvaluator : BaseDependsOnEvaluator<Observation>(Observation::class) {
    override fun meetsDependsOn(
        resource: Observation,
        normalizedProperty: String,
        dependsOnValue: String?,
        dependsOnExtensionValue: DynamicValue<*>?,
    ): Boolean {
        return when (normalizedProperty) {
            "observation.code.text" -> meetsCodeText(resource, dependsOnValue, dependsOnExtensionValue)
            "observation.code" -> meetsCode(resource, dependsOnValue, dependsOnExtensionValue)
            else -> false
        }
    }

    private fun meetsCodeText(
        resource: Observation,
        value: String?,
        extensionValue: DynamicValue<*>?,
    ): Boolean {
        if (extensionValue == null) {
            return resource.code?.text?.value == value
        }

        if (extensionValue.type != DynamicValueType.CODEABLE_CONCEPT) {
            throw IllegalStateException("Extension type is not valid for code text")
        }

        val codeableConcept = extensionValue.value as CodeableConcept
        return resource.code?.text?.value == codeableConcept.text?.value
    }

    private fun meetsCode(
        resource: Observation,
        value: String?,
        extensionValue: DynamicValue<*>?,
    ): Boolean {
        val codeableConcept =
            if (extensionValue == null) {
                JacksonManager.objectMapper.readValue<CodeableConcept>(value!!)
            } else {
                if (extensionValue.type != DynamicValueType.CODEABLE_CONCEPT) {
                    throw IllegalStateException("Extension type is not valid for code text")
                }

                extensionValue.value as CodeableConcept
            }

        return resource.code?.normalized() == codeableConcept.normalized()
    }
}
