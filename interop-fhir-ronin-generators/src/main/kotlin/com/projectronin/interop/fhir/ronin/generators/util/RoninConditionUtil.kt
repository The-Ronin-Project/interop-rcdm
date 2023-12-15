package com.projectronin.interop.fhir.ronin.generators.util

import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.metadata.ValueSetMetadata
import com.projectronin.interop.rcdm.registry.model.ValueSetList

val tenantSourceConditionExtension =
    listOf(
        Extension(
            url = Uri(RoninExtension.TENANT_SOURCE_CONDITION_CODE.value),
            value =
                DynamicValue(
                    DynamicValueType.CODEABLE_CONCEPT,
                    CodeableConcept(
                        text = "tenant-source-extension".asFHIR(),
                        coding =
                            listOf(
                                Coding(
                                    code = Code("tenant-source-code-extension"),
                                ),
                            ),
                    ),
                ),
        ),
    )

val conditionCodeExtension =
    Extension(
        url = Uri(RoninExtension.TENANT_SOURCE_CONDITION_CODE.value),
        value =
            DynamicValue(
                DynamicValueType.CODEABLE_CONCEPT,
                value =
                    CodeableConcept(
                        coding =
                            listOf(
                                Coding(
                                    system = CodeSystem.SNOMED_CT.uri,
                                    code = Code("1023001"),
                                    display = "Apnea".asFHIR(),
                                ),
                            ),
                    ),
            ),
    )

val encounterDiagnosisCategory =
    coding {
        system of CodeSystem.CONDITION_CATEGORY.uri
        code of Code("encounter-diagnosis")
    }

val subjectOptions = listOf("Patient")

val problemListCategory =
    coding {
        system of CodeSystem.CONDITION_CATEGORY.uri
        code of Code("problem-list-item")
    }

val healthConcernCategory =
    coding {
        system of CodeSystem.CONDITION_CATEGORY_HEALTH_CONCERN.uri
        code of Code("health-concern")
    }
val versionDate = "2023-03-01"
val icd10 = "http://hl7.org/fhir/sid/icd-10-cm"
val icd10Codes =
    listOf(
        Pair(
            "S85.401A",
            "Unspecified injury of lesser saphenous vein at lower leg level, right leg, initial encounter",
        ),
        Pair(
            "T46.2X2S",
            "Poisoning by other antidysrhythmic drugs, intentional self-harm, sequela",
        ),
        Pair(
            "H65.04",
            "Acute serous otitis media, recurrent, right ear",
        ),
        Pair(
            "S82.846P",
            "Nondisplaced bimalleolar fracture of unspecified lower leg, subsequent encounter for closed fracture with malunion",
        ),
        Pair(
            "S20.322",
            "Blister (nonthermal) of left front wall of thorax",
        ),
        Pair(
            "S95.011S",
            "Laceration of dorsal artery of right foot, sequela",
        ),
        Pair(
            "V90.01XA",
            "Drowning and submersion due to passenger ship overturning, initial encounter",
        ),
        Pair(
            "T63.5",
            "Toxic effect of contact with venomous fish",
        ),
        Pair(
            "S52.333N",
            "Displaced oblique fracture of shaft of unspecified radius, subsequent encounter for open fracture type " +
                "IIIA, IIIB, or IIIC with nonunion",
        ),
        Pair(
            "T50.903",
            "Poisoning by unspecified drugs, medicaments and biological substances, assault",
        ),
        Pair(
            "S04.71XS",
            "Injury of accessory nerve, right side, sequela",
        ),
        Pair(
            "T49.1X1A",
            "Poisoning by antipruritics, accidental (unintentional), initial encounter",
        ),
        Pair(
            "I80.291",
            "Phlebitis and thrombophlebitis of other deep vessels of right lower extremity",
        ),
        Pair(
            "T27.0XXS",
            "Burn of larynx and trachea, sequela",
        ),
        Pair(
            "I70.448",
            "Atherosclerosis of autologous vein bypass graft(s) of the left leg with ulceration of other part of lower leg",
        ),
        Pair(
            "V72.9XXD",
            "Unspecified occupant of bus injured in collision with two- " +
                "or three-wheeled motor vehicle in traffic accident, subsequent encounter",
        ),
        Pair("T22.361S", "Burn of third degree of right scapular region, sequela"),
    ).map {
        coding {
            system of icd10
            version of "2023"
            code of it.first
            display of it.second
        }
    }
