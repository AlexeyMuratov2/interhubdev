package com.example.interhubdev.email;

import java.util.List;
import java.util.Map;

/**
 * Immutable email message data.
 * Use the builder for convenient construction.
 *
 * @param to          recipient email address (required)
 * @param subject     email subject (required)
 * @param textBody    plain text content (optional if htmlBody is provided)
 * @param htmlBody    HTML content (optional if textBody is provided)
 * @param cc          carbon copy recipients (optional)
 * @param bcc         blind carbon copy recipients (optional)
 * @param replyTo     reply-to address (optional, uses from if not specified)
 * @param templateName template name for templated emails (optional)
 * @param templateVars template variables (optional, used with templateName)
 */
public record EmailMessage(
    String to,
    String subject,
    String textBody,
    String htmlBody,
    List<String> cc,
    List<String> bcc,
    String replyTo,
    String templateName,
    Map<String, Object> templateVars
) {
    /**
     * Creates a simple text email.
     */
    public static EmailMessage text(String to, String subject, String body) {
        return builder()
                .to(to)
                .subject(subject)
                .textBody(body)
                .build();
    }

    /**
     * Creates a simple HTML email.
     */
    public static EmailMessage html(String to, String subject, String htmlBody) {
        return builder()
                .to(to)
                .subject(subject)
                .htmlBody(htmlBody)
                .build();
    }

    /**
     * Creates a templated email.
     */
    public static EmailMessage templated(String to, String subject, String templateName, Map<String, Object> vars) {
        return builder()
                .to(to)
                .subject(subject)
                .templateName(templateName)
                .templateVars(vars)
                .build();
    }

    /**
     * Creates a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Check if this message has HTML content.
     */
    public boolean hasHtml() {
        return htmlBody != null && !htmlBody.isBlank();
    }

    /**
     * Check if this message uses a template.
     */
    public boolean isTemplated() {
        return templateName != null && !templateName.isBlank();
    }

    /**
     * Builder for EmailMessage.
     */
    public static class Builder {
        private String to;
        private String subject;
        private String textBody;
        private String htmlBody;
        private List<String> cc = List.of();
        private List<String> bcc = List.of();
        private String replyTo;
        private String templateName;
        private Map<String, Object> templateVars = Map.of();

        public Builder to(String to) {
            this.to = to;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder textBody(String textBody) {
            this.textBody = textBody;
            return this;
        }

        public Builder htmlBody(String htmlBody) {
            this.htmlBody = htmlBody;
            return this;
        }

        public Builder cc(List<String> cc) {
            this.cc = cc != null ? List.copyOf(cc) : List.of();
            return this;
        }

        public Builder cc(String... cc) {
            this.cc = cc != null ? List.of(cc) : List.of();
            return this;
        }

        public Builder bcc(List<String> bcc) {
            this.bcc = bcc != null ? List.copyOf(bcc) : List.of();
            return this;
        }

        public Builder bcc(String... bcc) {
            this.bcc = bcc != null ? List.of(bcc) : List.of();
            return this;
        }

        public Builder replyTo(String replyTo) {
            this.replyTo = replyTo;
            return this;
        }

        public Builder templateName(String templateName) {
            this.templateName = templateName;
            return this;
        }

        public Builder templateVars(Map<String, Object> templateVars) {
            this.templateVars = templateVars != null ? Map.copyOf(templateVars) : Map.of();
            return this;
        }

        public EmailMessage build() {
            if (to == null || to.isBlank()) {
                throw new IllegalArgumentException("Recipient (to) is required");
            }
            if (subject == null || subject.isBlank()) {
                throw new IllegalArgumentException("Subject is required");
            }
            if ((textBody == null || textBody.isBlank()) 
                && (htmlBody == null || htmlBody.isBlank())
                && (templateName == null || templateName.isBlank())) {
                throw new IllegalArgumentException("Either textBody, htmlBody, or templateName is required");
            }
            return new EmailMessage(to, subject, textBody, htmlBody, cc, bcc, replyTo, templateName, templateVars);
        }
    }
}
