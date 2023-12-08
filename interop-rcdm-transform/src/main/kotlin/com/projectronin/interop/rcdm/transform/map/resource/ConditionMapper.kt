package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.transform.map.BaseMapper
import com.projectronin.interop.rcdm.transform.map.MapResponse
import com.projectronin.interop.rcdm.transform.map.ResourceMapper
import com.projectronin.interop.rcdm.transform.util.getExtensionOrEmptyList
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import kotlin.reflect.KClass

/**
 * Mapper for [Condition]s.
 */
@Component
class ConditionMapper(
    registryClient: NormalizationRegistryClient,
    @Value("\${ronin.fhir.conditions.tenantsNotConditionMapped:mdaoc,1xrekpx5}")
    tenantsNotConditionMappedString: String,
) : ResourceMapper<Condition>, BaseMapper<Condition>(registryClient) {
    private val tenantsNotConditionMapped = tenantsNotConditionMappedString.split(",")

    override val supportedResource: KClass<Condition> = Condition::class

    override fun map(
        resource: Condition,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): MapResponse<Condition> {
        val validation = Validation()
        val parentContext = LocationContext(Condition::class)

        if (tenant.mnemonic in tenantsNotConditionMapped) {
            val tenantSourceConditionCode =
                resource.code.getExtensionOrEmptyList(RoninExtension.TENANT_SOURCE_CONDITION_CODE)

            val mapped =
                resource.copy(
                    extension = resource.extension + tenantSourceConditionCode,
                )
            return MapResponse(mapped, validation)
        }
        // Condition.code is a single CodeableConcept
        val mappedCode =
            resource.code?.let {
                getConceptMapping(it, Condition::code, resource, tenant, parentContext, validation, forceCacheReloadTS)
            }

        return MapResponse(
            mappedCode?.let {
                resource.copy(
                    code = it.codeableConcept,
                    extension = resource.extension + it.extension,
                )
            } ?: resource,
            validation,
        )
    }
}
