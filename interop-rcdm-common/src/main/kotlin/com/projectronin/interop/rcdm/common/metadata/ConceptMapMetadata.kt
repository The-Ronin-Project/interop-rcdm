package com.projectronin.interop.rcdm.common.metadata

import com.projectronin.interop.fhir.validate.IssueMetadata

data class ConceptMapMetadata(
    override val registryEntryType: String,
    val conceptMapName: String,
    val conceptMapUuid: String,
    val version: String,
) : IssueMetadata
