package com.projectronin.interop.rcdm.transform.normalization.element

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.rcdm.transform.introspection.TransformResult
import com.projectronin.interop.rcdm.transform.normalization.ElementNormalizer
import com.projectronin.interop.tenant.config.model.Tenant
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class CodeableConceptNormalizer : ElementNormalizer<CodeableConcept> {
    override val elementType: KClass<CodeableConcept> = CodeableConcept::class

    override fun normalize(element: CodeableConcept, tenant: Tenant): TransformResult<CodeableConcept> {
        // If text is populated on the codeable concept already, return as is.
        if (element.text?.value?.isNotEmpty() == true) {
            return TransformResult(element)
        }

        // When text isn't populated, pull from the single coding, or the single user selected coding
        val selectedCoding =
            element.coding.singleOrNull { it.userSelected?.value == true }
                ?: element.coding.singleOrNull()
        val normalizedCodeableConcept = if (selectedCoding != null && selectedCoding.display?.value?.isNotEmpty() == true) {
            element.copy(text = selectedCoding.display)
        } else {
            element
        }
        return TransformResult(normalizedCodeableConcept)
    }
}
