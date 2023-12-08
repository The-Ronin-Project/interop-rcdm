package com.projectronin.interop.rcdm.transform.introspection

data class TransformResult<T>(
    val element: T?,
    val removeFromElement: Boolean = false,
)
