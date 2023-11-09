package com.projectronin.interop.rcdm.common.util

import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.resource.Resource

@Suppress("UNCHECKED_CAST")
fun <T : Resource<T>> T.getIdentifiersOrNull(): List<Identifier>? {
    return runCatching {
        val field = this::class.java.getDeclaredField("identifier")
        field.isAccessible = true
        field.get(this) as List<Identifier>
    }.getOrNull()
}

fun <T : Resource<T>> T.getIdentifiers(): List<Identifier> {
    return getIdentifiersOrNull() ?: emptyList()
}
