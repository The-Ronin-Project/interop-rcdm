package com.projectronin.interop.rcdm.transform.profile

import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.Address
import com.projectronin.interop.fhir.r4.datatype.Attachment
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
import com.projectronin.interop.fhir.r4.datatype.Period
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Base64Binary
import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Date
import com.projectronin.interop.fhir.r4.datatype.primitive.DateTime
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRInteger
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.PositiveInt
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Communication
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.PatientCommunication
import com.projectronin.interop.fhir.r4.resource.PatientContact
import com.projectronin.interop.fhir.r4.resource.PatientLink
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.valueset.AdministrativeGender
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.fhir.r4.valueset.ContactPointUse
import com.projectronin.interop.fhir.r4.valueset.IdentifierUse
import com.projectronin.interop.fhir.r4.valueset.LinkType
import com.projectronin.interop.fhir.r4.valueset.NarrativeStatus
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.util.dataAbsentReasonExtension
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoninPatientTransformerTest {
    private val transformer = RoninPatientTransformer()

    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }

    private val identifierList =
        listOf(
            Identifier(
                type =
                    CodeableConcept(
                        text = "MRN".asFHIR(),
                    ),
                system = Uri("mrnSystem"),
                value = "An MRN".asFHIR(),
            ),
            Identifier(
                type = CodeableConcepts.RONIN_FHIR_ID,
                system = CodeSystem.RONIN_FHIR_ID.uri,
                value = "12345".asFHIR(),
            ),
            Identifier(
                type = CodeableConcepts.RONIN_MRN,
                system = CodeSystem.RONIN_MRN.uri,
                value = "An MRN".asFHIR(),
            ),
        )

    @Test
    fun `returns supported resource`() {
        assertEquals(Patient::class, transformer.supportedResource)
    }

    @Test
    fun `always qualifies`() {
        assertTrue(transformer.qualifies(Patient()))
    }

    @Test
    fun `transforms patient with all attributes`() {
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

        val patient =
            Patient(
                id = Id("12345"),
                meta =
                    Meta(
                        profile = listOf(Canonical("https://www.hl7.org/fhir/patient")),
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
                            value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR()),
                        ),
                    ),
                modifierExtension =
                    listOf(
                        Extension(
                            url = Uri("http://localhost/modifier-extension"),
                            value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR()),
                        ),
                    ),
                identifier = identifierList,
                active = FHIRBoolean.TRUE,
                name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
                telecom = telecom,
                gender = AdministrativeGender.FEMALE.asCode(),
                birthDate = Date("1975-07-05"),
                deceased = DynamicValue(DynamicValueType.BOOLEAN, FHIRBoolean.FALSE),
                address = listOf(Address(country = "USA".asFHIR())),
                maritalStatus = CodeableConcept(text = "M".asFHIR()),
                multipleBirth = DynamicValue(DynamicValueType.INTEGER, FHIRInteger(2)),
                photo = listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
                contact = listOf(PatientContact(name = HumanName(text = "Jane Doe".asFHIR()))),
                communication = listOf(PatientCommunication(language = CodeableConcept(text = "English".asFHIR()))),
                generalPractitioner = listOf(Reference(display = "GP".asFHIR())),
                managingOrganization = Reference(display = "organization".asFHIR()),
                link =
                    listOf(
                        PatientLink(
                            other = Reference(display = "other patient".asFHIR()),
                            type = LinkType.REPLACES.asCode(),
                        ),
                    ),
            )

        val transformResponse = transformer.transform(patient, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Patient", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(RoninProfile.PATIENT.canonical), source = Uri("source")),
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
                    value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR()),
                ),
            ),
            transformed.extension,
        )
        assertEquals(
            listOf(
                Extension(
                    url = Uri("http://localhost/modifier-extension"),
                    value = DynamicValue(DynamicValueType.STRING, "Value".asFHIR()),
                ),
            ),
            transformed.modifierExtension,
        )
        assertEquals(
            identifierList +
                listOf(
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
        assertEquals(listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))), transformed.name)
        assertEquals(telecom, transformed.telecom)
        assertEquals(AdministrativeGender.FEMALE.asCode(), transformed.gender)
        assertEquals(Date("1975-07-05"), transformed.birthDate)
        assertEquals(DynamicValue(type = DynamicValueType.BOOLEAN, value = FHIRBoolean.FALSE), transformed.deceased)
        assertEquals(listOf(Address(country = "USA".asFHIR())), transformed.address)
        assertEquals(CodeableConcept(text = "M".asFHIR()), transformed.maritalStatus)
        assertEquals(
            DynamicValue(type = DynamicValueType.INTEGER, value = FHIRInteger(2)),
            transformed.multipleBirth,
        )
        assertEquals(
            listOf(Attachment(contentType = Code("text"), data = Base64Binary("abcd"))),
            transformed.photo,
        )
        assertEquals(
            listOf(PatientCommunication(language = CodeableConcept(text = "English".asFHIR()))),
            transformed.communication,
        )
        assertEquals(listOf(Reference(display = "GP".asFHIR())), transformed.generalPractitioner)
        assertEquals(Reference(display = "organization".asFHIR()), transformed.managingOrganization)
        assertEquals(
            listOf(
                PatientLink(
                    other = Reference(display = "other patient".asFHIR()),
                    type = LinkType.REPLACES.asCode(),
                ),
            ),
            transformed.link,
        )
    }

    @Test
    fun `transform adds data absent reason extension when identifier does not have a system value`() {
        val identifiers = identifierList + Identifier(system = null, value = "something".asFHIR())
        val patient =
            Patient(
                id = Id("12345"),
                meta = Meta(source = Uri("fake-source-fake-url")),
                identifier = identifiers,
                name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
                gender = AdministrativeGender.FEMALE.asCode(),
                birthDate = Date("1975-07-05"),
            )

        val transformResponse = transformer.transform(patient, tenant)

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals(
            identifierList +
                listOf(
                    Identifier(
                        system = Uri(value = null, extension = dataAbsentReasonExtension),
                        value = "something".asFHIR(),
                        id = null,
                        extension = emptyList(),
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
    }

    @Test
    fun `transforms patient with minimum attributes`() {
        val patient =
            Patient(
                id = Id("12345"),
                meta = Meta(source = Uri("source")),
                identifier = identifierList,
                name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
                gender = AdministrativeGender.FEMALE.asCode(),
                birthDate = Date("1975-07-05"),
            )

        val transformResponse = transformer.transform(patient, tenant)

        val defaultCoding =
            Coding(
                system = CodeSystem.NULL_FLAVOR.uri,
                code = Code("NI"),
                display = "NoInformation".asFHIR(),
            )

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Patient", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(RoninProfile.PATIENT.canonical), source = Uri("source")),
            transformed.meta,
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.extension)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            identifierList +
                listOf(
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
        assertEquals(listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))), transformed.name)
        assertEquals(emptyList<ContactPoint>(), transformed.telecom)
        assertEquals(AdministrativeGender.FEMALE.asCode(), transformed.gender)
        assertEquals(Date("1975-07-05"), transformed.birthDate)
        assertNull(transformed.deceased)
        assertEquals(emptyList<Address>(), transformed.address)
        assertEquals(listOf(defaultCoding), transformed.maritalStatus?.coding)
        assertNull(transformed.multipleBirth)
        assertEquals(listOf<Attachment>(), transformed.photo)
        assertEquals(listOf<Communication>(), transformed.communication)
        assertEquals(listOf<Reference>(), transformed.generalPractitioner)
        assertNull(transformed.managingOrganization)
        assertEquals(listOf<PatientLink>(), transformed.link)
    }

    @Test
    fun `transforms patient with value extensions`() {
        val identifierWithExtension =
            Identifier(
                use = IdentifierUse.USUAL.asCode(),
                system = Uri("urn:oid:2.16.840.1.113883.4.1"),
                value =
                    FHIRString(
                        value = null,
                        extension =
                            listOf(
                                Extension(
                                    url = Uri("http://hl7.org/fhir/StructureDefinition/rendered-value"),
                                    value = DynamicValue(DynamicValueType.STRING, "xxx-xx-1234".asFHIR()),
                                ),
                            ),
                    ),
            )

        val identifiers = identifierList + identifierWithExtension

        val patient =
            Patient(
                id = Id("12345"),
                meta = Meta(source = Uri("source")),
                identifier = identifiers,
                name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
                gender = AdministrativeGender.FEMALE.asCode(),
                birthDate = Date("1975-07-05"),
            )

        val transformResponse = transformer.transform(patient, tenant)

        val defaultCoding =
            Coding(
                system = CodeSystem.NULL_FLAVOR.uri,
                code = Code("NI"),
                display = "NoInformation".asFHIR(),
            )

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Patient", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(RoninProfile.PATIENT.canonical), source = Uri("source")),
            transformed.meta,
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.extension)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            identifierList +
                listOf(
                    identifierWithExtension,
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
        assertEquals(listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))), transformed.name)
        assertEquals(emptyList<ContactPoint>(), transformed.telecom)
        assertEquals(AdministrativeGender.FEMALE.asCode(), transformed.gender)
        assertEquals(Date("1975-07-05"), transformed.birthDate)
        assertNull(transformed.deceased)
        assertEquals(emptyList<Address>(), transformed.address)
        assertEquals(listOf(defaultCoding), transformed.maritalStatus?.coding)
        assertNull(transformed.multipleBirth)
        assertEquals(listOf<Attachment>(), transformed.photo)
        assertEquals(listOf<Communication>(), transformed.communication)
        assertEquals(listOf<Reference>(), transformed.generalPractitioner)
        assertNull(transformed.managingOrganization)
        assertEquals(listOf<PatientLink>(), transformed.link)
    }

    @Test
    fun `transforms gender with data absent reason into an Unknown gender`() {
        val patient =
            Patient(
                id = Id("12345"),
                meta = Meta(source = Uri("source")),
                identifier = identifierList,
                name = listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))),
                gender = Code(value = null, extension = dataAbsentReasonExtension),
                birthDate = Date("1975-07-05"),
            )

        val transformResponse = transformer.transform(patient, tenant)

        val defaultCoding =
            Coding(
                system = CodeSystem.NULL_FLAVOR.uri,
                code = Code("NI"),
                display = "NoInformation".asFHIR(),
            )

        transformResponse!!
        assertEquals(0, transformResponse.embeddedResources.size)

        val transformed = transformResponse.resource
        assertEquals("Patient", transformed.resourceType)
        assertEquals(Id("12345"), transformed.id)
        assertEquals(
            Meta(profile = listOf(RoninProfile.PATIENT.canonical), source = Uri("source")),
            transformed.meta,
        )
        assertNull(transformed.implicitRules)
        assertNull(transformed.language)
        assertNull(transformed.text)
        assertEquals(listOf<Resource<*>>(), transformed.contained)
        assertEquals(listOf<Extension>(), transformed.extension)
        assertEquals(listOf<Extension>(), transformed.modifierExtension)
        assertEquals(
            identifierList +
                listOf(
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
        assertEquals(listOf(HumanName(family = "Doe".asFHIR(), use = Code("official"))), transformed.name)
        assertEquals(emptyList<ContactPoint>(), transformed.telecom)
        assertEquals(Code(value = "unknown", extension = dataAbsentReasonExtension), transformed.gender)
        assertEquals(Date("1975-07-05"), transformed.birthDate)
        assertNull(transformed.deceased)
        assertEquals(emptyList<Address>(), transformed.address)
        assertEquals(listOf(defaultCoding), transformed.maritalStatus?.coding)
        assertNull(transformed.multipleBirth)
        assertEquals(listOf<Attachment>(), transformed.photo)
        assertEquals(listOf<Communication>(), transformed.communication)
        assertEquals(listOf<Reference>(), transformed.generalPractitioner)
        assertNull(transformed.managingOrganization)
        assertEquals(listOf<PatientLink>(), transformed.link)
    }
}
