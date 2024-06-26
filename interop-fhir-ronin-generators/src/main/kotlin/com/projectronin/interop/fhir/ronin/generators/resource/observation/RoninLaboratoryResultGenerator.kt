package com.projectronin.interop.fhir.ronin.generators.resource.observation

import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.datatypes.coding
import com.projectronin.interop.fhir.generators.primitives.of
import com.projectronin.interop.fhir.generators.resources.ObservationGenerator
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Quantity
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Decimal
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.Observation
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.generators.resource.referenceData
import com.projectronin.interop.fhir.ronin.generators.util.generateCodeableConcept
import com.projectronin.interop.fhir.ronin.generators.util.generateReference
import com.projectronin.interop.fhir.ronin.generators.util.generateWithDefault
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.rcdm.common.enums.RoninProfile
import com.projectronin.interop.rcdm.common.metadata.ValueSetMetadata
import com.projectronin.interop.rcdm.registry.model.ValueSetList
import java.math.BigDecimal

/**
 * Helps generate ronin laboratory result  observation profile, applies meta and randomly generates an
 * acceptable code from the [possibleLaboratoryResultCodes] list, category is generated by laboratoryCategory below
 */
fun rcdmObservationLaboratoryResult(
    tenant: String,
    block: ObservationGenerator.() -> Unit,
): Observation {
    return rcdmBaseObservation(tenant) {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.OBSERVATION_LABORATORY_RESULT, tenant) {}
        category of
            listOf(
                codeableConcept {
                    coding of laboratoryCategory
                },
            )
        code of generateCodeableConcept(code.generate(), possibleLaboratoryResultCodes.codes.random())
        subject of generateReference(subject.generate(), subjectReferenceOptions, tenant, "Patient")
        value of generateWithDefault(value, valueQuantity)
    }
}

fun Patient.rcdmObservationLaboratoryResult(block: ObservationGenerator.() -> Unit): Observation {
    val data = this.referenceData()
    return rcdmObservationLaboratoryResult(data.tenantId) {
        block.invoke(this)
        subject of
            generateReference(
                subject.generate(),
                subjectReferenceOptions,
                data.tenantId,
                "Patient",
                data.udpId,
            )
    }
}

private val laboratoryCategory =
    listOf(
        coding {
            system of CodeSystem.OBSERVATION_CATEGORY.uri
            code of Code("laboratory")
        },
    )

private val valueQuantity =
    DynamicValue(
        DynamicValueType.QUANTITY,
        Quantity(
            value = Decimal(BigDecimal.valueOf(68.04)),
            unit = "kg".asFHIR(),
            system = CodeSystem.UCUM.uri,
            code = Code("kg"),
        ),
    )

