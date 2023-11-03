package com.projectronin.interop.rcdm.transform.util

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.rcdm.common.enums.RoninExtension

/**
 * Creates an [Extension] list using the supplied [url] and [CodeableConcept] object. Supports the calling transform
 * by checking input for nulls and returning a list or emptyList. This supports simpler list arithmetic in the caller.
 */
fun CodeableConcept?.getExtensionOrEmptyList(roninExtension: RoninExtension): List<Extension> {
    return this?.let {
        listOf(
            Extension(
                url = Uri(roninExtension.value),
                value = DynamicValue(
                    type = DynamicValueType.CODEABLE_CONCEPT,
                    value = this
                )
            )
        )
    } ?: emptyList()
}
