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
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Narrative
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.LocationHoursOfOperation
import com.projectronin.interop.fhir.r4.resource.LocationPosition
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.r4.valueset.DayOfWeek
import com.projectronin.interop.fhir.r4.valueset.LocationMode
import com.projectronin.interop.fhir.r4.valueset.LocationStatus
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

class RoninLocationTransformerTest {
    private val transformer = RoninLocationTransformer()

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }

    @Test
    fun `returns supported resource`() {
        assertEquals(Location::class, transformer.supportedResource)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(transformer.qualifies(Location()))
    }

    @Test
    fun `transforms location with all attributes`() {
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

        val operationalStatus =
            Coding(code = Code("O"), system = Uri(value = "http://terminology.hl7.org/CodeSystem/v2-0116"))
        val type =
            listOf(
                CodeableConcept(
                    text = "Diagnostic".asFHIR(),
                    coding =
                        listOf(
                            Coding(
                                code = Code("DX"),
                                system = Uri(value = "http://terminology.hl7.org/ValueSet/v3-ServiceDeliveryLocationRoleType"),
                            ),
                        ),
                ),
            )
        val physicalType =
            CodeableConcept(
                text = "Room".asFHIR(),
                coding =
                    listOf(
                        Coding(
                            code = Code("ro"),
                            system = Uri(value = "http://terminology.hl7.org/CodeSystem/location-physical-type"),
                        ),
                    ),
            )
        val hoursOfOperation =
            listOf(
                LocationHoursOfOperation(
                    daysOfWeek = listOf(DayOfWeek.SATURDAY.asCode(), DayOfWeek.SUNDAY.asCode()),
                    allDay = FHIRBoolean.TRUE,
                ),
            )
        val position = LocationPosition(longitude = Decimal(13.81531), latitude = Decimal(66.077132))
        val endpoint = listOf(Reference(reference = "Endpoint/4321".asFHIR()))
        val location =
            Location(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical("https://www.hl7.org/fhir/location")),
                        source = Uri("source"),
                    ),
                implicitRules = Uri("implicit-rules"),
                language = Code("en-US"),
                text = Narrative(status = NarrativeStatus.GENERATED.asCode(), div = "div".asFHIR()),
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
                status = LocationStatus.ACTIVE.asCode(),
                operationalStatus = operationalStatus,
                name = "My Office".asFHIR(),
                alias = listOf("Guest Room").asFHIR(),
                description = "Sun Room".asFHIR(),
                mode = LocationMode.INSTANCE.asCode(),
                type = type,
                telecom = telecom,
                address = Address(country = "USA".asFHIR()),
                physicalType = physicalType,
                position = position,
                managingOrganization = Reference(reference = "Organization/1234".asFHIR()),
                partOf = Reference(reference = "Location/1234".asFHIR()),
                hoursOfOperation = hoursOfOperation,
                availabilityExceptions = "Call for details".asFHIR(),
                endpoint = endpoint,
            )

        val transformResponse = transformer.transform(location, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Location", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.LOCATION.value)), source = Uri("source")),
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
        assertEquals(LocationStatus.ACTIVE.asCode(), transformed.status)
        assertEquals(operationalStatus, transformed.operationalStatus)
        assertEquals("My Office".asFHIR(), transformed.name)
        assertEquals("Guest Room".asFHIR(), transformed.alias.first())
        assertEquals("Sun Room".asFHIR(), transformed.description)
        assertEquals(LocationMode.INSTANCE.asCode(), transformed.mode)
        assertEquals(type, transformed.type)
        assertEquals(telecom, transformed.telecom)
        assertEquals(Address(country = "USA".asFHIR()), transformed.address)
        assertEquals(physicalType, transformed.physicalType)
        assertEquals(position, transformed.position)
        assertEquals(Reference(reference = "Organization/1234".asFHIR()), transformed.managingOrganization)
        assertEquals(Reference(reference = "Location/1234".asFHIR()), transformed.partOf)
        assertEquals(hoursOfOperation, transformed.hoursOfOperation)
        assertEquals("Call for details".asFHIR(), transformed.availabilityExceptions)
        assertEquals(listOf(Reference(reference = "Endpoint/4321".asFHIR())), transformed.endpoint)
    }

    @Test
    fun `transforms location with only required attributes`() {
        val location =
            Location(
                id = Id("12345"),
                meta = Meta(source = Uri("source")),
                name = "Name".asFHIR(),
            )

        val transformResponse = transformer.transform(location, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Location", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.LOCATION.value)), source = Uri("source")),
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
        assertNull(transformed.status)
        assertNull(transformed.operationalStatus)
        assertEquals("Name".asFHIR(), transformed.name)
        assertEquals(listOf<String>(), transformed.alias)
        assertNull(transformed.description)
        assertNull(transformed.mode)
        assertEquals(listOf<CodeableConcept>(), transformed.type)
        assertEquals(listOf<ContactPoint>(), transformed.telecom)
        assertNull(transformed.address)
        assertNull(transformed.physicalType)
        assertNull(transformed.position)
        assertNull(transformed.managingOrganization)
        assertNull(transformed.partOf)
        assertEquals(listOf<LocationHoursOfOperation>(), transformed.hoursOfOperation)
        assertNull(transformed.availabilityExceptions)
        assertEquals(listOf<Reference>(), transformed.endpoint)
    }

    @Test
    fun `transforms location when no name is provided`() {
        val location =
            Location(
                id = Id("12345"),
                meta = Meta(source = Uri("source")),
                identifier =
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
            )

        val locationTransformer = transformer.transformInternal(location, tenant)

        locationTransformer!!
        val transformed = locationTransformer.resource
        assertEquals(0, locationTransformer.embeddedResources.size)
        assertEquals("Unnamed Location", transformed.name?.value)
    }

    @Test
    fun `transforms location when empty name is provided`() {
        val location =
            Location(
                id = Id("12345"),
                meta = Meta(source = Uri("source")),
                identifier =
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
                name = "".asFHIR(),
            )

        val locationTransformer = transformer.transformInternal(location, tenant)

        locationTransformer!!
        val transformed = locationTransformer.resource
        assertEquals(0, locationTransformer.embeddedResources.size)
        assertEquals("Unnamed Location", transformed.name?.value)
    }

    @Test
    fun `transforms location with name containing id and extensions`() {
        val name =
            FHIRString(
                value = "Name",
                id = FHIRString("id"),
                extension =
                    listOf(
                        Extension(
                            url = Uri("http://localhost/extension"),
                            value = DynamicValue(DynamicValueType.STRING, "Value"),
                        ),
                    ),
            )
        val location =
            Location(
                id = Id("12345"),
                meta = Meta(source = Uri("source")),
                name = name,
            )

        val transformResponse = transformer.transform(location, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Location", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(Canonical(RoninProfile.LOCATION.value)), source = Uri("source")),
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
        assertNull(transformed.status)
        assertNull(transformed.operationalStatus)
        assertEquals(name, transformed.name)
        assertEquals(listOf<String>(), transformed.alias)
        assertNull(transformed.description)
        assertNull(transformed.mode)
        assertEquals(listOf<CodeableConcept>(), transformed.type)
        assertEquals(listOf<ContactPoint>(), transformed.telecom)
        assertNull(transformed.address)
        assertNull(transformed.physicalType)
        assertNull(transformed.position)
        assertNull(transformed.managingOrganization)
        assertNull(transformed.partOf)
        assertEquals(listOf<LocationHoursOfOperation>(), transformed.hoursOfOperation)
        assertNull(transformed.availabilityExceptions)
        assertEquals(listOf<Reference>(), transformed.endpoint)
    }
}
