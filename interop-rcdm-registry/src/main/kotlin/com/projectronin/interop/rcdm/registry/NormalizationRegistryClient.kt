package com.projectronin.interop.rcdm.registry

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.projectronin.interop.common.enums.CodedEnum
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.common.jackson.JacksonUtil
import com.projectronin.interop.datalake.oci.client.OCIClient
import com.projectronin.interop.fhir.r4.datatype.CodeableConcept
import com.projectronin.interop.fhir.r4.datatype.Coding
import com.projectronin.interop.fhir.r4.datatype.DynamicValue
import com.projectronin.interop.fhir.r4.datatype.DynamicValueType
import com.projectronin.interop.fhir.r4.datatype.Extension
import com.projectronin.interop.fhir.r4.datatype.primitive.Code
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ConceptMap
import com.projectronin.interop.fhir.r4.resource.ConceptMapDependsOn
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.resource.ValueSet
import com.projectronin.interop.rcdm.common.metadata.ConceptMapMetadata
import com.projectronin.interop.rcdm.common.metadata.ValueSetMetadata
import com.projectronin.interop.rcdm.registry.dependson.DependsOnEvaluator
import com.projectronin.interop.rcdm.registry.exception.MissingNormalizationContentException
import com.projectronin.interop.rcdm.registry.model.ConceptMapCodeableConcept
import com.projectronin.interop.rcdm.registry.model.ConceptMapCoding
import com.projectronin.interop.rcdm.registry.model.RoninConceptMap
import com.projectronin.interop.rcdm.registry.model.ValueSetList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import kotlin.reflect.KClass

