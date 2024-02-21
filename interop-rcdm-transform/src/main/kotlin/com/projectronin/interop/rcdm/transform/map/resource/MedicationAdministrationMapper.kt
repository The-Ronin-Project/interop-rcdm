package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.resource.MedicationAdministration
import com.projectronin.interop.fhir.r4.valueset.MedicationAdministrationStatus
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.transform.map.BaseMapper
import com.projectronin.interop.rcdm.transform.map.MapResponse
import com.projectronin.interop.rcdm.transform.map.ResourceMapper
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import kotlin.reflect.KClass

@Component
class MedicationAdministrationMapper(registryClient: NormalizationRegistryClient) :
    ResourceMapper<MedicationAdministration>,
    BaseMapper<MedicationAdministration>(registryClient) {
    override val supportedResource: KClass<MedicationAdministration> = MedicationAdministration::class

    override fun map(
        resource: MedicationAdministration,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): MapResponse<MedicationAdministration> {
        val validation = Validation()
        val parentContext = LocationContext(MedicationAdministration::class)
        val mappedStatus =
            resource.status?.value?.let {
                getConceptMappingForEnum<MedicationAdministrationStatus, MedicationAdministration>(
                    it,
                    MedicationAdministration::status,
                    "${parentContext.element}.status",
                    resource,
                    RoninExtension.TENANT_SOURCE_MEDICATION_ADMINISTRATION_STATUS,
                    tenant,
                    parentContext,
                    validation,
                    forceCacheReloadTS,
                )
            }
        return MapResponse(
            mappedStatus?.let {
                resource.copy(
                    status = it.coding.code?.copy(id = it.coding.id),
                    extension = resource.extension + it.extension,
                )
            } ?: resource,
            validation,
        )
    }
}
