package com.projectronin.interop.rcdm.validate.element

import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ContactPointValidatorTest {
    private val validator = ContactPointValidator()

    @Test
    fun `returns supported eleemnt`() {
        assertEquals(ContactPoint::class, validator.supportedElement)
    }

    @Test
    fun `validate fails if system is null`() {
        val contactPoint =
            ContactPoint(
                system = null,
                value = FHIRString("555-987-6543"),
            )

        val validation =
            validator.validate(contactPoint, listOf(RoninProfile.PATIENT), LocationContext("Patient", "telecom[0]"))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: system is a required element @ Patient.telecom[0].system",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if no system extensions`() {
        val contactPoint =
            ContactPoint(
                system =
                    Code(
                        value = "phone",
                        extension = listOf(),
                    ),
                value = FHIRString("555-987-6543"),
            )

        val validation =
            validator.validate(contactPoint, listOf(RoninProfile.PATIENT), LocationContext("Patient", "telecom[0]"))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_CNTCTPT_001: A single tenant source element system extension is required @ Patient.telecom[0].system",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if multiple system extension`() {
        val contactPoint =
            ContactPoint(
                system =
                    Code(
                        value = "phone",
                        extension =
                            listOf(
                                Extension(
                                    url = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.uri,
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                                Extension(
                                    url = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.uri,
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                            ),
                    ),
                value = FHIRString("555-987-6543"),
            )

        val validation =
            validator.validate(contactPoint, listOf(RoninProfile.PATIENT), LocationContext("Patient", "telecom[0]"))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_CNTCTPT_001: A single tenant source element system extension is required @ Patient.telecom[0].system",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if system extension has null url`() {
        val contactPoint =
            ContactPoint(
                system =
                    Code(
                        value = "phone",
                        extension =
                            listOf(
                                Extension(
                                    url = null,
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                            ),
                    ),
                value = FHIRString("555-987-6543"),
            )

        val validation =
            validator.validate(contactPoint, listOf(RoninProfile.PATIENT), LocationContext("Patient", "telecom[0]"))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_CNTCTPT_001: A single tenant source element system extension is required @ Patient.telecom[0].system",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if system extension has url with null value`() {
        val contactPoint =
            ContactPoint(
                system =
                    Code(
                        value = "phone",
                        extension =
                            listOf(
                                Extension(
                                    url = Uri(null),
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                            ),
                    ),
                value = FHIRString("555-987-6543"),
            )

        val validation =
            validator.validate(contactPoint, listOf(RoninProfile.PATIENT), LocationContext("Patient", "telecom[0]"))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_CNTCTPT_001: A single tenant source element system extension is required @ Patient.telecom[0].system",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if incorrect system extension url`() {
        val contactPoint =
            ContactPoint(
                system =
                    Code(
                        value = "phone",
                        extension =
                            listOf(
                                Extension(
                                    url = RoninExtension.RONIN_DATA_AUTHORITY_EXTENSION.uri,
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                            ),
                    ),
                value = FHIRString("555-987-6543"),
            )

        val validation =
            validator.validate(contactPoint, listOf(RoninProfile.PATIENT), LocationContext("Patient", "telecom[0]"))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_CNTCTPT_001: A single tenant source element system extension is required @ Patient.telecom[0].system",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if value is null`() {
        val contactPoint =
            ContactPoint(
                system =
                    Code(
                        value = "phone",
                        extension =
                            listOf(
                                Extension(
                                    url = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.uri,
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                            ),
                    ),
                value = null,
            )

        val validation =
            validator.validate(contactPoint, listOf(RoninProfile.PATIENT), LocationContext("Patient", "telecom[0]"))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR REQ_FIELD: value is a required element @ Patient.telecom[0].value",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if no use extensions`() {
        val contactPoint =
            ContactPoint(
                system =
                    Code(
                        value = "phone",
                        extension =
                            listOf(
                                Extension(
                                    url = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.uri,
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                            ),
                    ),
                use =
                    Code(
                        value = "mobile",
                        extension = listOf(),
                    ),
                value = FHIRString("555-987-6543"),
            )

        val validation =
            validator.validate(contactPoint, listOf(RoninProfile.PATIENT), LocationContext("Patient", "telecom[0]"))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_CNTCTPT_003: A single tenant source element use extension is required @ Patient.telecom[0].use",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if multiple use extensions`() {
        val contactPoint =
            ContactPoint(
                system =
                    Code(
                        value = "phone",
                        extension =
                            listOf(
                                Extension(
                                    url = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.uri,
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                            ),
                    ),
                use =
                    Code(
                        value = "mobile",
                        extension =
                            listOf(
                                Extension(
                                    url = RoninExtension.TENANT_SOURCE_TELECOM_USE.uri,
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                                Extension(
                                    url = RoninExtension.TENANT_SOURCE_TELECOM_USE.uri,
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                            ),
                    ),
                value = FHIRString("555-987-6543"),
            )

        val validation =
            validator.validate(contactPoint, listOf(RoninProfile.PATIENT), LocationContext("Patient", "telecom[0]"))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_CNTCTPT_003: A single tenant source element use extension is required @ Patient.telecom[0].use",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if use extension has null url`() {
        val contactPoint =
            ContactPoint(
                system =
                    Code(
                        value = "phone",
                        extension =
                            listOf(
                                Extension(
                                    url = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.uri,
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                            ),
                    ),
                use =
                    Code(
                        value = "mobile",
                        extension =
                            listOf(
                                Extension(
                                    url = null,
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                            ),
                    ),
                value = FHIRString("555-987-6543"),
            )

        val validation =
            validator.validate(contactPoint, listOf(RoninProfile.PATIENT), LocationContext("Patient", "telecom[0]"))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_CNTCTPT_003: A single tenant source element use extension is required @ Patient.telecom[0].use",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if use extension has url with null value`() {
        val contactPoint =
            ContactPoint(
                system =
                    Code(
                        value = "phone",
                        extension =
                            listOf(
                                Extension(
                                    url = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.uri,
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                            ),
                    ),
                use =
                    Code(
                        value = "mobile",
                        extension =
                            listOf(
                                Extension(
                                    url = Uri(null),
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                            ),
                    ),
                value = FHIRString("555-987-6543"),
            )

        val validation =
            validator.validate(contactPoint, listOf(RoninProfile.PATIENT), LocationContext("Patient", "telecom[0]"))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_CNTCTPT_003: A single tenant source element use extension is required @ Patient.telecom[0].use",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails if incorrect use extension url`() {
        val contactPoint =
            ContactPoint(
                system =
                    Code(
                        value = "phone",
                        extension =
                            listOf(
                                Extension(
                                    url = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.uri,
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                            ),
                    ),
                use =
                    Code(
                        value = "mobile",
                        extension =
                            listOf(
                                Extension(
                                    url = RoninExtension.TENANT_SOURCE_CONDITION_CODE.uri,
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                            ),
                    ),
                value = FHIRString("555-987-6543"),
            )

        val validation =
            validator.validate(contactPoint, listOf(RoninProfile.PATIENT), LocationContext("Patient", "telecom[0]"))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_CNTCTPT_003: A single tenant source element use extension is required @ Patient.telecom[0].use",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate succeeds with no use`() {
        val contactPoint =
            ContactPoint(
                system =
                    Code(
                        value = "phone",
                        extension =
                            listOf(
                                Extension(
                                    url = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.uri,
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                            ),
                    ),
                value = FHIRString("555-987-6543"),
            )

        val validation =
            validator.validate(contactPoint, listOf(RoninProfile.PATIENT), LocationContext("Patient", "telecom[0]"))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate succeeds with use`() {
        val contactPoint =
            ContactPoint(
                system =
                    Code(
                        value = "phone",
                        extension =
                            listOf(
                                Extension(
                                    url = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.uri,
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                            ),
                    ),
                use =
                    Code(
                        value = "mobile",
                        extension =
                            listOf(
                                Extension(
                                    url = RoninExtension.TENANT_SOURCE_TELECOM_USE.uri,
                                    value = DynamicValue(DynamicValueType.CODING, Coding(code = Code("phone"))),
                                ),
                            ),
                    ),
                value = FHIRString("555-987-6543"),
            )

        val validation =
            validator.validate(contactPoint, listOf(RoninProfile.PATIENT), LocationContext("Patient", "telecom[0]"))
        assertEquals(0, validation.issues().size)
    }
}
