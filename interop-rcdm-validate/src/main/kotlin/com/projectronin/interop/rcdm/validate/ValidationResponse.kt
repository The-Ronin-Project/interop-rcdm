package com.projectronin.interop.rcdm.validate

sealed interface ValidationResponse {
    val succeeded: Boolean
}

object PassedValidation : ValidationResponse {
    override val succeeded = true
}

data class FailedValidation(
    val failureMessage: String,
) : ValidationResponse {
    override val succeeded = false
}
