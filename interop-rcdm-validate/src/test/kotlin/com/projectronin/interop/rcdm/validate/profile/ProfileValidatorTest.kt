package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.CodeableConcepts
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Binary
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Organization
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.validate.resource.R4BinaryValidator
import com.projectronin.interop.fhir.r4.validate.resource.R4PatientValidator
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.fhir.validate.Validation
import com.projectronin.interop.fhir.validate.append
import com.projectronin.interop.rcdm.common.enums.RCDMVersion
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import io.github.classgraph.ClassGraph
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.stereotype.Component
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import com.projectronin.interop.fhir.validate.ProfileValidator as R4ProfileValidator

@Suppress("ktlint:standard:max-line-length")
class ProfileValidatorTest {
    class TestValidator : ProfileValidator<Patient>() {
        override val supportedResource: KClass<Patient> = Patient::class
        override val r4Validator: R4ProfileValidator<Patient> = R4PatientValidator
        override val profile: RoninProfile = RoninProfile.PATIENT
        override val rcdmVersion: RCDMVersion = RCDMVersion.V3_19_0
        override val profileVersion: Int = 1

        override fun validate(
            resource: Patient,
            validation: Validation,
            context: LocationContext,
        ) {
            validateRoninNormalizedCodeableConcept(
                resource.maritalStatus,
                Patient::maritalStatus,
                null,
                context,
                validation,
            )

            validateReferenceType(
                resource.managingOrganization,
                listOf(ResourceType.Organization),
                context.append(LocationContext(Patient::managingOrganization)),
                validation,
                resource.contained,
            )

            resource.generalPractitioner.forEachIndexed { index, reference ->
                validateReferenceType(
                    reference,
                    listOf(ResourceType.Practitioner),
                    context.append(LocationContext("", "generalPractitioner[$index]")),
                    validation,
                )
            }
        }
    }

    private val tenantIdentifier =
        Identifier(
            type = CodeableConcepts.RONIN_TENANT,
            system = CodeSystem.RONIN_TENANT.uri,
            value = "test".asFHIR(),
        )
    private val fhirIdIdentifier =
        Identifier(
            type = CodeableConcepts.RONIN_FHIR_ID,
            system = CodeSystem.RONIN_FHIR_ID.uri,
            value = "12345".asFHIR(),
        )
    private val dataAuthorityIdentifier =
        Identifier(
            type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
            system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
            value = "EHR Data Authority".asFHIR(),
        )
    private val requiredIdentifiers = listOf(tenantIdentifier, fhirIdIdentifier, dataAuthorityIdentifier)

    @Test
    fun `does not qualify if no meta`() {
        val patient =
            Patient(
                meta = null,
            )

        val qualified = TestValidator().qualifies(patient)
        assertFalse(qualified)
    }

    @Test
    fun `does not qualify if meta does not contain profile`() {
        val patient =
            Patient(
                meta = Meta(profile = listOf(RoninProfile.APPOINTMENT.canonical)),
            )

        val qualified = TestValidator().qualifies(patient)
        assertFalse(qualified)
    }

    @Test
    fun `qualifies if meta contains profile`() {
        val patient =
            Patient(
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
            )

        val qualified = TestValidator().qualifies(patient)
        assertTrue(qualified)
    }

