package com.projectronin.interop.rcdm.transform.map

import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.validate.Validation

data class MapResponse<R : Resource<R>>(
    val mappedResource: R?,
    val validation: Validation,
)
