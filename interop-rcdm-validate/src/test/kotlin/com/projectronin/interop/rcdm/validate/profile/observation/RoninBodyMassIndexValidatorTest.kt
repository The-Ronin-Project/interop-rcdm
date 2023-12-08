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

class RoninBodyMassIndexValidatorTest {
    private val bodyMassIndexCoding =
        Coding(
            system = CodeSystem.LOINC.uri,
            display = "Body Mass Index".asFHIR(),
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
                getRequiredValueSet("Observation.code", RoninProfile.OBSERVATION_BODY_MASS_INDEX.value)
            } returns ValueSetList(listOf(bodyMassIndexCoding), mockk())
        }
    private val validator = RoninBodyMassIndexValidator(registryClient)

    @Test
    fun `returns proper profile`() {
        assertEquals(RoninProfile.OBSERVATION_BODY_MASS_INDEX, validator.profile)
    }

    @Test
    fun `validate fails for invalid value`() {
        val bodyMassIndex =
            Observation(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.OBSERVATION_BODY_MASS_INDEX.canonical), source = Uri("source")),
                identifier = requiredIdentifiers,
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR())),
                        ),
                    ),
                status = com.projectronin.interop.fhir.r4.valueset.ObservationStatus.FINAL.asCode(),
                code = CodeableConcept(coding = listOf(bodyMassIndexCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category = listOf(vitalSignsCategoryConcept),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                value =
                    DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(15)),
                            unit = "lb/m2".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("lb/m2"),
                        ),
                    ),
                bodySite = null,
            )
        val validation = validator.validate(bodyMassIndex, LocationContext(Observation::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR INV_VALUE_SET: 'lb/m2' is outside of required value set @ Observation.valueQuantity.code",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate succeeds`() {
        val bodyMassIndex =
            Observation(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.OBSERVATION_BODY_MASS_INDEX.canonical), source = Uri("source")),
                identifier = requiredIdentifiers,
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_OBSERVATION_CODE.uri,
                            value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, CodeableConcept(text = "code".asFHIR())),
                        ),
                    ),
                status = com.projectronin.interop.fhir.r4.valueset.ObservationStatus.FINAL.asCode(),
                code = CodeableConcept(coding = listOf(bodyMassIndexCoding)),
                subject = Reference(reference = FHIRString("Patient/1234")),
                category = listOf(vitalSignsCategoryConcept),
                effective = DynamicValue(DynamicValueType.DATE_TIME, DateTime("2023")),
                value =
                    DynamicValue(
                        DynamicValueType.QUANTITY,
                        Quantity(
                            value = Decimal(BigDecimal.valueOf(15)),
                            unit = "kg/m2".asFHIR(),
                            system = CodeSystem.UCUM.uri,
                            code = Code("kg/m2"),
                        ),
                    ),
                bodySite = null,
            )
        val validation = validator.validate(bodyMassIndex, LocationContext(Observation::class))
        assertEquals(0, validation.issues().size)
    }
}
