package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.transform.map.BaseMapper
import com.projectronin.interop.rcdm.transform.map.MapResponse
import com.projectronin.interop.rcdm.transform.map.ResourceMapper
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import kotlin.reflect.KClass

@Component
class ObservationMapper(registryClient: NormalizationRegistryClient) : ResourceMapper<Observation>,
    BaseMapper<Observation>(registryClient) {
    override val supportedResource: KClass<Observation> = Observation::class

    override fun map(
        resource: Observation,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): MapResponse<Observation> {
        val validation = Validation()
        val parentContext = LocationContext(Observation::class)

        val newExtensions = mutableListOf<Extension>()

        val mappedCode =
            resource.code?.let { code ->
                getConceptMapping(
                    code,
                    Observation::code,
                    resource,
                    tenant,
                    parentContext,
                    validation,
                    forceCacheReloadTS,
                )?.let {
                    newExtensions.add(it.extension)
                    it.codeableConcept
                }
            } ?: resource.code

        val mappedValue =
            resource.value?.let { value ->
                if (value.type == DynamicValueType.CODEABLE_CONCEPT) {
                    val valueCodeableConcept = value.value as CodeableConcept

                    getConceptMapping(
                        valueCodeableConcept,
                        LocationContext("Observation", "valueCodeableConcept"),
                        resource,
                        tenant,
                        parentContext,
                        validation,
                        forceCacheReloadTS,
                    )?.let {
                        newExtensions.add(it.extension)
                        DynamicValue(DynamicValueType.CODEABLE_CONCEPT, it.codeableConcept)
                    }
                } else {
                    null
                }
            } ?: resource.value

        val mappedComponents =
            resource.component.mapIndexed { index, component ->
                val componentContext = parentContext.append(LocationContext("", "component[$index]"))

                val newComponentExtensions = mutableListOf<Extension>()

                val mappedComponentCode =
                    component.code?.let { code ->
                        getConceptMapping(
                            code,
                            LocationContext("Observation.component", "code"),
                            resource,
                            tenant,
                            componentContext,
                            validation,
                            forceCacheReloadTS,
                        )?.let {
                            newComponentExtensions.add(it.extension)
                            it.codeableConcept
                        }
                    } ?: component.code

                val mappedComponentValue =
                    component.value?.let { value ->
                        if (value.type == DynamicValueType.CODEABLE_CONCEPT) {
                            val valueCodeableConcept = value.value as CodeableConcept

                            getConceptMapping(
                                valueCodeableConcept,
                                LocationContext("Observation.component", "valueCodeableConcept"),
                                resource,
                                tenant,
                                componentContext,
                                validation,
                                forceCacheReloadTS,
                            )?.let {
                                newComponentExtensions.add(it.extension)
                                DynamicValue(DynamicValueType.CODEABLE_CONCEPT, it.codeableConcept)
                            }
                        } else {
                            null
                        }
                    } ?: component.value

                if (newComponentExtensions.isEmpty()) {
                    component
                } else {
                    component.copy(
                        code = mappedComponentCode,
                        value = mappedComponentValue,
                        extension = component.extension + newComponentExtensions,
                    )
                }
            }

        val mapped =
            resource.copy(
                code = mappedCode,
                value = mappedValue,
                component = mappedComponents,
                extension = resource.extension + newExtensions,
            )
        return MapResponse(mapped, validation)
    }
}
