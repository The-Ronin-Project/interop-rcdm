package com.projectronin.interop.rcdm.transform.profile.condition

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.dataAuthorityIdentifier
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninConditionProblemsAndHealthConcernsTransformerTest {
    private val transformer = RoninConditionProblemsAndHealthConcernsTransformer()

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "tenant"
        }

    @Test
    fun `returns correct profile`() {
        assertEquals(RoninProfile.CONDITION_PROBLEMS_CONCERNS, transformer.profile)
    }

    @Test
    fun `qualifies for problem-list-item category`() {
        val condition =
            Condition(
                id = Id("12345"),
                subject = Reference(display = FHIRString("subject")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.CONDITION_CATEGORY.uri,
                                        code = Code("problem-list-item"),
                                    ),
                                ),
                        ),
                    ),
            )

        assertTrue(transformer.qualifies(condition))
    }

    @Test
    fun `qualifies for health-concern category`() {
        val condition =
            Condition(
                id = Id("12345"),
                subject = Reference(display = FHIRString("subject")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri,
                                        code = Code("health-concern"),
                                    ),
                                ),
                        ),
                    ),
            )

        assertTrue(transformer.qualifies(condition))
    }

    @Test
    fun `does not qualify for unsupported category`() {
        val condition =
            Condition(
                id = Id("12345"),
                subject = Reference(display = FHIRString("subject")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.CONDITION_CATEGORY.uri,
                                        code = Code("encounter diagnosis"),
                                    ),
                                ),
                        ),
                    ),
            )

        assertFalse(transformer.qualifies(condition))
    }

    @Test
    fun `transforms required fields`() {
        val condition =
            Condition(
                id = Id("12345"),
                subject = Reference(display = FHIRString("subject")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.CONDITION_CATEGORY.uri,
                                        code = Code("problem-list-item"),
                                    ),
                                ),
                        ),
                    ),
            )

        val response = transformer.transform(condition, tenant)
        response!!

        val expectedCondition =
            Condition(
                id = Id("12345"),
                meta = Meta(profile = listOf(RoninProfile.CONDITION_PROBLEMS_CONCERNS.canonical)),
                identifier =
                    listOf(
                        Identifier(
                            type = CodeableConcepts.RONIN_FHIR_ID,
                            system = CodeSystem.RONIN_FHIR_ID.uri,
                            value = FHIRString("12345"),
                        ),
                        Identifier(
                            type = CodeableConcepts.RONIN_TENANT,
                            system = CodeSystem.RONIN_TENANT.uri,
                            value = FHIRString("tenant"),
                        ),
                        dataAuthorityIdentifier,
                    ),
                subject = Reference(display = FHIRString("subject")),
                category =
                    listOf(
                        CodeableConcept(
                            coding =
                                listOf(
                                    Coding(
                                        system = CodeSystem.CONDITION_CATEGORY.uri,
                                        code = Code("problem-list-item"),
                                    ),
                                ),
                        ),
                    ),
            )

        assertEquals(expectedCondition, response.resource)
        assertEquals(listOf<Resource<*>>(), response.embeddedResources)
    }
}
