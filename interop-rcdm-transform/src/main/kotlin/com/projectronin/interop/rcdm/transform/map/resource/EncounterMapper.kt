package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.resource.Encounter
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.transform.map.BaseMapper
import com.projectronin.interop.rcdm.transform.map.MapResponse
import com.projectronin.interop.rcdm.transform.map.ResourceMapper
import com.projectronin.interop.rcdm.transform.util.getExtensionOrEmptyList
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import kotlin.reflect.KClass

@Component
class EncounterMapper(registryClient: NormalizationRegistryClient) :
    ResourceMapper<Encounter>, BaseMapper<Encounter>(registryClient) {
    override val supportedResource: KClass<Encounter> = Encounter::class
    override fun map(
        resource: Encounter,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): MapResponse<Encounter> {
        val validation = Validation()
        val classExtension = resource.`class`.getExtensionOrEmptyList(RoninExtension.TENANT_SOURCE_ENCOUNTER_CLASS)

        return MapResponse(
            resource.let {
                resource.copy(
                    extension = resource.extension + classExtension
                )
            },
            validation
        )
    }
}
