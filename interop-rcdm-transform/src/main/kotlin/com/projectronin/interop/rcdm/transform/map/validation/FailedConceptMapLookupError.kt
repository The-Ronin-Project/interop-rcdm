package com.projectronin.interop.rcdm.transform.map.validation

import com.projectronin.interop.fhir.validate.FHIRError
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.ValidationIssueSeverity
import com.projectronin.interop.rcdm.common.metadata.ConceptMapMetadata
import kotlin.reflect.KProperty1

/**
 * Defines the error for a concept map lookup failure.
 * Either a concept map of this name was not found,
 * or the source value was not mapped to a target in this concept map.
 */
class FailedConceptMapLookupError(
    actualLocation: LocationContext,
    sourceValue: String,
    conceptMapName: String,
    metadata: List<ConceptMapMetadata>? = listOf(),
) :
    FHIRError(
            "NOV_CONMAP_LOOKUP",
            ValidationIssueSeverity.ERROR,
            "Tenant source value '$sourceValue' has no target defined in $conceptMapName",
            actualLocation,
            metadata,
        ) {
    /**
     * Creates an FailedConceptMapLookupError based off an explicit property.
     */
    constructor(actualLocation: KProperty1<*, *>, sourceValue: String, conceptMapName: String, metadata: List<ConceptMapMetadata>?) : this(
        LocationContext(actualLocation),
        sourceValue,
        conceptMapName,
        metadata,
    )
}
