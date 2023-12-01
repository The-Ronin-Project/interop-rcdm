package com.projectronin.interop.rcdm.transform.map

import com.projectronin.interop.common.enums.CodedEnum
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.validation.ConceptMapInvalidValueSetError
import com.projectronin.interop.rcdm.common.validation.getCodedEnumOrNull
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ConceptMapCodeableConcept
import com.projectronin.interop.rcdm.registry.model.ConceptMapCoding
import com.projectronin.interop.rcdm.registry.model.RoninConceptMap
import com.projectronin.interop.rcdm.transform.map.validation.FailedConceptMapLookupError
import com.projectronin.interop.tenant.config.model.Tenant
import java.time.LocalDateTime
import kotlin.reflect.KProperty1

/**
 * Base Mapper to handle common tasks when performing concept mapping on a [Resource] or [com.projectronin.interop.fhir.r4.element.Element]
 */
abstract class BaseMapper<C : Any>(protected val registryClient: NormalizationRegistryClient) {
    /**
     * Gets the concept mapping for the [codeableConcept] located at [elementProperty] on [resource]. This method also reports any lookup errors to the [validation].
     */
    protected fun <R : Resource<R>> getConceptMapping(
        codeableConcept: CodeableConcept,
        elementProperty: KProperty1<C, *>,
        resource: R,
        tenant: Tenant,
        parentContext: LocationContext,
        validation: Validation,
        forceCacheReloadTS: LocalDateTime?
    ): ConceptMapCodeableConcept? {
        val elementContext = LocationContext(elementProperty)
        return getConceptMapping(
            codeableConcept,
            elementContext,
            resource,
            tenant,
            parentContext,
            validation,
            forceCacheReloadTS
        )
    }

    /**
     * Gets the concept mapping for the [codeableConcept] located at [elementContext] on [resource]. This method also reports any lookup errors to the [validation].
     */
    protected fun <R : Resource<R>> getConceptMapping(
        codeableConcept: CodeableConcept,
        elementContext: LocationContext,
        resource: R,
        tenant: Tenant,
        parentContext: LocationContext,
        validation: Validation,
        forceCacheReloadTS: LocalDateTime?
    ): ConceptMapCodeableConcept? {
        val mappedCodeableConcept = registryClient.getConceptMapping(
            tenant.mnemonic,
            elementContext.toString(),
            codeableConcept,
            resource,
            forceCacheReloadTS
        )

        validation.apply {
            checkNotNull(
                mappedCodeableConcept,
                FailedConceptMapLookupError(
                    elementContext,
                    codeableConcept.coding.mapNotNull { it.code?.value }
                        .joinToString(", "),
                    "any $elementContext concept map for tenant '${tenant.mnemonic}'",
                    mappedCodeableConcept?.metadata
                ),
                parentContext
            )
        }
        return mappedCodeableConcept
    }

    /**
     * Gets the [T] enum for the [value] located at [elementProperty] on [resource]. This method also reports any lookup or conversion errors to the [validation].
     */
    protected inline fun <reified T, R : Resource<R>> getConceptMappingForEnum(
        value: String,
        elementProperty: KProperty1<C, *>,
        elementName: String,
        resource: R,
        extension: RoninExtension,
        tenant: Tenant,
        parentContext: LocationContext,
        validation: Validation,
        forceCacheReloadTS: LocalDateTime?
    ): ConceptMapCoding? where T : Enum<T>, T : CodedEnum<T> {
        val elementContext = LocationContext(elementProperty)
        val codingMapUri = RoninConceptMap.CODE_SYSTEMS.toUriString(tenant.mnemonic, elementContext.toString())

        val mappedCoding = registryClient.getConceptMappingForEnum(
            tenant.mnemonic,
            elementName,
            RoninConceptMap.CODE_SYSTEMS.toCoding(tenant.mnemonic, elementContext.toString(), value),
            T::class,
            extension.value,
            resource,
            forceCacheReloadTS
        )
        validation.apply {
            checkNotNull(
                mappedCoding,
                FailedConceptMapLookupError(
                    elementContext,
                    value,
                    codingMapUri,
                    mappedCoding?.metadata
                ),
                parentContext
            )
        }

        return mappedCoding?.let {
            val systemTarget = mappedCoding.coding.code?.value
            validation.apply {
                checkNotNull(
                    getCodedEnumOrNull<T>(systemTarget),
                    ConceptMapInvalidValueSetError(
                        elementContext,
                        codingMapUri,
                        value,
                        systemTarget,
                        mappedCoding.metadata
                    ),
                    parentContext
                )
            }
            mappedCoding
        }
    }
}
