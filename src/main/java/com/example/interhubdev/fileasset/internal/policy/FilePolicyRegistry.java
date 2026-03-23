package com.example.interhubdev.fileasset.internal.policy;

import com.example.interhubdev.fileasset.FilePolicyKey;
import com.example.interhubdev.fileasset.internal.FileAssetErrors;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves current and fixed policy definitions by key and version.
 */
@Component
public class FilePolicyRegistry {

    private final Map<PolicyRef, FileSecurityPolicy> policiesByRef;
    private final Map<FilePolicyKey, FileSecurityPolicy> currentPolicies;

    public FilePolicyRegistry(List<FileSecurityPolicy> policies) {
        this.policiesByRef = policies.stream()
            .collect(Collectors.toUnmodifiableMap(
                policy -> new PolicyRef(policy.key(), policy.version()),
                Function.identity(),
                (left, right) -> {
                    throw new IllegalStateException("Duplicate file policy definition for "
                        + left.key() + " version " + left.version());
                }
            ));

        this.currentPolicies = policies.stream()
            .collect(Collectors.toUnmodifiableMap(
                FileSecurityPolicy::key,
                Function.identity(),
                (left, right) -> Comparator.comparingInt(FileSecurityPolicy::version).compare(left, right) >= 0 ? left : right
            ));
    }

    public FileSecurityPolicy resolveCurrent(FilePolicyKey key) {
        FileSecurityPolicy policy = currentPolicies.get(key);
        if (policy == null) {
            throw FileAssetErrors.unsupportedPolicy(key);
        }
        return policy;
    }

    public FileSecurityPolicy resolveExact(FilePolicyKey key, int version) {
        FileSecurityPolicy policy = policiesByRef.get(new PolicyRef(key, version));
        if (policy == null) {
            throw FileAssetErrors.policyDefinitionMissing(key, version);
        }
        return policy;
    }

    private record PolicyRef(FilePolicyKey key, int version) {
    }
}
