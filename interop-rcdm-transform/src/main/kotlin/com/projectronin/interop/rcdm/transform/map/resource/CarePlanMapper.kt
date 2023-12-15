package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.transform.map.BaseMapper
import com.projectronin.interop.rcdm.transform.map.MapResponse
import com.projectronin.interop.rcdm.transform.map.ResourceMapper
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import kotlin.reflect.KClass

@Component
class CarePlanMapper(registryClient: NormalizationRegistryClient) :
    ResourceMapper<CarePlan>, BaseMapper<CarePlan>(registryClient) {
    override val supportedResource: KClass<CarePlan> = CarePlan::class

    override fun map(
        resource: CarePlan,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): MapResponse<CarePlan> {
        val validation = Validation()
        val parentContext = LocationContext(CarePlan::class)
        val mappedCategoryAndExtension = mutableMapOf<CodeableConcept, Extension>()

        resource.category.forEach { category ->
            getConceptMapping(
                category,
                CarePlan::category,
                resource,
                tenant,
                parentContext,
                validation,
                forceCacheReloadTS,
            )?.let {
                mappedCategoryAndExtension.put(it.codeableConcept, it.extension)
            }
        }
        val mappedCarePlan =
            resource.copy(
                category = mappedCategoryAndExtension.keys.toList(),
                extension = resource.extension + mappedCategoryAndExtension.values,
            )
        return MapResponse(mappedCarePlan, validation)
    }
}
