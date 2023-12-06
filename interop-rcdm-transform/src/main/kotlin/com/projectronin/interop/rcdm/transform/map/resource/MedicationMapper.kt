package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ConceptMapCodeableConcept
import com.projectronin.interop.rcdm.transform.map.BaseMapper
import com.projectronin.interop.rcdm.transform.map.MapResponse
import com.projectronin.interop.rcdm.transform.map.ResourceMapper
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import kotlin.reflect.KClass
@Component
class MedicationMapper(registryClient: NormalizationRegistryClient) :
    ResourceMapper<Medication>, BaseMapper<Medication>(registryClient) {
    override val supportedResource: KClass<Medication> = Medication::class
    override fun map(
        resource: Medication,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?
    ): MapResponse<Medication> {
        val validation = Validation()
        // val parentContext = LocationContext(Medication::class)

        // TODO: apply concept maps to get Medication.code
        // This just mocks the response we'd get from normalization client, once we want to start mapping
        // we'd replace this with
        //         val mappedCode = resource.code?.let {
        //             getConceptMapping(it, Medication::code, resource, tenant, parentContext, validation, forceCacheReloadTS)
        //         }
        val mappedCode = resource.code?.let {
            ConceptMapCodeableConcept(
                codeableConcept = it,
                extension = Extension(
                    url = RoninExtension.TENANT_SOURCE_MEDICATION_CODE.uri,
                    value = DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = it
                    )
                ),
                metadata = emptyList()
            )
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
