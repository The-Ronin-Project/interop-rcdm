package com.projectronin.interop.rcdm.transform

import com.projectronin.interop.common.logmarkers.LogMarkers
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.rcdm.common.validation.ValidationClient
import com.projectronin.interop.rcdm.transform.localization.Localizer
import com.projectronin.interop.rcdm.transform.map.MappingService
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.normalization.Normalizer
import com.projectronin.interop.rcdm.transform.profile.ProfileTransformer
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class TransformManager(
    private val normalizer: Normalizer,
    private val mappingService: MappingService,
    private val validationClient: ValidationClient,
    profileTransformers: List<ProfileTransformer<*>>,
    private val localizer: Localizer,
) {
    private val transformersByResource = profileTransformers.groupBy { it.supportedResource }

    private val logger = KotlinLogging.logger { }

    /**
     * Transforms the [resource] based off configured Ronin profiles.
     */
    fun <R : Resource<R>> transformResource(
        resource: R,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime? = null,
    ): TransformResponse<R>? {
        // Normalize ensures we have a standardized form for the resource before processing it.
        val normalizedResource = normalizer.normalize(resource, tenant)

        // Map ensures that we concept map any elements on the resource that need to be mapped to a standard.
        val mappedResource = mapResource(normalizedResource, tenant, forceCacheReloadTS) ?: return null

        // Transform ensures the resource is properly updated per RCDM standards
        val (transformedResource, embeddedResources) = transformResource(mappedResource, tenant) ?: return null

        // Localize ensures any references are properly updated to meet the expectations for persistent storage
        val localizedResource = localizer.localize(transformedResource, tenant)
        return TransformResponse(localizedResource, embeddedResources)
    }

    private fun <R : Resource<R>> mapResource(
        resource: R,
        tenant: Tenant,
        forceCacheReloadTS: LocalDateTime?,
    ): R? {
        val (mappedResource, validation) = mappingService.map(resource, tenant, forceCacheReloadTS)
        if (validation.hasIssues()) {
            // If we did not get back a mapped resource, we need to report out against the original.
            val reportedResource = mappedResource ?: resource
            logger.warn(LogMarkers.VALIDATION_ISSUE) { "Failed to map ${resource.resourceType}" }
            validation.issues()
                .forEach { logger.warn(LogMarkers.VALIDATION_ISSUE) { it } } // makes mirth debugging much easier
            validationClient.reportIssues(validation, reportedResource, tenant.mnemonic)

            if (validation.hasErrors()) {
                return null
            }
        }

        return mappedResource
    }

    @Suppress("UNCHECKED_CAST")
    private fun <R : Resource<R>> transformResource(
        resource: R,
        tenant: Tenant,
    ): TransformResponse<R>? {
        val transformers = transformersByResource[resource::class] as List<ProfileTransformer<R>>
        val (defaultTransformers, nonDefaultTransformers) = transformers.partition { it.isDefault }

        val qualified = nonDefaultTransformers.filter { it.qualifies(resource) }
        return if (qualified.isEmpty()) {
            val defaultTransformer = defaultTransformers.singleOrNull()
            if (defaultTransformer == null) {
                logger.error { "No qualified or default transformers found for ${resource.resourceType}/${resource.id}" }
                null
            } else {
                defaultTransformer.transform(resource, tenant)
            }
        } else {
            var transformed = TransformResponse(resource)
            qualified.forEach {
                val response = it.transform(transformed.resource, tenant)
                if (response == null) {
                    // If we couldn't transform one of the qualified types, we need to fail.
                    return null
                } else {
                    transformed =
                        TransformResponse(response.resource, transformed.embeddedResources + response.embeddedResources)
                }
            }
            transformed
        }
    }
}
