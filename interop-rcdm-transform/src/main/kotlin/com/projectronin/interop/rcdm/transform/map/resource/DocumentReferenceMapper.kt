package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.resource.DocumentReference
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
class DocumentReferenceMapper(registryClient: NormalizationRegistryClient) : ResourceMapper<DocumentReference>,
    BaseMapper<DocumentReference>(registryClient) {
    override val supportedResource: KClass<DocumentReference> = DocumentReference::class

    override fun map(
        resource: DocumentReference,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): MapResponse<DocumentReference> {
        val validation = Validation()
        val parentContext = LocationContext(DocumentReference::class)

        val mappedType =
            resource.type?.let {
                getConceptMapping(
                    it,
                    DocumentReference::type,
                    resource,
                    tenant,
                    parentContext,
                    validation,
                    forceCacheReloadTS,
                )
            }

        return MapResponse(
            mappedType?.let {
                resource.copy(
                    type = it.codeableConcept,
                    extension = resource.extension + it.extension,
                )
            } ?: resource,
            validation,
        )
    }
}
