package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.AvailableTime
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.NotAvailable
import com.projectronin.interop.fhir.r4.resource.PractitionerRole
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninPractitionerRoleTransformerTest {
    private val transformer = RoninPractitionerRoleTransformer()

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }

    @Test
    fun `returns supported resource`() {
        assertEquals(PractitionerRole::class, transformer.supportedResource)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(transformer.qualifies(PractitionerRole()))
    }

    @Test
    fun `transforms practitioner role with only required attributes`() {
        val practitionerRole =
            PractitionerRole(
                id = Id("12345"),
                meta = Meta(source = Uri("source")),
                practitioner = Reference(reference = "Practitioner/1234".asFHIR()),
                organization = Reference(reference = "Organization/5678".asFHIR()),
            )

        val transformResponse = transformer.transform(practitionerRole, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("PractitionerRole", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER_ROLE.value)), source = Uri("source")),
            transformed.meta,
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.extension)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
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
        assertNull(transformed.active)
        assertNull(transformed.period)
        assertEquals(Reference(reference = "Practitioner/1234".asFHIR()), transformed.practitioner)
        assertEquals(Reference(reference = "Organization/5678".asFHIR()), transformed.organization)
        assertEquals(listOf<CodeableConcept>(), transformed.code)
        assertEquals(listOf<CodeableConcept>(), transformed.specialty)
        assertEquals(listOf<Reference>(), transformed.location)
        assertEquals(listOf<Reference>(), transformed.healthcareService)
        assertEquals(listOf<ContactPoint>(), transformed.telecom)
        assertEquals(listOf<AvailableTime>(), transformed.availableTime)
        assertEquals(listOf<NotAvailable>(), transformed.notAvailable)
        assertNull(transformed.availabilityExceptions)
        assertEquals(listOf<Reference>(), transformed.endpoint)
    }

    @Test
    fun `transforms practitioner role with all attributes`() {
        val telecom =
            listOf(
                ContactPoint(
                    id = "12345".asFHIR(),
                    extension =
                        listOf(
                            Extension(
                                url = Uri("http://localhost/extension"),
                                value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR()),
                            ),
                        ),
                    system = ContactPointSystem.PHONE.asCode(),
                    use = ContactPointUse.MOBILE.asCode(),
                    value = "8675309".asFHIR(),
                    rank = PositiveInt(1),
                    period =
                        Period(
                            start = DateTime("2021-11-18"),
                            end = DateTime("2022-11-17"),
                        ),
                ),
                ContactPoint(
                    system = Code("telephone"),
                    value = "1112223333".asFHIR(),
                ),
            )

        val practitionerRole =
            PractitionerRole(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical("https://www.hl7.org/fhir/practitioner-role")),
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
                            url = Uri("http://localhost/extension"),
                            value = DynamicValue(DynamicValueType.STRING, "Value"),
                        ),
                    ),
                modifierExtension =
                    listOf(
                        Extension(
                            url = Uri("http://localhost/modifier-extension"),
                            value = DynamicValue(DynamicValueType.STRING, "Value"),
                        ),
                    ),
                identifier = listOf(Identifier(value = "id".asFHIR())),
                active = FHIRBoolean.TRUE,
                period = Period(end = DateTime("2022")),
                practitioner = Reference(reference = "Practitioner/1234".asFHIR()),
                organization = Reference(reference = "Organization/5678".asFHIR()),
                code = listOf(CodeableConcept(text = "code".asFHIR())),
                specialty = listOf(CodeableConcept(text = "specialty".asFHIR())),
                location = listOf(Reference(reference = "Location/9012".asFHIR())),
                healthcareService = listOf(Reference(reference = "HealthcareService/3456".asFHIR())),
                telecom = telecom,
                availableTime = listOf(AvailableTime(allDay = FHIRBoolean.FALSE)),
                notAvailable = listOf(NotAvailable(description = "Not available now".asFHIR())),
                availabilityExceptions = "exceptions".asFHIR(),
                endpoint = listOf(Reference(reference = "Endpoint/1357".asFHIR())),
            )

        val transformResponse = transformer.transform(practitionerRole, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("PractitionerRole", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.PRACTITIONER_ROLE.value)), source = Uri("source")),
            transformed.meta,
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()), transformed.text)
        assertEquals(
            listOf(Location(id = Id("67890"))),
            transformed.contained,
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value"),
                ),
            ),
            transformed.extension,
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value"),
                ),
            ),
            transformed.modifierExtension,
        )
        assertEquals(
            listOf(
                Identifier(value = "id".asFHIR()),
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
        assertEquals(FHIRBoolean.TRUE, transformed.active)
        assertEquals(Period(end = DateTime("2022")), transformed.period)
        assertEquals(Reference(reference = "Practitioner/1234".asFHIR()), transformed.practitioner)
        assertEquals(Reference(reference = "Organization/5678".asFHIR()), transformed.organization)
        assertEquals(listOf(CodeableConcept(text = "code".asFHIR())), transformed.code)
        assertEquals(listOf(CodeableConcept(text = "specialty".asFHIR())), transformed.specialty)
        assertEquals(listOf(Reference(reference = "Location/9012".asFHIR())), transformed.location)
        assertEquals(
            listOf(Reference(reference = "HealthcareService/3456".asFHIR())),
            transformed.healthcareService,
        )
        assertEquals(telecom, transformed.telecom)
        assertEquals(listOf(AvailableTime(allDay = FHIRBoolean.FALSE)), transformed.availableTime)
        assertEquals(listOf(NotAvailable(description = "Not available now".asFHIR())), transformed.notAvailable)
        assertEquals("exceptions".asFHIR(), transformed.availabilityExceptions)
        assertEquals(listOf(Reference(reference = "Endpoint/1357".asFHIR())), transformed.endpoint)
    }
}
