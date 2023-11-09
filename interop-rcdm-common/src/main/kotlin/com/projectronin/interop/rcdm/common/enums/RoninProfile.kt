package com.projectronin.interop.rcdm.common.enums

import com.projectronin.interop.fhir.r4.datatype.primitive.Canonical

/**
 * Ronin meta.profile values
 */
enum class RoninProfile(val value: String) {
    APPOINTMENT("http://projectronin.io/fhir/StructureDefinition/ronin-appointment"),
    CARE_PLAN("http://projectronin.io/fhir/StructureDefinition/ronin-carePlan"),
    CARE_TEAM("http://projectronin.io/fhir/StructureDefinition/ronin-careTeam"),
    CONCEPT_MAP("http://projectronin.io/fhir/StructureDefinition/ConceptMap"),
    CONDITION_PROBLEMS_CONCERNS("http://projectronin.io/fhir/StructureDefinition/ronin-conditionProblemsHealthConcerns"),
    CONDITION_ENCOUNTER_DIAGNOSIS("http://projectronin.io/fhir/StructureDefinition/ronin-conditionEncounterDiagnosis"),
    DIAGNOSTIC_REPORT_LABORATORY("http://projectronin.io/fhir/StructureDefinition/ronin-diagnosticReportLaboratoryReporting"),
    DIAGNOSTIC_REPORT_NOTE_EXCHANGE("http://projectronin.io/fhir/StructureDefinition/ronin-diagnosticReportNoteExchange"),
    DOCUMENT_REFERENCE("http://projectronin.io/fhir/StructureDefinition/ronin-documentReference"),
    ENCOUNTER("http://projectronin.io/fhir/StructureDefinition/ronin-encounter"),
    LOCATION("http://projectronin.io/fhir/StructureDefinition/ronin-location"),
    MEDICATION("http://projectronin.io/fhir/StructureDefinition/ronin-medication"),
    MEDICATION_ADMINISTRATION("http://projectronin.io/fhir/StructureDefinition/ronin-medicationAdministration"),
    MEDICATION_REQUEST("http://projectronin.io/fhir/StructureDefinition/ronin-medicationRequest"),
    MEDICATION_STATEMENT("http://projectronin.io/fhir/StructureDefinition/ronin-medicationStatement"),
    OBSERVATION_BLOOD_PRESSURE("http://projectronin.io/fhir/StructureDefinition/ronin-observationBloodPressure"),
    OBSERVATION_BODY_HEIGHT("http://projectronin.io/fhir/StructureDefinition/ronin-observationBodyHeight"),
    OBSERVATION_BODY_MASS_INDEX("http://projectronin.io/fhir/StructureDefinition/ronin-observationBodyMassIndex"),
    OBSERVATION_BODY_SURFACE_AREA("http://projectronin.io/fhir/StructureDefinition/ronin-observationBodySurfaceArea"),
    OBSERVATION_BODY_TEMPERATURE("http://projectronin.io/fhir/StructureDefinition/ronin-observationBodyTemperature"),
    OBSERVATION_BODY_WEIGHT("http://projectronin.io/fhir/StructureDefinition/ronin-observationBodyWeight"),
    OBSERVATION_HEART_RATE("http://projectronin.io/fhir/StructureDefinition/ronin-observationHeartRate"),
    OBSERVATION_LABORATORY_RESULT("http://projectronin.io/fhir/StructureDefinition/ronin-observationLaboratoryResult"),
    OBSERVATION_PULSE_OXIMETRY("http://projectronin.io/fhir/StructureDefinition/ronin-observationPulseOximetry"),
    OBSERVATION_RESPIRATORY_RATE("http://projectronin.io/fhir/StructureDefinition/ronin-observationRespiratoryRate"),
    OBSERVATION_STAGING_RELATED("http://projectronin.io/fhir/StructureDefinition/ronin-observationStagingRelated"),
    PROCEDURE("http://projectronin.io/fhir/StructureDefinition/ronin-procedure"),
    REQUEST_GROUP("http://projectronin.io/fhir/StructureDefinition/ronin-requestGroup"),
    SERVICE_REQUEST("http://projectronin.io/fhir/StructureDefinition/ronin-serviceRequest"),

    @Deprecated("ronin-vitalSigns is a retired profile", ReplaceWith(""), DeprecationLevel.WARNING)
    OBSERVATION_VITAL_SIGNS("http://projectronin.io/fhir/StructureDefinition/ronin-vitalSigns"),
    ORGANIZATION("http://projectronin.io/fhir/StructureDefinition/ronin-organization"),
    PATIENT("http://projectronin.io/fhir/StructureDefinition/ronin-patient"),
    PRACTITIONER("http://projectronin.io/fhir/StructureDefinition/ronin-practitioner"),
    PRACTITIONER_ROLE("http://projectronin.io/fhir/StructureDefinition/ronin-practitionerRole"),
    OBSERVATION("http://projectronin.io/fhir/StructureDefinition/ronin-observation");

    companion object {
        fun forUrl(url: String): RoninProfile? = RoninProfile.values().singleOrNull { it.value == url }
    }

    val canonical = Canonical(value)
}
