package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.resource.DocumentReference
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.transform.model.TransformResponse
import com.projectronin.interop.rcdm.transform.util.getRoninIdentifiers
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class RoninDocumentReferenceTransformer : ProfileTransformer<DocumentReference>() {
    override val supportedResource: KClass<DocumentReference> = DocumentReference::class
    override val profile: RoninProfile = RoninProfile.DOCUMENT_REFERENCE
    override val rcdmVersion: RCDMVersion = RCDMVersion.V3_25_0
    override val profileVersion: Int = 5

    override fun transformInternal(
        original: DocumentReference,
        tenant: Tenant,
    ): TransformResponse<DocumentReference>? {
        val transformed =
            original.copy(
                identifier = original.getRoninIdentifiers(tenant),
            )

        return TransformResponse(transformed)
    }
}