val possibleLaboratoryResultCodesList =
    listOf(
        Pair(
            "97966-6",
            "European house dust mite recombinant (rDer p) 2 IgG4 Ab [Mass/volume] in Serum",
        ),
        Pair(
            "8038-2",
            "Toxocara canis Ab [Units/volume] in Serum",
        ),
        Pair(
            "51465-3",
            "Paragonimus sp Ab [Titer] in Pleural fluid",
        ),
        Pair(
            "12388-5",
            "Phenylethylmalonamide [Mass/volume] in Serum or Plasma",
        ),
        Pair(
            "29602-0",
            "Chlorphentermine [Mass/volume] in Serum or Plasma by Confirmatory method",
        ),
        Pair(
            "94590-7",
            "Oncologic chromosome analysis in Tissue by Mate pair sequencing",
        ),
        Pair(
            "100341-7",
            "Rubella virus IgG Ab index [Units/volume] in Serum and CSF",
        ),
        Pair(
            "59798-9",
            "Ganciclovir [Mass/volume] in Plasma --peak",
        ),
        Pair(
            "2227-7",
            "Enteropeptidase [Enzymatic activity/volume] in Plasma",
        ),
        Pair(
            "73366-7",
            "Gabapentin induced platelet IgM Ab [Presence] in Serum or Plasma by Flow cytometry (FC)",
        ),
        Pair(
            "10484-4",
            "Glial fibrillary acidic protein Ag [Presence] in Tissue by Immune stain",
        ),
        Pair(
            "7598-6",
            "Perch IgG Ab [Units/volume] in Serum",
        ),
        Pair(
            "56891-5",
            "CD13+HLA-DR+ cells/100 cells in Specimen",
        ),
        Pair(
            "73240-4",
            "Nitroglycerin induced platelet IgM Ab [Presence] in Serum or Plasma by Flow cytometry (FC)",
        ),
        Pair(
            "26457-2",
            "Erythrocytes [#/volume] in Peritoneal fluid",
        ),
        Pair(
            "38295-2",
            "Methylene chloride [Mass/volume] in Water",
        ),
        Pair(
            "14375-0",
            "Polymorphonuclear cells/100 leukocytes in Peritoneal fluid by Manual count",
        ),
        Pair(
            "51431-5",
            "Gastrin [Mass/volume] in Serum or Plasma --pre or post XXX challenge",
        ),
        Pair(
            "53622-7",
            "von Willebrand factor (vWf) cleaving protease actual/normal in Platelet poor plasma by Chromogenic method",
        ),
        Pair(
            "93313-5",
            "Platelet glycoprotein disorder [Interpretation] in Blood by Flow cytometry (FC) Narrative",
        ),
        Pair(
            "20504-7",
            "Histiocytes/100 cells in Cerebral spinal fluid",
        ),
        Pair(
            "18942-3",
            "Meclocycline [Susceptibility]",
        ),
        Pair(
            "4411-5",
            "Promazine [Mass] of Dose",
        ),
        Pair(
            "58897-0",
            "Methyl ethyl ketone/Creatinine [Mass Ratio] in Urine",
        ),
        Pair(
            "97010-3",
            "Clot formation kaolin+tissue factor induced [Time] in Blood",
        ),
        Pair(
            "30564-9",
            "Tetradecadienoylcarnitine (C14:2) [Moles/volume] in Serum or Plasma",
        ),
        Pair(
            "61166-5",
            "Hepatitis B virus codon S202G [Presence] by Genotype method",
        ),
        Pair(
            "14459-2",
            "Virus identified in Penis by Culture",
        ),
        Pair(
            "48112-7",
            "Glutathione.reduced [Units/mass] in Red Blood Cells",
        ),
        Pair(
            "12528-6",
            "Norepinephrine [Mass/volume] in Plasma --4 hours post XXX challenge",
        ),
        Pair(
            "51367-1",
            "CD79a cells/100 cells in Bone marrow",
        ),
        Pair(
            "2279-8",
            "Fibrinopeptide A [Mass/volume] in Peritoneal fluid",
        ),
        Pair(
            "34446-5",
            "Erythrocytes [Presence] in Body fluid",
        ),
        Pair(
            "3466-0",
            "Chlorpheniramine [Mass/volume] in Serum or Plasma",
        ),
        Pair(
            "46125-1",
            "Cardiolipin Ab [Presence] in Serum by Immunoassay",
        ),
        Pair(
            "6985-6",
            "Beta lactamase.usual [Susceptibility]",
        ),
        Pair(
            "47350-4",
            "Streptococcus pneumoniae Danish serotype 33F IgG Ab [Ratio] in Serum --2nd specimen/1st specimen",
        ),
        Pair(
            "50363-1",
            "Common Pigweed IgE Ab/IgE total in Serum",
        ),
        Pair(
            "44803-5",
            "GNE gene mutations found [Identifier] in Blood or Tissue by Molecular genetics method Nominal",
        ),
        Pair(
            "80114-2",
            "2-Oxo-3-Hydroxy-Lysergate diethylamide [Mass/volume] in Blood by Confirmatory method",
        ),
        Pair(
            "51953-8",
            "Collection date of Blood",
        ),
        Pair(
            "12973-4",
            "Urea nitrogen [Mass/volume] in Peritoneal dialysis fluid --4th specimen",
        ),
        Pair(
            "30021-0",
            "Borrelia burgdorferi 23kD IgM Ab [Presence] in Synovial fluid by Immunoblot",
        ),
        Pair(
            "12462-8",
            "Corticotropin [Mass/volume] in Plasma --6th specimen post XXX challenge",
        ),
        Pair(
            "22081-4",
            "Afipia felis IgG Ab [Titer] in Serum",
        ),
        Pair(
            "50679-0",
            "First trimester maternal screen with nuchal translucency [Interpretation]",
        ),
        Pair(
            "20612-8",
            "CD7 cells/100 cells in Specimen",
        ),
        Pair(
            "47651-5",
            "Hydroxyproline [Moles/volume] in DBS",
        ),
        Pair(
            "75004-2",
            "Food Allergen Mix fp73 (Beef+Chicken+Pork+Lamb) IgE Ab [Units/volume] in Serum by Multidisk",
        ),
        Pair("40665-2", "Reticulocytes [#/volume] in Blood by Manual count"),
    ).map {
        coding {
            system of loinc
            version of "2.73"
            code of it.first
            display of it.second
        }
    }
val possibleLaboratoryResultCodes =
    ValueSetList(
        possibleLaboratoryResultCodesList,
        ValueSetMetadata(
            registryEntryType = "value_set",
            valueSetName = "RoninLaboratoryObservationResult",
            valueSetUuid = "29aded61-f243-41fa-bee9-c93129d66762",
            version = "2",
        ),
    )
