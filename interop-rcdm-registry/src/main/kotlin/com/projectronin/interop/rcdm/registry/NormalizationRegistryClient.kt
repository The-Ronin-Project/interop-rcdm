package com.projectronin.interop.rcdm.registry

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
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
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Uri
import com.projectronin.interop.fhir.r4.datatype.primitive.asFHIR
import com.projectronin.interop.fhir.r4.resource.ConceptMap
import com.projectronin.interop.fhir.r4.resource.ConceptMapDependsOn
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.fhir.r4.resource.ValueSet
import com.projectronin.interop.rcdm.common.enums.RoninExtension
import com.projectronin.interop.rcdm.common.metadata.ConceptMapMetadata
import com.projectronin.interop.rcdm.common.metadata.ValueSetMetadata
import com.projectronin.interop.rcdm.registry.dependson.DependsOnEvaluator
import com.projectronin.interop.rcdm.registry.exception.MissingNormalizationContentException
import com.projectronin.interop.rcdm.registry.model.ConceptMapCodeableConcept
import com.projectronin.interop.rcdm.registry.model.ConceptMapCoding
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
    private val defaultReloadHours: String = "12",
) {
    private val logger = KotlinLogging.logger { }

    private val dependsOnEvaluatorByType = dependsOnEvaluators.associateBy { it.resourceType }

    internal val isV4: Boolean = registryFileName.contains("/v4/")

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
        forceCacheReloadTS: LocalDateTime? = null,
    ): ConceptMapCodeableConcept? {
        val cacheKey =
            CacheKey(
                registryType = RegistryType.CONCEPT_MAP,
                elementName = elementName,
                tenantId = tenantMnemonic,
            )
        val registryItem = getConceptMapItem(cacheKey, forceCacheReloadTS)
        return registryItem?.let { codeableConcept.getConceptMapping(registryItem, resource) }
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
        forceCacheReloadTS: LocalDateTime? = null,
    ): ConceptMapCoding? where T : Enum<T>, T : CodedEnum<T> {
        val cacheKey =
            CacheKey(
                registryType = RegistryType.CONCEPT_MAP,
                elementName = elementName,
                tenantId = tenantMnemonic,
            )
        val registryItem = getConceptMapItem(cacheKey, forceCacheReloadTS)
        val codedEnum = enumClass.java.enumConstants.find { it.code == coding.code?.value }
        logger.debug { "Found CodedEnum $codedEnum for ($elementName, $tenantMnemonic)" }
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
        forceCacheReloadTS: LocalDateTime? = null,
    ): ValueSetList {
        val cacheKey =
            CacheKey(
                registryType = RegistryType.VALUE_SET,
                elementName = elementName,
                profileUrl = profileUrl,
            )
        return getValueSetRegistryItemValues(
            getValueSetItem(cacheKey, forceCacheReloadTS),
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
        forceCacheReloadTS: LocalDateTime? = null,
    ): ValueSetList {
        return getValueSet(elementName, profileUrl, forceCacheReloadTS).takeIf { it.codes.isNotEmpty() }
            ?: throw MissingNormalizationContentException("Required value set for $profileUrl and $elementName not found")
    }

    private fun <T : Resource<T>> getTarget(
        conceptMapItem: ConceptMapItem,
        sourceConcept: SourceConcept,
        resource: T,
    ): TargetConcept? {
        val potentialTargets = conceptMapItem.map[sourceConcept]
        if (potentialTargets == null) {
            logger.info { "Unable to find potential targets for $sourceConcept" }
            return null
        } else {
            logger.debug { "Found ${potentialTargets.size} potential targets for $sourceConcept" }
        }

        @Suppress("UNCHECKED_CAST")
        val dependsOnEvaluator = dependsOnEvaluatorByType[resource::class] as? DependsOnEvaluator<T>
        val matchedTargets =
            potentialTargets.filter {
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
            1 -> {
                val target = matchedTargets.single()
                logger.debug { "Matched target $target for $sourceConcept" }
                target
            }

            0 -> {
                logger.info { "Unable to match target from ${potentialTargets.size} potential targets for $sourceConcept" }
                null
            }

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
        resource: T,
    ): ConceptMapCodeableConcept? {
        val sourceConcept = SourceConcept(codeableConcept = this.normalized())
        val target = getTarget(conceptMapItem, sourceConcept, resource) ?: return null

        // if elementName is a CodeableConcept datatype: expect 1+ target.element
        return ConceptMapCodeableConcept(
            this.copy(
                id = target.element.first().targetId?.let { FHIRString(it) },
                text = target.text?.asFHIR(),
                coding =
                    target.element.map {
                        Coding(
                            system = Uri(it.system),
                            code = Code(it.value),
                            display = it.display.asFHIR(),
                            version = it.version.asFHIR(),
                        )
                    },
            ),
            createCodeableConceptExtension(conceptMapItem, this, target),
            conceptMapItem.metadata,
        )
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
        resource: T,
    ): ConceptMapCoding? {
        val sourceConcept = this.code?.let { SourceConcept(code = it) } ?: return null
        val target = getTarget(conceptMapItem, sourceConcept, resource) ?: return null

        // if elementName is a Code or Coding datatype: expect 1 target.element
        return target.element.first().let {
            ConceptMapCoding(
                Coding(
                    id = it.targetId?.let { t -> FHIRString(t) },
                    system = Uri(it.system),
                    code = Code(it.value),
                    display = it.display.asFHIR(),
                    version = it.version.asFHIR(),
                ),
                createCodingExtension(conceptMapItem, this, target),
                conceptMapItem.metadata,
            )
        }
    }

    private fun createCodeableConceptExtension(
        conceptMapItem: ConceptMapItem,
        codeableConcept: CodeableConcept,
        targetConcept: TargetConcept,
    ) = Extension(
        id = targetConcept.getSourceId(),
        url = Uri(conceptMapItem.sourceExtensionUrl),
        value = DynamicValue(type = DynamicValueType.CODEABLE_CONCEPT, value = codeableConcept),
    )

    private fun createCodingExtension(
        conceptMapItem: ConceptMapItem,
        coding: Coding,
        targetConcept: TargetConcept,
    ) = Extension(
        id = targetConcept.getSourceId(),
        url = Uri(conceptMapItem.sourceExtensionUrl),
        value = DynamicValue(type = DynamicValueType.CODING, value = coding),
    )

    private fun createCodingExtension(
        extensionUrl: String,
        coding: Coding,
    ) = Extension(
        url = Uri(extensionUrl),
        value = DynamicValue(type = DynamicValueType.CODING, value = coding),
    )

    private fun getConceptMapItem(
        key: CacheKey,
        forceCacheReloadTS: LocalDateTime?,
    ): ConceptMapItem? {
        return runBlocking {
            logger.trace { "Requesting registryMutex to retrieve $key" }
            registryMutex.withLock {
                logger.debug { "Acquired registryMutex - Retrieving $key from cache" }
                checkRegistryStatus(forceCacheReloadTS)

                conceptMapCache.get(key) {
                    registry[key]?.let { registryItems ->
                        val item =
                            ConceptMapItem(
                                registryItems.map { item ->
                                    getConceptMapData(item.filename).entries
                                }.flatten().associate { it.toPair() },
                                registryItems.map { it.sourceExtensionUrl }
                                    .distinct().singleOrNull()
                                    ?: throw MissingNormalizationContentException(
                                        "Concept map(s) for tenant '${registryItems.first().tenantId}' and " +
                                            "${registryItems.first().dataElement} have missing or inconsistent source extension URLs",
                                    ),
                                registryItems.map { item ->
                                    ConceptMapMetadata(
                                        registryEntryType = item.registryEntryType.value,
                                        conceptMapName = item.conceptMapName ?: "N/A",
                                        conceptMapUuid = item.conceptMapUuid ?: "N/A",
                                        version = item.version ?: "N/A",
                                    )
                                },
                            )
                        logger.info { "Computed ConceptMapItem $item with key $key" }
                        item
                    }
                }
            }
        }
    }

    private fun readGroupElementCode(groupElementCode: Code): Pair<SourceConcept, String?> {
        if (isV4) {
            val groupElementCodeText = groupElementCode.value!!
            if (!groupElementCodeText.contains("{")) {
                // No embedded JSON, represent as just a code
                return Pair(SourceConcept(code = Code(groupElementCodeText)), null)
            }

            val sourceCodeableConcept =
                if (groupElementCodeText.contains("valueCodeableConcept")) {
                    // Handle JSON entries wrapped in the valueCodeableConcept
                    JacksonManager.objectMapper.readValue<ConceptMapCode>(groupElementCodeText).valueCodeableConcept
                } else {
                    // Handle JSON structured directly as CodeableConcept objects
                    JacksonManager.objectMapper.readValue<CodeableConcept>(groupElementCodeText)
                }

            return Pair(SourceConcept(codeableConcept = sourceCodeableConcept.normalized()), null)
        }

        val sourceDataExtension =
            groupElementCode.extension.singleOrNull { it.url == RoninExtension.CANONICAL_SOURCE_DATA_EXTENSION.uri }
                ?: throw IllegalStateException(
                    "Could not create SourceConcept from $groupElementCode due to missing canonicalSourceData extension",
                )
        val sourceDataExtensionValue =
            sourceDataExtension.value
                ?: throw IllegalStateException(
                    "Could not create SourceConcept from $groupElementCode due to canonicalSourceData extension with no value",
                )

        val sourceConcept =
            when (sourceDataExtensionValue.type) {
                DynamicValueType.CODE -> SourceConcept(code = sourceDataExtensionValue.value as Code)
                DynamicValueType.CODEABLE_CONCEPT -> {
                    val codeableConcept = sourceDataExtensionValue.value as CodeableConcept
                    SourceConcept(codeableConcept = codeableConcept.normalized())
                }

                else -> throw IllegalStateException("Unknown canonicalSourceData extension value type found for $groupElementCode")
            }

        return Pair(sourceConcept, sourceDataExtension.id?.value)
    }

    private data class ConceptMapCode(val valueCodeableConcept: CodeableConcept)

    private fun getValueSetItem(
        key: CacheKey,
        forceCacheReloadTS: LocalDateTime?,
    ): ValueSetItem? {
        return runBlocking {
            logger.trace { "Requesting registryMutex to retrieve $key" }
            registryMutex.withLock {
                logger.debug { "Acquired registryMutex - Retrieving $key" }
                checkRegistryStatus(forceCacheReloadTS)

                valueSetCache.get(key) {
                    // if not found in cache, calculate and store
                    registry[key]?.let { registryItems ->
                        val item = registryItems.single()
                        val metadata =
                            ValueSetMetadata(
                                registryEntryType = item.registryEntryType.value,
                                valueSetName = item.valueSetName ?: "N/A",
                                valueSetUuid = item.valueSetUuid ?: "N/A",
                                version = item.version ?: "N/A",
                            )
                        val valueSetItem = ValueSetItem(set = getValueSetData(item.filename), metadata)
                        logger.info { "Computed ValueSetItem $valueSetItem with key $key" }
                        valueSetItem
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
                    version = it.version.asFHIR(),
                )
            } ?: emptyList(),
            valueSetItem?.metadata,
        )
    }

    /**
     * Checks the registry status and forces a reload, including cache invalidation, as needed.
     */
    internal fun checkRegistryStatus(forceCacheReloadTS: LocalDateTime?) {
        val cacheReloadTime = forceCacheReloadTS ?: LocalDateTime.now().minusHours(defaultReloadHours.toLong())

        if (registryLastUpdated.isBefore(cacheReloadTime)) {
            logger.info { "Reloading registry as it is now past cacheReloadTime of $cacheReloadTime" }
            reloadRegistry()
        }
    }

    /**
     * Reloads the registry. As a consequence of reloading the registry
     */
    private fun reloadRegistry() {
        logger.debug { "Reloading registry" }
        val newRegistry = getNewRegistry()
        if (newRegistry == null) {
            // Continue using the registry, but reset the time to force another attempt next time.
            logger.warn { "Failed to get new registry - continuing to use old registry" }
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
                profileUrl = item.profileUrl,
            )
        }
    }

    private fun loadRegistry(): List<NormalizationRegistryItem>? {
        return try {
            registryLastUpdated = LocalDateTime.now()
            JacksonUtil.readJsonList(
                ociClient.getObjectFromINFX(registryFileName)!!,
                NormalizationRegistryItem::class,
            )
        } catch (e: Exception) {
            logger.error { "Failed to load normalization registry: ${e.message}" }
            null // keep the 'old' registry in place
        }
    }

    private fun <T> purgeCache(
        cache: Cache<CacheKey, T>,
        oldRegistry: Map<CacheKey, List<NormalizationRegistryItem>>,
        newRegistry: Map<CacheKey, List<NormalizationRegistryItem>>,
    ) {
        val keysToInvalidate =
            cache.asMap().keys.mapNotNull { key ->
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

    internal fun getConceptMapData(filename: String): Map<SourceConcept, List<TargetConcept>> {
        val conceptMap =
            try {
                JacksonUtil.readJsonObject(
                    ociClient.getObjectFromINFX(filename)!!,
                    ConceptMap::class,
                )
            } catch (e: Exception) {
                logger.warn { "Error parsing ConceptMap from $filename ${e.message}" }
                return emptyMap()
            }
        // squish ConceptMap into more usable form
        val mutableMap = mutableMapOf<SourceConcept, MutableList<TargetConcept>>()
        conceptMap.group.forEach forEachGroup@{ group ->
            val targetSystem = group.target?.value ?: return@forEachGroup
            val targetVersion = group.targetVersion?.value ?: return@forEachGroup

            group.element?.forEach forEachElement@{ element ->
                val sourceCode = element.code ?: return@forEachElement
                val (sourceConcept, sourceId) = readGroupElementCode(sourceCode)

                val targetText = element.display?.value ?: return@forEachElement
                val targetList =
                    element.target.mapNotNull { target ->
                        target.code?.value?.let { targetCode ->
                            target.display?.value?.let { targetDisplay ->
                                TargetConceptMapValue(
                                    targetCode,
                                    targetSystem,
                                    targetDisplay,
                                    targetVersion,
                                    target.dependsOn,
                                    if (isV4) null else sourceId,
                                    if (isV4) null else target.id?.value,
                                )
                            }
                        }
                    }
                val targetConcept = TargetConcept(targetList, targetText)

                mutableMap.computeIfAbsent(sourceConcept) { mutableListOf() }.add(targetConcept)
            }
        }
        return mutableMap
    }

    private fun getValueSetData(filename: String): List<TargetValueSetValue> {
        val valueSet =
            try {
                JacksonUtil.readJsonObject(ociClient.getObjectFromINFX(filename)!!, ValueSet::class)
            } catch (e: Exception) {
                logger.warn { "Error parsing ValueSet from $filename ${e.message}" }
                return emptyList()
            }
        // squish ValueSet into more usable form
        return valueSet.expansion?.contains?.mapNotNull {
            val targetSystem = it.system?.value
            val targetVersion = it.version?.value
            val targetCode = it.code?.value
            val targetDisplay = it.display?.value
            if (targetSystem == null || targetVersion == null || targetCode == null || targetDisplay == null) {
                logger.warn { "Skipping value in ValueSet from $filename as required property is null $it" }
                null
            } else {
                TargetValueSetValue(targetCode, targetSystem, targetDisplay, targetVersion)
            }
        } ?: emptyList()
    }
}

internal data class CacheKey(
    val registryType: RegistryType,
    val elementName: String,
    // non-null for ConceptMap
    val tenantId: String? = null,
    // null for ConceptMap
    val profileUrl: String? = null,
)

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
internal data class NormalizationRegistryItem(
    val registryUuid: String,
    // i.e. 'Appointment.status'
    val dataElement: String,
    val filename: String,
    val version: String? = null,
    // non-null for ConceptMap
    val sourceExtensionUrl: String? = null,
    // i.e. 'Appointment' - repeated in data_element
    val resourceType: String,
    // null applies to all tenants
    val tenantId: String? = null,
    val profileUrl: String? = null,
    val conceptMapName: String? = null,
    val conceptMapUuid: String? = null,
    val valueSetName: String? = null,
    val valueSetUuid: String? = null,
    val registryEntryType: RegistryType,
)

enum class RegistryType(
    @JsonValue val value: String,
) {
    CONCEPT_MAP("concept_map"),
    VALUE_SET("value_set"),
}

internal data class ConceptMapItem(
    val map: Map<SourceConcept, List<TargetConcept>>,
    // non-null for ConceptMap
    val sourceExtensionUrl: String,
    val metadata: List<ConceptMapMetadata>,
)

internal data class ValueSetItem(
    val set: List<TargetValueSetValue>?,
    val metadata: ValueSetMetadata,
)

internal data class TargetValueSetValue(
    val value: String,
    val system: String,
    val display: String,
    val version: String,
)

internal data class TargetConceptMapValue(
    val value: String,
    val system: String,
    val display: String,
    val version: String,
    val dependsOn: List<ConceptMapDependsOn> = emptyList(),
    val sourceId: String? = null,
    val targetId: String? = null,
)

internal data class SourceConcept(
    val code: Code? = null,
    val codeableConcept: CodeableConcept? = null,
)

internal data class TargetConcept(val element: List<TargetConceptMapValue>, val text: String? = null) {
    fun getSourceId(): FHIRString? = element.first().sourceId?.let { FHIRString(it) }
}

internal fun CodeableConcept.normalized(): CodeableConcept =
    this.copy(
        coding =
            this.coding.map { it.copy(userSelected = null) }
                .sortedWith(
                    compareBy<Coding> { it.system?.value }.thenBy { it.code?.value }
                        .thenBy { it.display?.value }.thenBy { it.version?.value },
                ),
    )