val snomedCodes =
    listOf(
        Pair(
            "439365009",
            "Closed manual reduction of fracture of posterior malleolus (procedure)",
        ),
        Pair(
            "62850006",
            "Medical examination under sedation (procedure)",
        ),
        Pair(
            "722677008",
            "Primary mesothelioma of overlapping sites of retroperitoneum, peritoneum and omentum (disorder)",
        ),
        Pair(
            "283444006",
            "Cut of heel (disorder)",
        ),
        Pair(
            "212011007",
            "Corrosion of second degree of ankle and foot (disorder)",
        ),
        Pair(
            "4688008",
            "Poisoning caused by griseofulvin (disorder)",
        ),
        Pair(
            "109818006",
            "Male frozen pelvis (disorder)",
        ),
        Pair(
            "662911000124103",
            "Referral to Affordable Connectivity Program (procedure)",
        ),
        Pair(
            "735764005",
            "Laceration of thorax without foreign body (disorder)",
        ),
        Pair(
            "704100003",
            "Endoscopic extraction of calculus of urinary tract proper (procedure)",
        ),
        Pair(
            "399418000",
            "Scleral invasion by tumor cannot be determined (finding)",
        ),
        Pair(
            "16715971000119108",
            "Acute infarction of small intestine (disorder)",
        ),
        Pair(
            "238432007",
            "Bacillus Calmette-Guerin ulcer (disorder)",
        ),
        Pair(
            "698887005",
            "Main spoken language Aymara (finding)",
        ),
        Pair(
            "1162729000",
            "Modification of nutrition intake schedule to limit fasting (regime/therapy)",
        ),
        Pair(
            "233294005",
            "Removal of foreign body from arterial graft (procedure)",
        ),
        Pair(
            "446722008",
            "Determination of coreceptor tropism of Human immunodeficiency virus 1 (procedure)",
        ),
        Pair(
            "105362001",
            "Urinalysis, automated, without microscopy (procedure)",
        ),
        Pair(
            "298305003",
            "Finding of general balance (finding)",
        ),
        Pair(
            "196333005",
            "Mummified pulp (disorder)",
        ),
        Pair(
            "310538001",
            "Baby birth weight 2 to 2.5 kilogram (finding)",
        ),
        Pair(
            "34663006",
            "Contusion of brain (disorder)",
        ),
        Pair(
            "609468001",
            "Induced termination of pregnancy complicated by laceration of broad ligament (disorder)",
        ),
        Pair(
            "219375003",
            "War injury due to carbine bullet (disorder)",
        ),
        Pair(
            "88740003",
            "Thyrotoxicosis factitia with thyrotoxic crisis (disorder)",
        ),
        Pair(
            "174416009",
            "Closure of bowel fistula (procedure)",
        ),
        Pair(
            "300658002",
            "Able to perform activities involved in using transport (finding)",
        ),
        Pair(
            "31957000",
            "Superficial injury of nose without infection (disorder)",
        ),
        Pair(
            "1003397007",
            "Congenital atresia of intestine at multiple levels (disorder)",
        ),
        Pair(
            "2065009",
            "Dominant hereditary optic atrophy (disorder)",
        ),
        Pair(
            "66424002",
            "Manual reduction of closed fracture of acetabulum and skeletal traction (procedure)",
        ),
        Pair(
            "33490001",
            "Failed attempted abortion with fat embolism (disorder)",
        ),
        Pair("442327001", "Twin liveborn born in hospital (situation)"),
    ).map {
        coding {
            system of CodeSystem.SNOMED_CT.uri
            version of versionDate
            code of it.first
            display of it.second
        }
    }
val possibleConditionCodesList = icd10Codes + snomedCodes
val possibleConditionCodes =
    ValueSetList(
        possibleConditionCodesList,
        ValueSetMetadata(
            registryEntryType = "value_set",
            valueSetName = "RoninConditionCode",
            valueSetUuid = "201ad507-64f7-4429-810f-94bdbd51f80a",
            version = "4",
        ),
    )
