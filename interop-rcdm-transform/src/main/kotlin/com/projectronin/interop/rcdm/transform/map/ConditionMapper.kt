package com.projectronin.interop.rcdm.transform.map

import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.transform.map.validation.FailedConceptMapLookupError
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
    private val registryClient: NormalizationRegistryClient,
    @Value("\${ronin.fhir.conditions.tenantsNotConditionMapped:mdaoc,1xrekpx5}")
    tenantsNotConditionMappedString: String
) : ResourceMapper<Condition> {
    private val tenantsNotConditionMapped = tenantsNotConditionMappedString.split(",")

    override val supportedResource: KClass<Condition> = Condition::class

    override fun map(resource: Condition, tenant: Tenant, forceCacheReloadTS: LocalDateTime?): MapResponse<Condition> {
        val validation = Validation()
        val parentContext = LocationContext(Condition::class)

        if (tenant.mnemonic in tenantsNotConditionMapped) {
            val tenantSourceConditionCode =
                resource.code.getExtensionOrEmptyList(RoninExtension.TENANT_SOURCE_CONDITION_CODE)

            val mapped = resource.copy(
                extension = resource.extension + tenantSourceConditionCode
            )
            return MapResponse(mapped, validation)
        }
        // Condition.code is a single CodeableConcept
        val mappedCode = resource.code?.let { code ->
            val conditionCode = registryClient.getConceptMapping(
                tenant.mnemonic,
                "Condition.code",
                code,
                resource,
                forceCacheReloadTS
            )
            // validate the mapping we got, use code value to report issues
            validation.apply {
                checkNotNull(
                    conditionCode,
                    FailedConceptMapLookupError(
                        LocationContext(Condition::code),
                        code.coding.mapNotNull { it.code?.value }
                            .joinToString(", "),
                        "any Condition.code concept map for tenant '${tenant.mnemonic}'",
                        conditionCode?.metadata
                    ),
                    parentContext
                )
            }
            conditionCode
        }

        return MapResponse(
            mappedCode?.let {
                resource.copy(
                    code = it.codeableConcept,
                    extension = resource.extension + it.extension
                )
            } ?: resource,
            validation
        )
    }
}