    @Test
    fun `validate fails if no id`() {
        val patient =
            Patient(
                id = null,
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals("ERROR REQ_FIELD: id is a required element @ Patient.id", validation.issues().first().toString())
    }

    @Test
    fun `validate fails if no meta`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = null,
                identifier = requiredIdentifiers,
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: meta is a required element @ Patient.meta",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if no tenant identifier`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = listOf(fhirIdIdentifier, dataAuthorityIdentifier),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_TNNT_ID_001: Tenant identifier is required @ Patient.identifier",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if wrong type on tenant identifier`() {
        val badTenantIdentifier =
            Identifier(
                type = CodeableConcepts.RONIN_MRN,
                system = CodeSystem.RONIN_TENANT.uri,
                value = "test".asFHIR(),
            )
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = listOf(fhirIdIdentifier, dataAuthorityIdentifier, badTenantIdentifier),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_TNNT_ID_002: Tenant identifier provided without proper CodeableConcept defined @ Patient.identifier",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if tenant identifier has no value`() {
        val badTenantIdentifier =
            Identifier(
                type = CodeableConcepts.RONIN_TENANT,
                system = CodeSystem.RONIN_TENANT.uri,
                value = null,
            )
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = listOf(fhirIdIdentifier, dataAuthorityIdentifier, badTenantIdentifier),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_TNNT_ID_003: Tenant identifier value is required @ Patient.identifier",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if no fhir ID identifier`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = listOf(tenantIdentifier, dataAuthorityIdentifier),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_FHIR_ID_001: FHIR identifier is required @ Patient.identifier",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if wrong type on fhir ID identifier`() {
        val badFhirIdIdentifier =
            Identifier(
                type = CodeableConcepts.RONIN_MRN,
                system = CodeSystem.RONIN_FHIR_ID.uri,
                value = "test".asFHIR(),
            )
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = listOf(tenantIdentifier, dataAuthorityIdentifier, badFhirIdIdentifier),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_FHIR_ID_002: FHIR identifier provided without proper CodeableConcept defined @ Patient.identifier",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if fhir ID identifier has no value`() {
        val badFhirIdIdentifier =
            Identifier(
                type = CodeableConcepts.RONIN_FHIR_ID,
                system = CodeSystem.RONIN_FHIR_ID.uri,
                value = null,
            )
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = listOf(tenantIdentifier, dataAuthorityIdentifier, badFhirIdIdentifier),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_FHIR_ID_003: FHIR identifier value is required @ Patient.identifier",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if no data authority identifier`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = listOf(tenantIdentifier, fhirIdIdentifier),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_DAUTH_ID_001: Data Authority identifier is required @ Patient.identifier",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if wrong type on data authority identifier`() {
        val badDataAuthorityIdentifier =
            Identifier(
                type = CodeableConcepts.RONIN_MRN,
                system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                value = "test".asFHIR(),
            )
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = listOf(tenantIdentifier, fhirIdIdentifier, badDataAuthorityIdentifier),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_DAUTH_ID_002: Data Authority identifier provided without proper CodeableConcept defined @ Patient.identifier",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if data authority identifier has no value`() {
        val badDataAuthorityIdentifier =
            Identifier(
                type = CodeableConcepts.RONIN_DATA_AUTHORITY_ID,
                system = CodeSystem.RONIN_DATA_AUTHORITY.uri,
                value = null,
            )
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = listOf(tenantIdentifier, fhirIdIdentifier, badDataAuthorityIdentifier),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_DAUTH_ID_003: Data Authority identifier value is required @ Patient.identifier",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate succeeds for a resource with no identifier or contained support`() {
        class BinaryValidator : ProfileValidator<Binary>() {
            override val supportedResource: KClass<Binary> = Binary::class
            override val r4Validator: R4ProfileValidator<Binary> = R4BinaryValidator
            override val profile: RoninProfile = RoninProfile.PATIENT
            override val rcdmVersion: RCDMVersion = RCDMVersion.V3_19_0
            override val profileVersion: Int = 1

            override fun validate(
                resource: Binary,
                validation: Validation,
                context: LocationContext,
            ) {
                // all good
            }
        }

        val binary =
            Binary(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                contentType = Code("text/plain"),
            )

        val validation = BinaryValidator().validate(binary, LocationContext(Binary::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate warns if contained resource is present`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                contained = listOf(Organization()),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "WARNING RONIN_CONTAINED_RESOURCE: There is a Contained Resource present @ Patient.contained",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateRoninNormalizedCodeableConcept succeeds if null codeable concept`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                maritalStatus = null,
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validateRoninNormalizedCodeableConcept fails if no coding`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                maritalStatus =
                    CodeableConcept(
                        coding = listOf(),
                    ),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_NOV_CODING_002: Must contain exactly 1 coding @ Patient.maritalStatus.coding",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateRoninNormalizedCodeableConcept fails if multiple codings`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                maritalStatus =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("system"),
                                    code = Code("code"),
                                    display = FHIRString("display"),
                                ),
                                Coding(
                                    system = Uri("system2"),
                                    code = Code("code2"),
                                    display = FHIRString("display2"),
                                ),
                            ),
                    ),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_NOV_CODING_002: Must contain exactly 1 coding @ Patient.maritalStatus.coding",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateRoninNormalizedCodeableConcept fails if coding has null system`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                maritalStatus =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = null,
                                    code = Code("code"),
                                    display = FHIRString("display"),
                                ),
                            ),
                    ),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_NOV_CODING_003: Coding system cannot be null or blank @ Patient.maritalStatus.coding[0].system",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateRoninNormalizedCodeableConcept fails if coding has system with null value`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                maritalStatus =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri(null),
                                    code = Code("code"),
                                    display = FHIRString("display"),
                                ),
                            ),
                    ),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_NOV_CODING_003: Coding system cannot be null or blank @ Patient.maritalStatus.coding[0].system",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateRoninNormalizedCodeableConcept fails if coding has system with blank value`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                maritalStatus =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri(""),
                                    code = Code("code"),
                                    display = FHIRString("display"),
                                ),
                            ),
                    ),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_NOV_CODING_003: Coding system cannot be null or blank @ Patient.maritalStatus.coding[0].system",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateRoninNormalizedCodeableConcept fails if coding has null code`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                maritalStatus =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("system"),
                                    code = null,
                                    display = FHIRString("display"),
                                ),
                            ),
                    ),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_NOV_CODING_004: Coding code cannot be null or blank @ Patient.maritalStatus.coding[0].code",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateRoninNormalizedCodeableConcept fails if coding has code with null value`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                maritalStatus =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("system"),
                                    code = Code(null),
                                    display = FHIRString("display"),
                                ),
                            ),
                    ),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_NOV_CODING_004: Coding code cannot be null or blank @ Patient.maritalStatus.coding[0].code",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateRoninNormalizedCodeableConcept fails if coding has code with blank value`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                maritalStatus =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("system"),
                                    code = Code(""),
                                    display = FHIRString("display"),
                                ),
                            ),
                    ),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_NOV_CODING_004: Coding code cannot be null or blank @ Patient.maritalStatus.coding[0].code",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateRoninNormalizedCodeableConcept fails if coding has null display`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                maritalStatus =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("system"),
                                    code = Code("code"),
                                    display = null,
                                ),
                            ),
                    ),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_NOV_CODING_005: Coding display cannot be null or blank @ Patient.maritalStatus.coding[0].display",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateRoninNormalizedCodeableConcept fails if coding has display with null value`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                maritalStatus =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("system"),
                                    code = Code("code"),
                                    display = FHIRString(null),
                                ),
                            ),
                    ),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_NOV_CODING_005: Coding display cannot be null or blank @ Patient.maritalStatus.coding[0].display",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateRoninNormalizedCodeableConcept fails if coding has display with blank value`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                maritalStatus =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("system"),
                                    code = Code("code"),
                                    display = FHIRString(""),
                                ),
                            ),
                    ),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_NOV_CODING_005: Coding display cannot be null or blank @ Patient.maritalStatus.coding[0].display",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateRoninNormalizedCodeableConcept succeeds`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                maritalStatus =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("system"),
                                    code = Code("code"),
                                    display = FHIRString("display"),
                                ),
                            ),
                    ),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validateReferenceType ignores null references`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                managingOrganization = null,
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validateReferenceType succeeds when reference is remote and for type`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                generalPractitioner = listOf(Reference(reference = "Practitioner/test-1234".asFHIR())),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validateReferenceType fails when reference is remote and for invalid type`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                generalPractitioner = listOf(Reference(reference = "Patient/test-1234".asFHIR())),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR INV_REF_TYPE: reference can only be one of the following: Practitioner @ Patient.generalPractitioner[0].reference",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateReferenceType succeeds when reference is local and for contained resource with proper type`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                managingOrganization = Reference(reference = "#5678".asFHIR()),
                contained = listOf(Organization(id = Id("5678"))),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        // It will have a contained resource warning
        assertEquals(1, validation.issues().size)
        assertEquals(
            "WARNING RONIN_CONTAINED_RESOURCE: There is a Contained Resource present @ Patient.contained",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validateReferenceType fails when reference is local and for contained resource with invalid type`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                managingOrganization = Reference(reference = "#5678".asFHIR()),
                contained = listOf(Location(id = Id("5678"))),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        // It will also have a contained resource warning
        assertEquals(2, validation.issues().size)
        assertEquals(
            "WARNING RONIN_CONTAINED_RESOURCE: There is a Contained Resource present @ Patient.contained",
            validation.issues()[0].toString(),
        )
        assertEquals(
            "ERROR INV_REF_TYPE: reference can only be one of the following: Organization @ Patient.managingOrganization.reference",
            validation.issues()[1].toString(),
        )
    }

    @Test
    fun `validateReferenceType succeeds when reference is local and no contained resources`() {
        val patient =
            Patient(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PATIENT.canonical)),
                identifier = requiredIdentifiers,
                managingOrganization = Reference(reference = "#5678".asFHIR()),
            )

        val validation = TestValidator().validate(patient, LocationContext(Patient::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_REQ_REF_1: Contained resource is required if a local reference is provided @ Patient.managingOrganization.reference",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `all classes are annotated`() {
        val classes =
            ClassGraph().acceptPackages("com.projectronin.interop.rcdm.validate.profile")
                .enableClassInfo()
                .scan().use {
                    it.getSubclasses(ProfileValidator::class.java).filterNot { classInfo ->
                        classInfo.isAbstract || classInfo.isInnerClass || classInfo.isInterface
                    }.map { classInfo ->
                        @Suppress("UNCHECKED_CAST")
                        classInfo.loadClass().kotlin as KClass<ProfileValidator<*>>
                    }
                }
        classes.forEach {
            assertNotNull(it.findAnnotation<Component>()) {
                "${it.simpleName} is not annotated with @Component"
            }
        }
    }
}
