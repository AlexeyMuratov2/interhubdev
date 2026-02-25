/**
 * Port interfaces implemented by adapters (other modules). Exposed as a named interface
 * so that the adapter module can depend on "subject :: port" and implement these ports
 * without referencing non-exposed types.
 */
@org.springframework.modulith.NamedInterface("port")
package com.example.interhubdev.subject.port;
