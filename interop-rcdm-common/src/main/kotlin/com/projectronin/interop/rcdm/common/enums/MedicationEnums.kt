package com.projectronin.interop.rcdm.common.enums

import com.projectronin.interop.fhir.r4.datatype.primitive.Code

enum class OriginalMedDataType(val value: Code) {
    LiteralReference(Code("literal reference")),
    LogicalReference(Code("logical reference")),
    ContainedReference(Code("contained reference")),
    CodeableConcept(Code("codeable concept")),
    ;

    companion object { // helper to check value of extension populated from medication[x]
        infix fun from(value: Any?): OriginalMedDataType? = value?.let { OriginalMedDataType.values().firstOrNull { it.value == value } }
    }
}

enum class OriginalDynamicType(val value: String) {
    CodeableConcept("CODEABLE_CONCEPT"),
}
