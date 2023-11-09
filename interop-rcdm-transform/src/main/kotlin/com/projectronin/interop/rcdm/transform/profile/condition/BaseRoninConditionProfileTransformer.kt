package com.projectronin.interop.rcdm.transform.profile.condition

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.profile.ProfileTransformer
import com.projectronin.interop.rcdm.transform.util.getRoninIdentifiers
import com.projectronin.interop.rcdm.transform.util.qualifiesForValueSet
import com.projectronin.interop.tenant.config.model.Tenant
import kotlin.reflect.KClass

/**
 * Defines base attributes for transforming a [Condition].
 */
abstract class BaseRoninConditionProfileTransformer : ProfileTransformer<Condition>() {
    override val supportedResource: KClass<Condition> = Condition::class
    override val isDefault: Boolean = false

    open fun getQualifyingCategories(): List<Coding> = emptyList()

    override fun qualifies(resource: Condition): Boolean =
        resource.category.qualifiesForValueSet(getQualifyingCategories())

    override fun transformInternal(original: Condition, tenant: Tenant): TransformResponse<Condition>? {
        val transformed = original.copy(
            identifier = original.getRoninIdentifiers(tenant)
        )
        return TransformResponse(transformed)
    }
}
