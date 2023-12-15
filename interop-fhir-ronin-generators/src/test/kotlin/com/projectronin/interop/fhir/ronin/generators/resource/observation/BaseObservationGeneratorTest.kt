package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.DynamicValues
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.datatypes.reference
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.generators.resources.observationComponent
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.BaseGeneratorSpringTest
import com.projectronin.interop.fhir.ronin.generators.resource.TENANT_MNEMONIC
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.fhir.ronin.generators.util.tenantSourceConditionExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class BaseObservationGeneratorTest : BaseGeneratorSpringTest() {
    @Test
    fun `example use for roninObservation`() {
        // Create roninObs with attributes you need, provide the tenant
        val roninObservation =
            rcdmObservation(TENANT_MNEMONIC) {
                // to test an attribute like status - provide the value
                status of Code("registered-different")
                code of
                    codeableConcept {
                        coding of
                            listOf(
                                coding {
                                    system of "http://loinc.org"
                                    version of "0.01"
                                    code of Code("94499-1")
                                    display of "Respiratory viral pathogens DNA and RNA panel " +
                                        "- Respiratory specimen Qualitative by NAA with probe detection"
                                },
                            )
                        // text is kept if provided otherwise only a code.coding is generated
                        text of "Respiratory viral pathogens DNA and RNA panel"
                    }
            }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObservationJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObservation)

        // Uncomment to take a peek at the JSON
        // println(roninObservationJSON)
        assertNotNull(roninObservationJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmObservation - missing required fields generated`() {
        // create patient and observation for tenant
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val roninObservation = rcdmPatient.rcdmObservation {}

        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObservationJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObservation)

        // Uncomment to take a peek at the JSON
        // println(roninObservationJSON)
        assertNotNull(roninObservationJSON)
        assertNotNull(roninObservation.meta)
        assertEquals(
            roninObservation.meta!!.profile[0].value,
            RoninProfile.OBSERVATION.value,
        )
        assertNotNull(roninObservation.status)
        assertEquals(1, roninObservation.category.size)
        assertNotNull(roninObservation.code)
        assertNotNull(roninObservation.subject)
        assertNotNull(roninObservation.subject?.type?.extension)
        assertTrue(roninObservation.subject?.reference?.value?.split("/")?.first() in subjectBaseReferenceOptions)
        assertEquals("social-history", roninObservation.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObservation.category[0].coding[0].system)
        assertNotNull(roninObservation.status)
        assertNotNull(roninObservation.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `generates valid base roninObservation`() {
        val roninObservation = rcdmObservation(TENANT_MNEMONIC) {}
        assertNotNull(roninObservation.id)
        assertNotNull(roninObservation.meta)
        assertEquals(
            roninObservation.meta!!.profile[0].value,
            RoninProfile.OBSERVATION.value,
        )
        assertNull(roninObservation.implicitRules)
        assertNull(roninObservation.language)
        assertNull(roninObservation.text)
        assertEquals(0, roninObservation.contained.size)
        assertEquals(listOf(tenantSourceObservationCodeExtension), roninObservation.extension)
        assertEquals(0, roninObservation.modifierExtension.size)
        assertTrue(roninObservation.identifier.size >= 3)
        assertTrue(roninObservation.identifier.any { it.value == "test".asFHIR() })
        assertTrue(roninObservation.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObservation.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        val patientFHIRId =
            roninObservation.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant =
            roninObservation.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninObservation.id?.value.toString())
        assertEquals(TENANT_MNEMONIC, tenant)
        assertNotNull(roninObservation.status)
        assertEquals(1, roninObservation.category.size)
        assertNotNull(roninObservation.code)
        assertTrue(roninObservation.subject?.reference?.value?.split("/")?.first() in subjectBaseReferenceOptions)
        assertNotNull(roninObservation.subject?.type?.extension)
        assertEquals("social-history", roninObservation.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObservation.category[0].coding[0].system)
        assertNotNull(roninObservation.status)
        assertNotNull(roninObservation.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `generates base roninObservation with params`() {
        val roninObservation =
            rcdmObservation(TENANT_MNEMONIC) {
                id of Id("Id-Id")
                identifier of
                    listOf(
                        Identifier(
                            system = Uri("testsystem"),
                            value = "tomato".asFHIR(),
                        ),
                    )
                status of Code("fake-status")
                category of
                    listOf(
                        codeableConcept {
                            coding of
                                listOf(
                                    coding {
                                        system of "fake-system"
                                    },
                                )
                        },
                    )
                code of
                    codeableConcept {
                        text of "fake text goes here"
                    }
            }
        assertEquals("test-Id-Id", roninObservation.id?.value)
        assertNotNull(roninObservation.meta)
        assertEquals(
            roninObservation.meta!!.profile[0].value,
            RoninProfile.OBSERVATION.value,
        )
        assertNull(roninObservation.implicitRules)
        assertNull(roninObservation.language)
        assertNull(roninObservation.text)
        assertEquals(0, roninObservation.contained.size)
        assertEquals(listOf(tenantSourceObservationCodeExtension), roninObservation.extension)
        assertEquals(0, roninObservation.modifierExtension.size)
        assertTrue(roninObservation.identifier.size >= 3)
        assertTrue(roninObservation.identifier.any { it.value == "test".asFHIR() })
        assertTrue(roninObservation.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObservation.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObservation.status)
        assertEquals(1, roninObservation.category.size)
        assertNotNull(roninObservation.code)
        assertNotNull(roninObservation.subject?.type?.extension)
        assertTrue(roninObservation.subject?.reference?.value?.split("/")?.first() in subjectBaseReferenceOptions)
        assertNotNull(roninObservation.subject?.type?.extension)
        assertEquals("social-history", roninObservation.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObservation.category[0].coding[0].system)
        assertNotNull(roninObservation.status)
        assertNotNull(roninObservation.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `validates for base observation`() {
        val baseObs = rcdmObservation(TENANT_MNEMONIC) {}
        val validation = service.validate(baseObs, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertNotNull(baseObs.meta)
        assertNotNull(baseObs.identifier)
        assertTrue(baseObs.identifier.size >= 3)
        assertNotNull(baseObs.status)
        assertNotNull(baseObs.code)
        assertNotNull(baseObs.subject?.type?.extension)
        assertTrue(baseObs.subject?.reference?.value?.split("/")?.first() in subjectBaseReferenceOptions)
    }

    @Test
    fun `validation for base observation with existing identifier`() {
        val baseObs =
            rcdmObservation(TENANT_MNEMONIC) {
                identifier of
                    listOf(
                        Identifier(
                            system = Uri("testsystem"),
                            value = "tomato".asFHIR(),
                        ),
                    )
            }
        val validation = service.validate(baseObs, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertNotNull(baseObs.meta)
        assertNotNull(baseObs.identifier)
        assertEquals(4, baseObs.identifier.size)
        assertNotNull(baseObs.status)
        assertNotNull(baseObs.code)
        assertNotNull(baseObs.subject?.type?.extension)
        assertTrue(baseObs.subject?.reference?.value?.split("/")?.first() in subjectBaseReferenceOptions)
    }

    @Test
    fun `valid subject input - validate succeeds`() {
        val roninObservation =
            rcdmObservation(TENANT_MNEMONIC) {
                subject of rcdmReference("Patient", "456")
            }
        val validation = service.validate(roninObservation, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/456", roninObservation.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmObservation validates`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val baseObs = rcdmPatient.rcdmObservation {}
        val validation = service.validate(baseObs, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertNotNull(baseObs.meta)
        assertNotNull(baseObs.identifier)
        assertTrue(baseObs.identifier.size >= 3)
        assertNotNull(baseObs.status)
        assertNotNull(baseObs.code)
        assertNotNull(baseObs.subject?.type?.extension)
        assertTrue(baseObs.subject?.reference?.value?.split("/")?.first() in subjectBaseReferenceOptions)
    }

    @Test
    fun `rcdmPatient rcdmObservation - valid subject input overrides base patient - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val baseObs =
            rcdmPatient.rcdmObservation {
                subject of rcdmReference("Patient", "456")
            }
        val validation = service.validate(baseObs, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/456", baseObs.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmObservation - base patient overrides invalid subject input - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val baseObs =
            rcdmPatient.rcdmObservation {
                subject of reference("Patient", "456")
            }
        val validation = service.validate(baseObs, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/${rcdmPatient.id?.value}", baseObs.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmObservation - fhir id input for both - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) { id of "99" }
        val baseObs =
            rcdmPatient.rcdmObservation {
                id of "88"
            }
        val validation = service.validate(baseObs, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(3, baseObs.identifier.size)
        val values = baseObs.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 3)
        assertTrue(values.contains("88".asFHIR()))
        assertTrue(values.contains(TENANT_MNEMONIC.asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-88", baseObs.id?.value)
        assertEquals("test-99", rcdmPatient.id?.value)
        assertEquals("Patient/test-99", baseObs.subject?.reference?.value)
    }

    @Test
    fun `generates code extension when extensions are provided`() {
        val observation =
            rcdmBaseObservation(TENANT_MNEMONIC) {
                extension of tenantSourceConditionExtension
            }

        assertEquals(
            tenantSourceConditionExtension + tenantSourceObservationCodeExtension,
            observation.extension,
        )
    }

    @Test
    fun `generates value extension when codeable concept value is provided`() {
        val observation =
            rcdmBaseObservation(TENANT_MNEMONIC) {
                value of DynamicValues.codeableConcept(codeableConcept { })
            }

        assertEquals(
            listOf(tenantSourceObservationCodeExtension, tenantSourceObservationValueExtension),
            observation.extension,
        )
    }

    @Test
    fun `does not generate value extension when other value type is provided`() {
        val observation =
            rcdmBaseObservation(TENANT_MNEMONIC) {
                value of DynamicValues.string("value")
            }

        assertEquals(
            listOf(tenantSourceObservationCodeExtension),
            observation.extension,
        )
    }

    @Test
    fun `generates component code extension when component with code is provided`() {
        val observation =
            rcdmBaseObservation(TENANT_MNEMONIC) {
                component of
                    listOf(
                        observationComponent {
                            code of codeableConcept { }
                        },
                    )
            }

        assertEquals(
            listOf(tenantSourceObservationComponentCodeExtension),
            observation.component.first().extension,
        )
    }

    @Test
    fun `generates component value extension when component with codeable concept value is provided`() {
        val observation =
            rcdmBaseObservation(TENANT_MNEMONIC) {
                component of
                    listOf(
                        observationComponent {
                            code of codeableConcept { }
                            value of DynamicValues.codeableConcept(codeableConcept { })
                        },
                    )
            }

        assertEquals(
            listOf(tenantSourceObservationComponentCodeExtension, tenantSourceObservationComponentValueExtension),
            observation.component.first().extension,
        )
    }

    @Test
    fun `does not generate component value extension when component with other value type is provided`() {
        val observation =
            rcdmBaseObservation(TENANT_MNEMONIC) {
                component of
                    listOf(
                        observationComponent {
                            code of codeableConcept { }
                            value of DynamicValues.string("value")
                        },
                    )
            }

        assertEquals(
            listOf(tenantSourceObservationComponentCodeExtension),
            observation.component.first().extension,
        )
    }

    @Test
    fun `does not generate component value extension when component is provided without a value`() {
        val observation =
            rcdmBaseObservation(TENANT_MNEMONIC) {
                component of
                    listOf(
                        observationComponent {
                            code of codeableConcept { }
                            value of null
                        },
                    )
            }

        assertEquals(
            listOf(tenantSourceObservationComponentCodeExtension),
            observation.component.first().extension,
        )
    }
}
