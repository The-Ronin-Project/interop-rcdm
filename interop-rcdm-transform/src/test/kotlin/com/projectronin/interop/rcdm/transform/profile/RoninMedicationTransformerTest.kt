package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.Ratio
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Batch
import com.projectronin.interop.fhir.r4.resource.Ingredient
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninMedicationTransformerTest {
    private val transformer = RoninMedicationTransformer()

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }

    // re-used codes to make the tests cleaner
    private val vitaminDCode = Code("11253")
    private val medicationCodingList =
        listOf(
            Coding(system = CodeSystem.RXNORM.uri, code = vitaminDCode, display = "Vitamin D".asFHIR()),
        )

    @Test
    fun `returns supported resource`() {
        assertEquals(Medication::class, transformer.supportedResource)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(transformer.qualifies(Medication()))
    }

    @Test
    fun `transform - succeeds with just required attributes`() {
        val medication =
            Medication(
                id = Id("12345"),
                meta = Meta(source = Uri("source")),
                code =
                    CodeableConcept(
                        text = "b".asFHIR(),
                        coding = medicationCodingList,
                    ),
            )

        val transformResponse = transformer.transform(medication, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(Id("12345"), transformed.id)
        assertEquals(3, transformed.identifier.size)
        assertEquals(
            listOf(
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR(),
                ),
            ),
            transformed.identifier,
        )
        assertEquals(medication.code, transformed.code)
    }

    @Test
    fun `transform - succeeds with all attributes present - ingredient item is type REFERENCE`() {
        val medication =
            Medication(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical("http://hl7.org/fhir/R4/Medication.html")),
                        source = Uri("source"),
                    ),
                implicitRules = Uri("implicit-rules"),
                language = Code("en-US"),
                text =
                    Narrative(
                        status = com.projectronin.interop.fhir.r4.valueset.NarrativeStatus.GENERATED.asCode(),
                        div = "div".asFHIR(),
                    ),
                contained = listOf(Location(id = Id("67890"))),
                extension =
                    listOf(
                        Extension(
                            url = Uri("http://hl7.org/extension-1"),
                            value = DynamicValue(DynamicValueType.STRING, "value"),
                        ),
                    ),
                modifierExtension =
                    listOf(
                        Extension(
                            url = Uri("http://localhost/modifier-extension"),
                            value = DynamicValue(DynamicValueType.STRING, "Value"),
                        ),
                    ),
                identifier = listOf(Identifier(value = "67890".asFHIR())),
                code =
                    CodeableConcept(
                        text = "b".asFHIR(),
                        coding = medicationCodingList,
                    ),
                status = com.projectronin.interop.fhir.r4.valueset.MedicationStatus.ACTIVE.asCode(),
                manufacturer = Reference(reference = "Organization/c".asFHIR()),
                form =
                    CodeableConcept(
                        text = "d".asFHIR(),
                        coding = medicationCodingList,
                    ),
                amount =
                    Ratio(
                        numerator =
                            Quantity(
                                value = Decimal(1.5),
                                unit = "mg".asFHIR(),
                                system = CodeSystem.UCUM.uri,
                                code = Code("mg"),
                            ),
                        denominator =
                            Quantity(
                                value = Decimal(1.0),
                                unit = "mg".asFHIR(),
                                system = CodeSystem.UCUM.uri,
                                code = Code("mg"),
                            ),
                    ),
                ingredient =
                    listOf(
                        Ingredient(
                            item =
                                DynamicValue(
                                    type = DynamicValueType.REFERENCE,
                                    value = Reference(reference = "Substance/item".asFHIR()),
                                ),
                            isActive = FHIRBoolean.TRUE,
                            strength =
                                Ratio(
                                    numerator =
                                        Quantity(
                                            value = Decimal(0.5),
                                            unit = "mg".asFHIR(),
                                            system = CodeSystem.UCUM.uri,
                                            code = Code("mg"),
                                        ),
                                    denominator =
                                        Quantity(
                                            value = Decimal(1.0),
                                            unit = "mg".asFHIR(),
                                            system = CodeSystem.UCUM.uri,
                                            code = Code("mg"),
                                        ),
                                ),
                        ),
                    ),
                batch =
                    Batch(
                        lotNumber = "e".asFHIR(),
                        expirationDate = DateTime("2022-10-14"),
                    ),
            )

        // transformation
        val transformResponse = transformer.transform(medication, tenant)

        transformResponse!!

        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            RoninProfile.MEDICATION.value,
            transformed.meta!!.profile[0].value,
        )
        assertEquals(medication.implicitRules, transformed.implicitRules)
        assertEquals(medication.language, transformed.language)
        assertEquals(medication.text, transformed.text)
        assertEquals(medication.contained, transformed.contained)
        assertEquals(medication.extension, transformed.extension)
        assertEquals(medication.modifierExtension, transformed.modifierExtension)
        assertEquals(4, transformed.identifier.size)
        assertEquals(
            listOf(
                Identifier(value = "67890".asFHIR()),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR(),
                ),
            ),
            transformed.identifier,
        )
        assertEquals(medication.code, transformed.code)
        assertEquals(Code(value = "active"), transformed.status)
        assertEquals(Reference(reference = "Organization/c".asFHIR()), transformed.manufacturer)
        assertEquals(medication.form, transformed.form)
        assertEquals(medication.amount, transformed.amount)
        assertEquals(DynamicValueType.REFERENCE, transformed.ingredient[0].item?.type)
        assertEquals(
            Reference(reference = "Substance/item".asFHIR()),
            transformed.ingredient[0].item?.value,
        )
        assertEquals(medication.batch, transformed.batch)
    }

    @Test
    fun `transform succeeds with all attributes present - ingredient item is type CODEABLE_CONCEPT`() {
        val medication =
            Medication(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical("http://hl7.org/fhir/R4/Medication.html")),
                        source = Uri("source"),
                    ),
                implicitRules = Uri("implicit-rules"),
                language = Code("en-US"),
                text =
                    Narrative(
                        status = com.projectronin.interop.fhir.r4.valueset.NarrativeStatus.GENERATED.asCode(),
                        div = "div".asFHIR(),
                    ),
                contained = listOf(Location(id = Id("67890"))),
                extension =
                    listOf(
                        Extension(
                            url = Uri("http://hl7.org/extension-1"),
                            value = DynamicValue(DynamicValueType.STRING, "value"),
                        ),
                    ),
                modifierExtension =
                    listOf(
                        Extension(
                            url = Uri("http://localhost/modifier-extension"),
                            value = DynamicValue(DynamicValueType.STRING, "Value"),
                        ),
                    ),
                identifier = listOf(Identifier(value = "67890".asFHIR())),
                code =
                    CodeableConcept(
                        text = "b".asFHIR(),
                        coding = medicationCodingList,
                    ),
                status = com.projectronin.interop.fhir.r4.valueset.MedicationStatus.ACTIVE.asCode(),
                manufacturer = Reference(reference = "Organization/c".asFHIR()),
                form =
                    CodeableConcept(
                        text = "d".asFHIR(),
                        coding = medicationCodingList,
                    ),
                amount =
                    Ratio(
                        numerator =
                            Quantity(
                                value = Decimal(1.5),
                                unit = "mg".asFHIR(),
                                system = CodeSystem.UCUM.uri,
                                code = Code("mg"),
                            ),
                        denominator =
                            Quantity(
                                value = Decimal(1.0),
                                unit = "mg".asFHIR(),
                                system = CodeSystem.UCUM.uri,
                                code = Code("mg"),
                            ),
                    ),
                ingredient =
                    listOf(
                        Ingredient(
                            item =
                                DynamicValue(
                                    DynamicValueType.CODEABLE_CONCEPT,
                                    CodeableConcept(
                                        text = "f".asFHIR(),
                                        coding = medicationCodingList,
                                    ),
                                ),
                            isActive = FHIRBoolean.TRUE,
                            strength =
                                Ratio(
                                    numerator =
                                        Quantity(
                                            value = Decimal(0.5),
                                            unit = "mg".asFHIR(),
                                            system = CodeSystem.UCUM.uri,
                                            code = Code("mg"),
                                        ),
                                    denominator =
                                        Quantity(
                                            value = Decimal(1.0),
                                            unit = "mg".asFHIR(),
                                            system = CodeSystem.UCUM.uri,
                                            code = Code("mg"),
                                        ),
                                ),
                        ),
                    ),
                batch =
                    Batch(
                        lotNumber = "e".asFHIR(),
                        expirationDate = DateTime("2022-10-14"),
                    ),
            )

        // transformation
        val transformResponse = transformer.transform(medication, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            RoninProfile.MEDICATION.value,
            transformed.meta!!.profile[0].value,
        )
        assertEquals(medication.implicitRules, transformed.implicitRules)
        assertEquals(medication.language, transformed.language)
        assertEquals(medication.text, transformed.text)
        assertEquals(medication.contained, transformed.contained)
        assertEquals(medication.extension, transformed.extension)
        assertEquals(medication.modifierExtension, transformed.modifierExtension)
        assertEquals(4, transformed.identifier.size)
        assertEquals(
            listOf(
                Identifier(value = "67890".asFHIR()),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR(),
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR(),
                ),
            ),
            transformed.identifier,
        )
        assertEquals(medication.code, transformed.code)
        assertEquals(Code(value = "active"), transformed.status)
        assertEquals(Reference(reference = "Organization/c".asFHIR()), transformed.manufacturer)
        assertEquals(medication.form, transformed.form)
        assertEquals(medication.amount, transformed.amount)
        assertEquals(medication.ingredient, transformed.ingredient)
        assertEquals(medication.ingredient, transformed.ingredient)
        assertEquals(medication.batch, transformed.batch)
    }
}
