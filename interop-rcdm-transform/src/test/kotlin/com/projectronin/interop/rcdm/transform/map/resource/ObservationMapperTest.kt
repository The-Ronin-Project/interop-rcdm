package com.projectronin.interop.rcdm.transform.map.resource

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.ObservationComponent
import com.projectronin.interop.fhir.r4.valueset.ObservationStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ConceptMapCodeableConcept
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@Suppress("ktlint:standard:max-line-length")
class ObservationMapperTest {
    private val registryClient = mockk<NormalizationRegistryClient>()
    private val mapper = ObservationMapper(registryClient)

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "tenant"
        }

    @Test
    fun `supported resource is Observation`() {
        assertEquals(Observation::class, mapper.supportedResource)
    }

    @Test
    fun `maps when no mappable elements`() {
        val observation =
            Observation(
                status = ObservationStatus.FINAL.asCode(),
                code = null,
                value = null,
                component = listOf(),
            )

        val (mappedResource, validation) = mapper.map(observation, tenant, null)
        assertEquals(observation, mappedResource)
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `maps code`() {
        val code =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val observation =
            Observation(
                status = ObservationStatus.FINAL.asCode(),
                code = code,
                value = null,
                component = listOf(),
            )

        val mappedCode =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.code",
                code,
                observation,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedCode, mappedExtension, listOf())

        val (mappedResource, validation) = mapper.map(observation, tenant, null)
        mappedResource!!
        assertEquals(mappedCode, mappedResource.code)
        assertNull(mappedResource.value)
        assertEquals(listOf<ObservationComponent>(), mappedResource.component)
        assertEquals(listOf(mappedExtension), mappedResource.extension)

        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `handles failed code mapping`() {
        val code =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val observation =
            Observation(
                status = ObservationStatus.FINAL.asCode(),
                code = code,
                value = null,
                component = listOf(),
            )

        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.code",
                code,
                observation,
                null,
            )
        } returns null

        val (mappedResource, validation) = mapper.map(observation, tenant, null)
        assertEquals(observation, mappedResource)

        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR NOV_CONMAP_LOOKUP: Tenant source value '12345' has no target defined in any Observation.code concept map for tenant 'tenant' @ Observation.code",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `maps value when codeable concept`() {
        val value =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val observation =
            Observation(
                status = ObservationStatus.FINAL.asCode(),
                code = null,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, value),
                component = listOf(),
            )

        val mappedValue =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_VALUE.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, value),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.valueCodeableConcept",
                value,
                observation,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedValue, mappedExtension, listOf())

        val (mappedResource, validation) = mapper.map(observation, tenant, null)
        mappedResource!!
        assertNull(mappedResource.code)
        assertEquals(DynamicValue(DynamicValueType.CODEABLE_CONCEPT, mappedValue), mappedResource.value)
        assertEquals(listOf<ObservationComponent>(), mappedResource.component)
        assertEquals(listOf(mappedExtension), mappedResource.extension)

        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `does not map value when not codeable concept`() {
        val observation =
            Observation(
                status = ObservationStatus.FINAL.asCode(),
                code = null,
                value = DynamicValue(DynamicValueType.BOOLEAN, FHIRBoolean.TRUE),
                component = listOf(),
            )

        val (mappedResource, validation) = mapper.map(observation, tenant, null)
        assertEquals(observation, mappedResource)
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `handles failed value mapping`() {
        val value =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val observation =
            Observation(
                status = ObservationStatus.FINAL.asCode(),
                code = null,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, value),
                component = listOf(),
            )

        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.valueCodeableConcept",
                value,
                observation,
                null,
            )
        } returns null

        val (mappedResource, validation) = mapper.map(observation, tenant, null)
        assertEquals(observation, mappedResource)

        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR NOV_CONMAP_LOOKUP: Tenant source value '12345' has no target defined in any Observation.valueCodeableConcept concept map for tenant 'tenant' @ Observation.valueCodeableConcept",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `maps component code`() {
        val code =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val observation =
            Observation(
                status = ObservationStatus.FINAL.asCode(),
                code = null,
                value = null,
                component =
                    listOf(
                        ObservationComponent(
                            code = code,
                        ),
                    ),
            )

        val mappedCode =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.component.code",
                code,
                observation,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedCode, mappedExtension, listOf())

        val (mappedResource, validation) = mapper.map(observation, tenant, null)
        mappedResource!!
        assertNull(mappedResource.code)
        assertNull(mappedResource.value)
        assertEquals(
            listOf(
                ObservationComponent(
                    extension = listOf(mappedExtension),
                    code = mappedCode,
                ),
            ),
            mappedResource.component,
        )
        assertEquals(listOf<Extension>(), mappedResource.extension)

        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `handles failed component code mapping`() {
        val code =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val observation =
            Observation(
                status = ObservationStatus.FINAL.asCode(),
                code = null,
                value = null,
                component = listOf(ObservationComponent(code = code)),
            )

        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.component.code",
                code,
                observation,
                null,
            )
        } returns null

        val (mappedResource, validation) = mapper.map(observation, tenant, null)
        assertEquals(observation, mappedResource)

        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR NOV_CONMAP_LOOKUP: Tenant source value '12345' has no target defined in any Observation.component.code concept map for tenant 'tenant' @ Observation.component[0].code",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `maps component value when codeable concept`() {
        val value =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val observation =
            Observation(
                status = ObservationStatus.FINAL.asCode(),
                code = null,
                value = null,
                component =
                    listOf(
                        ObservationComponent(
                            code = null,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, value),
                        ),
                    ),
            )

        val mappedValue =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_VALUE.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, value),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.component.valueCodeableConcept",
                value,
                observation,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedValue, mappedExtension, listOf())

        val (mappedResource, validation) = mapper.map(observation, tenant, null)
        mappedResource!!
        assertNull(mappedResource.code)
        assertNull(mappedResource.value)
        assertEquals(
            listOf(
                ObservationComponent(
                    extension = listOf(mappedExtension),
                    code = null,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, mappedValue),
                ),
            ),
            mappedResource.component,
        )
        assertEquals(listOf<Extension>(), mappedResource.extension)

        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `does not map component value when not codeable concept`() {
        val observation =
            Observation(
                status = ObservationStatus.FINAL.asCode(),
                code = null,
                value = null,
                component =
                    listOf(
                        ObservationComponent(
                            code = null,
                            value = DynamicValue(DynamicValueType.BOOLEAN, FHIRBoolean.TRUE),
                        ),
                    ),
            )

        val (mappedResource, validation) = mapper.map(observation, tenant, null)
        assertEquals(observation, mappedResource)
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `handles failed component value mapping`() {
        val value =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val observation =
            Observation(
                status = ObservationStatus.FINAL.asCode(),
                code = null,
                value = null,
                component =
                    listOf(
                        ObservationComponent(
                            code = null,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, value),
                        ),
                    ),
            )

        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.component.valueCodeableConcept",
                value,
                observation,
                null,
            )
        } returns null

        val (mappedResource, validation) = mapper.map(observation, tenant, null)
        assertEquals(observation, mappedResource)

        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR NOV_CONMAP_LOOKUP: Tenant source value '12345' has no target defined in any Observation.component.valueCodeableConcept concept map for tenant 'tenant' @ Observation.component[0].valueCodeableConcept",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `handles component with no mappable elements`() {
        val observation =
            Observation(
                status = ObservationStatus.FINAL.asCode(),
                code = null,
                value = null,
                component =
                    listOf(
                        ObservationComponent(
                            code = null,
                            value = null,
                        ),
                    ),
            )

        val (mappedResource, validation) = mapper.map(observation, tenant, null)
        assertEquals(observation, mappedResource)
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `handles observation with all mappable elements`() {
        val code =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val value =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val interpretation =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val componentCode =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val componentValue =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val componentInterp =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val observation =
            Observation(
                status = ObservationStatus.FINAL.asCode(),
                code = code,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, value),
                interpretation = listOf(interpretation),
                component =
                    listOf(
                        ObservationComponent(
                            code = componentCode,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, componentValue),
                            interpretation = listOf(componentInterp),
                        ),
                    ),
            )

        val mappedCode =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedCodeExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.code",
                code,
                observation,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedCode, mappedCodeExtension, listOf())

        val mappedValue =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedValueExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_VALUE.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, value),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.valueCodeableConcept",
                value,
                observation,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedValue, mappedValueExtension, listOf())
        val mappedInterpretation =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedInterpretationExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_INTERPRETATION.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, interpretation),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.interpretation",
                value,
                observation,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedInterpretation, mappedInterpretationExtension, listOf())
        val mappedComponentCode =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedComponentCodeExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.component.code",
                componentCode,
                observation,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedComponentCode, mappedComponentCodeExtension, listOf())

        val mappedComponentValue =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedComponentValueExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_VALUE.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, value),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.component.valueCodeableConcept",
                componentValue,
                observation,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedComponentValue, mappedComponentValueExtension, listOf())

        val mappedComponentInterp =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedComponentInterpExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_INTERPRETATION.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, componentInterp),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.component.interpretation",
                componentInterp,
                observation,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedComponentInterp, mappedComponentInterpExtension, listOf())
        val (mappedResource, validation) = mapper.map(observation, tenant, null)
        mappedResource!!
        assertEquals(mappedCode, mappedResource.code)
        assertEquals(DynamicValue(DynamicValueType.CODEABLE_CONCEPT, mappedValue), mappedResource.value)
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = mappedInterpretation.coding,
                    extension = listOf(mappedInterpretationExtension),
                ),
            ),
            mappedResource.interpretation,
        )
        assertEquals(
            listOf(
                ObservationComponent(
                    extension = listOf(mappedComponentCodeExtension, mappedComponentValueExtension),
                    code = mappedComponentCode,
                    value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, mappedComponentValue),
                    interpretation =
                        listOf(
                            CodeableConcept(
                                coding = mappedComponentInterp.coding,
                                extension = listOf(mappedComponentInterpExtension),
                            ),
                        ),
                ),
            ),
            mappedResource.component,
        )
        assertEquals(listOf(mappedCodeExtension, mappedValueExtension), mappedResource.extension)

        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `supports forced cache reload`() {
        val code =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val observation =
            Observation(
                status = ObservationStatus.FINAL.asCode(),
                code = code,
                value = null,
                component = listOf(),
            )

        val mappedCode =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, code),
            )

        val forceCacheReloadTS = LocalDateTime.now()
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.code",
                code,
                observation,
                forceCacheReloadTS,
            )
        } returns ConceptMapCodeableConcept(mappedCode, mappedExtension, listOf())

        val (mappedResource, validation) = mapper.map(observation, tenant, forceCacheReloadTS)
        mappedResource!!
        assertEquals(mappedCode, mappedResource.code)
        assertNull(mappedResource.value)
        assertEquals(listOf<ObservationComponent>(), mappedResource.component)
        assertEquals(listOf(mappedExtension), mappedResource.extension)

        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `maps component interpretation`() {
        val requiredCode =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("00000")),
                    ),
            )
        val interp =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val observation =
            Observation(
                status = ObservationStatus.FINAL.asCode(),
                code = null,
                value = null,
                component =
                    listOf(
                        ObservationComponent(
                            code = requiredCode,
                            interpretation = listOf(interp),
                        ),
                    ),
            )

        val mappedInterp =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedCode =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("11111")),
                    ),
            )
        val mappedInterpExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_INTERPRETATION.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, interp),
            )
        val mappedCodeExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, requiredCode),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.component.interpretation",
                interp,
                observation,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedInterp, mappedInterpExtension, listOf())
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.component.code",
                requiredCode,
                observation,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedCode, mappedCodeExtension, listOf())

        val (mappedResource, validation) = mapper.map(observation, tenant, null)
        mappedResource!!
        assertNull(mappedResource.code)
        assertNull(mappedResource.value)
        assertEquals(
            listOf(
                ObservationComponent(
                    code = mappedCode,
                    extension = listOf(mappedCodeExtension),
                    interpretation =
                        listOf(
                            CodeableConcept(
                                coding = mappedInterp.coding,
                                extension = listOf(mappedInterpExtension),
                            ),
                        ),
                ),
            ),
            mappedResource.component,
        )
        assertEquals(listOf<Extension>(), mappedResource.extension)

        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `maps multiple component interpretation`() {
        val requiredCode =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("00000")),
                    ),
            )
        val interp1 =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val interp2 =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("22222")),
                    ),
            )
        val observation =
            Observation(
                status = ObservationStatus.FINAL.asCode(),
                code = null,
                value = null,
                component =
                    listOf(
                        ObservationComponent(
                            code = requiredCode,
                            interpretation = listOf(interp1, interp2),
                        ),
                    ),
            )

        val mappedInterp1 =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedInterp2 =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("33333")),
                    ),
            )
        val mappedCode =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("11111")),
                    ),
            )
        val mappedInterp1Extension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_INTERPRETATION.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, interp1),
            )
        val mappedInterp2Extension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_INTERPRETATION.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, interp2),
            )
        val mappedCodeExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, requiredCode),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.component.interpretation",
                interp1,
                observation,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedInterp1, mappedInterp1Extension, listOf())
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.component.interpretation",
                interp2,
                observation,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedInterp2, mappedInterp2Extension, listOf())
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.component.code",
                requiredCode,
                observation,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedCode, mappedCodeExtension, listOf())

        val (mappedResource, validation) = mapper.map(observation, tenant, null)
        mappedResource!!
        assertNull(mappedResource.code)
        assertNull(mappedResource.value)
        assertEquals(
            listOf(
                ObservationComponent(
                    code = mappedCode,
                    extension = listOf(mappedCodeExtension),
                    interpretation =
                        listOf(
                            CodeableConcept(
                                coding = mappedInterp1.coding,
                                extension = listOf(mappedInterp1Extension),
                            ),
                            CodeableConcept(
                                coding = mappedInterp2.coding,
                                extension = listOf(mappedInterp2Extension),
                            ),
                        ),
                ),
            ),
            mappedResource.component,
        )
        assertEquals(listOf<Extension>(), mappedResource.extension)

        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `maps interpretation`() {
        val requiredCode =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("00000")),
                    ),
            )
        val interp1 =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("12345")),
                    ),
            )
        val observation =
            Observation(
                status = ObservationStatus.FINAL.asCode(),
                code = null,
                value = null,
                interpretation = listOf(interp1),
                component =
                    listOf(
                        ObservationComponent(
                            code = requiredCode,
                        ),
                    ),
            )

        val mappedInterp1 =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("67890")),
                    ),
            )
        val mappedCode =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(system = Uri("http://snomed.info/sct"), code = Code("11111")),
                    ),
            )
        val mappedInterp1Extension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_INTERPRETATION.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, interp1),
            )
        val mappedCodeExtension =
            Extension(
                url = RoninExtension.TENANT_SOURCE_OBSERVATION_COMPONENT_CODE.uri,
                value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, requiredCode),
            )
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.interpretation",
                interp1,
                observation,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedInterp1, mappedInterp1Extension, listOf())
        every {
            registryClient.getConceptMapping(
                "tenant",
                "Observation.component.code",
                requiredCode,
                observation,
                null,
            )
        } returns ConceptMapCodeableConcept(mappedCode, mappedCodeExtension, listOf())

        val (mappedResource, validation) = mapper.map(observation, tenant, null)
        mappedResource!!
        assertNull(mappedResource.code)
        assertNull(mappedResource.value)
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = mappedInterp1.coding,
                    extension = listOf(mappedInterp1Extension),
                ),
            ),
            mappedResource.interpretation,
        )
        assertEquals(
            listOf(
                ObservationComponent(
                    code = mappedCode,
                    extension = listOf(mappedCodeExtension),
                ),
            ),
            mappedResource.component,
        )
        assertEquals(listOf<Extension>(), mappedResource.extension)

        assertEquals(0, validation.issues().size)
    }
}
