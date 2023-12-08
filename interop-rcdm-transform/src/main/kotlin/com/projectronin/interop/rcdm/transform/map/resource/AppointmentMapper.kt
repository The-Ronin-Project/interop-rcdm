package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.valueset.AppointmentStatus
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
class AppointmentMapper(registryClient: NormalizationRegistryClient) :
    ResourceMapper<Appointment>, BaseMapper<Appointment>(registryClient) {
    override val supportedResource: KClass<Appointment> = Appointment::class

    override fun map(
        resource: Appointment,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): MapResponse<Appointment> {
        val validation = Validation()
        val parentContext = LocationContext(Appointment::class)

        val mappedStatus =
            resource.status?.value?.let {
                getConceptMappingForEnum<AppointmentStatus, Appointment>(
                    it,
                    Appointment::status,
                    "${parentContext.element}.status",
                    resource,
                    RoninExtension.TENANT_SOURCE_APPOINTMENT_STATUS,
                    tenant,
                    parentContext,
                    validation,
                    forceCacheReloadTS,
                )
            }

        return MapResponse(
            mappedStatus?.let {
                resource.copy(
                    status = it.coding.code,
                    extension = resource.extension + it.extension,
                )
            } ?: resource,
            validation,
        )
    }
}
