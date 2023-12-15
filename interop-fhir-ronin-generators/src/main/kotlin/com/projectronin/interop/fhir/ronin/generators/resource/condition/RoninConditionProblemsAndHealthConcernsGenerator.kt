package com.projectronin.interop.fhir.ronin.generators.resource.condition

import com.projectronin.interop.fhir.generators.datatypes.codeableConcept
import com.projectronin.interop.fhir.generators.resources.ConditionGenerator
import com.projectronin.interop.fhir.generators.resources.condition
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.resource.Condition
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.fhir.ronin.generators.resource.observation.subjectReferenceOptions
import com.projectronin.interop.fhir.ronin.generators.resource.referenceData
import com.projectronin.interop.fhir.ronin.generators.util.generateCodeableConcept
import com.projectronin.interop.fhir.ronin.generators.util.generateReference
import com.projectronin.interop.fhir.ronin.generators.util.generateUdpId
import com.projectronin.interop.fhir.ronin.generators.util.healthConcernCategory
import com.projectronin.interop.fhir.ronin.generators.util.includeExtensions
import com.projectronin.interop.fhir.ronin.generators.util.possibleConditionCodes
import com.projectronin.interop.fhir.ronin.generators.util.problemListCategory
import com.projectronin.interop.fhir.ronin.generators.util.rcdmIdentifiers
import com.projectronin.interop.fhir.ronin.generators.util.rcdmMeta
import com.projectronin.interop.fhir.ronin.generators.util.subjectOptions
import com.projectronin.interop.fhir.ronin.generators.util.tenantSourceConditionExtension
import com.projectronin.interop.rcdm.common.enums.RoninProfile

fun rcdmConditionProblemsAndHealthConcerns(
    tenant: String,
    block: ConditionGenerator.() -> Unit,
): Condition {
    return condition {
        block.invoke(this)
        meta of rcdmMeta(RoninProfile.CONDITION_PROBLEMS_CONCERNS, tenant) {}
        extension of includeExtensions(extension.generate(), tenantSourceConditionExtension)
        generateUdpId(id.generate(), tenant).let {
            id of it
            identifier of rcdmIdentifiers(tenant, identifier, it.value)
        }
        category of category.generate() + listOf(codeableConcept { coding of listOf(possibleCategories.random()) })
        code.generate()?.let { generateCodeableConcept(it, possibleConditionCodes.codes.random()) }
            ?: (code of CodeableConcept(coding = listOf(possibleConditionCodes.codes.random())))
        subject of generateReference(subject.generate(), subjectOptions, tenant, "Patient")
    }
}

fun Patient.rcdmConditionProblemsAndHealthConcerns(block: ConditionGenerator.() -> Unit): Condition {
    val data = this.referenceData()
    return rcdmConditionProblemsAndHealthConcerns(data.tenantId) {
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

val possibleCategories = listOf(problemListCategory, healthConcernCategory)
