package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.HumanName
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Organization
import com.projectronin.interop.fhir.r4.resource.OrganizationContact
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninOrganizationTransformerTest {
    private val transformer = RoninOrganizationTransformer()

    private val tenant = mockk<Tenant> {
        every { mnemonic } returns "test"
    }

    @Test
    fun `returns supported resource`() {
        assertEquals(Organization::class, transformer.supportedResource)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(transformer.qualifies(Organization()))
    }

    @Test
    fun `transform organization with all attributes`() {
        val organization = Organization(
            id = Id("12345"),
            meta = Meta(
                profile = listOf(Canonical("http://hl7.org/fhir/R4/organization.html")),
                source = Uri("source")
            ),
            implicitRules = Uri("implicit-rules"),
            language = Code("en-US"),
            text = Narrative(status = com.projectronin.interop.fhir.r4.valueset.NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
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
            active = true.asFHIR(),
            type = listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/organization-type"),
                            code = Code("prov"),
                            display = "Healthcare Provider".asFHIR()
                        )
                    )
                )
            ),
            name = "Organization Name".asFHIR(),
            alias = listOf(
                "Other Organization Name".asFHIR(),
                "Organization also known as...".asFHIR()
            ),
            telecom = listOf(
                ContactPoint(
                    id = "FAKEID".asFHIR(),
                    system = Code("phone"),
                    value = "555-555-5555".asFHIR(),
                    use = Code("work")
                )
            ),
            address = listOf(
                Address(
                    country = "USA".asFHIR()
                )
            ),
            partOf = Reference(reference = "Organization/super".asFHIR()),
            contact = listOf(
                OrganizationContact(
                    purpose = CodeableConcept(
                        coding = listOf(
                            Coding(
                                code = Code("fake")
                            )
                        )
                    ),
                    name = HumanName(
                        given = listOf(
                            "FakeName".asFHIR()
                        )
                    ),
                    telecom = listOf(
                        ContactPoint(
                            system = Code("phone"),
                            value = "555-555-5555".asFHIR()
                        )
                    ),
                    address = Address(
                        country = "USA".asFHIR()
                    )
                )
            ),
            endpoint = listOf(
                Reference(
                    reference = "Endpoint/1357".asFHIR()
                )
            )
        )

        val transformResponse = transformer.transform(organization, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource

        assertEquals("Organization", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.ORGANIZATION.value)), source = Uri("source")),
            transformed.meta
        )
        assertEquals(Uri("implicit-rules"), transformed.implicitRules)
        assertEquals(Code("en-US"), transformed.language)
        assertEquals(Narrative(status = com.projectronin.interop.fhir.r4.valueset.NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()), transformed.text)
        assertEquals(
            listOf(Location(id = Id("67890"))),
            transformed.contained
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://hl7.org/extension-1"),
                    value = DynamicValue(DynamicValueType.STRING, "value")
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
        Assertions.assertNotNull(transformed.active)
        assertEquals(
            listOf(
                CodeableConcept(
                    coding = listOf(
                        Coding(
                            system = Uri("http://terminology.hl7.org/CodeSystem/organization-type"),
                            code = Code("prov"),
                            display = "Healthcare Provider".asFHIR()
                        )
                    )
                )
            ),
            transformed.type
        )
        Assertions.assertNotNull(transformed.name)
        assertEquals(
            listOf(
                "Other Organization Name".asFHIR(),
                "Organization also known as...".asFHIR()
            ),
            transformed.alias
        )
        assertEquals(
            listOf(
                ContactPoint(
                    id = "FAKEID".asFHIR(),
                    system = Code("phone"),
                    value = "555-555-5555".asFHIR(),
                    use = Code("work")
                )
            ),
            transformed.telecom
        )
        assertEquals(
            listOf(
                Address(
                    country = "USA".asFHIR()
                )
            ),
            transformed.address
        )
        assertEquals(Reference(reference = "Organization/super".asFHIR()), transformed.partOf)
        assertEquals(
            listOf(
                OrganizationContact(
                    purpose = CodeableConcept(
                        coding = listOf(
                            Coding(
                                code = Code("fake")
                            )
                        )
                    ),
                    name = HumanName(
                        given = listOf(
                            "FakeName".asFHIR()
                        )
                    ),
                    telecom = listOf(
                        ContactPoint(
                            system = Code("phone"),
                            value = "555-555-5555".asFHIR()
                        )
                    ),
                    address = Address(
                        country = "USA".asFHIR()
                    )
                )
            ),
            transformed.contact
        )
        assertEquals(
            listOf(
                Reference(
                    reference = "Endpoint/1357".asFHIR()
                )
            ),
            transformed.endpoint
        )
    }

    @Test
    fun `transform organization with only required attributes`() {
        val organization = Organization(
            id = Id("12345"),
            meta = Meta(source = Uri("source")),
            name = "Organization name".asFHIR(),
            active = true.asFHIR()
        )

        val transformResponse = transformer.transform(organization, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource

        assertEquals("Organization", transformed.resourceType)
        assertEquals(Id(value = "12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.ORGANIZATION.value)), source = Uri("source")),
            transformed.meta
        )
        Assertions.assertNull(transformed.implicitRules)
        Assertions.assertNull(transformed.language)
        Assertions.assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            listOf(
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
        Assertions.assertNotNull(transformed.active)
        assertEquals(listOf<CodeableConcept>(), transformed.type)
        Assertions.assertNotNull(transformed.name)
        assertEquals(listOf<String>(), transformed.alias)
        assertEquals(listOf<ContactPoint>(), transformed.telecom)
        assertEquals(listOf<Address>(), transformed.address)
        Assertions.assertNull(transformed.partOf)
        assertEquals(listOf<OrganizationContact>(), transformed.contact)
        assertEquals(listOf<Reference>(), transformed.endpoint)
    }
}
