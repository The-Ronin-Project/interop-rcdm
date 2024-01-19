package com.projectronin.interop.rcdm.validate.profile

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Meta
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Procedure
import com.projectronin.interop.fhir.r4.validate.resource.R4ProcedureValidator
import com.projectronin.interop.fhir.r4.valueset.EventStatus
import com.projectronin.interop.fhir.validate.LocationContext
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.validate.profile.util.requiredIdentifiers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RoninProcedureValidatorTest {
    private val validator = RoninProcedureValidator()

    @Test
    fun `returns supported resource`() {
        assertEquals(Procedure::class, validator.supportedResource)
    }

    @Test
    fun `returns R4 validator`() {
        assertEquals(R4ProcedureValidator, validator.r4Validator)
    }

    @Test
    fun `returns profile`() {
        assertEquals(RoninProfile.PROCEDURE, validator.profile)
    }

    @Test
    fun `validate succeeds`() {
        val procedure =
            Procedure(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PROCEDURE.canonical)),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_PROCEDURE_CODE.uri,
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODEABLE_CONCEPT,
                                    value = Coding(code = Code("normalizedCode"), system = Uri("Code")),
                                ),
                        ),
                    ),
                identifier = requiredIdentifiers,
                code =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("Code"),
                                    code = Code(value = "normalizedCode"),
                                ),
                            ),
                    ),
                status = Code(EventStatus.UNKNOWN.code),
                subject = Reference(reference = "Patient".asFHIR()),
            )
        val validation = validator.validate(procedure, LocationContext(Procedure::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate succeeds with code and code extension`() {
        val procedure =
            Procedure(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PROCEDURE.canonical)),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_PROCEDURE_CODE.uri,
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODEABLE_CONCEPT,
                                    value = Coding(code = Code("normalizedCode")),
                                ),
                        ),
                    ),
                identifier = requiredIdentifiers,
                code =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("Code"),
                                    code = Code(value = "normalizedCode"),
                                ),
                            ),
                    ),
                status = Code(EventStatus.UNKNOWN.code),
                subject = Reference(reference = "Patient".asFHIR()),
            )
        val validation = validator.validate(procedure, LocationContext(Procedure::class))
        assertEquals(0, validation.issues().size)
    }

    @Test
    fun `validate fails with no performed based on status`() {
        val procedure =
            Procedure(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PROCEDURE.canonical)),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_PROCEDURE_CODE.uri,
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODEABLE_CONCEPT,
                                    value = Coding(code = Code("something")),
                                ),
                        ),
                    ),
                identifier = requiredIdentifiers,
                code =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("Code"),
                                    code = Code(value = "something"),
                                ),
                            ),
                    ),
                status = Code(EventStatus.COMPLETED.code),
                subject = Reference(reference = "Patient".asFHIR()),
            )
        val validation = validator.validate(procedure, LocationContext(Procedure::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR USCORE_PROC_001: Performed SHALL be present if the status is 'completed' or 'in-progress' @ Procedure.performed",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails with wrong extension uri`() {
        val procedure =
            Procedure(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PROCEDURE.canonical)),
                extension =
                    listOf(
                        Extension(
                            url = Uri("this.is.wrong"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODEABLE_CONCEPT,
                                    value = Coding(code = Code("something")),
                                ),
                        ),
                    ),
                identifier = requiredIdentifiers,
                code =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("Code"),
                                    code = Code(value = "something"),
                                ),
                            ),
                    ),
                status = Code(EventStatus.UNKNOWN.code),
                subject = Reference(reference = "Patient".asFHIR()),
            )
        val validation = validator.validate(procedure, LocationContext(Procedure::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR RONIN_PROC_001: Tenant source procedure code extension is missing or invalid @ Procedure.extension",
            validation.issues().first().toString(),
        )
    }

    @Test
    fun `validate fails without code`() {
        val procedure =
            Procedure(
                id = Id("1234"),
                meta = Meta(profile = listOf(RoninProfile.PROCEDURE.canonical)),
                extension =
                    listOf(
                        Extension(
                            url = RoninExtension.TENANT_SOURCE_PROCEDURE_CODE.uri,
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODEABLE_CONCEPT,
                                    value = Coding(code = Code("something")),
                                ),
                        ),
                    ),
                identifier = requiredIdentifiers,
                code = null,
                status = Code(EventStatus.UNKNOWN.code),
                subject = Reference(reference = "Patient".asFHIR()),
            )
        val validation = validator.validate(procedure, LocationContext(Procedure::class))
        assertEquals(1, validation.issues().size)
        assertEquals(
            "ERROR USCORE_PROC_002: Procedure code is missing or invalid @ Procedure.code",
            validation.issues().first().toString(),
        )
    }
}
