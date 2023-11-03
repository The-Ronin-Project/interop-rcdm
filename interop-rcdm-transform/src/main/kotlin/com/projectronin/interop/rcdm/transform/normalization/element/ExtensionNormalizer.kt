package com.projectronin.interop.rcdm.transform.normalization.element

import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.transform.introspection.TransformResult
import com.projectronin.interop.rcdm.transform.normalization.ElementNormalizer
import com.projectronin.interop.tenant.config.model.Tenant
import mu.KotlinLogging
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class ExtensionNormalizer : ElementNormalizer<Extension> {
    private val logger = KotlinLogging.logger { }

    override val elementType: KClass<Extension> = Extension::class

    override fun normalize(element: Extension, tenant: Tenant): TransformResult<Extension> {
        return if ((RoninExtension.values().find { it.value == element.url?.value } != null) ||
            (element.url != null && (element.value != null || element.extension.isNotEmpty()))
        ) {
            TransformResult(element)
        } else {
            logger.info { "Extension filtered out: $element" }
            TransformResult(null, true)
        }
    }
}
