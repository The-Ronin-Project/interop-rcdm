package com.projectronin.interop.rcdm.transform.normalization

import com.projectronin.interop.fhir.r4.datatype.Age
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.ContactPoint
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.Range
import com.projectronin.interop.fhir.r4.datatype.Reference
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRBoolean
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.element.Element
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.resource.MedicationRequest
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.RequestGroup
import com.projectronin.interop.fhir.util.asCode
import com.projectronin.interop.rcdm.transform.introspection.TransformResult
import com.projectronin.interop.rcdm.transform.normalization.element.CodeableConceptNormalizer
import com.projectronin.interop.rcdm.transform.normalization.element.CodingNormalizer
import com.projectronin.interop.rcdm.transform.normalization.element.ExtensionNormalizer
import com.projectronin.interop.rcdm.transform.normalization.element.IdentifierNormalizer
import com.projectronin.interop.tenant.config.model.Tenant
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class NormalizerTest {
    private val ageNormalizer =
        mockk<ElementNormalizer<Age>> {
            every { elementType } returns Age::class
        }
    private val rangeNormalizer =
        mockk<ElementNormalizer<Range>> {
            every { elementType } returns Range::class
        }
    private val normalizer = Normalizer(listOf(ageNormalizer, rangeNormalizer))

    private val tenant = mockk<Tenant>()

    @Test
    fun `normalize works when no elements present`() {
        data class TestItem(
            val value: String,
            val value2: Int,
        )

        val item = TestItem("value", 2)
        val normalizedItem = normalizer.normalize(item, tenant)
        assertEquals(item, normalizedItem)
    }

    @Test
    fun `normalize works when no matching elements present`() {
        data class TestItem(
            val value: String,
            val value2: CodeableConcept,
        )

        val item = TestItem("value", CodeableConcept(text = "codeable concept".asFHIR()))
        val normalizedItem = normalizer.normalize(item, tenant)
        assertEquals(item, normalizedItem)
    }

    @Test
    fun `normalize works when matching element present`() {
        data class TestItem(
            val value: String,
            val value2: Age,
        )

        val age = Age(value = Decimal(BigDecimal.TEN))
        val normalizedAge = Age(value = Decimal(BigDecimal.TEN), system = Uri("age"))
        every { ageNormalizer.normalize(age, tenant) } returns TransformResult(normalizedAge)

        val item = TestItem("value", age)
        val normalizedItem = normalizer.normalize(item, tenant)
        assertEquals(item.value, normalizedItem.value)
        assertEquals(normalizedAge, normalizedItem.value2)
    }

    @Test
    fun `normalize works when matching element present in nested object`() {
        data class TestItem(
            val value: String,
            val value2: Age,
        ) : Element<TestItem> {
            override val extension: List<Extension> = listOf()
            override val id: FHIRString? = null
        }

        data class TestParent(
            val item: TestItem,
        )

        val age = Age(value = Decimal(BigDecimal.TEN))
        val normalizedAge = Age(value = Decimal(BigDecimal.TEN), system = Uri("age"))
        every { ageNormalizer.normalize(age, tenant) } returns TransformResult(normalizedAge)

        val item = TestItem("value", age)
        val parent = TestParent(item)
        val normalizedParent = normalizer.normalize(parent, tenant)

        val normalizedItem = normalizedParent.item
        assertEquals(item.value, normalizedItem.value)
        assertEquals(normalizedAge, normalizedItem.value2)
    }
}

// These tests are to ensure the new Normalizer works with all cases setup in the prior version, so it's not really a unit test.
// These are all the prior Normalizer tests minus ones that explicitly tested the normalize functionality through internal methods.
class LegacyNormalizerTest {
    private val tenant =
        mockk<Tenant> {
            every { mnemonic } returns "test"
        }

