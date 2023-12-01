package com.projectronin.interop.rcdm.transform.profile.observation

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.rcdm.common.util.qualifiesForValueSet
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.profile.ProfileTransformer
import com.projectronin.interop.rcdm.transform.util.getRoninIdentifiers
import com.projectronin.interop.tenant.config.model.Tenant
import kotlin.reflect.KClass

abstract class BaseRoninObservationTransformer(protected val registryClient: NormalizationRegistryClient) :
    ProfileTransformer<Observation>() {
    override val supportedResource: KClass<Observation> = Observation::class
    override val isDefault: Boolean = false

    open fun getQualifyingCategories(): List<Coding> = emptyList()

    open fun getQualifyingCodes(): List<Coding> =
        registryClient.getRequiredValueSet("Observation.code", profile.value).codes

    protected open fun getTransformedBodySite(bodySite: CodeableConcept?) = bodySite

    override fun qualifies(resource: Observation): Boolean {
        return resource.category.qualifiesForValueSet(getQualifyingCategories()) &&
            resource.code.qualifiesForValueSet(getQualifyingCodes())
    }

    override fun transformInternal(original: Observation, tenant: Tenant): TransformResponse<Observation>? {
        val transformed = original.copy(
            identifier = original.getRoninIdentifiers(tenant),
            bodySite = getTransformedBodySite(original.bodySite)
        )
        return TransformResponse(transformed)
    }
}
