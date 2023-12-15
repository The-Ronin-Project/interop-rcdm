package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.DynamicValues
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.datatypes.quantity
import com.projectronin.interop.fhir.generators.datatypes.reference
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.generators.resources.observationComponent
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.ronin.generators.resource.BaseGeneratorSpringTest
import com.projectronin.interop.fhir.ronin.generators.resource.TENANT_MNEMONIC
import com.projectronin.interop.fhir.ronin.generators.resource.normalizationRegistryClient
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
import com.projectronin.interop.fhir.ronin.generators.util.rcdmReference
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.metadata.ValueSetMetadata
import com.projectronin.interop.rcdm.registry.model.ValueSetList
import io.mockk.every
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
class RoninBloodPressureGeneratorTest : BaseGeneratorSpringTest() {
    @BeforeEach
    fun normalizationClient() {
        every { normalizationRegistryClient.getRequiredValueSet("Observation.component:systolic.code", any()) } returns systolicCoding
        every { normalizationRegistryClient.getRequiredValueSet("Observation.component:diastolic.code", any()) } returns diastolicCoding
        every {
            normalizationRegistryClient.getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_BLOOD_PRESSURE.value)
        } returns possibleBloodPressureCodes
    }

    private val systolicCoding =
        ValueSetList(
            listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("8480-6"))),
            ValueSetMetadata(
                registryEntryType = "value_set",
                valueSetName = "systolicbloodpressure",
                valueSetUuid = "800f4e2b-d716-44e8-9183-87d3ed8cba9b",
                version = "1",
            ),
        )
    private val diastolicCoding =
        ValueSetList(
            listOf(Coding(system = CodeSystem.LOINC.uri, code = Code("8462-4"))),
            ValueSetMetadata(
                registryEntryType = "value_set",
                valueSetName = "diastolicbloodpressure",
                valueSetUuid = "0718bdad-3386-4193-8b47-1cf9220b4bb3",
                version = "1",
            ),
        )

    @Test
    fun `example use for roninObservationBloodPressure`() {
        // Create Blood Pressure Obs with attributes you need, provide the tenant
        val roninObsBloodPressure =
            rcdmObservationBloodPressure(TENANT_MNEMONIC) {
                // if you want to test for a specific status
                status of Code("registered-different")
                // test for a new or different code
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
        val roninObsBloodPressureJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsBloodPressure)

        // Uncomment to take a peek at the JSON
        // println(roninObsBloodPressureJSON)
        assertNotNull(roninObsBloodPressureJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmObservationBloodPressure - missing required fields generated`() {
        // create patient and observation for tenant
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val roninObsBloodPressure = rcdmPatient.rcdmObservationBloodPressure {}
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsBloodPressureJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsBloodPressure)

        // Uncomment to take a peek at the JSON
        // println(roninObsBloodPressureJSON)
        assertNotNull(roninObsBloodPressureJSON)
        assertNotNull(roninObsBloodPressure.meta)
        assertEquals(
            roninObsBloodPressure.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_BLOOD_PRESSURE.value,
        )
        assertNotNull(roninObsBloodPressure.status)
        assertEquals(1, roninObsBloodPressure.category.size)
        assertNotNull(roninObsBloodPressure.code)
        assertNotNull(roninObsBloodPressure.subject)
        assertNotNull(roninObsBloodPressure.subject?.type?.extension)
        assertEquals("vital-signs", roninObsBloodPressure.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsBloodPressure.category[0].coding[0].system)
        assertNotNull(roninObsBloodPressure.status)
        assertNotNull(roninObsBloodPressure.code?.coding?.get(0)?.code?.value)
        assertNotNull(roninObsBloodPressure.id)
        val patientFHIRId =
            roninObsBloodPressure.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant =
            roninObsBloodPressure.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninObsBloodPressure.id?.value.toString())
    }

    @Test
    fun `generates valid roninObservationBloodPressure Observation`() {
        val roninObsBloodPressure = rcdmObservationBloodPressure(TENANT_MNEMONIC) {}
        assertNotNull(roninObsBloodPressure.id)
        assertNotNull(roninObsBloodPressure.meta)
        assertEquals(
            roninObsBloodPressure.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_BLOOD_PRESSURE.value,
        )
        assertNull(roninObsBloodPressure.implicitRules)
        assertNull(roninObsBloodPressure.language)
        assertNull(roninObsBloodPressure.text)
        assertEquals(0, roninObsBloodPressure.contained.size)
        assertEquals(1, roninObsBloodPressure.extension.size)
        assertEquals(0, roninObsBloodPressure.modifierExtension.size)
        assertTrue(roninObsBloodPressure.identifier.size >= 3)
        assertTrue(roninObsBloodPressure.identifier.any { it.value == "test".asFHIR() })
        assertTrue(roninObsBloodPressure.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObsBloodPressure.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObsBloodPressure.status)
        assertEquals(1, roninObsBloodPressure.category.size)
        assertNotNull(roninObsBloodPressure.code)
        assertNotNull(roninObsBloodPressure.subject)
        assertNotNull(roninObsBloodPressure.subject?.type?.extension)
        assertEquals("vital-signs", roninObsBloodPressure.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsBloodPressure.category[0].coding[0].system)
        assertNotNull(roninObsBloodPressure.status)
        assertNotNull(roninObsBloodPressure.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `roninObservationBloodPressure with params`() {
        val roninObsBloodPressure =
            rcdmObservationBloodPressure(TENANT_MNEMONIC) {
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
        assertEquals("test-Id-Id", roninObsBloodPressure.id?.value)
        assertEquals(4, roninObsBloodPressure.identifier.size)
        assertTrue(
            roninObsBloodPressure.identifier.contains(
                Identifier(
                    system = Uri("testsystem"),
                    value = "tomato".asFHIR(),
                ),
            ),
        )
        assertEquals("fake-status", roninObsBloodPressure.status?.value)
        assertNotNull(roninObsBloodPressure.category[0].coding[0].system)
        assertEquals("fake text goes here", roninObsBloodPressure.code?.text?.value)
        assertTrue(roninObsBloodPressure.subject?.reference?.value?.startsWith("Patient/test-") == true)
    }

    @Test
    fun `validates for rcdmObservationBloodPressure`() {
        val bpObs = rcdmObservationBloodPressure(TENANT_MNEMONIC) {}
        val validation = service.validate(bpObs, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
    }

    @Test
    fun `validation for rcdmObservationBloodPressure with existing identifier`() {
        val bpObs =
            rcdmObservationBloodPressure(TENANT_MNEMONIC) {
                identifier of
                    listOf(
                        Identifier(
                            system = Uri("testsystem"),
                            value = "tomato".asFHIR(),
                        ),
                    )
            }
        val validation = service.validate(bpObs, TENANT_MNEMONIC).succeeded
        assertTrue(validation)
        assertNotNull(bpObs.meta)
        assertNotNull(bpObs.identifier)
        assertEquals(4, bpObs.identifier.size)
        assertNotNull(bpObs.status)
        assertNotNull(bpObs.code)
        assertNotNull(bpObs.subject)
    }

    @Test
    fun `valid subject input - validation succeeds`() {
        val bpObs =
            rcdmObservationBloodPressure(TENANT_MNEMONIC) {
                subject of rcdmReference("Patient", "456")
            }
        val validation = service.validate(bpObs, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertTrue(bpObs.subject?.reference?.value == "Patient/456")
    }

    @Test
    fun `rcdmPatient rcdmObservationBloodPressure validates`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val bpObs = rcdmPatient.rcdmObservationBloodPressure {}
        val validation = service.validate(bpObs, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertNotNull(bpObs.meta)
        assertNotNull(bpObs.identifier)
        assertTrue(bpObs.identifier.size >= 3)
        assertNotNull(bpObs.status)
        assertNotNull(bpObs.code)
        assertNotNull(bpObs.subject?.type?.extension)
        assertEquals("Patient", bpObs.subject?.decomposedType())
    }

    @Test
    fun `rcdmPatient rcdmObservationBloodPressure - valid subject input overrides base patient - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val bpObs =
            rcdmPatient.rcdmObservationBloodPressure {
                subject of rcdmReference("Patient", "456")
            }
        val validation = service.validate(bpObs, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/456", bpObs.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmObservationBloodPressure - base patient overrides invalid subject input - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val bpObs =
            rcdmPatient.rcdmObservationBloodPressure {
                subject of reference("Patient", "456")
            }
        val validation = service.validate(bpObs, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals("Patient/${rcdmPatient.id?.value}", bpObs.subject?.reference?.value)
    }

    @Test
    fun `rcdmPatient rcdmObservationBloodPressure - fhir id input for both - validate succeeds`() {
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) { id of "99" }
        val bpObs =
            rcdmPatient.rcdmObservationBloodPressure {
                id of "88"
            }
        val validation = service.validate(bpObs, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertEquals(3, bpObs.identifier.size)
        val values = bpObs.identifier.mapNotNull { it.value }.toSet()
        assertTrue(values.size == 3)
        assertTrue(values.contains("88".asFHIR()))
        assertTrue(values.contains(TENANT_MNEMONIC.asFHIR()))
        assertTrue(values.contains("EHR Data Authority".asFHIR()))
        assertEquals("test-88", bpObs.id?.value)
        assertEquals("test-99", rcdmPatient.id?.value)
        assertEquals("Patient/test-99", bpObs.subject?.reference?.value)
    }

    @Test
    fun `roninObservationBloodPressure with custom components`() {
        val tenant = "ronin"
        val patientId = "123456"
        val diastolicValue = 80
        val systolicValue = 110
        val units = "mmHg"
        val effectiveDate = "2023-07-30"

        val roninBloodPressure =
            rcdmObservationBloodPressure(tenant) {
                subject of rcdmReference("Patient", patientId)
                effective of DynamicValues.dateTime(DateTime(effectiveDate))
                component of
                    listOf(
                        observationComponent {
                            code of
                                codeableConcept {
                                    coding of
                                        listOf(
                                            coding {
                                                system of "http://loinc.org"
                                                code of Code("8462-4")
                                            },
                                        )
                                    text of "Diastolic".asFHIR()
                                }
                            value of
                                DynamicValues.quantity(
                                    quantity {
                                        value of Decimal(diastolicValue.toBigDecimal())
                                        unit of units.asFHIR()
                                        system of CodeSystem.UCUM.uri
                                        code of Code(units)
                                    },
                                )
                        },
                        observationComponent {
                            code of
                                codeableConcept {
                                    coding of
                                        listOf(
                                            coding {
                                                system of "http://loinc.org"
                                                code of Code("8480-6")
                                            },
                                        )
                                    text of "Systolic".asFHIR()
                                }
                            value of
                                DynamicValues.quantity(
                                    quantity {
                                        value of Decimal(systolicValue.toBigDecimal())
                                        unit of units.asFHIR()
                                        system of CodeSystem.UCUM.uri
                                        code of Code(units)
                                    },
                                )
                        },
                    )
            }

        // Uncomment to take a peek at the JSON
        // println(JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninBloodPressure))

        assertEquals(
            diastolicValue,
            (roninBloodPressure.component[0].value!!.value as? Quantity)?.value?.value?.toInt(),
        )
        assertEquals(systolicValue, (roninBloodPressure.component[1].value!!.value as? Quantity)?.value?.value?.toInt())
    }
}
