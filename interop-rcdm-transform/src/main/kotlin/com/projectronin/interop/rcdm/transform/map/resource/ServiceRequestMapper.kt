package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.resource.ServiceRequest
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
class ServiceRequestMapper(registryClient: NormalizationRegistryClient) :
    ResourceMapper<ServiceRequest>, BaseMapper<ServiceRequest>(registryClient) {
    override val supportedResource: KClass<ServiceRequest> = ServiceRequest::class

    override fun map(
        resource: ServiceRequest,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): MapResponse<ServiceRequest> {
        val validation = Validation()
        val parentContext = LocationContext(ServiceRequest::class)
        val newExtensions = mutableListOf<Extension>()

        val mappedCategory =
            resource.category.firstOrNull()?.let { category ->
                getConceptMapping(
                    category,
                    ServiceRequest::category,
                    resource,
                    tenant,
                    parentContext,
                    validation,
                    forceCacheReloadTS,
                )?.let {
                    newExtensions.add(it.extension)
                    it.codeableConcept
                }
            } ?: resource.category.first()

        val mappedCode =
            resource.code?.let { code ->
                getConceptMapping(
                    code,
                    ServiceRequest::code,
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

        val mappedServiceRequest =
            resource.copy(
                code = mappedCode,
                category = listOf(mappedCategory),
                extension = resource.extension + newExtensions,
            )

        return MapResponse(mappedServiceRequest, validation)
    }
}
