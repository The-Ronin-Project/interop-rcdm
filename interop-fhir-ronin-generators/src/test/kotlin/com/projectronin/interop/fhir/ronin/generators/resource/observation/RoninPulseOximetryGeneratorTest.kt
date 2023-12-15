package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ObservationComponent
import com.projectronin.interop.fhir.ronin.generators.resource.BaseGeneratorSpringTest
import com.projectronin.interop.fhir.ronin.generators.resource.TENANT_MNEMONIC
import com.projectronin.interop.fhir.ronin.generators.resource.normalizationRegistryClient
import com.projectronin.interop.fhir.ronin.generators.resource.rcdmPatient
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
class RoninPulseOximetryGeneratorTest : BaseGeneratorSpringTest() {
    @BeforeEach
    fun normalizationClient() {
        every {
            normalizationRegistryClient.getRequiredValueSet("Observation.component:FlowRate.code", any())
        } returns flowRateCoding
        every {
            normalizationRegistryClient.getRequiredValueSet("Observation.component:Concentration.code", any())
        } returns concentrationCoding

        every {
            normalizationRegistryClient.getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_PULSE_OXIMETRY.value)
        } returns possiblePulseOximetryCodes
    }

    private val flowRateCoding =
        ValueSetList(
            listOf(
                Coding(system = CodeSystem.LOINC.uri, code = Code("3151-8")),
            ),
            ValueSetMetadata(
                registryEntryType = "value_set",
                valueSetName = "inhaledoxygenflowrate",
                valueSetUuid = "44bf0223-00e1-4424-89ee-048a2f8dbf7d",
                version = "2",
            ),
        )
    private val concentrationCoding =
        ValueSetList(
            listOf(
                Coding(system = CodeSystem.LOINC.uri, code = Code("3150-0")),
            ),
            ValueSetMetadata(
                registryEntryType = "value_set",
                valueSetName = "inhaledoxygenconcentration",
                valueSetUuid = "74e038f6-d57c-483e-90cb-215af0a5e0ed",
                version = "2",
            ),
        )

    @Test
    fun `example use for roninObservationPulseOximetry`() {
        // Create PulseOximetry Obs with attributes you need, provide the tenant
        val roninObsPulseOximetry =
            rcdmObservationPulseOximetry(TENANT_MNEMONIC) {
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
                        text of "Respiratory viral pathogens DNA and RNA panel"
                    }
            }
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsPulseOximetryJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsPulseOximetry)

        // Uncomment to take a peek at the JSON
        // println(roninObsBloodPressureJSON)
        assertNotNull(roninObsPulseOximetryJSON)
    }

    @Test
    fun `example use for rcdmPatient rcdmObservationPulseOximetry - missing required fields generated`() {
        // create patient and observation for tenant
        val rcdmPatient = rcdmPatient(TENANT_MNEMONIC) {}
        val roninObsPulseOximetry = rcdmPatient.rcdmObservationPulseOximetry {}
        // This object can be serialized to JSON to be injected into your workflow, all required R4 attributes wil be generated
        val roninObsPulseOximetryJSON =
            JacksonManager.objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(roninObsPulseOximetry)

        // Uncomment to take a peek at the JSON
        // println(roninObsPulseOximetryJSON)
        assertNotNull(roninObsPulseOximetryJSON)
        assertNotNull(roninObsPulseOximetry.meta)
        assertEquals(
            roninObsPulseOximetry.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_PULSE_OXIMETRY.value,
        )
        assertNotNull(roninObsPulseOximetry.status)
        assertEquals(1, roninObsPulseOximetry.category.size)
        assertNotNull(roninObsPulseOximetry.code)
        assertNotNull(roninObsPulseOximetry.subject)
        assertNotNull(roninObsPulseOximetry.subject?.type?.extension)
        assertEquals("vital-signs", roninObsPulseOximetry.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsPulseOximetry.category[0].coding[0].system)
        assertNotNull(roninObsPulseOximetry.status)
        assertNotNull(roninObsPulseOximetry.code?.coding?.get(0)?.code?.value)
        assertNotNull(roninObsPulseOximetry.id)
        val patientFHIRId =
            roninObsPulseOximetry.identifier.firstOrNull { it.system == CodeSystem.RONIN_FHIR_ID.uri }?.value?.value.toString()
        val tenant =
            roninObsPulseOximetry.identifier.firstOrNull { it.system == CodeSystem.RONIN_TENANT.uri }?.value?.value.toString()
        assertEquals("$tenant-$patientFHIRId", roninObsPulseOximetry.id?.value.toString())
    }

    @Test
    fun `generates valid roninObservationPulseOximetry Observation`() {
        val roninObsPulseOx = rcdmObservationPulseOximetry(TENANT_MNEMONIC) {}
        assertNotNull(roninObsPulseOx.id)
        assertNotNull(roninObsPulseOx.meta)
        assertEquals(
            roninObsPulseOx.meta!!.profile[0].value,
            RoninProfile.OBSERVATION_PULSE_OXIMETRY.value,
        )
        assertNull(roninObsPulseOx.implicitRules)
        assertNull(roninObsPulseOx.language)
        assertNull(roninObsPulseOx.text)
        assertEquals(0, roninObsPulseOx.contained.size)
        assertEquals(1, roninObsPulseOx.extension.size)
        assertEquals(0, roninObsPulseOx.modifierExtension.size)
        assertTrue(roninObsPulseOx.identifier.size >= 3)
        assertTrue(roninObsPulseOx.identifier.any { it.value == "test".asFHIR() })
        assertTrue(roninObsPulseOx.identifier.any { it.value == "EHR Data Authority".asFHIR() })
        assertTrue(roninObsPulseOx.identifier.any { it.system == CodeSystem.RONIN_FHIR_ID.uri })
        assertNotNull(roninObsPulseOx.status)
        assertEquals(1, roninObsPulseOx.category.size)
        assertNotNull(roninObsPulseOx.code)
        assertNotNull(roninObsPulseOx.subject)
        assertNotNull(roninObsPulseOx.subject?.type?.extension)
        assertEquals("vital-signs", roninObsPulseOx.category[0].coding[0].code?.value)
        assertEquals(CodeSystem.OBSERVATION_CATEGORY.uri, roninObsPulseOx.category[0].coding[0].system)
        assertNotNull(roninObsPulseOx.status)
        assertNotNull(roninObsPulseOx.code?.coding?.get(0)?.code?.value)
    }

    @Test
    fun `validates for roninObservationPulseOximetry`() {
        val roninPulseOximetry = rcdmObservationPulseOximetry(TENANT_MNEMONIC) {}
        val validation = service.validate(roninPulseOximetry, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
    }

    @Test
    fun `validation for roninObservationPulseOximetry with existing identifier`() {
        val roninPulseOximetry =
            rcdmObservationPulseOximetry(TENANT_MNEMONIC) {
                identifier of
                    listOf(
                        Identifier(
                            system = Uri("testsystem"),
                            value = "tomato".asFHIR(),
                        ),
                    )
            }
        val validation = service.validate(roninPulseOximetry, TENANT_MNEMONIC)
        assertTrue(validation.succeeded)
        assertNotNull(roninPulseOximetry.meta)
        assertNotNull(roninPulseOximetry.identifier)
        assertEquals(4, roninPulseOximetry.identifier.size)
        assertNotNull(roninPulseOximetry.status)
        assertNotNull(roninPulseOximetry.code)
        assertNotNull(roninPulseOximetry.subject)
        assertNotNull(roninPulseOximetry.value)
        assertNotNull(roninPulseOximetry.component)
    }

    @Test
    fun `rcdmObservationPulseOximetry with custom value`() {
        val valueQuantity =
            DynamicValue(
                DynamicValueType.QUANTITY,
                Quantity(
                    value = Decimal(94.toBigDecimal()),
                    unit = "% O2".asFHIR(),
                    system = CodeSystem.UCUM.uri,
                    code = Code("% 02"),
                ),
            )

        val rcdmObservationPulseOximetry =
            rcdmObservationPulseOximetry(TENANT_MNEMONIC) {
                value of valueQuantity
            }

        assertEquals(valueQuantity, rcdmObservationPulseOximetry.value)
    }

    @Test
    fun `rcdmObservationPulseOximetry with custom component`() {
        val pulseOxComponent =
            listOf(
                ObservationComponent(
                    code =
                        CodeableConcept(
                            coding = flowRateCoding.codes,
                            text = "Flow Rate".asFHIR(),
                        ),
                    value =
                        DynamicValue(
                            DynamicValueType.QUANTITY,
                            Quantity(
                                value = Decimal(120.toBigDecimal()),
                                unit = "L/min".asFHIR(),
                                system = CodeSystem.UCUM.uri,
                                code = Code("L/min"),
                            ),
                        ),
                ),
                ObservationComponent(
                    code =
                        CodeableConcept(
                            coding = concentrationCoding.codes,
                            text = "Concentration".asFHIR(),
                        ),
                    value =
                        DynamicValue(
                            DynamicValueType.QUANTITY,
                            Quantity(
                                value = Decimal(63.toBigDecimal()),
                                unit = "%".asFHIR(),
                                system = CodeSystem.UCUM.uri,
                                code = Code("%"),
                            ),
                        ),
                ),
            )

        val rcdmObservationPulseOximetry =
            rcdmObservationPulseOximetry(TENANT_MNEMONIC) {
                component of pulseOxComponent
            }

        val expectedPulseOxComponent =
            listOf(
                ObservationComponent(
                    extension = listOf(tenantSourceObservationComponentCodeExtension),
                    code =
                        CodeableConcept(
                            coding = flowRateCoding.codes,
                            text = "Flow Rate".asFHIR(),
                        ),
                    value =
                        DynamicValue(
                            DynamicValueType.QUANTITY,
                            Quantity(
                                value = Decimal(120.toBigDecimal()),
                                unit = "L/min".asFHIR(),
                                system = CodeSystem.UCUM.uri,
                                code = Code("L/min"),
                            ),
                        ),
                ),
                ObservationComponent(
                    extension = listOf(tenantSourceObservationComponentCodeExtension),
                    code =
                        CodeableConcept(
                            coding = concentrationCoding.codes,
                            text = "Concentration".asFHIR(),
                        ),
                    value =
                        DynamicValue(
                            DynamicValueType.QUANTITY,
                            Quantity(
                                value = Decimal(63.toBigDecimal()),
                                unit = "%".asFHIR(),
                                system = CodeSystem.UCUM.uri,
                                code = Code("%"),
                            ),
                        ),
                ),
            )
        assertEquals(expectedPulseOxComponent, rcdmObservationPulseOximetry.component)
    }
}