@Component
class NormalizationRegistryClient(
    private val ociClient: OCIClient,
    dependsOnEvaluators: List<DependsOnEvaluator<*>>,
    @Value("\${oci.infx.registry.file}")
    private val registryFileName: String,
    @Value("\${oci.infx.registry.refresh.hours:12}")
    private val defaultReloadHours: String = "12" // use string to prevent issues
) {
    private val logger = KotlinLogging.logger { }

    private val dependsOnEvaluatorByType = dependsOnEvaluators.associateBy { it.resourceType }

    internal var conceptMapCache = Caffeine.newBuilder().build<CacheKey, ConceptMapItem>()
    internal var valueSetCache = Caffeine.newBuilder().build<CacheKey, ValueSetItem>()

    internal var registry = mapOf<CacheKey, List<NormalizationRegistryItem>>()
    internal var registryLastUpdated = LocalDateTime.MIN

    private var registryMutex = Mutex()

    /**
     * Get a CodeableConcept mapping from the DataNormalizationRegistry.
     * The input CodeableConcept.coding value and system values must not be null.
     * Return a [Triple] with the transformed [CodeableConcept] as the first, a CodeableConcept [Extension] as the
     * second, and the [ConceptMapMetadata] as the third
     * or null if no such mapping could be found. The [Extension] represents the original value before mapping.
     */
    fun <T : Resource<T>> getConceptMapping(
        tenantMnemonic: String,
        elementName: String,
        codeableConcept: CodeableConcept,
        resource: T,
        forceCacheReloadTS: LocalDateTime? = null
    ): ConceptMapCodeableConcept? {
        val cacheKey = CacheKey(
            registryType = RegistryType.CONCEPT_MAP,
            elementName = elementName,
            tenantId = tenantMnemonic
        )
        val registryItem = getConceptMapItem(cacheKey, forceCacheReloadTS)
        return registryItem?.let { codeableConcept.getConceptMapping(registryItem, resource) }
    }

    /**
     * Get a Coding mapping from the DataNormalizationRegistry.
     * The input Coding value and system must not be null.
     * Return a [Triple] with the transformed [Coding] as the first, a Coding [Extension] as the second, and
     * the [ConceptMapMetadata] as the third
     * or null to match any element of the type [elementName].
     */
    fun <T : Resource<T>> getConceptMapping(
        tenantMnemonic: String,
        elementName: String,
        coding: Coding,
        resource: T,
        forceCacheReloadTS: LocalDateTime? = null
    ): ConceptMapCoding? {
        val cacheKey = CacheKey(
            registryType = RegistryType.CONCEPT_MAP,
            elementName = elementName,
            tenantId = tenantMnemonic
        )
        val registryItem = getConceptMapItem(cacheKey, forceCacheReloadTS)
        return registryItem?.let { coding.getConceptMapping(registryItem, resource) }
    }

    /**
     * Get a concept map from the DataNormalizationRegistry whose result matches an enum class.
     * Returns a [Triple] with the transformed [Coding] as the first, an [Extension] as the second,
     * the [ConceptMapMetadata] as the third, or null if no such mapping could be found.
     * The [Extension] represents the original value before mapping.
     * The enum class enumerates the values the caller can expect as return values from this concept map.
     * If there is no concept map found, but the input Coding.code value is correct for the enumClass,
     * return the input [Coding] with a source [Extension] using the enumExtensionUrl provided by the caller.
     */
    fun <T, R : Resource<R>> getConceptMappingForEnum(
        tenantMnemonic: String,
        elementName: String,
        coding: Coding,
        enumClass: KClass<T>,
        enumExtensionUrl: String,
        resource: R,
        forceCacheReloadTS: LocalDateTime? = null
    ): ConceptMapCoding? where T : Enum<T>, T : CodedEnum<T> {
        val cacheKey = CacheKey(
            registryType = RegistryType.CONCEPT_MAP,
            elementName = elementName,
            tenantId = tenantMnemonic
        )
        val registryItem = getConceptMapItem(cacheKey, forceCacheReloadTS)
        val codedEnum = enumClass.java.enumConstants.find { it.code == coding.code?.value }
        return codedEnum?.let {
            ConceptMapCoding(coding, createCodingExtension(enumExtensionUrl, coding), registryItem?.metadata)
        } ?: registryItem?.let { coding.getConceptMapping(registryItem, resource) }
    }

    /**
     * Get a value set from the DataNormalizationRegistry.
     * Returns a [Pair] of a list of [Coding] and [ValueSetMetadata] or null if no such value set could be found.
     * @param elementName the name of the element being mapped, i.e.
     *        "Appointment.status" or "Patient.telecom.use".
     * @param profileUrl URL of an RCDM or FHIR profile
     */
    fun getValueSet(
        elementName: String,
        profileUrl: String,
        forceCacheReloadTS: LocalDateTime? = null
    ): ValueSetList {
        val cacheKey = CacheKey(
            registryType = RegistryType.VALUE_SET,
            elementName = elementName,
            profileUrl = profileUrl
        )
        return getValueSetRegistryItemValues(
            getValueSetItem(cacheKey, forceCacheReloadTS)
        )
    }

    /**
     * Get a value set from the DataNormalizationRegistry, when that value set
     * is required (not preferred or example) for that element, per the profile.
     * Returns a [Pair] of a list of [Coding] and [ValueSetMetadata] or null if no such value set could be found.
     * @param elementName the name of the element being mapped, i.e.
     *        "Appointment.status" or "Patient.telecom.use".
     * @param profileUrl URL of an RCDM or FHIR profile
     * @throws MissingNormalizationContentException if value set is not found.
     */
    fun getRequiredValueSet(
        elementName: String,
        profileUrl: String,
        forceCacheReloadTS: LocalDateTime? = null
    ): ValueSetList {
        return getValueSet(elementName, profileUrl, forceCacheReloadTS).takeIf { it.codes.isNotEmpty() }
            ?: throw MissingNormalizationContentException("Required value set for $profileUrl and $elementName not found")
    }

    private fun <T : Resource<T>> getTarget(
        conceptMapItem: ConceptMapItem,
        sourceConcept: SourceConcept,
        resource: T
    ): TargetConcept? {
        val potentialTargets = conceptMapItem.map[sourceConcept] ?: return null

        @Suppress("UNCHECKED_CAST")
        val dependsOnEvaluator = dependsOnEvaluatorByType[resource::class] as? DependsOnEvaluator<T>
        val matchedTargets = potentialTargets.filter {
            val dependsOn = it.element.first().dependsOn
            if (dependsOn.isEmpty()) {
                // If there are no dependsOn, then it always matches
                true
            } else {
                // Check if the dependsOn meets the evaluator requirements.
                // If there is not an evaluator for dependsOn, but dependsOn is provided, we do not want to consider this matched
                dependsOnEvaluator?.meetsDependsOn(resource, dependsOn) ?: false
            }
        }

        return when (matchedTargets.size) {
            1 -> matchedTargets.single()
            0 -> null
            else -> throw IllegalStateException("Multiple qualified TargetConcepts found for $sourceConcept")
        }
    }

    /**
     * Find a CodeableConcept mapping in the DataNormalizationRegistry that
     * matches the Coding entries in the input CodeableConcept. The match is on
     * system and value; other Coding attributes are ignored. Coding lists that
     * match must contain the same system and value pairs, but in any order.
     *
     * When a match is found, the returned CodeableConcept will contain Coding
     * entry(ies) with these 4 attributes only: system, code, display, version.
     * These attributes come from the group.source, target.code, target.display,
     * and group.targetVersion values from the DataNormalizationRegistry.
     * also see readGroupElementCode()
     */
    private fun <T : Resource<T>> CodeableConcept.getConceptMapping(
        conceptMapItem: ConceptMapItem,
        resource: T
    ): ConceptMapCodeableConcept? {
        val sourceConcept = getSourceConcept() ?: return null
        val target = getTarget(conceptMapItem, sourceConcept, resource) ?: return null

        // if elementName is a CodeableConcept datatype: expect 1+ target.element
        return ConceptMapCodeableConcept(
            this.copy(
                text = target.text?.asFHIR(),
                coding = target.element.map {
                    Coding(
                        system = Uri(it.system),
                        code = Code(it.value),
                        display = it.display.asFHIR(),
                        version = it.version.asFHIR()
                    )
                }
            ),
            createCodeableConceptExtension(conceptMapItem, this),
            conceptMapItem.metadata
        )
    }

    /**
     * if only the text of the codeable concept is available/there is no coding create the source-key from text.
     * otherwise use coding to create the source-key using code and system
     */
    private fun CodeableConcept.getSourceConcept(): SourceConcept? {
        val sourceKeys = coding.mapNotNull { it.getSourceKey() ?: return null }.toSet()
        // add text to SourceConcept
        return SourceConcept(sourceKeys, text?.value)
    }

    /**
     * Find a Coding mapping in the DataNormalizationRegistry that matches the
     * input Coding system and value. Matching ignores other attributes.
     *
     * When a match is found, the returned Coding will contain these attributes
     * only: system, code, display, version.These attributes come from the
     * group.source, target.code, target.display, and group.targetVersion
     * values from the DataNormalizationRegistry.
     */
    private fun <T : Resource<T>> Coding.getConceptMapping(
        conceptMapItem: ConceptMapItem,
        resource: T
    ): ConceptMapCoding? {
        val sourceConcept = getSourceConcept() ?: return null
        val target = getTarget(conceptMapItem, sourceConcept, resource) ?: return null

        // if elementName is a Code or Coding datatype: expect 1 target.element
        return target.element.first().let {
            ConceptMapCoding(
                Coding(
                    system = Uri(it.system),
                    code = Code(it.value),
                    display = it.display.asFHIR(),
                    version = it.version.asFHIR()
                ),
                createCodingExtension(conceptMapItem, this),
                conceptMapItem.metadata
            )
        }
    }

    private fun Coding.getSourceConcept(): SourceConcept? = getSourceKey()?.let { SourceConcept(setOf(it)) }

    private fun Coding.getSourceKey(): SourceKey? {
        val sourceVal = this.code?.value ?: return null
        val sourceSystem = this.system?.value ?: return null
        val sourceDisplay = this.display?.value
        val sourceVersion = this.version?.value
        val agnosticSourceSystem = getTenantAgnosticCodeSystem(sourceSystem)
        return SourceKey(sourceVal, agnosticSourceSystem, sourceDisplay, sourceVersion)
    }

    private fun createCodeableConceptExtension(conceptMapItem: ConceptMapItem, codeableConcept: CodeableConcept) =
        Extension(
            url = Uri(conceptMapItem.sourceExtensionUrl),
            value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = codeableConcept)
        )

    private fun createCodingExtension(conceptMapItem: ConceptMapItem, coding: Coding) = Extension(
        url = Uri(conceptMapItem.sourceExtensionUrl),
        value = DynamicValue(type = DynamicValueType.CODING, value = coding)
    )

    private fun createCodingExtension(extensionUrl: String, coding: Coding) = Extension(
        url = Uri(extensionUrl),
        value = DynamicValue(type = DynamicValueType.CODING, value = coding)
    )

    private fun getConceptMapItem(
        key: CacheKey,
        forceCacheReloadTS: LocalDateTime?
    ): ConceptMapItem? {
        return runBlocking {
            registryMutex.withLock {
                checkRegistryStatus(forceCacheReloadTS)

                conceptMapCache.get(key) {
                    registry[key]?.let { registryItems ->
                        ConceptMapItem(
                            registryItems.map { item ->
                                getConceptMapData(item.filename).entries
                            }.flatten().associate { it.toPair() },
                            registryItems.map { it.sourceExtensionUrl }
                                .distinct().singleOrNull()
                                ?: throw MissingNormalizationContentException(
                                    "Concept map(s) for tenant '${registryItems.first().tenantId}' and ${registryItems.first().dataElement} have missing or inconsistent source extension URLs"
                                ),
                            registryItems.map { item ->
                                ConceptMapMetadata(
                                    registryEntryType = item.registryEntryType.value,
                                    conceptMapName = item.conceptMapName ?: "N/A",
                                    conceptMapUuid = item.conceptMapUuid ?: "N/A",
                                    version = item.version ?: "N/A"
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    private fun getTenantAgnosticCodeSystem(system: String): String =
        if (RoninConceptMap.CODE_SYSTEMS.isMappedUri(system)) {
            RoninConceptMap.CODE_SYSTEMS.toTenantAgnosticUri(system)
        } else {
            system
        }

    /**
     * Read a ConceptMap group.element.code value into a SourceConcept so that
     * we can match it against the Coding entries from a source CodeableConcept.
     * group.element.code may be a simple code value, but for maps of
     * CodeableConcept to CodeableConcept it will contain a JSON-representation of the concept.
     */
    private fun readGroupElementCode(
        groupElementCode: String,
        tenantAgnosticSourceSystem: String
    ): SourceConcept {
        if (!groupElementCode.contains("{")) {
            // No embedded JSON, represent as just a code
            return SourceConcept(setOf(SourceKey(groupElementCode, tenantAgnosticSourceSystem)))
        }

        val sourceCodeableConcept = if (groupElementCode.contains("valueCodeableConcept")) {
            // Handle JSON entries wrapped in the valueCodeableConcept
            JacksonManager.objectMapper.readValue<ConceptMapCode>(groupElementCode).valueCodeableConcept
        } else {
            // Handle JSON structured directly as CodeableConcept objects
            JacksonManager.objectMapper.readValue<CodeableConcept>(groupElementCode)
        }

        return sourceCodeableConcept.getSourceConcept()
            ?: throw IllegalStateException("Could not create SourceConcept from $groupElementCode")
    }

    private data class ConceptMapCode(val valueCodeableConcept: CodeableConcept)

    private fun getValueSetItem(
        key: CacheKey,
        forceCacheReloadTS: LocalDateTime?
    ): ValueSetItem? {
        return runBlocking {
            registryMutex.withLock {
                checkRegistryStatus(forceCacheReloadTS)

                valueSetCache.get(key) {
                    // if not found in cache, calculate and store
                    registry[key]?.let { registryItems ->
                        val item = registryItems.single()
                        val metadata = ValueSetMetadata(
                            registryEntryType = item.registryEntryType.value,
                            valueSetName = item.valueSetName ?: "N/A",
                            valueSetUuid = item.valueSetUuid ?: "N/A",
                            version = item.version ?: "N/A"
                        )
                        ValueSetItem(set = getValueSetData(item.filename), metadata)
                    }
                }
            }
        }
    }

    private fun getValueSetRegistryItemValues(valueSetItem: ValueSetItem?): ValueSetList {
        return ValueSetList(
            valueSetItem?.set?.map {
                Coding(
                    system = Uri(it.system),
                    code = Code(it.value),
                    display = it.display.asFHIR(),
                    version = it.version.asFHIR()
                )
            } ?: emptyList(),
            valueSetItem?.metadata
        )
    }

    /**
     * Checks the registry status and forces a reload, including cache invalidation, as needed.
     */
    internal fun checkRegistryStatus(forceCacheReloadTS: LocalDateTime?) {
        val cacheReloadTime = forceCacheReloadTS ?: LocalDateTime.now().minusHours(defaultReloadHours.toLong())

        if (registryLastUpdated.isBefore(cacheReloadTime)) {
            reloadRegistry()
        }
    }

    /**
     * Reloads the registry. As a consequence of reloading the registry
     */
    private fun reloadRegistry() {
        val newRegistry = getNewRegistry()
        if (newRegistry == null) {
            // Continue using the registry, but reset the time to force another attempt next time.
            registryLastUpdated = LocalDateTime.MIN
            return
        }

        purgeCache(conceptMapCache, registry, newRegistry)
        purgeCache(valueSetCache, registry, newRegistry)

        registry = newRegistry
        registryLastUpdated = LocalDateTime.now()
    }

    private fun getNewRegistry(): Map<CacheKey, List<NormalizationRegistryItem>>? {
        return loadRegistry()?.groupBy { item ->
            CacheKey(
                registryType = item.registryEntryType,
                elementName = item.dataElement,
                tenantId = item.tenantId,
                profileUrl = item.profileUrl
            )
        }
    }

    private fun loadRegistry(): List<NormalizationRegistryItem>? {
        return try {
            registryLastUpdated = LocalDateTime.now()
            JacksonUtil.readJsonList(
                ociClient.getObjectFromINFX(registryFileName)!!,
                NormalizationRegistryItem::class
            )
        } catch (e: Exception) {
            logger.error { "Failed to load normalization registry: ${e.message}" }
            null // keep the 'old' registry in place
        }
    }

    private fun <T> purgeCache(
        cache: Cache<CacheKey, T>,
        oldRegistry: Map<CacheKey, List<NormalizationRegistryItem>>,
        newRegistry: Map<CacheKey, List<NormalizationRegistryItem>>
    ) {
        val keysToInvalidate = cache.asMap().keys.mapNotNull { key ->
            val newItems = newRegistry[key]
            val oldItems = oldRegistry[key]

            if (newItems == null || oldItems != newItems) {
                // If the key is not in the new registry, or its items have changed in any way, we want to keep the key for invalidation
                key
            } else {
                null
            }
        }
        cache.invalidateAll(keysToInvalidate)
    }

    private fun getConceptMapData(filename: String): Map<SourceConcept, List<TargetConcept>> {
        val conceptMap = try {
            JacksonUtil.readJsonObject(
                ociClient.getObjectFromINFX(filename)!!,
                ConceptMap::class
            )
        } catch (e: Exception) {
            logger.info { e.message }
            return emptyMap()
        }
        // squish ConceptMap into more usable form
        val mutableMap = mutableMapOf<SourceConcept, MutableList<TargetConcept>>()
        conceptMap.group.forEach forEachGroup@{ group ->
            val targetSystem = group.target?.value ?: return@forEachGroup
            val sourceSystem = group.source?.value ?: return@forEachGroup
            val targetVersion = group.targetVersion?.value ?: return@forEachGroup
            val agnosticSourceSystem = getTenantAgnosticCodeSystem(sourceSystem)
            group.element?.forEach forEachElement@{ element ->
                val targetText = element.display?.value ?: return@forEachElement
                val sourceCode = element.code?.value ?: return@forEachElement
                val targetList = element.target.mapNotNull { target ->
                    target.code?.value?.let { targetCode ->
                        target.display?.value?.let { targetDisplay ->
                            TargetValue(
                                targetCode,
                                targetSystem,
                                targetDisplay,
                                targetVersion,
                                target.dependsOn
                            )
                        }
                    }
                }
                val targetConcept = TargetConcept(targetList, targetText)
                val sourceConcept = readGroupElementCode(sourceCode, agnosticSourceSystem)

                mutableMap.computeIfAbsent(sourceConcept) { mutableListOf() }.add(targetConcept)
            }
        }
        return mutableMap
    }

    private fun getValueSetData(filename: String): List<TargetValue> {
        val valueSet = try {
            JacksonUtil.readJsonObject(ociClient.getObjectFromINFX(filename)!!, ValueSet::class)
        } catch (e: Exception) {
            logger.info { e.message }
            return emptyList()
        }
        // squish ValueSet into more usable form
        return valueSet.expansion?.contains?.mapNotNull {
            val targetSystem = it.system?.value
            val targetVersion = it.version?.value
            val targetCode = it.code?.value
            val targetDisplay = it.display?.value
            if (targetSystem == null || targetVersion == null || targetCode == null || targetDisplay == null) {
                null
            } else {
                TargetValue(targetCode, targetSystem, targetDisplay, targetVersion)
            }
        } ?: emptyList()
    }
}

internal data class CacheKey(
    val registryType: RegistryType,
    val elementName: String,
    val tenantId: String? = null, // non-null for ConceptMap
    val profileUrl: String? = null // null for ConceptMap
)

internal data class NormalizationRegistryItem(
    val registryUuid: String,
    val dataElement: String, // i.e. 'Appointment.status'
    val filename: String,
    val version: String? = null,
    val sourceExtensionUrl: String? = null, // non-null for ConceptMap
    val resourceType: String, // i.e. 'Appointment' - repeated in data_element
    val tenantId: String? = null, // null applies to all tenants
    val profileUrl: String? = null,
    val conceptMapName: String? = null,
    val conceptMapUuid: String? = null,
    val valueSetName: String? = null,
    val valueSetUuid: String? = null,
    val registryEntryType: RegistryType
)

enum class RegistryType(@JsonValue val value: String) {
    CONCEPT_MAP("concept_map"),
    VALUE_SET("value_set")
}

internal data class ConceptMapItem(
    val map: Map<SourceConcept, List<TargetConcept>>,
    val sourceExtensionUrl: String, // non-null for ConceptMap
    val metadata: List<ConceptMapMetadata>
)

internal data class ValueSetItem(
    val set: List<TargetValue>?,
    val metadata: ValueSetMetadata
)

internal data class SourceKey(
    val value: String,
    val system: String?,
    val display: String? = null,
    val version: String? = null
)

internal data class TargetValue(
    val value: String,
    val system: String,
    val display: String,
    val version: String,
    val dependsOn: List<ConceptMapDependsOn> = emptyList()
)

internal data class SourceConcept(val element: Set<SourceKey>, val text: String? = null)
internal data class TargetConcept(val element: List<TargetValue>, val text: String? = null)