    private val extensions =
        listOf(
            Extension(
                id = "5678".asFHIR(),
                url = Uri("http://normalhost/extension"),
                value = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = "Patient/1234".asFHIR())),
            ),
        )

    private val normalizer =
        Normalizer(
            listOf(
                CodingNormalizer(),
                IdentifierNormalizer(),
                CodeableConceptNormalizer(),
                ExtensionNormalizer(),
            ),
        )

    @Test
    fun `normalize works for object with no unnormalized types supplied`() {
        val location = Location(id = Id("1234"))
        val normalizedLocation = normalizer.normalize(location, tenant)
        assertEquals(location, normalizedLocation)
    }

    @Test
    fun `normalize works for object with nested normalizable value with no normalization`() {
        val location =
            Location(
                id = Id("1234"),
                operationalStatus =
                    Coding(
                        id = "12345".asFHIR(),
                        extension = extensions,
                        system = Uri("non-normalizable-system"),
                        version = "version".asFHIR(),
                        code = Code("code"),
                        display = "Display".asFHIR(),
                        userSelected = FHIRBoolean.TRUE,
                    ),
            )
        val normalizedLocation = normalizer.normalize(location, tenant)
        assertEquals(location, normalizedLocation)
    }

    @Test
    fun `normalize works for object with nested normalizable value with normalization`() {
        val location =
            Location(
                id = Id("1234"),
                operationalStatus =
                    Coding(
                        id = "12345".asFHIR(),
                        extension = extensions,
                        system = Uri("urn:oid:2.16.840.1.113883.6.1"),
                        version = "version".asFHIR(),
                        code = Code("code"),
                        display = "Display".asFHIR(),
                        userSelected = FHIRBoolean.TRUE,
                    ),
            )
        val normalizedLocation = normalizer.normalize(location, tenant)

        val expectedLocation =
            Location(
                id = Id("1234"),
                operationalStatus =
                    Coding(
                        id = "12345".asFHIR(),
                        extension = extensions,
                        system = Uri("http://loinc.org"),
                        version = "version".asFHIR(),
                        code = Code("code"),
                        display = "Display".asFHIR(),
                        userSelected = FHIRBoolean.TRUE,
                    ),
            )
        assertEquals(expectedLocation, normalizedLocation)
    }

    @Test
    fun `normalize extension with no value`() {
        val patient =
            Patient(
                extension =
                    listOf(
                        Extension(
                            url = Uri("something here"),
                        ),
                    ),
            )
        val normalizedExtension = normalizer.normalize(patient, tenant)
        assertEquals(normalizedExtension.extension.size, 0)
    }

    @Test
    fun `normalize extension with no url`() {
        val patient =
            Patient(
                extension =
                    listOf(
                        Extension(
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value =
                                        Coding(
                                            system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                            code = Code(value = "abc"),
                                        ),
                                ),
                        ),
                    ),
            )
        val normalizedExtension = normalizer.normalize(patient, tenant)
        assertEquals(0, normalizedExtension.extension.size)
    }

    @Test
    fun `normalize extension with non-ronin url and value`() {
        val patient =
            Patient(
                extension =
                    listOf(
                        Extension(
                            url = Uri("some-extension-url"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value =
                                        Coding(
                                            system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                            code = Code(value = "abc"),
                                        ),
                                ),
                        ),
                    ),
            )
        val expectedExtension =
            listOf(
                Extension(
                    url = Uri("some-extension-url"),
                    value =
                        DynamicValue(
                            type = DynamicValueType.CODING,
                            value =
                                Coding(
                                    system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                    code = Code(value = "abc"),
                                ),
                        ),
                ),
            )
        val normalizedExtension = normalizer.normalize(patient, tenant)
        assertEquals(normalizedExtension.extension, expectedExtension)
    }

    @Test
    fun `normalize extension with non-ronin url and extension`() {
        val patient =
            Patient(
                extension =
                    listOf(
                        Extension(
                            url = Uri("some-extension-url"),
                            extension =
                                listOf(
                                    Extension(
                                        url = Uri("some-extension-url-2"),
                                        value =
                                            DynamicValue(
                                                type = DynamicValueType.CODING,
                                                value =
                                                    Coding(
                                                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                                        code = Code(value = "abc"),
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )
        val expectedExtension =
            listOf(
                Extension(
                    url = Uri("some-extension-url"),
                    extension =
                        listOf(
                            Extension(
                                url = Uri("some-extension-url-2"),
                                value =
                                    DynamicValue(
                                        type = DynamicValueType.CODING,
                                        value =
                                            Coding(
                                                system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                                code = Code(value = "abc"),
                                            ),
                                    ),
                            ),
                        ),
                ),
            )
        val normalizedExtension = normalizer.normalize(patient, tenant)
        assertEquals(normalizedExtension.extension, expectedExtension)
    }

    @Test
    fun `normalize extension will filter sub-extensions`() {
        val patient =
            Patient(
                extension =
                    listOf(
                        Extension(
                            url = Uri("some-extension-url"),
                            extension =
                                listOf(
                                    Extension(
                                        url = Uri("some-extension-url-2"),
                                        value =
                                            DynamicValue(
                                                type = DynamicValueType.CODING,
                                                value =
                                                    Coding(
                                                        system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                                        code = Code(value = "abc"),
                                                    ),
                                            ),
                                    ),
                                    Extension(
                                        url = Uri("some-extension-url-3"),
                                    ),
                                ),
                        ),
                    ),
            )
        val expectedExtension =
            listOf(
                Extension(
                    url = Uri("some-extension-url"),
                    extension =
                        listOf(
                            Extension(
                                url = Uri("some-extension-url-2"),
                                value =
                                    DynamicValue(
                                        type = DynamicValueType.CODING,
                                        value =
                                            Coding(
                                                system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                                code = Code(value = "abc"),
                                            ),
                                    ),
                            ),
                        ),
                ),
            )
        val normalizedExtension = normalizer.normalize(patient, tenant)
        assertEquals(normalizedExtension.extension, expectedExtension)
    }

    @Test
    fun `normalize extension with ronin url and value`() {
        val patient =
            Patient(
                extension =
                    listOf(
                        Extension(
                            url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value =
                                        Coding(
                                            system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                            code = Code(value = "abc"),
                                        ),
                                ),
                        ),
                    ),
            )
        val expectedExtension =
            listOf(
                Extension(
                    url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                    value =
                        DynamicValue(
                            type = DynamicValueType.CODING,
                            value =
                                Coding(
                                    system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                    code = Code(value = "abc"),
                                ),
                        ),
                ),
            )
        val normalizedExtension = normalizer.normalize(patient, tenant)
        assertEquals(normalizedExtension.extension, expectedExtension)
    }

    @Test
    fun `normalize extension within resource that has ronin url and value`() {
        val patient =
            Patient(
                id = Id("12345"),
                extension =
                    listOf(
                        Extension(
                            url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value =
                                        Coding(
                                            system = Uri("http://projectronin.io/fhir/CodeSystem/test/AppointmentStatus"),
                                            code = Code(value = "abc"),
                                        ),
                                ),
                        ),
                    ),
            )
        val normalizedExtension = normalizer.normalize(patient, tenant)
        assertEquals(normalizedExtension, patient)
    }

    @Test
    fun `normalize extension within resource that has ronin url`() {
        val patient =
            Patient(
                id = Id("12345"),
                extension =
                    listOf(
                        Extension(
                            url = Uri("http://projectronin.io/fhir/StructureDefinition/Extension/tenant-sourceAppointmentStatus"),
                        ),
                    ),
            )
        val normalizedExtension = normalizer.normalize(patient, tenant)
        assertEquals(normalizedExtension, patient)
    }

    @Test
    fun `normalize extension within resource without value is dropped`() {
        val requestGroup =
            RequestGroup(
                id = Id("1234"),
                extension =
                    listOf(
                        Extension(
                            url = Uri("this-should-be-removed-1"),
                        ),
                    ),
                status = Code("active"),
                intent = Code("order"),
                basedOn =
                    listOf(
                        Reference(
                            type = Uri("Patient"),
                            extension =
                                listOf(
                                    Extension(
                                        url = Uri("this-should-be-removed-2"),
                                    ),
                                ),
                        ),
                    ),
            )

        val expectedRequestGroup =
            RequestGroup(
                id = Id("1234"),
                status = Code("active"),
                intent = Code("order"),
                basedOn =
                    listOf(
                        Reference(
                            type = Uri("Patient"),
                            extension = emptyList(),
                        ),
                    ),
            )

        val normalizedExtension = normalizer.normalize(requestGroup, tenant)
        assertEquals(expectedRequestGroup, normalizedExtension)
    }

    @Test
    fun `normalize extension within multiple urls and values`() {
        val requestGroup =
            RequestGroup(
                id = Id("1234"),
                extension =
                    listOf(
                        Extension(
                            url = Uri("this-should-be-removed"),
                        ),
                        Extension(
                            url = Uri("this-should-be-kept"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value =
                                        Coding(
                                            code = Code(value = "abc"),
                                        ),
                                ),
                        ),
                        Extension(
                            url = Uri("this-should-be-kept"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value =
                                        Coding(
                                            code = Code(value = "def"),
                                        ),
                                ),
                        ),
                        Extension(
                            url = Uri("this-should-be-removed"),
                        ),
                    ),
                status = Code("active"),
                intent = Code("order"),
                basedOn =
                    listOf(
                        Reference(
                            type = Uri("Patient"),
                            extension =
                                listOf(
                                    Extension(
                                        url = Uri("this-is-an-extension-url-should-be-removed"),
                                    ),
                                ),
                        ),
                    ),
            )

        val normalized = normalizer.normalize(requestGroup, tenant)
        assertEquals(normalized.extension.size, 2)
        assertEquals(normalized.basedOn[0].extension.size, 0)
    }

    @Test
    fun `normalize extension within multiple urls and values - keeps basedOn`() {
        val requestGroup =
            RequestGroup(
                id = Id("1234"),
                extension =
                    listOf(
                        Extension(
                            url = Uri("this-should-be-removed"),
                        ),
                        Extension(
                            url = Uri("this-should-be-kept"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value =
                                        Coding(
                                            code = Code(value = "abc"),
                                        ),
                                ),
                        ),
                        Extension(
                            url = Uri("this-should-be-kept"),
                            value =
                                DynamicValue(
                                    type = DynamicValueType.CODING,
                                    value =
                                        Coding(
                                            code = Code(value = "def"),
                                        ),
                                ),
                        ),
                        Extension(
                            url = Uri("this-should-be-removed"),
                        ),
                    ),
                status = Code("active"),
                intent = Code("order"),
                basedOn =
                    listOf(
                        Reference(
                            type = Uri("Patient"),
                            extension =
                                listOf(
                                    Extension(
                                        url = Uri("this-is-an-extension-url-should-be-removed"),
                                    ),
                                    Extension(
                                        url = Uri("this-should-be-kept"),
                                        value =
                                            DynamicValue(
                                                type = DynamicValueType.CODING,
                                                value =
                                                    Coding(
                                                        code = Code(value = "def"),
                                                    ),
                                            ),
                                    ),
                                ),
                        ),
                    ),
            )

        val normalized = normalizer.normalize(requestGroup, tenant)
        assertEquals(normalized.extension.size, 2)
        assertEquals(normalized.basedOn[0].extension.size, 1)
    }

    @Test
    fun `normalize maintains contained resources`() {
        val medication =
            Medication(
                id = Id("67890"),
                code = CodeableConcept(text = FHIRString("Medication")),
            )
        val medicationRequest =
            MedicationRequest(
                id = Id("12345"),
                contained = listOf(medication),
                intent = com.projectronin.interop.fhir.r4.valueset.MedicationRequestIntent.ORDER.asCode(),
                status = com.projectronin.interop.fhir.r4.valueset.MedicationRequestStatus.ACTIVE.asCode(),
                medication = DynamicValue(DynamicValueType.REFERENCE, Reference(reference = FHIRString("#67890"))),
                subject = Reference(reference = FHIRString("Patient/13579")),
            )
        val normalized = normalizer.normalize(medicationRequest, tenant)
        assertEquals(medicationRequest, normalized)
    }

    @Test
    fun `normalize maintains telecom`() {
        val patient =
            Patient(
                id = Id("12345"),
                telecom =
                    listOf(
                        ContactPoint(
                            system = com.projectronin.interop.fhir.r4.valueset.ContactPointSystem.EMAIL.asCode(),
                            value = "bob@bobster.com".asFHIR(),
                        ),
                        ContactPoint(
                            system = com.projectronin.interop.fhir.r4.valueset.ContactPointSystem.PHONE.asCode(),
                            value = "5551234567".asFHIR(),
                        ),
                    ),
            )
        val normalized = normalizer.normalize(patient, tenant)
        assertEquals(patient, normalized)
    }
}
