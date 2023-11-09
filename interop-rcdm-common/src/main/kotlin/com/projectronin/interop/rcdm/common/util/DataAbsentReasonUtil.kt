package com.projectronin.interop.rcdm.common.util

import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.element.Element

val dataAbsentReasonExtension = listOf(
    Extension(
        url = Uri("http://hl7.org/fhir/StructureDefinition/data-absent-reason"),
        value = DynamicValue(
            type = DynamicValueType.CODE,
            value = Code("unknown")
        )
    )
)

/**
 * True when any [Element] in the [List] contains a data absent reason in its extensions.
 */
fun <T : Element<T>> List<T>?.hasDataAbsentReason(): Boolean {
    return this?.any { it.hasDataAbsentReason() }
        ?: false
}

/**
 * True when an [Element] contains a data absent reason in its extensions.
 */
fun <T : Element<T>> T?.hasDataAbsentReason(): Boolean {
    return this?.extension?.any { it.url?.value == "http://hl7.org/fhir/StructureDefinition/data-absent-reason" }
        ?: false
}
