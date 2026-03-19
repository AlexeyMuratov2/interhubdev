package com.example.interhubdev.storedfile.internal.policy;

import com.example.interhubdev.storedfile.DeliveryContext;
import com.example.interhubdev.storedfile.FileSafetyClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Evaluates whether delivery is allowed: intersection of file's FileSafetyClass and requested DeliveryContext.
 * New context cannot escalate trust; only same or stricter mode allowed.
 */
@Component
@Slf4j
public class DeliveryPolicyEvaluator {

    /**
     * Check if the file's safety class allows the requested delivery context.
     *
     * @param safetyClass   file's assigned safety class (must be non-null for ACTIVE files)
     * @param deliveryContext requested delivery context
     * @return true if delivery is allowed
     */
    public boolean isDeliveryAllowed(FileSafetyClass safetyClass, DeliveryContext deliveryContext) {
        if (safetyClass == null) {
            log.warn("Delivery check: safetyClass is null, denying");
            return false;
        }
        if (deliveryContext == null) {
            log.warn("Delivery check: deliveryContext is null, denying");
            return false;
        }
        boolean allowed = switch (safetyClass) {
            case GENERAL_USER_ATTACHMENT_ONLY -> deliveryContext == DeliveryContext.ATTACHMENT_ONLY;
        };
        if (!allowed) {
            log.debug("Delivery denied: safetyClass={}, deliveryContext={}", safetyClass, deliveryContext);
        }
        return allowed;
    }
}
