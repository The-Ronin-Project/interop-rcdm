package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Annotation
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.SimpleQuantity
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRInteger
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Markdown
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.CarePlan
import com.projectronin.interop.fhir.r4.resource.CarePlanActivity
import com.projectronin.interop.fhir.r4.resource.CarePlanDetail
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninCarePlanTransformerTest {
    private val transformer = RoninCarePlanTransformer()

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `returns supported resource`() {
        assertEquals(CarePlan::class, transformer.supportedResource)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(transformer.qualifies(CarePlan()))
    }

    @Test
    fun `transforms care-plan with all attributes`() {
        val carePlan = CarePlan(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/encounter.html")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(
                status = com.projectronin.interop.fhir.r4.valueset.NarrativeStatus.GENERATED.asCode(),
                div = "div".asFHIR()
            ),
            contained = listOf(Location(id = Id("67890"))),
            extension = listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
                )
            ),
            modifierExtension = listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            identifier = listOf(Identifier(value = "id".asFHIR())),
            instantiatesCanonical = listOf(
                Canonical(
                    value = "canonical"
                )
            ),
            instantiatesUri = listOf(Uri("uri")),
            basedOn = listOf(Reference(reference = "reference".asFHIR())),
            replaces = listOf(Reference(reference = "reference".asFHIR())),
            partOf = listOf(Reference(reference = "reference".asFHIR())),
            status = com.projectronin.interop.fhir.r4.valueset.RequestStatus.DRAFT.asCode(),
            intent = com.projectronin.interop.fhir.r4.valueset.CarePlanIntent.OPTION.asCode(),
            category = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CAREPLAN_CATEGORY.uri,
                            code = Code("assess-plan")
                        )
                    )
                )
            ),
            title = "CarePlan Title".asFHIR(),
            description = "CarePlan Description".asFHIR(),
            subject = Reference(reference = "Patient/test-1234".asFHIR()),
            encounter = Reference(reference = "reference".asFHIR()),
            period = Period(start = DateTime("2021"), end = DateTime("2022")),
            created = DateTime("2022"),
            author = Reference(reference = "author123".asFHIR()),
            contributor = listOf(
                Reference(reference = "contributor123".asFHIR())
            ),
            careTeam = listOf(
                Reference(reference = "careteam123".asFHIR())
            ),
            addresses = listOf(
                Reference(reference = "address123".asFHIR())
            ),
            supportingInfo = listOf(
                Reference(reference = "supportingInfo123".asFHIR())
            ),
            goal = listOf(
                Reference(reference = "goal123".asFHIR())
            ),
            activity = listOf(
                CarePlanActivity(
                    id = "67890".asFHIR(),
                    extension = listOf(
                        Extension(
                            url = Uri("http://localhost/extension"),
                            value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR())
                        )
                    ),
                    modifierExtension = listOf(
                        Extension(
                            url = Uri("http://localhost/modifier-extension"),
                            value = DynamicValue(DynamicValueType.INTEGER, FHIRInteger(1))
                        )
                    ),
                    outcomeCodeableConcept = listOf(
                        CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("http://terminology.hl7.org/CodeSystem/diagnosis-role"),
                                    code = Code("DD"),
                                    display = "Discharge diagnosis".asFHIR()
                                )
                            )
                        )
                    ),
                    outcomeReference = listOf(
                        Reference(reference = "outcome".asFHIR())
                    ),
                    progress = listOf(
                        Annotation(text = Markdown("123"))
                    ),
                    detail = CarePlanDetail(
                        id = "12345".asFHIR(),
                        extension = listOf(
                            Extension(
                                url = Uri("http://localhost/extension"),
                                value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR())
                            )
                        ),
                        modifierExtension = listOf(
                            Extension(
                                url = Uri("http://localhost/modifier-extension"),
                                value = DynamicValue(DynamicValueType.INTEGER, FHIRInteger(1))
                            )
                        ),
                        kind = Code("Appointment"),
                        instantiatesCanonical = listOf(
                            Canonical(
                                value = "canonical"
                            )
                        ),
                        instantiatesUri = listOf(Uri("uri")),
                        code = CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("http://terminology.hl7.org/CodeSystem/diagnosis-role"),
                                    code = Code("DD"),
                                    display = "Discharge diagnosis".asFHIR()
                                )
                            )
                        ),
                        reasonCode = listOf(
                            CodeableConcept(
                                coding = listOf(
                                    Coding(
                                        system = Uri("http://terminology.hl7.org/CodeSystem/diagnosis-role"),
                                        code = Code("DD"),
                                        display = "Discharge diagnosis".asFHIR()
                                    )
                                )
                            )
                        ),
                        goal = listOf(
                            Reference(reference = "ABC123".asFHIR())
                        ),
                        status = Code("scheduled"),
                        statusReason = CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("http://terminology.hl7.org/CodeSystem/diagnosis-role"),
                                    code = Code("DD"),
                                    display = "Discharge diagnosis".asFHIR()
                                )
                            )
                        ),
                        doNotPerform = FHIRBoolean.TRUE,
                        scheduled = DynamicValue(DynamicValueType.STRING, "Value".asFHIR()),
                        location = Reference(reference = "DEF123".asFHIR()),
                        performer = listOf(Reference(reference = "GHI123".asFHIR())),
                        product = DynamicValue(
                            DynamicValueType.CODEABLE_CONCEPT,
                            CodeableConcept(text = "product".asFHIR())
                        ),
                        dailyAmount = SimpleQuantity(value = Decimal(1.1)),
                        quantity = SimpleQuantity(value = Decimal(2.2)),
                        description = "Description".asFHIR()
                    )
                )
            )
        )

        val transformResponse = transformer.transform(carePlan, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource

        assertEquals("CarePlan", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.CARE_PLAN.value)), source = Uri("source")),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(
            Narrative(
                status = com.projectronin.interop.fhir.r4.valueset.NarrativeStatus.GENERATED.asCode(),
                div = "div".asFHIR()
            ),
            transformed.text
        )
        assertEquals(
            listOf(Location(id = Id("67890"))),
            transformed.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
                ),
                Extension(
                    url = RoninExtension.TENANT_SOURCE_CARE_PLAN_CATEGORY.uri,
                    value = DynamicValue(
                        DynamicValueType.CODEABLE_CONCEPT,
                        CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = CodeSystem.CAREPLAN_CATEGORY.uri,
                                    code = Code("assess-plan")
                                )
                            )
                        )
                    )
                )
            ),
            transformed.extension
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value")
                )
            ),
            transformed.modifierExtension
        )
        assertEquals(
            listOf(
                Identifier(value = "id".asFHIR()),
                Identifier(
                    type = CodeableConcepts.RONIN_FHIR_ID,
                    system = CodeSystem.RONIN_FHIR_ID.uri,
                    value = "12345".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_TENANT,
                    system = CodeSystem.RONIN_TENANT.uri,
                    value = "test".asFHIR()
                ),
                Identifier(
                    type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                    system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                    value = "EHR Data Authority".asFHIR()
                )
            ),
            transformed.identifier
        )
        assertEquals(
            listOf(
                Canonical(value = "canonical")
            ),
            transformed.instantiatesCanonical
        )
        assertEquals(
            listOf(
                Uri(value = "uri")
            ),
            transformed.instantiatesUri
        )
        assertEquals(
            listOf(
                Reference(
                    reference = "reference".asFHIR()
                )
            ),
            transformed.basedOn
        )
        assertEquals(
            listOf(
                Reference(
                    reference = "reference".asFHIR()
                )
            ),
            transformed.replaces
        )
        assertEquals(
            listOf(
                Reference(
                    reference = "reference".asFHIR()
                )
            ),
            transformed.partOf
        )
        assertEquals(
            com.projectronin.interop.fhir.r4.valueset.RequestStatus.DRAFT.asCode(),
            transformed.status
        )
        assertEquals(
            com.projectronin.interop.fhir.r4.valueset.CarePlanIntent.OPTION.asCode(),
            transformed.intent
        )
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = CodeSystem.CAREPLAN_CATEGORY.uri,
                            code = Code("assess-plan")
                        )
                    )
                )
            ),
            transformed.category
        )
        assertEquals("CarePlan Title".asFHIR(), transformed.title)
        assertEquals("CarePlan Description".asFHIR(), transformed.description)
        assertEquals(Reference(reference = "Patient/test-1234".asFHIR()), transformed.subject)
        assertEquals(Reference(reference = "reference".asFHIR()), transformed.encounter)
        assertEquals(
            Period(
                start = DateTime("2021"),
                end = DateTime("2022")
            ),
            transformed.period
        )
        assertEquals(DateTime("2022"), transformed.created)
        assertEquals(Reference(reference = "author123".asFHIR()), transformed.author)
        assertEquals(listOf(Reference(reference = "contributor123".asFHIR())), transformed.contributor)
        assertEquals(listOf(Reference(reference = "careteam123".asFHIR())), transformed.careTeam)
        assertEquals(listOf(Reference(reference = "address123".asFHIR())), transformed.addresses)
        assertEquals(listOf(Reference(reference = "supportingInfo123".asFHIR())), transformed.supportingInfo)
        assertEquals(listOf(Reference(reference = "goal123".asFHIR())), transformed.goal)
        assertEquals(
            listOf(
                CarePlanActivity(
                    id = "67890".asFHIR(),
                    extension = listOf(
                        Extension(
                            url = Uri("http://localhost/extension"),
                            value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR())
                        )
                    ),
                    modifierExtension = listOf(
                        Extension(
                            url = Uri("http://localhost/modifier-extension"),
                            value = DynamicValue(DynamicValueType.INTEGER, FHIRInteger(1))
                        )
                    ),
                    outcomeCodeableConcept = listOf(
                        CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("http://terminology.hl7.org/CodeSystem/diagnosis-role"),
                                    code = Code("DD"),
                                    display = "Discharge diagnosis".asFHIR()
                                )
                            )
                        )
                    ),
                    outcomeReference = listOf(
                        Reference(reference = "outcome".asFHIR())
                    ),
                    progress = listOf(
                        Annotation(text = Markdown("123"))
                    ),
                    detail = CarePlanDetail(
                        id = "12345".asFHIR(),
                        extension = listOf(
                            Extension(
                                url = Uri("http://localhost/extension"),
                                value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR())
                            )
                        ),
                        modifierExtension = listOf(
                            Extension(
                                url = Uri("http://localhost/modifier-extension"),
                                value = DynamicValue(DynamicValueType.INTEGER, FHIRInteger(1))
                            )
                        ),
                        kind = Code("Appointment"),
                        instantiatesCanonical = listOf(
                            Canonical(
                                value = "canonical"
                            )
                        ),
                        instantiatesUri = listOf(Uri("uri")),
                        code = CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("http://terminology.hl7.org/CodeSystem/diagnosis-role"),
                                    code = Code("DD"),
                                    display = "Discharge diagnosis".asFHIR()
                                )
                            )
                        ),
                        reasonCode = listOf(
                            CodeableConcept(
                                coding = listOf(
                                    Coding(
                                        system = Uri("http://terminology.hl7.org/CodeSystem/diagnosis-role"),
                                        code = Code("DD"),
                                        display = "Discharge diagnosis".asFHIR()
                                    )
                                )
                            )
                        ),
                        goal = listOf(
                            Reference(reference = "ABC123".asFHIR())
                        ),
                        status = Code("scheduled"),
                        statusReason = CodeableConcept(
                            coding = listOf(
                                Coding(
                                    system = Uri("http://terminology.hl7.org/CodeSystem/diagnosis-role"),
                                    code = Code("DD"),
                                    display = "Discharge diagnosis".asFHIR()
                                )
                            )
                        ),
                        doNotPerform = FHIRBoolean.TRUE,
                        scheduled = DynamicValue(DynamicValueType.STRING, "Value".asFHIR()),
                        location = Reference(reference = "DEF123".asFHIR()),
                        performer = listOf(Reference(reference = "GHI123".asFHIR())),
                        product = DynamicValue(
                            DynamicValueType.CODEABLE_CONCEPT,
                            CodeableConcept(text = "product".asFHIR())
                        ),
                        dailyAmount = SimpleQuantity(value = Decimal(1.1)),
                        quantity = SimpleQuantity(value = Decimal(2.2)),
                        description = "Description".asFHIR()
                    )
                )
            ),
            transformed.activity
        )
    }
}
