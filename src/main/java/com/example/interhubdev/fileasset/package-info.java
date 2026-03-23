/**
 * Fileasset module - future owner of file lifecycle, storage, upload security, classification,
 * processing and final status.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Own {@link com.example.interhubdev.fileasset.FileAssetApi} and the canonical
 *   {@code FileAsset} lifecycle.</li>
 *   <li>Keep all physical storage coordinates internal to the module.</li>
 *   <li>Resolve and execute file security policies selected by external business modules via
 *   {@link com.example.interhubdev.fileasset.FilePolicyKey}.</li>
 *   <li>Manage expiration, orphan cleanup, background processing and final activation status.</li>
 * </ul>
 *
 * <h2>Boundaries</h2>
 * <ul>
 *   <li>Business modules choose only {@link com.example.interhubdev.fileasset.FilePolicyKey}.</li>
 *   <li>Business modules do not pass business identifiers, rule fragments or storage hints.</li>
 *   <li>All policy execution stays inside this module.</li>
 *   <li>Storage coordinates never leave this module's public API.</li>
 * </ul>
 *
 * <h2>Legacy coexistence</h2>
 * <p>
 * {@code stored_file} remains the legacy storage metadata model during migration. This module is
 * introduced in parallel and does not change existing HTTP contracts yet.
 * </p>
 */
@org.springframework.modulith.ApplicationModule(
    displayName = "Fileasset",
    allowedDependencies = {"error", "outbox"}
)
package com.example.interhubdev.fileasset;
