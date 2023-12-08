package com.projectronin.interop.rcdm.transform.model

import com.projectronin.interop.fhir.r4.resource.Resource

/**
 * Response from performing a transform operation including the transformed [resource] and any [embeddedResources] discovered or created during the transformation.
 */
data class TransformResponse<R : Resource<R>>(
    val resource: R,
    val embeddedResources: List<Resource<*>> = emptyList(),
)
