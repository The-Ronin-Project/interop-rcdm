package com.projectronin.interop.rcdm.registry

import com.projectronin.interop.common.jackson.JacksonUtil
import com.projectronin.interop.datalake.oci.client.OCIClient
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Appointment
import com.projectronin.interop.fhir.r4.resource.ConceptMap
import com.projectronin.interop.fhir.r4.resource.ConceptMapDependsOn
import com.projectronin.interop.fhir.r4.resource.Medication
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.r4.resource.ValueSet
import com.projectronin.interop.fhir.r4.valueset.ContactPointSystem
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.metadata.ConceptMapMetadata
import com.projectronin.interop.rcdm.common.metadata.ValueSetMetadata
import com.projectronin.interop.rcdm.registry.dependson.DependsOnEvaluator
import com.projectronin.interop.rcdm.registry.exception.MissingNormalizationContentException
import com.projectronin.interop.rcdm.registry.model.RoninConceptMap
import com.projectronin.interop.rcdm.registry.model.ValueSetList
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime

@Suppress("ktlint:standard:max-line-length")
class NormalizationRegistryClientTest {
    private val ociClient = mockk<OCIClient>()
    private val registryPath = "/DataNormalizationRegistry/v5/registry.json"
    private val medicationDependsOnEvaluator =
        mockk<DependsOnEvaluator<Medication>> { every { resourceType } returns Medication::class }

    private val client = NormalizationRegistryClient(ociClient, listOf(medicationDependsOnEvaluator), registryPath)

    private val tenant = "test"

    private val sourceAtoTargetAAA =
        SourceConcept(
            codeableConcept = CodeableConcept(coding = listOf(Coding(code = Code("valueA"), system = Uri("systemA")))),
        ) to
            listOf(
                TargetConcept(
                    text = "textAAA",
                    element =
                        listOf(
                            TargetConceptMapValue(
                                "targetValueAAA",
                                "targetSystemAAA",
                                "targetDisplayAAA",
                                "targetVersionAAA",
                            ),
                        ),
                ),
            )
    private val sourceBtoTargetBBB =
        SourceConcept(
            codeableConcept = CodeableConcept(coding = listOf(Coding(code = Code("valueB"), system = Uri("systemB")))),
        ) to
            listOf(
                TargetConcept(
                    text = "textBBB",
                    element =
                        listOf(
                            TargetConceptMapValue(
                                "targetValueBBB",
                                "targetSystemBBB",
                                "targetDisplayBBB",
                                "targetVersionBBB",
                            ),
                        ),
                ),
            )

    private val mapA = mapOf(sourceAtoTargetAAA)
    private val mapAB = mapOf(sourceAtoTargetAAA, sourceBtoTargetBBB)

    private val testConceptMap =
        """
            {
              "resourceType": "ConceptMap",
              "title": "Test Observations Mashup (for Dev Testing ONLY)",
              "id": "TestObservationsMashup-id",
              "name": "TestObservationsMashup-name",
              "contact": [
                {
                  "name": "Interops (for Dev Testing ONLY)"
                }
              ],
              "url": "http://projectronin.io/fhir/StructureDefinition/ConceptMap/TestObservationsMashup",
              "description": "Interops  (for Dev Testing ONLY)",
              "purpose": "Testing",
              "publisher": "Project Ronin",
              "experimental": true,
              "date": "2023-05-26",
              "version": 1,
              "group": [
                {
                  "source": "http://projectronin.io/fhir/CodeSystem/test/TestObservationsMashup",
                  "sourceVersion": "1.0",
                  "target": "http://loinc.org",
                  "targetVersion": "0.0.1",
                  "element": [
                    {
                      "id": "06c19b8e4718f1bf6e81f992cfc12c1e",
                      "_code": {
                        "extension": [ {
                          "id": "sourceExtension1",
                          "url": "http://projectronin.io/fhir/StructureDefinition/Extension/canonicalSourceData",
                          "valueCodeableConcept": {
                            "coding": [ {
                              "code": "72166-2",
                              "system": "http://loinc.org"
                            } ]
                          }
                        } ]
                      },
                      "display": "Tobacco smoking status",
                      "target": [
                        {
                          "id": "836cc342c39afcc7a8dee6277abc7b75",
                          "code": "72166-2",
                          "display": "Tobacco smoking status",
                          "equivalence": "equivalent",
                          "comment": null
                        }
                      ]
                    },
                    {
                      "id": "921618c5505606cff5626f3468d4b396",
                      "_code": {
                        "extension": [ {
                          "id": "sourceExtension1",
                          "url": "http://projectronin.io/fhir/StructureDefinition/Extension/canonicalSourceData",
                          "valueCodeableConcept": {
                            "coding": [ {
                              "code": "85354-9",
                              "system": "http://loinc.org",
                              "display": "Blood pressure panel with all children optional"
                            } ]
                          }
                        } ]
                      },
                      "display": "Blood pressure",
                      "target": [
                        {
                          "id": "51afcac380447929e3f8d493590ded03",
                          "code": "85354-9",
                          "display": "Blood pressure panel with all children optional",
                          "equivalence": "equivalent",
                          "comment": null
                        },
                        {
                          "id": "d7eb5bea0cbb03b5a4d43b3ef663e39f",
                          "code": "3141-9",
                          "display": "Body weight Measured",
                          "equivalence": "equivalent",
                          "comment": null
                        },
                        {
                          "id": "e2495acfcd6ab8ebc776a3a2ae8bd6ce",
                          "code": "55284-4",
                          "display": "Blood pressure systolic and diastolic",
                          "equivalence": "equivalent",
                          "comment": null
                        }
                      ]
                    },
                    {
                      "id": "5aec5a329873f3842748e2beeb69643b",
                      "_code": {
                        "extension": [ {
                          "id": "sourceExtension2",
                          "url": "http://projectronin.io/fhir/StructureDefinition/Extension/canonicalSourceData",
                          "valueCodeableConcept": {
                            "coding": [ {
                              "code": "21704910",
                              "system": "https://fhir.cerner.com/ec2458f2-1e24-41c8-b71b-0e701af7583d/codeSet/72",
                              "display": "Potassium Level"
                            }, {
                              "code": "2823-3",
                              "system": "http://loinc.org"
                            } ]
                          }
                        } ]
                      },
                      "display": "Potassium Level",
                      "target": [
                        {
                          "id": "e194363d552c9edf5c106fffc01236e1",
                          "code": "2823-3",
                          "display": "Potassium [Moles/volume] in Serum or Plasma",
                          "equivalence": "equivalent",
                          "comment": null
                        }
                      ]
                    }
                  ]
                }
              ],
              "extension": [
                {
                  "url": "http://projectronin.io/fhir/StructureDefinition/Extension/ronin-conceptMapSchema",
                  "valueString": "1.0.0"
                }
              ],
              "meta": {
                "lastUpdated": "2023-05-26T12:49:56.285403+00:00"
              }
        }
        """.trimIndent()

    private val testStagingConceptMap =
        """
        {
          "resourceType": "ConceptMap",
          "title": "TEST Staging Observation (Code) to Ronin Cancer Staging",
          "id": "TestObservationsStaging-id",
          "name": "TestObservationsStaging-name",
          "contact": [
            {
              "name": "Interops TEST Staging (for Dev Testing ONLY)"
            }
          ],
          "url": "http://projectronin.io/fhir/StructureDefinition/ConceptMap/TestObservationsStaging",
          "description": "This concept map is mapping from Staging Observations to value set Ronin Condition Code for data normalization",
          "purpose": "TEST for derivation service, used primarily by INFX and DP",
          "publisher": "Project Ronin",
          "experimental": false,
          "date": "2023-05-26",
          "version": 2,
          "group": [
            {
              "source": "http://projectronin.io/fhir/CodeSystem/v7r1eczk/PSJStagingRelatedObservationsCode",
              "sourceVersion": "1.0",
              "target": "http://snomed.info/sct",
              "targetVersion": "2023-03-01",
              "element": [
                {
                  "id": "972b1bb1122980f015626063d44c950f",
                  "_code": {
                    "extension": [ {
                      "id": "sourceExtension1",
                      "url": "http://projectronin.io/fhir/StructureDefinition/Extension/canonicalSourceData",
                      "valueCodeableConcept": {
                        "coding": [ {
                          "code": "EPIC#44065",
                          "system": "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688",
                          "display": "Clark's level"
                        } ]
                      }
                    } ]
                  },
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - PROGNOSTIC INDICATORS - CLARK'S LEVEL",
                  "target": [
                    {
                      "id": "325f634512313c32178c27b5e9a53929",
                      "code": "385347004",
                      "display": "Clark melanoma level finding (finding)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "827f75db7803e5fc9ba811c19ccf4fbb",
                  "_code": {
                    "extension": [ {
                      "id": "sourceExtension1",
                      "url": "http://projectronin.io/fhir/StructureDefinition/Extension/canonicalSourceData",
                      "valueCodeableConcept": {
                        "coding": [ {
                          "code": "EPIC#42391",
                          "system": "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688",
                          "display": "lymph-vascular invasion (LVI)"
                        } ]
                      }
                    } ]
                  },
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - LYMPH-VASCULAR INVASION (LVI)",
                  "target": [
                    {
                      "id": "aa883e707553eb434789bb77ffb823bb",
                      "code": "371512006",
                      "display": "Presence of direct invasion by primary malignant neoplasm to lymphatic vessel and/or small blood vessel (observable entity)",
                      "equivalence": "narrower",
                      "comment": "source-is-broader-than-target"
                    }
                  ]
                },
                {
                  "id": "fc8e25ae3ed568f4dbf4b5d447452a18",
                  "_code": {
                    "extension": [ {
                      "id": "sourceExtension1",
                      "url": "http://projectronin.io/fhir/StructureDefinition/Extension/canonicalSourceData",
                      "valueCodeableConcept": {
                        "coding": [ {
                          "code": "SNOMED#246111003",
                          "system": "http://snomed.info/sct"
                        }, {
                          "code": "EPIC#42388",
                          "system": "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688",
                          "display": "anatomic stage/prognostic group"
                        } ]
                      }
                    } ]
                  },
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - ANATOMIC STAGE/PROGNOSTIC GROUP",
                  "target": [
                    {
                      "id": "ae2c9f8e00c7426b0557cb1f4f0dded5",
                      "code": "246111003",
                      "display": "Prognostic score (attribute)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "58ca30808a5de848ab6f0e33f8f5e1e3",
                  "_code": {
                    "extension": [ {
                      "id": "sourceExtension1",
                      "url": "http://projectronin.io/fhir/StructureDefinition/Extension/canonicalSourceData",
                      "valueCodeableConcept": {
                        "coding": [ {
                          "code": "EPIC#31000073346",
                          "system": "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688",
                          "display": "WHO/ISUP grade (low/high)"
                        } ]
                      }
                    } ]
                  },
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - WHO/ISUP GRADE (LOW/HIGH)",
                  "target": [
                    {
                      "id": "e7674b7ff6e68a57d7859c1b643264f1",
                      "code": "396659000",
                      "display": "Histologic grade of urothelial carcinoma by World Health Organization and International Society of Urological Pathology technique (observable entity)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                },
                {
                  "id": "8ba61a5649cbd2f326d526acdcc459ff",
                  "_code": {
                    "extension": [ {
                      "id": "sourceExtension1",
                      "url": "http://projectronin.io/fhir/StructureDefinition/Extension/canonicalSourceData",
                      "valueCodeableConcept": {
                        "coding": [ {
                          "code": "SNOMED#260767000",
                          "system": "http://snomed.info/sct"
                        }, {
                          "code": "EPIC#42384",
                          "system": "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688",
                          "display": "regional lymph nodes (N)"
                        } ]
                      }
                    } ]
                  },
                  "display": "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - TNM CLASSIFICATION - AJCC N - REGIONAL LYMPH NODES (N)",
                  "target": [
                    {
                      "id": "73cb15052d834f3ffe2fb2484c2d7ef9",
                      "code": "260767000",
                      "display": "N - Regional lymph node stage (attribute)",
                      "equivalence": "equivalent",
                      "comment": null
                    }
                  ]
                }
              ]
            }
          ],
          "extension": [
            {
              "url": "http://projectronin.io/fhir/StructureDefinition/Extension/ronin-conceptMapSchema",
              "valueString": "1.0.0"
            }
          ],
          "meta": {
            "lastUpdated": "2023-05-26T17:20:40.696664+00:00"
          }
        }        
        """.trimIndent()
    private val conceptMapMetadata =
        ConceptMapMetadata(
            registryEntryType = RegistryType.CONCEPT_MAP.value,
            conceptMapName = "test-concept-map",
            conceptMapUuid = "573b456efca5-03d51d53-1a31-49a9-af74",
            version = "1",
        )
    private val valueSetMetadata =
        ValueSetMetadata(
            registryEntryType = RegistryType.VALUE_SET.value,
            valueSetName = "test-value-set",
            valueSetUuid = "03d51d53-1a31-49a9-af74-573b456efca5",
            version = "2",
        )

