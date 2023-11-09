package com.projectronin.interop.rcdm.transform.util

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.util.toFhirIdentifier
import com.projectronin.interop.rcdm.common.util.dataAuthorityIdentifier
import com.projectronin.interop.rcdm.common.util.getIdentifiers
import com.projectronin.interop.tenant.config.model.Tenant

fun <T : Resource<T>> T.getFhirIdentifiers(): List<Identifier> =
    id?.toFhirIdentifier()?.let { listOf(it) } ?: emptyList()

/**
 * Converts this tenant into the appropriate FHIR [Identifier]
 */
fun Tenant.toFhirIdentifier() =
    Identifier(
        type = CodeableConcepts.RONIN_TENANT,
        system = CodeSystem.RONIN_TENANT.uri,
        value = FHIRString(mnemonic)
    )

fun <T : Resource<T>> T.getRoninIdentifiers(tenant: Tenant): List<Identifier> {
    val identifierSet = mutableSetOf<Identifier>()
    identifierSet.addAll(this.getIdentifiers())
    identifierSet.addAll(this.getFhirIdentifiers())
    identifierSet.add(tenant.toFhirIdentifier())
    identifierSet.add(dataAuthorityIdentifier)
    return identifierSet.toSet().toList()
}
