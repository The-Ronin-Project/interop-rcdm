# interop-rcdm

## Layout

This project is divided into multiple projects involved in the RCDM process.

* [interop-rcdm-common](interop-rcdm-common) contains the shared functionality needed across 2 or more projects
* [interop-rcdm-registry](interop-rcdm-registry) contains the functionality needed for reading the INFX-managed Data
  Normalization Registry.
* [interop-rcdm-transform](interop-rcdm-transform) contains our Transformation logic that converts R4 resources into the
  appropriate Ronin-defined profiles.
* [interop-rcdm-validate](interop-rcdm-validate) contains our Validation logic that enforces the RCDM profiles.
* [interop-fhir-ronin-generators](interop-fhir-ronin-generators) contains our RCDM data generators.

## Updating profiles

Ensure that you add the
latest [RCDMVersion](interop-rcdm-common/src/main/kotlin/com/projectronin/interop/rcdm/common/enums/RCDMVersion.kt).

For any profiles you update, update their RCDM version and update the profile-specific version for transform.

## Adding support for new profiles

### Common

All profiles are defined
in [RoninProfile](interop-rcdm-common/src/main/kotlin/com/projectronin/interop/rcdm/common/enums/RoninProfile.kt).
You will need to add a new entry here for the profile(s) you are adding.

If the profile includes new extensions, they should be added
to [RoninExtension](interop-rcdm-common/src/main/kotlin/com/projectronin/interop/rcdm/common/enums/RoninExtension.kt).

### Transformers

When adding support for new profiles, you will need to create a
new [ProfileTransformer](interop-rcdm-transform/src/main/kotlin/com/projectronin/interop/rcdm/transform/profile/ProfileTransformer.kt)
for each
profile. Each ProfileTransformer should define the key elements of transforming to the profile except for Meta, which is
handling by the base implementation.

If the new profile requires concept mapping, you should create
a [ResourceMapper](interop-rcdm-transform/src/main/kotlin/com/projectronin/interop/rcdm/transform/map/ResourceMapper.kt)
for the resource where you map against the Data Normalization Registry.

There are
also [ElementMappers](interop-rcdm-transform/src/main/kotlin/com/projectronin/interop/rcdm/transform/map/ElementMapper.kt)
that may be needed if there are common datatypes that need to be consistently mapped. One such example
is [ContactPoint](interop-rcdm-transform/src/main/kotlin/com/projectronin/interop/rcdm/transform/map/element/ContactPointMapper.kt).

### Validators

You will also need to create a
new [ProfileValidator](interop-rcdm-validate/src/main/kotlin/com/projectronin/interop/rcdm/validate/profile/ProfileValidator.kt)
for each profile.

If the profile contains an element that needs consistent validation throughout our RCDM profiles, you should create
an [ElementValidator](interop-rcdm-validate/src/main/kotlin/com/projectronin/interop/rcdm/validate/element/ElementValidator.kt)
for them.