    @BeforeEach
    fun setUp() {
        mockkObject(JacksonUtil)
    }

    @AfterEach
    fun tearDown() {
        client.conceptMapCache.invalidateAll()
        client.valueSetCache.invalidateAll()
        unmockkAll()
    }

    @Test
    fun `getConceptMapData supports V4 formats`() {
        // We support 3 different formats of data for V4.
        val conceptMapJson =
            """
                {
                  "resourceType": "ConceptMap",
                  "title": "Test Observations Mashup (for Dev Testing ONLY)",
                  "id": "TestObservationsMashup-id",
                  "name": "TestObservationsMashup-name",
                  "contact": [
                    {
                      "name": "Interops (for Dev Testing ONLY)"
                    }
                  ],
                  "url": "http://projectronin.io/fhir/StructureDefinition/ConceptMap/TestObservationsMashup",
                  "description": "Interops  (for Dev Testing ONLY)",
                  "purpose": "Testing",
                  "publisher": "Project Ronin",
                  "experimental": true,
                  "date": "2023-05-26",
                  "version": 1,
                  "group": [
                    {
                      "source": "http://projectronin.io/fhir/CodeSystem/test/TestObservationsMashup",
                      "sourceVersion": "1.0",
                      "target": "http://loinc.org",
                      "targetVersion": "0.0.1",
                      "element": [
                        {
                          "id": "06c19b8e4718f1bf6e81f992cfc12c1e",
                          "code": "{\"valueCodeableConcept\": {\"coding\": [{\"code\": \"72166-2\", \"display\": null, \"system\": \"http://loinc.org\"}, {\"code\": \"Something else\", \"display\": null, \"system\": \"http://joshscodingsystem.org\"}]}}",
                          "display": "Tobacco smoking status",
                          "target": [
                            {
                              "id": "836cc342c39afcc7a8dee6277abc7b75",
                              "code": "72166-2",
                              "display": "Tobacco smoking status",
                              "equivalence": "equivalent",
                              "comment": null
                            }
                          ]
                        },
                        {
                          "id": "64be7ece9ab75a3eff827188c55c64db",
                          "code": "{\"coding\": [{\"code\": \"363905002\", \"display\": \"Details of alcohol drinking behavior (observable entity)\", \"system\": \"http://snomed.info/sct\"}]}",
                          "display": "Details of alcohol drinking behavior (observable entity)",
                          "target": [
                            {
                              "id": "8a3e6071d30318b9fbc23f3a1cd7de5a",
                              "code": "74013-4",
                              "display": "Alcoholic drinks per day",
                              "equivalence": "narrower",
                              "comment": "source-is-broader-than-target"
                            }
                          ]
                        },
                        {
                          "id": "8015a7419c2d19d6f515ccec9fb86c94",
                          "code": "Tobacco",
                          "display": "Tobacco",
                          "target": [
                            {
                              "id": "751254b27af4ca8f6ea012292dc864cc",
                              "code": "88028-6",
                              "display": "Tobacco use panel",
                              "equivalence": "equivalent",
                              "comment": null
                            }
                          ]
                        }
                      ]
                    }
                  ],
                  "extension": [
                    {
                      "url": "http://projectronin.io/fhir/StructureDefinition/Extension/ronin-conceptMapSchema",
                      "valueString": "1.0.0"
                    }
                  ],
                  "meta": {
                    "lastUpdated": "2023-05-26T12:49:56.285403+00:00"
                  }
            }
            """.trimIndent()

        every { ociClient.getObjectFromINFX("sample-data.json") } returns conceptMapJson

        val client = NormalizationRegistryClient(ociClient, listOf(), "/DataNormalizationRegistry/v4/registry.json")
        val data = client.getConceptMapData("sample-data.json")
        assertEquals(3, data.size)

        val sourceConcept1 =
            SourceConcept(
                codeableConcept =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(system = Uri("http://joshscodingsystem.org"), code = Code("Something else")),
                                Coding(system = Uri("http://loinc.org"), code = Code("72166-2")),
                            ),
                    ),
            )
        val targetConcept1 =
            TargetConcept(
                listOf(
                    TargetConceptMapValue(
                        "72166-2",
                        "http://loinc.org",
                        "Tobacco smoking status",
                        "0.0.1",
                    ),
                ),
                "Tobacco smoking status",
            )
        assertEquals(listOf(targetConcept1), data[sourceConcept1])

        val sourceConcept2 =
            SourceConcept(
                codeableConcept =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = Uri("http://snomed.info/sct"),
                                    code = Code("363905002"),
                                    display = "Details of alcohol drinking behavior (observable entity)".asFHIR(),
                                ),
                            ),
                    ),
            )
        val targetConcept2 =
            TargetConcept(
                listOf(
                    TargetConceptMapValue(
                        "74013-4",
                        "http://loinc.org",
                        "Alcoholic drinks per day",
                        "0.0.1",
                    ),
                ),
                "Details of alcohol drinking behavior (observable entity)",
            )
        assertEquals(listOf(targetConcept2), data[sourceConcept2])

        val sourceConcept3 = SourceConcept(code = Code("Tobacco"))
        val targetConcept3 =
            TargetConcept(
                listOf(TargetConceptMapValue("88028-6", "http://loinc.org", "Tobacco use panel", "0.0.1")),
                "Tobacco",
            )
        assertEquals(listOf(targetConcept3), data[sourceConcept3])
    }

    @Test
    fun `getConceptMappingForEnum with no matching registry - and source value is bad for enum - tries registry and fails`() {
        val coding =
            RoninConceptMap.CODE_SYSTEMS.toCoding(
                tenant,
                "ContactPoint.system",
                "MyPhone",
            )
        val mapping =
            client.getConceptMappingForEnum(
                tenant,
                "Patient.telecom.system",
                coding,
                ContactPointSystem::class,
                RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                mockk<Patient>(),
            )
        assertNull(mapping)
    }

    @Test
    fun `getConceptMappingForEnum with no matching registry - and source value is good for enum - returns enum as Coding`() {
        val coding =
            RoninConceptMap.CODE_SYSTEMS.toCoding(
                tenant,
                "ContactPoint.system",
                "phone",
            )
        val mapping =
            client.getConceptMappingForEnum(
                tenant,
                "Patient.telecom.system",
                coding,
                ContactPointSystem::class,
                RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                mockk<Patient>(),
            )
        assertNotNull(mapping)
        mapping!!
        assertEquals(
            coding,
            mapping.coding,
        )
        assertEquals(
            Extension(
                url = RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.uri,
                value = DynamicValue(DynamicValueType.CODING, value = coding),
            ),
            mapping.extension,
        )
    }

    @Test
    fun `getConceptMappingForEnum with match found in registry - returns target and extension`() {
        val registry1 =
            ConceptMapItem(
                sourceExtensionUrl = "ext1",
                map =
                    mapOf(
                        SourceConcept(
                            code = Code("MyPhone"),
                        ) to
                            listOf(
                                TargetConcept(
                                    element =
                                        listOf(
                                            TargetConceptMapValue(
                                                "good-or-bad-for-enum",
                                                "good-or-bad-for-enum",
                                                "good-or-bad-for-enum",
                                                "1",
                                                sourceId = "source1",
                                                targetId = "target1",
                                            ),
                                        ),
                                    text = "good-or-bad-for-enum, not validated here",
                                ),
                            ),
                    ),
                metadata = listOf(conceptMapMetadata),
            )
        val key =
            CacheKey(
                RegistryType.CONCEPT_MAP,
                "Patient.telecom.system",
                "test",
            )
        client.conceptMapCache.put(key, registry1)
        client.registryLastUpdated = LocalDateTime.now()
        val coding =
            RoninConceptMap.CODE_SYSTEMS.toCoding(
                tenant,
                "ContactPoint.system",
                "MyPhone",
            )
        val mapping =
            client.getConceptMappingForEnum(
                tenant,
                "Patient.telecom.system",
                coding,
                ContactPointSystem::class,
                RoninExtension.TENANT_SOURCE_TELECOM_SYSTEM.value,
                mockk<Patient>(),
            )!!
        assertEquals(
            Coding(
                system = Uri("good-or-bad-for-enum"),
                code = Code("good-or-bad-for-enum"),
                display = "good-or-bad-for-enum".asFHIR(),
                version = "1".asFHIR(),
            ),
            mapping.coding,
        )
        assertEquals(
            Extension(
                url = Uri("ext1"),
                value = DynamicValue(DynamicValueType.CODING, value = coding),
            ),
            mapping.extension,
        )
    }

    @Test
    fun `getValueSet with no matching registry`() {
        val mapping =
            client.getValueSet(
                "Patient.telecom.system",
                "specialAppointment",
            )
        assertTrue(mapping.codes.isEmpty())
    }

    @Test
    fun `getValueSet pulls registry and returns set`() {
        val vsTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Appointment.status",
                    registryUuid = "01234",
                    filename = "file3.json",
                    valueSetName = "AppointmentStatus",
                    valueSetUuid = "vs-333",
                    registryEntryType = RegistryType.VALUE_SET,
                    version = "1",
                    resourceType = "Appointment",
                    profileUrl = "specialAppointment",
                ),
                NormalizationRegistryItem(
                    dataElement = "Patient.telecom.use",
                    registryUuid = "56789",
                    filename = "file4.json",
                    valueSetName = "PatientTelecomUse",
                    valueSetUuid = "vs-4444",
                    registryEntryType = RegistryType.VALUE_SET,
                    version = "1",
                    resourceType = "Patient",
                    profileUrl = "specialPatient",
                ),
            )
        val valueSetMetadata1 =
            ValueSetMetadata(
                registryEntryType = RegistryType.VALUE_SET.value,
                valueSetName = "AppointmentStatus",
                valueSetUuid = "vs-333",
                version = "1",
            )
        val valueSetMetadata2 =
            ValueSetMetadata(
                registryEntryType = RegistryType.VALUE_SET.value,
                valueSetName = "PatientTelecomUse",
                valueSetUuid = "vs-4444",
                version = "1",
            )
        val mockkSet1 =
            mockk<ValueSet> {
                every { expansion?.contains } returns
                    listOf(
                        mockk {
                            every { system?.value.toString() } returns "system1"
                            every { version?.value.toString() } returns "version1"
                            every { code?.value.toString() } returns "code1"
                            every { display?.value.toString() } returns "display1"
                        },
                    )
            }
        val mockkSet2 =
            mockk<ValueSet> {
                every { expansion?.contains } returns
                    listOf(
                        mockk {
                            every { system?.value.toString() } returns "system2"
                            every { version?.value.toString() } returns "version2"
                            every { code?.value.toString() } returns "code2"
                            every { display?.value.toString() } returns "display2"
                        },
                    )
            }
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns vsTestRegistry
        every { ociClient.getObjectFromINFX("file3.json") } returns "setJson1"
        every { JacksonUtil.readJsonObject("setJson1", ValueSet::class) } returns mockkSet1
        every { ociClient.getObjectFromINFX("file4.json") } returns "setJson2"
        every { JacksonUtil.readJsonObject("setJson2", ValueSet::class) } returns mockkSet2

        val valueSet1 = client.getValueSet("Appointment.status", "specialAppointment")
        val expectedCoding1 =
            ValueSetList(
                listOf(
                    Coding(
                        system = Uri(value = "system1"),
                        code = Code(value = "code1"),
                        display = FHIRString(value = "display1"),
                        version = FHIRString(value = "version1"),
                    ),
                ),
                valueSetMetadata1,
            )
        assertEquals(valueSet1, expectedCoding1)

        val valueSet2 = client.getValueSet("Patient.telecom.use", "specialPatient")
        val expectedCoding2 =
            ValueSetList(
                listOf(
                    Coding(
                        system = Uri(value = "system2"),
                        code = Code(value = "code2"),
                        display = FHIRString(value = "display2"),
                        version = FHIRString(value = "version2"),
                    ),
                ),
                valueSetMetadata2,
            )
        assertEquals(valueSet2, expectedCoding2)
    }

    @Test
    fun `getValueSet with special profile match`() {
        val registry1 =
            ValueSetItem(
                set =
                    listOf(
                        TargetValueSetValue(
                            "code1",
                            "system1",
                            "display1",
                            "version1",
                        ),
                    ),
                metadata = valueSetMetadata,
            )
        val key =
            CacheKey(
                RegistryType.VALUE_SET,
                "Patient.telecom.system",
                null,
                "specialPatient",
            )
        client.valueSetCache.put(key, registry1)
        client.registryLastUpdated = LocalDateTime.now()

        val mapping =
            client.getValueSet(
                "Patient.telecom.system",
                "specialPatient",
            )
        assertEquals(1, mapping.codes.size)
        assertEquals(Code("code1"), mapping.codes[0].code)
    }

    @Test
    fun `universal getRequiredValueSet with profile match`() {
        val registry1 =
            ValueSetItem(
                set =
                    listOf(
                        TargetValueSetValue(
                            "code1",
                            "system1",
                            "display1",
                            "version1",
                        ),
                    ),
                metadata = valueSetMetadata,
            )
        val key =
            CacheKey(
                RegistryType.VALUE_SET,
                "Patient.telecom.system",
                null,
                "specialPatient",
            )
        client.valueSetCache.put(key, registry1)
        client.registryLastUpdated = LocalDateTime.now()
        val actualValueSet =
            client.getRequiredValueSet(
                "Patient.telecom.system",
                "specialPatient",
            )
        assertEquals(1, actualValueSet.codes.size)
        assertEquals(Code("code1"), actualValueSet.codes[0].code)
    }

    @Test
    fun `ensure getRequiredValueSet fails when value set is not found`() {
        every {
            JacksonUtil.readJsonList(
                any(),
                NormalizationRegistryItem::class,
            )
        } returns listOf()

        val exception =
            assertThrows<MissingNormalizationContentException> {
                client.getRequiredValueSet("Patient.telecom.system", "specialPatient")
            }
        assertEquals(
            "Required value set for specialPatient and Patient.telecom.system not found",
            exception.message,
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept with no matching registry`() {
        val coding =
            RoninConceptMap.CODE_SYSTEMS.toCoding(
                tenant,
                "ContactPoint.system",
                "phone",
            )
        val concept = CodeableConcept(coding = listOf(coding))
        val mapping =
            client.getConceptMapping(
                tenant,
                "Patient.telecom.system",
                concept,
                mockk<Patient>(),
            )
        assertNull(mapping)
    }

    @Test
    fun `getConceptMapping for CodeableConcept pulls new registry and maps`() {
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Appointment.status",
                    registryUuid = "12345",
                    filename = "file1.json",
                    conceptMapName = "AppointmentStatus-tenant",
                    conceptMapUuid = "cm-111",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = "ext1",
                    resourceType = "Appointment",
                    tenantId = "test",
                ),
                NormalizationRegistryItem(
                    dataElement = "Patient.telecom.use",
                    registryUuid = "67890",
                    filename = "file2.json",
                    conceptMapName = "PatientTelecomUse-tenant",
                    conceptMapUuid = "cm-222",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = "ext2",
                    resourceType = "Patient",
                    tenantId = "test",
                ),
            )
        val mockkMap1 =
            mockk<ConceptMap> {
                every { group } returns
                    listOf(
                        mockk {
                            every { target?.value } returns "targetSystemAAA"
                            every { targetVersion?.value } returns "targetVersionAAA"
                            every { source?.value } returns "sourceSystemA"
                            every { element } returns
                                listOf(
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueA"),
                                                                    system = Uri("sourceSystemA"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceA",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextAAA"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueAAA"
                                                    every { display?.value } returns "targetDisplayAAA"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueB"),
                                                                    system = Uri("sourceSystemA"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceB",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextBBB"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueBBB"
                                                    every { display?.value } returns "targetDisplayBBB"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                )
                        },
                    )
            }
        val mockkMap2 =
            mockk<ConceptMap> {
                every { group } returns
                    listOf(
                        mockk {
                            every { target?.value } returns "targetSystem222"
                            every { targetVersion?.value } returns "targetVersion222"
                            every { source?.value } returns "sourceSystem2"
                            every { element } returns
                                listOf(
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValue2"),
                                                                    system = Uri("sourceSystem2"),
                                                                ),
                                                            ),
                                                    ),
                                                    "source2",
                                                ),
                                            )
                                        every { display?.value } returns "targetText222"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValue222"
                                                    every { display?.value } returns "targetDisplay222"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                )
                        },
                    )
            }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "mapJson1"
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2
        val coding1 =
            Coding(
                code = Code(value = "sourceValueA"),
                system = Uri(value = "sourceSystemA"),
            )
        val concept1 =
            CodeableConcept(
                coding = listOf(coding1),
            )
        val mapping1 =
            client.getConceptMapping(
                tenant,
                "Appointment.status",
                concept1,
                mockk<Appointment>(),
            )!!
        assertEquals(mapping1.codeableConcept.coding.first().code!!.value, "targetValueAAA")
        assertEquals(mapping1.codeableConcept.coding.first().system!!.value, "targetSystemAAA")
        assertEquals(mapping1.extension.url!!.value, "ext1")
        assertEquals(mapping1.extension.value!!.value, concept1)
        val coding2 =
            Coding(
                code = Code(value = "sourceValue2"),
                system = Uri(value = "sourceSystem2"),
            )
        val concept2 =
            CodeableConcept(
                coding = listOf(coding2),
            )
        val mapping2 =
            client.getConceptMapping(
                tenant,
                "Patient.telecom.use",
                concept2,
                mockk<Patient>(),
            )!!
        assertEquals(mapping2.codeableConcept.coding.first().code!!.value, "targetValue222")
        assertEquals(mapping2.codeableConcept.coding.first().system!!.value, "targetSystem222")
        assertEquals(mapping2.extension.url!!.value, "ext2")
        assertEquals(mapping2.extension.value!!.value, concept2)
    }

    fun `getConceptMapping for CodeableConcept - correctly selects 1 entry from many in same map`() {
        val registry =
            ConceptMapItem(
                sourceExtensionUrl = "ext-AB",
                map = mapAB,
                metadata = listOf(conceptMapMetadata),
            )
        val key =
            CacheKey(
                RegistryType.CONCEPT_MAP,
                "Observation.code",
                tenant,
            )
        client.conceptMapCache.put(key, registry)
        client.registryLastUpdated = LocalDateTime.now()

        val sourceCoding1 =
            Coding(
                system = Uri("systemA"),
                code = Code("valueA"),
            )
        val sourceConcept1 = CodeableConcept(coding = listOf(sourceCoding1))
        val targetCoding1 =
            Coding(
                system = Uri("targetSystemAAA"),
                code = Code("targetValueAAA"),
                display = "targetDisplayAAA".asFHIR(),
                version = "targetVersionAAA".asFHIR(),
            )
        val targetConcept1 =
            CodeableConcept(
                text = "textAAA".asFHIR(),
                coding = listOf(targetCoding1),
            )
        val targetSourceExtension1 =
            Extension(
                url = Uri(value = "ext-AB"),
                value =
                    DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = sourceConcept1,
                    ),
            )
        val mappedResult1 =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                sourceConcept1,
                mockk<Observation>(),
            )
        assertEquals(
            targetConcept1,
            mappedResult1?.codeableConcept,
        )
        assertEquals(
            targetSourceExtension1,
            mappedResult1?.extension,
        )

        val sourceCoding2 =
            Coding(
                system = Uri("systemB"),
                code = Code("valueB"),
            )
        val sourceConcept2 = CodeableConcept(coding = listOf(sourceCoding2))
        val targetCoding2 =
            Coding(
                system = Uri("targetSystemBBB"),
                code = Code("targetValueBBB"),
                display = "targetDisplayBBB".asFHIR(),
                version = "targetVersionBBB".asFHIR(),
            )
        val targetConcept2 =
            CodeableConcept(
                text = "textBBB".asFHIR(),
                coding = listOf(targetCoding2),
            )
        val targetSourceExtension2 =
            Extension(
                url = Uri(value = "ext-AB"),
                value =
                    DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = sourceConcept2,
                    ),
            )
        val mappedResult2 =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                sourceConcept2,
                mockk<Observation>(),
            )
        assertEquals(
            targetConcept2,
            mappedResult2?.codeableConcept,
        )
        assertEquals(
            targetSourceExtension2,
            mappedResult2?.extension,
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - map found - contains no matching code`() {
        val registry1 =
            ConceptMapItem(
                sourceExtensionUrl = "sourceExtensionUrl",
                map = mapA,
                metadata = listOf(conceptMapMetadata),
            )
        val key1hr =
            CacheKey(
                RegistryType.CONCEPT_MAP,
                "Observation.code",
                tenant,
            )
        client.conceptMapCache.put(key1hr, registry1)

        val sourceCoding =
            Coding(
                system = Uri("systemB"),
                code = Code("valueB"),
            )
        val sourceConcept = CodeableConcept(coding = listOf(sourceCoding))

        val mappedResult =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                sourceConcept,
                mockk<Observation>(),
            )
        assertNull(mappedResult)
    }

    @Test
    fun `getConceptMapping for CodeableConcept - map found - target text replaces non-empty source text`() {
        val registry1 =
            ConceptMapItem(
                sourceExtensionUrl = "extl",
                map =
                    mapOf(
                        SourceConcept(
                            codeableConcept =
                                CodeableConcept(
                                    coding = listOf(Coding(code = Code("valueA"), system = Uri("systemA"))),
                                    text = "to-be-replaced".asFHIR(),
                                ),
                        ) to
                            listOf(
                                TargetConcept(
                                    text = "replaced-it",
                                    element =
                                        listOf(
                                            TargetConceptMapValue(
                                                "AAA",
                                                "AAA",
                                                "AAA",
                                                "AAA",
                                            ),
                                        ),
                                ),
                            ),
                    ),
                metadata = listOf(conceptMapMetadata),
            )
        val key1 =
            CacheKey(
                RegistryType.CONCEPT_MAP,
                "Observation.code",
                tenant,
            )
        client.conceptMapCache.put(key1, registry1)
        client.registryLastUpdated = LocalDateTime.now()

        val sourceCoding =
            Coding(
                system = Uri("systemA"),
                code = Code("valueA"),
            )
        val sourceConcept =
            CodeableConcept(
                text = "to-be-replaced".asFHIR(),
                coding = listOf(sourceCoding),
            )
        val targetCoding =
            Coding(
                system = Uri("AAA"),
                code = Code("AAA"),
                display = "AAA".asFHIR(),
                version = "AAA".asFHIR(),
            )
        val targetConcept =
            CodeableConcept(
                text = "replaced-it".asFHIR(),
                coding = listOf(targetCoding),
            )
        val targetSourceExtension =
            Extension(
                url = Uri(value = "extl"),
                value =
                    DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = sourceConcept,
                    ),
            )

        val mappedResult =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                sourceConcept,
                mockk<Observation>(),
            )
        assertEquals(
            targetConcept,
            mappedResult?.codeableConcept,
        )
        assertEquals(
            targetSourceExtension,
            mappedResult?.extension,
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept concatenates multiple matching concept maps`() {
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "11111",
                    filename = "file1.json",
                    conceptMapName = "Staging-test-1",
                    conceptMapUuid = "cm-111",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = "ObservationCode-1",
                    resourceType = "Observation",
                    tenantId = "test",
                ),
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "22222",
                    filename = "file2.json",
                    conceptMapName = "AllVitals-test-2",
                    conceptMapUuid = "cm-222",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = "ObservationCode-1",
                    resourceType = "Observation",
                    tenantId = "test",
                ),
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "33333",
                    filename = "file3.json",
                    conceptMapName = "HeartRate-test-3",
                    conceptMapUuid = "cm-333",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = "ObservationCode-1",
                    resourceType = "Observation",
                    tenantId = "test",
                ),
            )
        val mockkMap1 =
            mockk<ConceptMap> {
                every { group } returns
                    listOf(
                        mockk {
                            every { target?.value } returns "targetSystem-1"
                            every { targetVersion?.value } returns "targetVersion-1"
                            every { source?.value } returns "system-Staging-1"
                            every { element } returns
                                listOf(
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueA"),
                                                                    system = Uri("system-Staging-1"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceA",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextAAA"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueAAA"
                                                    every { display?.value } returns "targetDisplayAAA"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueB"),
                                                                    system = Uri("system-Staging-1"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceB",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextBBB"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueBBB"
                                                    every { display?.value } returns "targetDisplayBBB"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                )
                        },
                    )
            }
        val mockkMap2 =
            mockk<ConceptMap> {
                every { group } returns
                    listOf(
                        mockk {
                            every { target?.value } returns "targetSystem-2"
                            every { targetVersion?.value } returns "targetVersion-2"
                            every { source?.value } returns "system-AllVitals-2"
                            every { element } returns
                                listOf(
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueX"),
                                                                    system = Uri("system-AllVitals-2"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceX",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextXXX"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueXXX"
                                                    every { display?.value } returns "targetDisplayXXX"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueY"),
                                                                    system = Uri("system-AllVitals-2"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceY",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextYYY"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueYYY"
                                                    every { display?.value } returns "targetDisplayYYY"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueZ"),
                                                                    system = Uri("system-AllVitals-2"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceZ",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextZZZ"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueZZZ"
                                                    every { display?.value } returns "targetDisplayZZZ"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                )
                        },
                    )
            }
        val mockkMap3 =
            mockk<ConceptMap> {
                every { group } returns
                    listOf(
                        mockk {
                            every { target?.value } returns "targetSystem-3"
                            every { targetVersion?.value } returns "targetVersion-3"
                            every { source?.value } returns "system-HeartRate-3"
                            every { element } returns
                                listOf(
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueC"),
                                                                    system = Uri("system-HeartRate-3"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceC",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextCCC"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueCCC"
                                                    every { display?.value } returns "targetDisplayCCC"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueD"),
                                                                    system = Uri("system-HeartRate-3"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceD",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextDDD"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueDDD"
                                                    every { display?.value } returns "targetDisplayDDD"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                )
                        },
                    )
            }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "mapJson1"
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2
        every { ociClient.getObjectFromINFX("file3.json") } returns "mapJson3"
        every { JacksonUtil.readJsonObject("mapJson3", ConceptMap::class) } returns mockkMap3
        val coding1 =
            Coding(
                code = Code(value = "sourceValueB"),
                system = Uri(value = "system-Staging-1"),
            )
        val concept1 =
            CodeableConcept(
                coding = listOf(coding1),
            )
        val mapping1 =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept1,
                mockk<Observation>(),
            )!!
        assertEquals("targetTextBBB", mapping1.codeableConcept.text!!.value)
        assertEquals(1, mapping1.codeableConcept.coding.size)
        assertEquals("targetSystem-1", mapping1.codeableConcept.coding[0].system!!.value)
        assertEquals("targetValueBBB", mapping1.codeableConcept.coding[0].code!!.value)
        assertEquals("targetDisplayBBB", mapping1.codeableConcept.coding[0].display!!.value)
        assertEquals("ObservationCode-1", mapping1.extension.url!!.value)
        assertEquals(concept1, mapping1.extension.value!!.value)
        val coding2 =
            Coding(
                code = Code(value = "sourceValueZ"),
                system = Uri(value = "system-AllVitals-2"),
            )
        val concept2 =
            CodeableConcept(
                coding = listOf(coding2),
            )
        val mapping2 =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept2,
                mockk<Observation>(),
            )!!
        assertEquals("targetTextZZZ", mapping2.codeableConcept.text!!.value)
        assertEquals(1, mapping2.codeableConcept.coding.size)
        assertEquals("targetSystem-2", mapping2.codeableConcept.coding[0].system!!.value)
        assertEquals("targetValueZZZ", mapping2.codeableConcept.coding[0].code!!.value)
        assertEquals("targetDisplayZZZ", mapping2.codeableConcept.coding[0].display!!.value)
        assertEquals("ObservationCode-1", mapping2.extension.url!!.value)
        assertEquals(concept2, mapping2.extension.value!!.value)
        val coding3 =
            Coding(
                code = Code(value = "sourceValueC"),
                system = Uri(value = "system-HeartRate-3"),
            )
        val concept3 =
            CodeableConcept(
                coding = listOf(coding3),
            )
        val mapping3 =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept3,
                mockk<Observation>(),
            )!!
        assertEquals("targetTextCCC", mapping3.codeableConcept.text!!.value)
        assertEquals(1, mapping3.codeableConcept.coding.size)
        assertEquals("targetSystem-3", mapping3.codeableConcept.coding[0].system!!.value)
        assertEquals("targetValueCCC", mapping3.codeableConcept.coding[0].code!!.value)
        assertEquals("targetDisplayCCC", mapping3.codeableConcept.coding[0].display!!.value)
        assertEquals("ObservationCode-1", mapping3.extension.url!!.value)
        assertEquals(concept3, mapping3.extension.value!!.value)
    }

    @Test
    fun `getConceptMapping for CodeableConcept concatenates multiple matching concept maps - excludes non-matching entries`() {
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "11111",
                    filename = "file1.json",
                    conceptMapName = "Staging-test-1",
                    conceptMapUuid = "cm-111",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = "ObservationCode-1",
                    resourceType = "Observation",
                    tenantId = "test",
                ),
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "22222",
                    filename = "file2.json",
                    conceptMapName = "AllVitals-test-2",
                    conceptMapUuid = "cm-222",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = "ObservationCode-1",
                    resourceType = "Observation",
                    tenantId = "test",
                ),
                NormalizationRegistryItem(
                    dataElement = "Appointment.status",
                    registryUuid = "33333",
                    filename = "file3.json",
                    conceptMapName = "Appointment-status-test",
                    conceptMapUuid = "cm-333",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = "Appointment-status-test",
                    resourceType = "Appointment",
                    tenantId = "test",
                ),
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "33333",
                    filename = "file3.json",
                    conceptMapName = "HeartRate-test-3",
                    conceptMapUuid = "cm-333",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = "ObservationCode-1",
                    resourceType = "Observation",
                    // wrong tenantId
                    tenantId = "other",
                ),
                NormalizationRegistryItem(
                    // wrong elementName
                    dataElement = "Appointment.status",
                    registryUuid = "44444",
                    filename = "file4.json",
                    conceptMapName = "Appointment-status-test",
                    conceptMapUuid = "cm-444",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = "Appointment-status-test",
                    resourceType = "Appointment",
                    tenantId = "test",
                ),
            )
        val mockkMap1 =
            mockk<ConceptMap> {
                every { group } returns
                    listOf(
                        mockk {
                            every { target?.value } returns "targetSystem-1"
                            every { targetVersion?.value } returns "targetVersion-1"
                            every { source?.value } returns "system-Staging-1"
                            every { element } returns
                                listOf(
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueA"),
                                                                    system = Uri("system-Staging-1"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceA",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextAAA"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueAAA"
                                                    every { display?.value } returns "targetDisplayAAA"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueB"),
                                                                    system = Uri("system-Staging-1"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceB",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextBBB"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueBBB"
                                                    every { display?.value } returns "targetDisplayBBB"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                )
                        },
                    )
            }
        val mockkMap2 =
            mockk<ConceptMap> {
                every { group } returns
                    listOf(
                        mockk {
                            every { target?.value } returns "targetSystem-2"
                            every { targetVersion?.value } returns "targetVersion-2"
                            every { source?.value } returns "system-AllVitals-2"
                            every { element } returns
                                listOf(
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueX"),
                                                                    system = Uri("system-AllVitals-2"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceX",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextXXX"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueXXX"
                                                    every { display?.value } returns "targetDisplayXXX"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueY"),
                                                                    system = Uri("system-AllVitals-2"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceY",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextYYY"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueYYY"
                                                    every { display?.value } returns "targetDisplayYYY"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueZ"),
                                                                    system = Uri("system-AllVitals-2"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceZ",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextZZZ"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueZZZ"
                                                    every { display?.value } returns "targetDisplayZZZ"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                )
                        },
                    )
            }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "mapJson1"
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2
        val coding1 =
            Coding(
                code = Code(value = "sourceValueB"),
                system = Uri(value = "system-Staging-1"),
            )
        val concept1 =
            CodeableConcept(
                coding = listOf(coding1),
            )
        val mapping1 =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept1,
                mockk<Observation>(),
            )!!
        assertEquals("targetTextBBB", mapping1.codeableConcept.text!!.value)
        assertEquals(1, mapping1.codeableConcept.coding.size)
        assertEquals("targetSystem-1", mapping1.codeableConcept.coding[0].system!!.value)
        assertEquals("targetValueBBB", mapping1.codeableConcept.coding[0].code!!.value)
        assertEquals("targetDisplayBBB", mapping1.codeableConcept.coding[0].display!!.value)
        assertEquals("ObservationCode-1", mapping1.extension.url!!.value)
        assertEquals(concept1, mapping1.extension.value!!.value)
        val coding2 =
            Coding(
                code = Code(value = "sourceValueZ"),
                system = Uri(value = "system-AllVitals-2"),
            )
        val concept2 =
            CodeableConcept(
                coding = listOf(coding2),
            )
        val mapping2 =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept2,
                mockk<Observation>(),
            )!!
        assertEquals("targetTextZZZ", mapping2.codeableConcept.text!!.value)
        assertEquals(1, mapping2.codeableConcept.coding.size)
        assertEquals("targetSystem-2", mapping2.codeableConcept.coding[0].system!!.value)
        assertEquals("targetValueZZZ", mapping2.codeableConcept.coding[0].code!!.value)
        assertEquals("targetDisplayZZZ", mapping2.codeableConcept.coding[0].display!!.value)
        assertEquals("ObservationCode-1", mapping2.extension.url!!.value)
        assertEquals(concept2, mapping2.extension.value!!.value)
        val coding3 =
            Coding(
                code = Code(value = "sourceValueC"),
                system = Uri(value = "system-HeartRate-3"),
            )
        val concept3 =
            CodeableConcept(
                coding = listOf(coding3),
            )
        val mapping3 =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept3,
                mockk<Observation>(),
            )
        assertNull(mapping3)
        val coding4 =
            Coding(
                code = Code(value = "arrived"),
                system = Uri(value = "AppointmentStatus-4"),
            )
        val concept4 =
            CodeableConcept(
                coding = listOf(coding4),
            )
        val mapping4 =
            client.getConceptMapping(
                tenant,
                "Appointment.status",
                concept4,
                mockk<Appointment>(),
            )
        assertNull(mapping4)
    }

    @Test
    fun `getConceptMapping for CodeableConcept concatenates multiple matching concept maps - multiple entries in target Coding lists`() {
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "11111",
                    filename = "file1.json",
                    conceptMapName = "Staging-test-1",
                    conceptMapUuid = "cm-111",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = "ObservationCode-1",
                    resourceType = "Observation",
                    tenantId = "test",
                ),
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "22222",
                    filename = "file2.json",
                    conceptMapName = "AllVitals-test-2",
                    conceptMapUuid = "cm-222",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = "ObservationCode-1",
                    resourceType = "Observation",
                    tenantId = "test",
                ),
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "33333",
                    filename = "file3.json",
                    conceptMapName = "HeartRate-test-3",
                    conceptMapUuid = "cm-333",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = "ObservationCode-1",
                    resourceType = "Observation",
                    tenantId = "test",
                ),
            )
        val mockkMap1 =
            mockk<ConceptMap> {
                every { group } returns
                    listOf(
                        mockk {
                            every { target?.value } returns "targetSystem-1"
                            every { targetVersion?.value } returns "targetVersion-1"
                            every { source?.value } returns "system-Staging-1"
                            every { element } returns
                                listOf(
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueAB"),
                                                                    system = Uri("system-Staging-1"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceAB",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextAB"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueAAA"
                                                    every { display?.value } returns "targetDisplayAAA"
                                                    every { dependsOn } returns emptyList()
                                                },
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueBBB"
                                                    every { display?.value } returns "targetDisplayBBB"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                )
                        },
                    )
            }
        val mockkMap2 =
            mockk<ConceptMap> {
                every { group } returns
                    listOf(
                        mockk {
                            every { target?.value } returns "targetSystem-2"
                            every { targetVersion?.value } returns "targetVersion-2"
                            every { source?.value } returns "system-AllVitals-2"
                            every { element } returns
                                listOf(
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueXYZ"),
                                                                    system = Uri("system-AllVitals-2"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceXYZ",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextXYZ"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueXXX"
                                                    every { display?.value } returns "targetDisplayXXX"
                                                    every { dependsOn } returns emptyList()
                                                },
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueYYY"
                                                    every { display?.value } returns "targetDisplayYYY"
                                                    every { dependsOn } returns emptyList()
                                                },
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueZZZ"
                                                    every { display?.value } returns "targetDisplayZZZ"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                )
                        },
                    )
            }
        val mockkMap3 =
            mockk<ConceptMap> {
                every { group } returns
                    listOf(
                        mockk {
                            every { target?.value } returns "targetSystem-3"
                            every { targetVersion?.value } returns "targetVersion-3"
                            every { source?.value } returns "system-HeartRate-3"
                            every { element } returns
                                listOf(
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueCD"),
                                                                    system = Uri("system-HeartRate-3"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceCD",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextCD"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueCCC"
                                                    every { display?.value } returns "targetDisplayCCC"
                                                    every { dependsOn } returns emptyList()
                                                },
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueDDD"
                                                    every { display?.value } returns "targetDisplayDDD"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                )
                        },
                    )
            }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "mapJson1"
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2
        every { ociClient.getObjectFromINFX("file3.json") } returns "mapJson3"
        every { JacksonUtil.readJsonObject("mapJson3", ConceptMap::class) } returns mockkMap3
        val coding1 =
            Coding(
                code = Code(value = "sourceValueAB"),
                system = Uri(value = "system-Staging-1"),
            )
        val concept1 =
            CodeableConcept(
                coding = listOf(coding1),
            )
        val mapping1 =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept1,
                mockk<Observation>(),
            )!!
        assertEquals("targetTextAB", mapping1.codeableConcept.text!!.value)
        assertEquals(2, mapping1.codeableConcept.coding.size)
        assertEquals("targetSystem-1", mapping1.codeableConcept.coding[0].system!!.value)
        assertEquals("targetValueAAA", mapping1.codeableConcept.coding[0].code!!.value)
        assertEquals("targetDisplayAAA", mapping1.codeableConcept.coding[0].display!!.value)
        assertEquals("targetSystem-1", mapping1.codeableConcept.coding[1].system!!.value)
        assertEquals("targetValueBBB", mapping1.codeableConcept.coding[1].code!!.value)
        assertEquals("targetDisplayBBB", mapping1.codeableConcept.coding[1].display!!.value)
        assertEquals("ObservationCode-1", mapping1.extension.url!!.value)
        assertEquals(concept1, mapping1.extension.value!!.value)
        val coding2 =
            Coding(
                code = Code(value = "sourceValueXYZ"),
                system = Uri(value = "system-AllVitals-2"),
            )
        val concept2 =
            CodeableConcept(
                coding = listOf(coding2),
            )
        val mapping2 =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept2,
                mockk<Observation>(),
            )!!
        assertEquals("targetTextXYZ", mapping2.codeableConcept.text!!.value)
        assertEquals(3, mapping2.codeableConcept.coding.size)
        assertEquals("targetSystem-2", mapping2.codeableConcept.coding[0].system!!.value)
        assertEquals("targetValueXXX", mapping2.codeableConcept.coding[0].code!!.value)
        assertEquals("targetDisplayXXX", mapping2.codeableConcept.coding[0].display!!.value)
        assertEquals("targetSystem-2", mapping2.codeableConcept.coding[1].system!!.value)
        assertEquals("targetValueYYY", mapping2.codeableConcept.coding[1].code!!.value)
        assertEquals("targetDisplayYYY", mapping2.codeableConcept.coding[1].display!!.value)
        assertEquals("targetSystem-2", mapping2.codeableConcept.coding[2].system!!.value)
        assertEquals("targetValueZZZ", mapping2.codeableConcept.coding[2].code!!.value)
        assertEquals("targetDisplayZZZ", mapping2.codeableConcept.coding[2].display!!.value)
        assertEquals("ObservationCode-1", mapping2.extension.url!!.value)
        assertEquals(concept2, mapping2.extension.value!!.value)
        val coding3 =
            Coding(
                code = Code(value = "sourceValueCD"),
                system = Uri(value = "system-HeartRate-3"),
            )
        val concept3 =
            CodeableConcept(
                coding = listOf(coding3),
            )
        val mapping3 =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept3,
                mockk<Observation>(),
            )!!
        assertEquals("targetTextCD", mapping3.codeableConcept.text!!.value)
        assertEquals(2, mapping3.codeableConcept.coding.size)
        assertEquals("targetSystem-3", mapping3.codeableConcept.coding[0].system!!.value)
        assertEquals("targetValueCCC", mapping3.codeableConcept.coding[0].code!!.value)
        assertEquals("targetDisplayCCC", mapping3.codeableConcept.coding[0].display!!.value)
        assertEquals("targetSystem-3", mapping3.codeableConcept.coding[1].system!!.value)
        assertEquals("targetValueDDD", mapping3.codeableConcept.coding[1].code!!.value)
        assertEquals("targetDisplayDDD", mapping3.codeableConcept.coding[1].display!!.value)
        assertEquals("ObservationCode-1", mapping3.extension.url!!.value)
        assertEquals(concept3, mapping3.extension.value!!.value)
    }

    @Test
    fun `getConceptMapping for CodeableConcept - 1 match - TESTING`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "registry-uuid",
                    filename = "file1.json",
                    conceptMapName = "TestObservationsMashup",
                    conceptMapUuid = "TestObservationsMashup-uuid",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = sourceUrl,
                    resourceType = "Observation",
                    tenantId = "test",
                ),
            )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testConceptMapTest
        val concept =
            CodeableConcept(
                text = "Yellow".asFHIR(),
                coding = listOf(),
            )
        val mapping =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept,
                mockk<Observation>(),
            )!!
        assertEquals(
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code("371244009"),
                            system = Uri("http://snomed.info/sct"),
                            version = "0.0.1".asFHIR(),
                            display = "Yellow color (qualifier value)".asFHIR(),
                        ),
                    ),
                text = "Yellow".asFHIR(),
            ),
            mapping.codeableConcept,
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept),
            ),
            mapping.extension,
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element code - source Coding has 1 member - match found`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "registry-uuid",
                    filename = "file1.json",
                    conceptMapName = "TestObservationsMashup",
                    conceptMapUuid = "TestObservationsMashup-uuid",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = sourceUrl,
                    resourceType = "Observation",
                    tenantId = "test",
                ),
            )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testConceptMap
        val concept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code(value = "72166-2"),
                            system = Uri(value = "http://loinc.org"),
                        ),
                    ),
            )
        val mapping =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept,
                mockk<Observation>(),
            )!!
        assertEquals(
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code("72166-2"),
                            system = Uri("http://loinc.org"),
                            version = "0.0.1".asFHIR(),
                            display = "Tobacco smoking status".asFHIR(),
                        ),
                    ),
                text = "Tobacco smoking status".asFHIR(),
            ),
            mapping.codeableConcept,
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept),
            ),
            mapping.extension,
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element code - source Coding has 2 members in order - match found`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "c4a396d7-1fa1-41e5-9184-85c25eec47a4",
                    filename = "file1.json",
                    conceptMapName = "TestObservationsMashup",
                    conceptMapUuid = "TestObservationsMashup-uuid",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = sourceUrl,
                    resourceType = "Observation",
                    tenantId = "test",
                ),
            )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testConceptMap
        val concept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code(value = "21704910"),
                            system = Uri(value = "https://fhir.cerner.com/ec2458f2-1e24-41c8-b71b-0e701af7583d/codeSet/72"),
                            display = "Potassium Level".asFHIR(),
                        ),
                        Coding(
                            code = Code(value = "2823-3"),
                            system = Uri(value = "http://loinc.org"),
                        ),
                    ),
            )
        val mapping =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept,
                mockk<Observation>(),
            )!!
        assertEquals(
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code("2823-3"),
                            system = Uri("http://loinc.org"),
                            version = "0.0.1".asFHIR(),
                            display = "Potassium [Moles/volume] in Serum or Plasma".asFHIR(),
                        ),
                    ),
                text = "Potassium Level".asFHIR(),
            ),
            mapping.codeableConcept,
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept),
            ),
            mapping.extension,
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element code - source Coding has 2 members out of order - match found`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "c4a396d7-1fa1-41e5-9184-85c25eec47a4",
                    filename = "file1.json",
                    conceptMapName = "Cerncodeobservationstoloinc",
                    conceptMapUuid = "ef731708-e333-4933-af74-6bf97cb4077e",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = sourceUrl,
                    resourceType = "Observation",
                    tenantId = "test",
                ),
            )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testConceptMap
        val concept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code(value = "2823-3"),
                            system = Uri(value = "http://loinc.org"),
                        ),
                        Coding(
                            code = Code(value = "21704910"),
                            system = Uri(value = "https://fhir.cerner.com/ec2458f2-1e24-41c8-b71b-0e701af7583d/codeSet/72"),
                            display = "Potassium Level".asFHIR(),
                        ),
                    ),
            )
        val mapping =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept,
                mockk<Observation>(),
            )!!
        assertEquals(
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code("2823-3"),
                            system = Uri("http://loinc.org"),
                            version = "0.0.1".asFHIR(),
                            display = "Potassium [Moles/volume] in Serum or Plasma".asFHIR(),
                        ),
                    ),
                text = "Potassium Level".asFHIR(),
            ),
            mapping.codeableConcept,
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept),
            ),
            mapping.extension,
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element code - source Coding has partial match but too few members - no match`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "c4a396d7-1fa1-41e5-9184-85c25eec47a4",
                    filename = "file1.json",
                    conceptMapName = "TestObservationsMashup",
                    conceptMapUuid = "TestObservationsMashup-uuid",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = sourceUrl,
                    resourceType = "Observation",
                    tenantId = "test",
                ),
            )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testConceptMap
        val concept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code(value = "21704910"),
                            system = Uri(value = "https://fhir.cerner.com/ec2458f2-1e24-41c8-b71b-0e701af7583d/codeSet/72"),
                        ),
                    ),
            )
        val mapping =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept,
                mockk<Observation>(),
            )
        assertNull(mapping)
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element code - source Coding has partial match but too many members - no match`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "c4a396d7-1fa1-41e5-9184-85c25eec47a4",
                    filename = "file1.json",
                    conceptMapName = "TestObservationsMashup",
                    conceptMapUuid = "TestObservationsMashup-uuid",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = sourceUrl,
                    resourceType = "Observation",
                    tenantId = "test",
                ),
            )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testConceptMap
        val concept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code(value = "21704910"),
                            system = Uri(value = "https://fhir.cerner.com/ec2458f2-1e24-41c8-b71b-0e701af7583d/codeSet/72"),
                        ),
                        Coding(
                            code = Code(value = "2823-3"),
                            system = Uri(value = "http://loinc.org"),
                        ),
                        Coding(
                            code = Code(value = "72166-2"),
                            system = Uri(value = "http://loinc.org"),
                        ),
                    ),
            )
        val mapping =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept,
                mockk<Observation>(),
            )
        assertNull(mapping)
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element code - match found - multiple targets are returned correctly`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "registry-uuid",
                    filename = "file1.json",
                    conceptMapName = "TestObservationsMashup",
                    conceptMapUuid = "TestObservationsMashup-uuid",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = sourceUrl,
                    resourceType = "Observation",
                    tenantId = "test",
                ),
            )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testConceptMap
        val concept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code(value = "85354-9"),
                            system = Uri(value = "http://loinc.org"),
                            display = "Blood pressure panel with all children optional".asFHIR(),
                        ),
                    ),
            )
        val mapping =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept,
                mockk<Observation>(),
            )!!
        assertEquals(
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code("85354-9"),
                            system = Uri("http://loinc.org"),
                            version = "0.0.1".asFHIR(),
                            display = "Blood pressure panel with all children optional".asFHIR(),
                        ),
                        Coding(
                            code = Code("3141-9"),
                            system = Uri("http://loinc.org"),
                            version = "0.0.1".asFHIR(),
                            display = "Body weight Measured".asFHIR(),
                        ),
                        Coding(
                            code = Code("55284-4"),
                            system = Uri("http://loinc.org"),
                            version = "0.0.1".asFHIR(),
                            display = "Blood pressure systolic and diastolic".asFHIR(),
                        ),
                    ),
                text = "Blood pressure".asFHIR(),
            ),
            mapping.codeableConcept,
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept),
            ),
            mapping.extension,
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element codes from a staging map - source Coding has 1 member - match found`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "registry-uuid",
                    filename = "file1.json",
                    conceptMapName = "TestObservationsStaging",
                    conceptMapUuid = "TestObservationsStaging-uuid",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = sourceUrl,
                    resourceType = "Observation",
                    tenantId = "test",
                ),
            )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testStagingConceptMap

        // ' in Coding.display, ' in text
        // {[{EPIC#44065, Clark's level, urn:oid:1.2.840.114350.1.13.297.2.7.2.727688}], FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - PROGNOSTIC INDICATORS - CLARK'S LEVEL}"
        val concept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code(value = "EPIC#44065"),
                            system = Uri(value = "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688"),
                            display = "Clark's level".asFHIR(),
                        ),
                    ),
            )
        val mapping =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept,
                mockk<Observation>(),
            )!!
        assertEquals(
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code("385347004"),
                            system = Uri("http://snomed.info/sct"),
                            version = "2023-03-01".asFHIR(),
                            display = "Clark melanoma level finding (finding)".asFHIR(),
                        ),
                    ),
                text = "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - PROGNOSTIC INDICATORS - CLARK'S LEVEL".asFHIR(),
            ),
            mapping.codeableConcept,
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept),
            ),
            mapping.extension,
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element codes from a staging map - source Coding has 2 members in order - match found`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "c4a396d7-1fa1-41e5-9184-85c25eec47a4",
                    filename = "file1.json",
                    conceptMapName = "TestObservationsStaging",
                    conceptMapUuid = "TestObservationsStaging-uuid",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = sourceUrl,
                    resourceType = "Observation",
                    tenantId = "test",
                ),
            )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testStagingConceptMap

        // / in Coding.display, 2 Coding
        // {[{SNOMED#246111003, null, http://snomed.info/sct}, {EPIC#42388, anatomic stage/prognostic group, urn:oid:1.2.840.114350.1.13.297.2.7.2.727688}], FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - ANATOMIC STAGE/PROGNOSTIC GROUP}
        val concept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code(value = "SNOMED#246111003"),
                            system = Uri(value = "http://snomed.info/sct"),
                        ),
                        Coding(
                            code = Code(value = "EPIC#42388"),
                            system = Uri(value = "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688"),
                            display = "anatomic stage/prognostic group".asFHIR(),
                        ),
                    ),
            )
        val mapping =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept,
                mockk<Observation>(),
            )!!
        assertEquals(
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code("246111003"),
                            system = Uri("http://snomed.info/sct"),
                            version = "2023-03-01".asFHIR(),
                            display = "Prognostic score (attribute)".asFHIR(),
                        ),
                    ),
                text = "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - ANATOMIC STAGE/PROGNOSTIC GROUP".asFHIR(),
            ),
            mapping.codeableConcept,
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept),
            ),
            mapping.extension,
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element codes from a staging map - source Coding has 2 members out of order - match found`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "c4a396d7-1fa1-41e5-9184-85c25eec47a4",
                    filename = "file1.json",
                    conceptMapName = "TestObservationsStaging",
                    conceptMapUuid = "TestObservationsStaging-uuid",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = sourceUrl,
                    resourceType = "Observation",
                    tenantId = "test",
                ),
            )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testStagingConceptMap

        // / in Coding.display, 2 Coding out of order
        // {[{SNOMED#246111003, null, http://snomed.info/sct}, {EPIC#42388, anatomic stage/prognostic group, urn:oid:1.2.840.114350.1.13.297.2.7.2.727688}], FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - ANATOMIC STAGE/PROGNOSTIC GROUP}
        val concept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code(value = "EPIC#42388"),
                            system = Uri(value = "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688"),
                            display = "anatomic stage/prognostic group".asFHIR(),
                        ),
                        Coding(
                            code = Code(value = "SNOMED#246111003"),
                            system = Uri(value = "http://snomed.info/sct"),
                        ),
                    ),
            )
        val mapping =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept,
                mockk<Observation>(),
            )!!
        assertEquals(
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code("246111003"),
                            system = Uri("http://snomed.info/sct"),
                            version = "2023-03-01".asFHIR(),
                            display = "Prognostic score (attribute)".asFHIR(),
                        ),
                    ),
                text = "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - ANATOMIC STAGE/PROGNOSTIC GROUP".asFHIR(),
            ),
            mapping.codeableConcept,
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept),
            ),
            mapping.extension,
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element codes from a staging map - more punctuation cases - match found`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "c4a396d7-1fa1-41e5-9184-85c25eec47a4",
                    filename = "file1.json",
                    conceptMapName = "TestObservationsStaging",
                    conceptMapUuid = "TestObservationsStaging-uuid",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = sourceUrl,
                    resourceType = "Observation",
                    tenantId = "test",
                ),
            )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testStagingConceptMap

        // () in Coding.display, () in text
        // {[{EPIC#42391, lymph-vascular invasion (LVI), urn:oid:1.2.840.114350.1.13.297.2.7.2.727688}], FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - LYMPH-VASCULAR INVASION (LVI)}
        val concept1 =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code(value = "EPIC#42391"),
                            system = Uri(value = "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688"),
                            display = "lymph-vascular invasion (LVI)".asFHIR(),
                        ),
                    ),
            )
        val mapping1 =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept1,
                mockk<Observation>(),
            )!!
        assertEquals(
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code("371512006"),
                            system = Uri("http://snomed.info/sct"),
                            version = "2023-03-01".asFHIR(),
                            display = "Presence of direct invasion by primary malignant neoplasm to lymphatic vessel and/or small blood vessel (observable entity)".asFHIR(),
                        ),
                    ),
                text = "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - LYMPH-VASCULAR INVASION (LVI)".asFHIR(),
            ),
            mapping1.codeableConcept,
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept1),
            ),
            mapping1.extension,
        )

        // (/) in Coding.display, / (/) in ntext
        // {[{EPIC#31000073346, WHO/ISUP grade (low/high), urn:oid:1.2.840.114350.1.13.297.2.7.2.727688}], FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - WHO/ISUP GRADE (LOW/HIGH)}
        val concept2 =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code(value = "EPIC#31000073346"),
                            system = Uri(value = "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688"),
                            display = "WHO/ISUP grade (low/high)".asFHIR(),
                        ),
                    ),
            )
        val mapping2 =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept2,
                mockk<Observation>(),
            )!!
        assertEquals(
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code("396659000"),
                            system = Uri("http://snomed.info/sct"),
                            version = "2023-03-01".asFHIR(),
                            display = "Histologic grade of urothelial carcinoma by World Health Organization and International Society of Urological Pathology technique (observable entity)".asFHIR(),
                        ),
                    ),
                text = "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - WHO/ISUP GRADE (LOW/HIGH)".asFHIR(),
            ),
            mapping2.codeableConcept,
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept2),
            ),
            mapping2.extension,
        )

        // () and - in Coding.display, 2 Coding, nothing found in map, / () in text
        // unexpected spacing within group.element.code
        //    "code": " { [ { SNOMED#260767000 ,null , http://snomed.info/sct} ,{EPIC#42384     , regional lymph nodes (N),urn:oid:1.2.840.114350.1.13.297.2.7.2.727688   } ] , FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - TNM CLASSIFICATION - AJCC N - REGIONAL LYMPH NODES (N)   }   "
        val concept3 =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code(value = "SNOMED#260767000"),
                            system = Uri(value = "http://snomed.info/sct"),
                        ),
                        Coding(
                            code = Code(value = "EPIC#42384"),
                            system = Uri(value = "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688"),
                            display = "regional lymph nodes (N)".asFHIR(),
                        ),
                    ),
            )
        val mapping3 =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept3,
                mockk<Observation>(),
            )!!
        assertEquals(
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code("260767000"),
                            system = Uri("http://snomed.info/sct"),
                            version = "2023-03-01".asFHIR(),
                            display = "N - Regional lymph node stage (attribute)".asFHIR(),
                        ),
                    ),
                text = "FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - TNM CLASSIFICATION - AJCC N - REGIONAL LYMPH NODES (N)".asFHIR(),
            ),
            mapping3.codeableConcept,
        )
        assertEquals(
            Extension(
                url = Uri(sourceUrl),
                value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = concept3),
            ),
            mapping3.extension,
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - parse formatted group element codes from a staging map - source Coding has 2 members in order - match not found`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "c4a396d7-1fa1-41e5-9184-85c25eec47a4",
                    filename = "file1.json",
                    conceptMapName = "TestObservationsStaging",
                    conceptMapUuid = "TestObservationsStaging-uuid",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = sourceUrl,
                    resourceType = "Observation",
                    tenantId = "test",
                ),
            )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns testStagingConceptMap

        // / in Coding.display, 2 Coding, 1 with typo in code value
        // {[{SNOMED#246111003, null, http://snomed.info/sct}, {EPIC#42388, anatomic stage/prognostic group, urn:oid:1.2.840.114350.1.13.297.2.7.2.727688}], FINDINGS - PHYSICAL EXAM - ONCOLOGY - STAGING - ANATOMIC STAGE/PROGNOSTIC GROUP}
        val concept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code(value = "SNOMED#246111003"),
                            system = Uri(value = "http://snomed.info/sct"),
                        ),
                        Coding(
                            // deliberate typo (hard to see)
                            code = Code(value = "EPIC#442388"),
                            system = Uri(value = "urn:oid:1.2.840.114350.1.13.297.2.7.2.727688"),
                        ),
                    ),
            )
        val mapping =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                concept,
                mockk<Observation>(),
            )
        assertNull(mapping)
    }

    @Test
    fun `getConceptMapping for CodeableConcept concatenates multiple matching concept maps - errors if URLs do not agree`() {
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "11111",
                    filename = "file1.json",
                    conceptMapName = "Staging-test-1",
                    conceptMapUuid = "cm-111",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = "ObservationCode-1",
                    resourceType = "Observation",
                    tenantId = "test",
                ),
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "22222",
                    filename = "file2.json",
                    conceptMapName = "AllVitals-test-2",
                    conceptMapUuid = "cm-222",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = "ObservationCode-2",
                    resourceType = "Observation",
                    tenantId = "test",
                ),
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "33333",
                    filename = "file3.json",
                    conceptMapName = "HeartRate-test-3",
                    conceptMapUuid = "cm-333",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = "ObservationCode-3",
                    resourceType = "Observation",
                    tenantId = "test",
                ),
            )
        val mockkMap1 =
            mockk<ConceptMap> {
                every { group } returns
                    listOf(
                        mockk {
                            every { target?.value } returns "targetSystem-1"
                            every { targetVersion?.value } returns "targetVersion-1"
                            every { source?.value } returns "system-Staging-1"
                            every { element } returns
                                listOf(
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueA"),
                                                                    system = Uri("system-Staging-1"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceA",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextAAA"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueAAA"
                                                    every { display?.value } returns "targetDisplayAAA"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueB"),
                                                                    system = Uri("system-Staging-1"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceB",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextBBB"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueBBB"
                                                    every { display?.value } returns "targetDisplayBBB"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                )
                        },
                    )
            }
        val mockkMap2 =
            mockk<ConceptMap> {
                every { group } returns
                    listOf(
                        mockk {
                            every { target?.value } returns "targetSystem-2"
                            every { targetVersion?.value } returns "targetVersion-2"
                            every { source?.value } returns "system-AllVitals-2"
                            every { element } returns
                                listOf(
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueX"),
                                                                    system = Uri("system-AllVitals-2"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceX",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextXXX"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueXXX"
                                                    every { display?.value } returns "targetDisplayXXX"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueY"),
                                                                    system = Uri("system-AllVitals-2"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceY",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextYYY"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueYYY"
                                                    every { display?.value } returns "targetDisplayYYY"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueZ"),
                                                                    system = Uri("system-AllVitals-2"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceZ",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextZZZ"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueZZZ"
                                                    every { display?.value } returns "targetDisplayZZZ"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                )
                        },
                    )
            }
        val mockkMap3 =
            mockk<ConceptMap> {
                every { group } returns
                    listOf(
                        mockk {
                            every { target?.value } returns "targetSystem-3"
                            every { targetVersion?.value } returns "targetVersion-3"
                            every { source?.value } returns "system-HeartRate-3"
                            every { element } returns
                                listOf(
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueC"),
                                                                    system = Uri("system-HeartRate-3"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceC",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextCCC"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueCCC"
                                                    every { display?.value } returns "targetDisplayCCC"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                    mockk {
                                        every { code?.extension } returns
                                            listOf(
                                                createClinicalSourceDataExtension(
                                                    CodeableConcept(
                                                        coding =
                                                            listOf(
                                                                Coding(
                                                                    code = Code("sourceValueD"),
                                                                    system = Uri("system-HeartRate-3"),
                                                                ),
                                                            ),
                                                    ),
                                                    "sourceD",
                                                ),
                                            )
                                        every { display?.value } returns "targetTextDDD"
                                        every { target } returns
                                            listOf(
                                                mockk {
                                                    every { id } returns null
                                                    every { code?.value } returns "targetValueDDD"
                                                    every { display?.value } returns "targetDisplayDDD"
                                                    every { dependsOn } returns emptyList()
                                                },
                                            )
                                    },
                                )
                        },
                    )
            }
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns "mapJson1"
        every { JacksonUtil.readJsonObject("mapJson1", ConceptMap::class) } returns mockkMap1
        every { ociClient.getObjectFromINFX("file2.json") } returns "mapJson2"
        every { JacksonUtil.readJsonObject("mapJson2", ConceptMap::class) } returns mockkMap2
        every { ociClient.getObjectFromINFX("file3.json") } returns "mapJson3"
        every { JacksonUtil.readJsonObject("mapJson3", ConceptMap::class) } returns mockkMap3
        val coding =
            Coding(
                code = Code(value = "sourceValueB"),
                system = Uri(value = "system-Staging-1"),
            )
        val concept =
            CodeableConcept(
                text = "ignore-1".asFHIR(),
                coding = listOf(coding),
            )
        val exception =
            assertThrows<MissingNormalizationContentException> {
                client.getConceptMapping(
                    tenant,
                    "Observation.code",
                    concept,
                    mockk<Observation>(),
                )
            }

        assertEquals(
            "Concept map(s) for tenant 'test' and Observation.code have missing or inconsistent source extension URLs",
            exception.message,
        )
    }

    @Test
    fun `getConceptMapping for CodeableConcept - source code contains no valueCodeableConcept attribute wrapper`() {
        val sourceUrl = "tenant-sourceObservationCode"
        val cmTestRegistry =
            listOf(
                NormalizationRegistryItem(
                    dataElement = "Observation.code",
                    registryUuid = "registry-uuid",
                    filename = "file1.json",
                    conceptMapName = "TestObservationsMashup",
                    conceptMapUuid = "TestObservationsMashup-uuid",
                    registryEntryType = RegistryType.CONCEPT_MAP,
                    version = "1",
                    sourceExtensionUrl = sourceUrl,
                    resourceType = "Observation",
                    tenantId = "test",
                ),
            )
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns cmTestRegistry
        every { ociClient.getObjectFromINFX("file1.json") } returns
            """
                {
                  "resourceType": "ConceptMap",
                  "title": "Test Observations Mashup (for Dev Testing ONLY)",
                  "id": "TestObservationsMashup-id",
                  "name": "TestObservationsMashup-name",
                  "contact": [
                    {
                      "name": "Interops (for Dev Testing ONLY)"
                    }
                  ],
                  "url": "http://projectronin.io/fhir/StructureDefinition/ConceptMap/TestObservationsMashup",
                  "description": "Interops  (for Dev Testing ONLY)",
                  "purpose": "Testing",
                  "publisher": "Project Ronin",
                  "experimental": true,
                  "date": "2023-05-26",
                  "version": 1,
                  "group": [
                    {
                      "source": "http://projectronin.io/fhir/CodeSystem/test/TestObservationsMashup",
                      "sourceVersion": "1.0",
                      "target": "http://loinc.org",
                      "targetVersion": "0.0.1",
                      "element": [
                        {
                          "id": "06c19b8e4718f1bf6e81f992cfc12c1e",
                          "_code": {
                            "extension": [ {
                              "id": "sourceExtension1",
                              "url": "http://projectronin.io/fhir/StructureDefinition/Extension/canonicalSourceData",
                              "valueCodeableConcept": {
                                "coding": [ {
                                  "code": "363905002",
                                  "system": "http://snomed.info/sct",
                                  "display": "Details of alcohol drinking behavior (observable entity)"
                                } ]
                              }
                            } ]
                          },
                          "display": "Tobacco smoking status",
                          "target": [
                            {
                              "id": "836cc342c39afcc7a8dee6277abc7b75",
                              "code": "72166-2",
                              "display": "Tobacco smoking status",
                              "equivalence": "equivalent",
                              "comment": null
                            }
                          ]
                        }
                      ]
                    }
                  ],
                  "extension": [
                    {
                      "url": "http://projectronin.io/fhir/StructureDefinition/Extension/ronin-conceptMapSchema",
                      "valueString": "1.0.0"
                    }
                  ],
                  "meta": {
                    "lastUpdated": "2023-05-26T12:49:56.285403+00:00"
                  }
            }
            """.trimIndent()
        val concept =
            CodeableConcept(
                coding =
                    listOf(
                        Coding(
                            code = Code(value = "363905002"),
                            system = Uri(value = "http://snomed.info/sct"),
                            display = "Details of alcohol drinking behavior (observable entity)".asFHIR(),
                        ),
                    ),
            )

        val mapping = client.getConceptMapping(tenant, "Observation.code", concept, mockk<Observation>())
        assertNotNull(mapping)
        assertEquals("72166-2", mapping?.codeableConcept?.coding?.first()?.code?.value)
    }

    private val dependsOn =
        ConceptMapDependsOn(
            property = Uri("medication.form"),
            value =
                FHIRString(
                    null,
                    extension =
                        listOf(
                            createClinicalSourceDataExtension(
                                CodeableConcept(text = "some-form-value".asFHIR()),
                                "dependsOn1",
                            ),
                        ),
                ),
        )
    private val dependsOn2 =
        ConceptMapDependsOn(
            property = Uri("medication.amount"),
            value =
                FHIRString(
                    null,
                    extension =
                        listOf(
                            createClinicalSourceDataExtension(
                                CodeableConcept(text = "some-amount-value".asFHIR()),
                                "dependsOn2",
                            ),
                        ),
                ),
        )

    private val mappingWithDependsOnTargets =
        SourceConcept(
            codeableConcept = CodeableConcept(coding = listOf(Coding(code = Code("valueA"), system = Uri("systemA")))),
        ) to
            listOf(
                TargetConcept(
                    text = "textAAA",
                    element =
                        listOf(
                            TargetConceptMapValue(
                                "targetValueAAA",
                                "targetSystemAAA",
                                "targetDisplayAAA",
                                "targetVersionAAA",
                                listOf(dependsOn),
                            ),
                        ),
                ),
                TargetConcept(
                    text = "textBBB",
                    element =
                        listOf(
                            TargetConceptMapValue(
                                "targetValueBBB",
                                "targetSystemBBB",
                                "targetDisplayBBB",
                                "targetVersionBBB",
                                listOf(dependsOn2),
                            ),
                        ),
                ),
            )

    @Test
    fun `dependsOnEvaluator not found for target with no dependsOn data`() {
        val registry =
            ConceptMapItem(
                sourceExtensionUrl = "ext-AB",
                map = mapAB,
                metadata = listOf(conceptMapMetadata),
            )
        val key =
            CacheKey(
                RegistryType.CONCEPT_MAP,
                "Observation.code",
                tenant,
            )
        client.conceptMapCache.put(key, registry)
        client.registryLastUpdated = LocalDateTime.now()

        val sourceCoding =
            Coding(
                system = Uri("systemA"),
                code = Code("valueA"),
            )
        val sourceConcept = CodeableConcept(coding = listOf(sourceCoding))

        val targetCoding =
            Coding(
                system = Uri("targetSystemAAA"),
                code = Code("targetValueAAA"),
                display = "targetDisplayAAA".asFHIR(),
                version = "targetVersionAAA".asFHIR(),
            )
        val targetConcept =
            CodeableConcept(
                text = "textAAA".asFHIR(),
                coding = listOf(targetCoding),
            )
        val targetSourceExtension =
            Extension(
                url = Uri(value = "ext-AB"),
                value =
                    DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = sourceConcept,
                    ),
            )
        val mappedResult =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                sourceConcept,
                mockk<Observation>(),
            )
        assertEquals(
            targetConcept,
            mappedResult?.codeableConcept,
        )
        assertEquals(
            targetSourceExtension,
            mappedResult?.extension,
        )

        verify(exactly = 0) { medicationDependsOnEvaluator.meetsDependsOn(any(), any()) }
    }

    @Test
    fun `dependsOnEvaluator found for target with no dependsOn data`() {
        val registry =
            ConceptMapItem(
                sourceExtensionUrl = "ext-AB",
                map = mapAB,
                metadata = listOf(conceptMapMetadata),
            )
        val key =
            CacheKey(
                RegistryType.CONCEPT_MAP,
                "Medication.code",
                tenant,
            )
        client.conceptMapCache.put(key, registry)
        client.registryLastUpdated = LocalDateTime.now()

        val sourceCoding =
            Coding(
                system = Uri("systemA"),
                code = Code("valueA"),
            )
        val sourceConcept = CodeableConcept(coding = listOf(sourceCoding))
        val targetCoding =
            Coding(
                system = Uri("targetSystemAAA"),
                code = Code("targetValueAAA"),
                display = "targetDisplayAAA".asFHIR(),
                version = "targetVersionAAA".asFHIR(),
            )
        val targetConcept =
            CodeableConcept(
                text = "textAAA".asFHIR(),
                coding = listOf(targetCoding),
            )
        val targetSourceExtension =
            Extension(
                url = Uri(value = "ext-AB"),
                value =
                    DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = sourceConcept,
                    ),
            )
        val mappedResult =
            client.getConceptMapping(
                tenant,
                "Medication.code",
                sourceConcept,
                mockk<Medication>(),
            )
        assertEquals(
            targetConcept,
            mappedResult?.codeableConcept,
        )
        assertEquals(
            targetSourceExtension,
            mappedResult?.extension,
        )

        verify(exactly = 0) { medicationDependsOnEvaluator.meetsDependsOn(any(), any()) }
    }

    @Test
    fun `dependsOnEvaluator not found for target with dependsOn data`() {
        val registry =
            ConceptMapItem(
                sourceExtensionUrl = "ext-AB",
                map = mapOf(mappingWithDependsOnTargets),
                metadata = listOf(conceptMapMetadata),
            )
        val key =
            CacheKey(
                RegistryType.CONCEPT_MAP,
                "Observation.code",
                tenant,
            )
        client.conceptMapCache.put(key, registry)
        client.registryLastUpdated = LocalDateTime.now()

        val sourceCoding =
            Coding(
                system = Uri("systemA"),
                code = Code("valueA"),
            )
        val sourceConcept = CodeableConcept(coding = listOf(sourceCoding))
        val mappedResult =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                sourceConcept,
                mockk<Observation>(),
            )
        assertNull(mappedResult)

        verify(exactly = 0) { medicationDependsOnEvaluator.meetsDependsOn(any(), any()) }
    }

    @Test
    fun `dependsOnEvaluator found and no target dependsOn is met`() {
        val registry =
            ConceptMapItem(
                sourceExtensionUrl = "ext-AB",
                map = mapOf(mappingWithDependsOnTargets),
                metadata = listOf(conceptMapMetadata),
            )
        val key =
            CacheKey(
                RegistryType.CONCEPT_MAP,
                "Observation.code",
                tenant,
            )
        client.conceptMapCache.put(key, registry)
        client.registryLastUpdated = LocalDateTime.now()

        val medication = mockk<Medication>()

        every { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn)) } returns false
        every { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn2)) } returns false

        val sourceCoding =
            Coding(
                system = Uri("systemA"),
                code = Code("valueA"),
            )
        val sourceConcept = CodeableConcept(coding = listOf(sourceCoding))
        val mappedResult =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                sourceConcept,
                medication,
            )
        assertNull(mappedResult)

        verify(exactly = 1) { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn)) }
        verify(exactly = 1) { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn2)) }
    }

    @Test
    fun `dependsOnEvaluator found and single target dependsOn is met`() {
        val registry =
            ConceptMapItem(
                sourceExtensionUrl = "ext-AB",
                map = mapOf(mappingWithDependsOnTargets),
                metadata = listOf(conceptMapMetadata),
            )
        val key =
            CacheKey(
                RegistryType.CONCEPT_MAP,
                "Observation.code",
                tenant,
            )
        client.conceptMapCache.put(key, registry)
        client.registryLastUpdated = LocalDateTime.now()

        val medication = mockk<Medication>()

        every { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn)) } returns true
        every { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn2)) } returns false

        val sourceCoding =
            Coding(
                system = Uri("systemA"),
                code = Code("valueA"),
            )
        val sourceConcept = CodeableConcept(coding = listOf(sourceCoding))
        val targetCoding =
            Coding(
                system = Uri("targetSystemAAA"),
                code = Code("targetValueAAA"),
                display = "targetDisplayAAA".asFHIR(),
                version = "targetVersionAAA".asFHIR(),
            )
        val targetConcept =
            CodeableConcept(
                text = "textAAA".asFHIR(),
                coding = listOf(targetCoding),
            )
        val targetSourceExtension =
            Extension(
                url = Uri(value = "ext-AB"),
                value =
                    DynamicValue(
                        type = DynamicValueType.CODEABLE_CONCEPT,
                        value = sourceConcept,
                    ),
            )
        val mappedResult =
            client.getConceptMapping(
                tenant,
                "Observation.code",
                sourceConcept,
                medication,
            )
        assertEquals(
            targetConcept,
            mappedResult?.codeableConcept,
        )
        assertEquals(
            targetSourceExtension,
            mappedResult?.extension,
        )

        verify(exactly = 1) { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn)) }
        verify(exactly = 1) { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn2)) }
    }

    @Test
    fun `dependsOnEvaluator found and multiple target dependsOn are met`() {
        val registry =
            ConceptMapItem(
                sourceExtensionUrl = "ext-AB",
                map = mapOf(mappingWithDependsOnTargets),
                metadata = listOf(conceptMapMetadata),
            )
        val key =
            CacheKey(
                RegistryType.CONCEPT_MAP,
                "Observation.code",
                tenant,
            )
        client.conceptMapCache.put(key, registry)
        client.registryLastUpdated = LocalDateTime.now()

        val medication = mockk<Medication>()

        every { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn)) } returns true
        every { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn2)) } returns true

        val sourceCoding =
            Coding(
                system = Uri("systemA"),
                code = Code("valueA"),
            )
        val sourceConcept = CodeableConcept(coding = listOf(sourceCoding))
        val exception =
            assertThrows<IllegalStateException> {
                client.getConceptMapping(
                    tenant,
                    "Observation.code",
                    sourceConcept,
                    medication,
                )
            }
        assertTrue(exception.message?.startsWith("Multiple qualified TargetConcepts found for") ?: false)

        verify(exactly = 1) { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn)) }
        verify(exactly = 1) { medicationDependsOnEvaluator.meetsDependsOn(medication, listOf(dependsOn2)) }
    }

    @Test
    fun `registry invalidates cache item missing from new registry`() {
        val registryItem1 =
            NormalizationRegistryItem(
                dataElement = "Patient.telecom.system",
                registryUuid = "12345",
                filename = "file1.json",
                conceptMapName = "PatientTelecomSystem-tenant",
                conceptMapUuid = "cm-111",
                registryEntryType = RegistryType.CONCEPT_MAP,
                version = "1",
                sourceExtensionUrl = "ext1",
                resourceType = "Patient",
                tenantId = "test",
            )
        val key1 =
            CacheKey(
                RegistryType.CONCEPT_MAP,
                "Patient.telecom.system",
                "test",
            )

        val key2 =
            CacheKey(
                RegistryType.CONCEPT_MAP,
                "Patient.telecom.use",
                "test",
            )
        val registryItem2 =
            NormalizationRegistryItem(
                dataElement = "Patient.telecom.use",
                registryUuid = "67890",
                filename = "file2.json",
                conceptMapName = "PatientTelecomUse-tenant",
                conceptMapUuid = "cm-222",
                registryEntryType = RegistryType.CONCEPT_MAP,
                version = "1",
                sourceExtensionUrl = "ext2",
                resourceType = "Patient",
                tenantId = "test",
            )

        client.registry = mapOf(key1 to listOf(registryItem1), key2 to listOf(registryItem2))
        client.registryLastUpdated = LocalDateTime.MIN

        val registry1 =
            ConceptMapItem(
                sourceExtensionUrl = "ext1",
                map =
                    mapOf(
                        SourceConcept(
                            code = Code("MyPhone"),
                        ) to
                            listOf(
                                TargetConcept(
                                    element =
                                        listOf(
                                            TargetConceptMapValue(
                                                "good-or-bad-for-enum",
                                                "good-or-bad-for-enum",
                                                "good-or-bad-for-enum",
                                                "1",
                                            ),
                                        ),
                                    text = "good-or-bad-for-enum, not validated here",
                                ),
                            ),
                    ),
                metadata = listOf(conceptMapMetadata),
            )
        client.conceptMapCache.put(key1, registry1)

        val registry2 =
            ConceptMapItem(
                sourceExtensionUrl = "ext1",
                map =
                    mapOf(
                        SourceConcept(
                            code = Code("MyPhone"),
                        ) to
                            listOf(
                                TargetConcept(
                                    element =
                                        listOf(
                                            TargetConceptMapValue(
                                                "good-or-bad-for-enum",
                                                "good-or-bad-for-enum",
                                                "good-or-bad-for-enum",
                                                "1",
                                            ),
                                        ),
                                    text = "good-or-bad-for-enum, not validated here",
                                ),
                            ),
                    ),
                metadata = listOf(conceptMapMetadata),
            )
        client.conceptMapCache.put(key2, registry2)

        val newRegistry = listOf(registryItem1)
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns newRegistry

        client.checkRegistryStatus(LocalDateTime.now())

        assertNotEquals(LocalDateTime.MIN, client.registryLastUpdated)
        assertEquals(mapOf(key1 to listOf(registryItem1)), client.registry)
        assertEquals(registry1, client.conceptMapCache.getIfPresent(key1))
        assertNull(client.conceptMapCache.getIfPresent(key2))
    }

    @Test
    fun `registry invalidates cache item that has changed in new registry`() {
        val registryItem1 =
            NormalizationRegistryItem(
                dataElement = "Patient.telecom.system",
                registryUuid = "12345",
                filename = "file1.json",
                conceptMapName = "PatientTelecomSystem-tenant",
                conceptMapUuid = "cm-111",
                registryEntryType = RegistryType.CONCEPT_MAP,
                version = "1",
                sourceExtensionUrl = "ext1",
                resourceType = "Patient",
                tenantId = "test",
            )
        val key1 =
            CacheKey(
                RegistryType.CONCEPT_MAP,
                "Patient.telecom.system",
                "test",
            )

        val key2 =
            CacheKey(
                RegistryType.CONCEPT_MAP,
                "Patient.telecom.use",
                "test",
            )
        val registryItem2 =
            NormalizationRegistryItem(
                dataElement = "Patient.telecom.use",
                registryUuid = "67890",
                filename = "file2.json",
                conceptMapName = "PatientTelecomUse-tenant",
                conceptMapUuid = "cm-222",
                registryEntryType = RegistryType.CONCEPT_MAP,
                version = "1",
                sourceExtensionUrl = "ext2",
                resourceType = "Patient",
                tenantId = "test",
            )

        client.registry = mapOf(key1 to listOf(registryItem1), key2 to listOf(registryItem2))
        client.registryLastUpdated = LocalDateTime.MIN

        val registry1 =
            ConceptMapItem(
                sourceExtensionUrl = "ext1",
                map =
                    mapOf(
                        SourceConcept(
                            code = Code("MyPhone"),
                        ) to
                            listOf(
                                TargetConcept(
                                    element =
                                        listOf(
                                            TargetConceptMapValue(
                                                "good-or-bad-for-enum",
                                                "good-or-bad-for-enum",
                                                "good-or-bad-for-enum",
                                                "1",
                                            ),
                                        ),
                                    text = "good-or-bad-for-enum, not validated here",
                                ),
                            ),
                    ),
                metadata = listOf(conceptMapMetadata),
            )
        client.conceptMapCache.put(key1, registry1)

        val registry2 =
            ConceptMapItem(
                sourceExtensionUrl = "ext1",
                map =
                    mapOf(
                        SourceConcept(
                            code = Code("MyPhone"),
                        ) to
                            listOf(
                                TargetConcept(
                                    element =
                                        listOf(
                                            TargetConceptMapValue(
                                                "good-or-bad-for-enum",
                                                "good-or-bad-for-enum",
                                                "good-or-bad-for-enum",
                                                "1",
                                            ),
                                        ),
                                    text = "good-or-bad-for-enum, not validated here",
                                ),
                            ),
                    ),
                metadata = listOf(conceptMapMetadata),
            )
        client.conceptMapCache.put(key2, registry2)

        val registryItem3 =
            NormalizationRegistryItem(
                dataElement = "Patient.telecom.use",
                registryUuid = "67890",
                filename = "file2.json",
                conceptMapName = "PatientTelecomUse-tenant",
                conceptMapUuid = "cm-222",
                registryEntryType = RegistryType.CONCEPT_MAP,
                version = "2",
                sourceExtensionUrl = "ext2",
                resourceType = "Patient",
                tenantId = "test",
            )

        val newRegistry = listOf(registryItem1, registryItem3)
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns newRegistry

        client.checkRegistryStatus(LocalDateTime.now())

        assertNotEquals(LocalDateTime.MIN, client.registryLastUpdated)
        assertEquals(mapOf(key1 to listOf(registryItem1), key2 to listOf(registryItem3)), client.registry)
        assertEquals(registry1, client.conceptMapCache.getIfPresent(key1))
        assertNull(client.conceptMapCache.getIfPresent(key2))
    }

    @Test
    fun `registry does not invalidate cache item that has not changed in new registry`() {
        val registryItem1 =
            NormalizationRegistryItem(
                dataElement = "Patient.telecom.system",
                registryUuid = "12345",
                filename = "file1.json",
                conceptMapName = "PatientTelecomSystem-tenant",
                conceptMapUuid = "cm-111",
                registryEntryType = RegistryType.CONCEPT_MAP,
                version = "1",
                sourceExtensionUrl = "ext1",
                resourceType = "Patient",
                tenantId = "test",
            )
        val key1 =
            CacheKey(
                RegistryType.CONCEPT_MAP,
                "Patient.telecom.system",
                "test",
            )

        val key2 =
            CacheKey(
                RegistryType.CONCEPT_MAP,
                "Patient.telecom.use",
                "test",
            )
        val registryItem2 =
            NormalizationRegistryItem(
                dataElement = "Patient.telecom.use",
                registryUuid = "67890",
                filename = "file2.json",
                conceptMapName = "PatientTelecomUse-tenant",
                conceptMapUuid = "cm-222",
                registryEntryType = RegistryType.CONCEPT_MAP,
                version = "1",
                sourceExtensionUrl = "ext2",
                resourceType = "Patient",
                tenantId = "test",
            )

        client.registry = mapOf(key1 to listOf(registryItem1), key2 to listOf(registryItem2))
        client.registryLastUpdated = LocalDateTime.MIN

        val registry1 =
            ConceptMapItem(
                sourceExtensionUrl = "ext1",
                map =
                    mapOf(
                        SourceConcept(
                            code = Code("MyPhone"),
                        ) to
                            listOf(
                                TargetConcept(
                                    element =
                                        listOf(
                                            TargetConceptMapValue(
                                                "good-or-bad-for-enum",
                                                "good-or-bad-for-enum",
                                                "good-or-bad-for-enum",
                                                "1",
                                            ),
                                        ),
                                    text = "good-or-bad-for-enum, not validated here",
                                ),
                            ),
                    ),
                metadata = listOf(conceptMapMetadata),
            )
        client.conceptMapCache.put(key1, registry1)

        val registry2 =
            ConceptMapItem(
                sourceExtensionUrl = "ext1",
                map =
                    mapOf(
                        SourceConcept(
                            code = Code("MyPhone"),
                        ) to
                            listOf(
                                TargetConcept(
                                    element =
                                        listOf(
                                            TargetConceptMapValue(
                                                "good-or-bad-for-enum",
                                                "good-or-bad-for-enum",
                                                "good-or-bad-for-enum",
                                                "1",
                                            ),
                                        ),
                                    text = "good-or-bad-for-enum, not validated here",
                                ),
                            ),
                    ),
                metadata = listOf(conceptMapMetadata),
            )
        client.conceptMapCache.put(key2, registry2)

        val newRegistry = listOf(registryItem1, registryItem2)
        mockkObject(JacksonUtil)
        every { ociClient.getObjectFromINFX(registryPath) } returns "registryJson"
        every {
            JacksonUtil.readJsonList(
                "registryJson",
                NormalizationRegistryItem::class,
            )
        } returns newRegistry

        client.checkRegistryStatus(LocalDateTime.now())

        assertNotEquals(LocalDateTime.MIN, client.registryLastUpdated)
        assertEquals(mapOf(key1 to listOf(registryItem1), key2 to listOf(registryItem2)), client.registry)
        assertEquals(registry1, client.conceptMapCache.getIfPresent(key1))
        assertEquals(registry2, client.conceptMapCache.getIfPresent(key2))
    }

    private val testConceptMapTest =
        """
        {
          "resourceType": "ConceptMap",
          "id": "TestObservationsMashup-id",
          "name": "TestObservationsMashup-name",
          "url": "http://projectronin.io/fhir/ConceptMap/03659ed9-c591-4bbc-9bcf-37260e0e402f",
          "description": "Observations Values to Ronin Observation Values",
          "purpose": null,
          "experimental": false,
          "date": "2023-10-09T18:20:47.059Z",
          "version": 7,
          "group": [
            {
              "source": "http://projectronin.io/fhir/CodeSystem/test/TestObservationsMashup",
              "sourceVersion": "1.0",
              "target": "http://snomed.info/sct",
              "targetVersion": "0.0.1",
              "element": [
                {
                  "id": "fb9b8c03a2a75da276874f3dea768fd3",
                  "_code": {
                    "extension": [ {
                      "id": "sourceExtension1",
                      "url": "http://projectronin.io/fhir/StructureDefinition/Extension/canonicalSourceData",
                      "valueCodeableConcept": {
                        "text": "Yellow"
                      }
                    } ]
                  },
                  "display": "Yellow",
                  "target": [
                    {
                      "id": "50ef6185b78f2f5f0dad3f34017e102a",
                      "code": "371244009",
                      "display": "Yellow color (qualifier value)",
                      "equivalence": "equivalent"
                    }
                  ]
                }
              ]
            }
          ],
          "extension": [
            {
              "url": "http://projectronin.io/fhir/StructureDefinition/Extension/ronin-conceptMapSchema",
              "valueString": "3.0.0"
            }
          ],
          "meta": {
            "profile": [
              "http://projectronin.io/fhir/StructureDefinition/ronin-conceptMap"
            ]
          }
        }
        """.trimIndent()

    fun createClinicalSourceDataExtension(
        codeableConcept: CodeableConcept,
        id: String,
    ) = Extension(
        id = id.asFHIR(),
        url = RoninExtension.CANONICAL_SOURCE_DATA_EXTENSION.uri,
        value = DynamicValue(DynamicValueType.CODEABLE_CONCEPT, codeableConcept),
    )
}
