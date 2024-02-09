package com.projectronin.interop.rcdm.registry.dependson

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ConceptMapDependsOn
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BaseDependsOnEvaluatorTest {
    @Test
    fun `throws exception if no property is set on dependsOn`() {
        val evaluator = CapturingDependsOnEvaluator()
        val dependsOn =
            ConceptMapDependsOn(
                property = null,
                value = FHIRString("value"),
            )
        val exception = assertThrows<IllegalStateException> { evaluator.meetsDependsOn(mockk(), listOf(dependsOn)) }
        assertEquals("Null property found for DependsOn: $dependsOn", exception.message)
    }

    @Test
    fun `throws exception if no value is set on dependsOn`() {
        val evaluator = CapturingDependsOnEvaluator()
        val dependsOn =
            ConceptMapDependsOn(
                property = Uri("observation.code"),
                value = null,
            )
        val exception = assertThrows<IllegalStateException> { evaluator.meetsDependsOn(mockk(), listOf(dependsOn)) }
        assertEquals("Null value found for DependsOn: $dependsOn", exception.message)
    }

    @Test
    fun `throws exception when no value and no canonical source data extension found`() {
        val evaluator = CapturingDependsOnEvaluator()
        val dependsOn =
            ConceptMapDependsOn(
                property = Uri("observation.code"),
                value = FHIRString(null, extension = listOf()),
            )
        val exception = assertThrows<IllegalStateException> { evaluator.meetsDependsOn(mockk(), listOf(dependsOn)) }
        assertEquals(
            "DependsOn has a value with null data and no canonical source data extension: $dependsOn",
            exception.message,
        )
    }

    @Test
    fun `throws exception when no value and canonical source data extension has no value`() {
        val evaluator = CapturingDependsOnEvaluator()
        val dependsOn =
            ConceptMapDependsOn(
                property = Uri("observation.code"),
                value =
                    FHIRString(
                        null,
                        extension =
                            listOf(
                                Extension(
                                    url = RoninExtension.CANONICAL_SOURCE_DATA_EXTENSION.uri,
                                    value = null,
                                ),
                            ),
                    ),
            )
        val exception = assertThrows<IllegalStateException> { evaluator.meetsDependsOn(mockk(), listOf(dependsOn)) }
        assertEquals(
            "DependsOn has a value with null data and no canonical source data extension: $dependsOn",
            exception.message,
        )
    }

    @Test
    fun `treats properties as case-sensitive`() {
        val evaluator = CapturingDependsOnEvaluator()
        val dependsOn =
            ConceptMapDependsOn(
                property = Uri("Observation.cOdE"),
                value = FHIRString("value"),
            )
        val met = evaluator.meetsDependsOn(mockk(), listOf(dependsOn))
        assertTrue(met)

        assertEquals(mapOf("observation.code" to listOf("value")), evaluator.propertyToValue)
    }

    @Test
    fun `returns true when all dependsOn are met`() {
        val evaluator = CapturingDependsOnEvaluator()
        val dependsOn1 =
            ConceptMapDependsOn(
                property = Uri("Observation.cOdE"),
                value = FHIRString("value"),
            )
        val extensionValue2 = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "true".asFHIR()))
        val dependsOn2 =
            ConceptMapDependsOn(
                property = Uri("observation.code"),
                value =
                    FHIRString(
                        null,
                        extension =
                            listOf(
                                Extension(
                                    url = RoninExtension.CANONICAL_SOURCE_DATA_EXTENSION.uri,
                                    value = extensionValue2,
                                ),
                            ),
                    ),
            )
        val met = evaluator.meetsDependsOn(mockk(), listOf(dependsOn1, dependsOn2))
        assertTrue(met)

        assertEquals(mapOf("observation.code" to listOf("value", extensionValue2)), evaluator.propertyToValue)
    }

    @Test
    fun `returns false when single dependsOn is not met`() {
        val evaluator = CapturingDependsOnEvaluator()
        val extensionValue1 = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "value".asFHIR()))
        val dependsOn1 =
            ConceptMapDependsOn(
                property = Uri("Observation.cOdE"),
                value =
                    FHIRString(
                        null,
                        extension =
                            listOf(
                                Extension(
                                    url = RoninExtension.CANONICAL_SOURCE_DATA_EXTENSION.uri,
                                    value = extensionValue1,
                                ),
                            ),
                    ),
            )
        val extensionValue2 = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "false".asFHIR()))
        val dependsOn2 =
            ConceptMapDependsOn(
                property = Uri("observation.code"),
                value =
                    FHIRString(
                        null,
                        extension =
                            listOf(
                                Extension(
                                    url = RoninExtension.CANONICAL_SOURCE_DATA_EXTENSION.uri,
                                    value = extensionValue2,
                                ),
                            ),
                    ),
            )
        val met = evaluator.meetsDependsOn(mockk(), listOf(dependsOn1, dependsOn2))
        assertFalse(met)

        assertEquals(mapOf("observation.code" to listOf(extensionValue1, extensionValue2)), evaluator.propertyToValue)
    }

    class CapturingDependsOnEvaluator : BaseDependsOnEvaluator<Observation>(Observation::class) {
        val propertyToValue = mutableMapOf<String, MutableList<Any>>()

        override fun meetsDependsOn(
            resource: Observation,
            normalizedProperty: String,
            dependsOnValue: String?,
            dependsOnExtensionValue: DynamicValue<*>?,
        ): Boolean {
            val realValue = dependsOnExtensionValue ?: dependsOnValue!!

            propertyToValue.computeIfAbsent(normalizedProperty) { mutableListOf() }.add(realValue)

            val compareValue = (dependsOnExtensionValue?.value as? CodeableConcept)?.text?.value ?: dependsOnValue!!
            return compareValue != "false"
        }
    }
}
