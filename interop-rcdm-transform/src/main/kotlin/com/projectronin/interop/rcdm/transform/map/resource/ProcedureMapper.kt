package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.resource.Procedure
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
class ProcedureMapper(registryClient: NormalizationRegistryClient) :
    ResourceMapper<Procedure>, BaseMapper<Procedure>(registryClient) {
    override val supportedResource: KClass<Procedure> = Procedure::class

    override fun map(
        resource: Procedure,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): MapResponse<Procedure> {
        val validation = Validation()
        val parentContext = LocationContext(Procedure::class)
        val newExtensions = mutableListOf<Extension>()

        // Procedure.code is a single CodeableConcept
        val mappedCodePair =
            resource.code?.let { code ->
                getConceptMapping(
                    code,
                    Procedure::code,
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

        val mappedProcedure =
            resource.copy(
                code = mappedCodePair,
                extension = resource.extension + newExtensions,
            )

        return MapResponse(mappedProcedure, validation)
    }
}
