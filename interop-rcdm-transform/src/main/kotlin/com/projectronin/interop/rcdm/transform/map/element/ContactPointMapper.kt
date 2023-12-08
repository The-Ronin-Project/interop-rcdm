package com.projectronin.interop.rcdm.transform.map.element

import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.transform.map.BaseMapper
import com.projectronin.interop.rcdm.transform.map.ElementMapper
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import kotlin.reflect.KClass

@Component
class ContactPointMapper(registryClient: NormalizationRegistryClient) : ElementMapper<ContactPoint>,
    BaseMapper<ContactPoint>(registryClient) {
    override val supportedElement: KClass<ContactPoint> = ContactPoint::class

    override fun <R : Resource<R>> map(
        element: ContactPoint,
        resource: R,
        tenant: Tenant,
        parentContext: LocationContext,
        validation: Validation,
        forceCacheReloadTS: LocalDateTime?,
    ): ContactPoint? {
        val mappedSystem =
            element.system?.value?.let {
                getConceptMappingForEnum<ContactPointSystem, R>(
                    it,
                    ContactPoint::system,
                    "${parentContext.element}.telecom.system",
                    resource,
                    RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM,
                    tenant,
                    parentContext,
                    validation,
                    forceCacheReloadTS,
                )?.let { response ->
                    Code(value = response.coding.code?.value, extension = listOf(response.extension))
                }
            } ?: element.system

        val mappedUse =
            element.use?.value?.let {
                getConceptMappingForEnum<ContactPointUse, R>(
                    it,
                    ContactPoint::use,
                    "${parentContext.element}.telecom.use",
                    resource,
                    RoninExtension.TENANT_SOURCE_TELECOM_USE,
                    tenant,
                    parentContext,
                    validation,
                    forceCacheReloadTS,
                )?.let { response ->
                    Code(value = response.coding.code?.value, extension = listOf(response.extension))
                }
            } ?: element.use

        return element.copy(
            system = mappedSystem,
            use = mappedUse,
        )
    }
}
