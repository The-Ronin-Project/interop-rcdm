package com.projectronin.interop.rcdm.transform.util

import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding

/**
 * Check whether the system and code in a Coding match a getValueSet() result from the NormalizationRegistryClient
 */
fun Coding?.isInValueSet(qualifyList: List<Coding>): Boolean {
    this?.let { coding ->
        return (qualifyList.isEmpty() || qualifyList.any { (it.system == coding.system) && (it.code == coding.code) })
    }
    return false
}

/**
 * Check whether any CodeableConcept within a list of CodeableConcept
 * contains a Coding that matches a Coding from the input [qualifyList] of Coding
 */
fun List<CodeableConcept>?.qualifiesForValueSet(qualifyList: List<Coding>): Boolean {
    return qualifyList.isEmpty() || (this?.let { it.any { con -> con.qualifiesForValueSet(qualifyList) } } == true)
}

/**
 * Check whether a CodeableConcept contains a Coding that matches a Coding from the input [qualifyList] of Coding
 */
fun CodeableConcept?.qualifiesForValueSet(qualifyList: List<Coding>): Boolean {
    return qualifyList.isEmpty() || (this?.let { it.coding.any { it.isInValueSet(qualifyList) } } == true)
}
