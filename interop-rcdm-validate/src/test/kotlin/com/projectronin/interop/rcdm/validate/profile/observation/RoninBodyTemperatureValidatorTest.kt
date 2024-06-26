package com.projectronin.interop.rcdm.validate.profile.observation

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.registry.NormalizationRegistryClient
import com.projectronin.interop.rcdm.registry.model.ValueSetList
import com.projectronin.interop.rcdm.validate.profile.util.requiredIdentifiers
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@Suppress("ktlint:standard:max-line-length")
class RoninBodyTemperatureValidatorTest {
    private val bodyTemperatureCoding =
        Coding(
            system = CodeSystem.LOINC.uri,
            display = "Body Temperature".asFHIR(),
            code = Code("1234-5"),
        )

    private val vitalSignsCategoryConcept =
        CodeableConcept(
            coding =
                listOf(
                    Coding(
                        system = CodeSystem.OBSERVATION_CATEGORY.uri,
                        code = Code("vital-signs"),
                    ),
                ),
        )

    private val registryClient =
        mockk<NormalizationRegistryClient> {
            every {
                getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_BODY_TEMPERATURE.value)
            } returns ValueSetList(listOf(bodyTemperatureCoding), mockk())
        }
    private val validator = RoninBodyTemperatureValidator(registryClient)

    @Test
    fun `returns proper profile`() {
        assertEquals(RoninProfile.OBSERVATION_BODY_TEMPERATURE, validator.profile)
    }

    @Test
    fun `validate fails for invalid value type`() {
        val bodyTemperature =
            Observation(
                id = Id("1234"),
                meta =
                    Meta(
                        profile = listOf(RoninProfile.OBSERVATION_BODY_TEMPERATURE.canonical),
                        source = Uri("source"),
                    ),
                identifier = requiredIdentifiers,
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(text = "code".asFHIR()),
                                ),
                        ),
                    ),
                status = com.projectronin.interop.fhir.r4.valueset.ObservationStatus.FINAL.asCode(),
                code = CodeableConcept(coding = listOf(bodyTemperatureCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category = listOf(vitalSignsCategoryConcept),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                value =
                    DynamicValue(
                        DynamicValueType.STRING,
                        FHIRString("value"),
                    ),
                bodySite = null,
            )
        val validation = validator.validate(bodyTemperature, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_INV_DYN_VAL: http://projectronin.io/fhir/StructureDefinition/ronin-observationBodyTemperature profile restricts value to one of: Quantity @ Observation.value",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails for invalid value`() {
        val bodyTemperature =
            Observation(
                id = Id("1234"),
                meta =
                    Meta(
                        profile = listOf(RoninProfile.OBSERVATION_BODY_TEMPERATURE.canonical),
                        source = Uri("source"),
                    ),
                identifier = requiredIdentifiers,
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(text = "code".asFHIR()),
                                ),
                        ),
                    ),
                status = com.projectronin.interop.fhir.r4.valueset.ObservationStatus.FINAL.asCode(),
                code = CodeableConcept(coding = listOf(bodyTemperatureCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category = listOf(vitalSignsCategoryConcept),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                value =
                    DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(310)),
                            unit = "K".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("K"),
                        ),
                    ),
                bodySite = null,
            )
        val validation = validator.validate(bodyTemperature, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR INV_VALUE_SET: 'K' is outside of required value set @ Observation.valueQuantity.code",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate succeeds`() {
        val bodyTemperature =
            Observation(
                id = Id("1234"),
                meta =
                    Meta(
                        profile = listOf(RoninProfile.OBSERVATION_BODY_TEMPERATURE.canonical),
                        source = Uri("source"),
                    ),
                identifier = requiredIdentifiers,
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                            value =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(text = "code".asFHIR()),
                                ),
                        ),
                    ),
                status = com.projectronin.interop.fhir.r4.valueset.ObservationStatus.FINAL.asCode(),
                code = CodeableConcept(coding = listOf(bodyTemperatureCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category = listOf(vitalSignsCategoryConcept),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                value =
                    DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(37)),
                            unit = "C".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("Cel"),
                        ),
                    ),
                bodySite = null,
            )
        val validation = validator.validate(bodyTemperature, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }
}
