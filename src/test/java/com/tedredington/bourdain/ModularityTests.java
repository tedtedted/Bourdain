package com.tedredington.bourdain;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Verifies the module boundaries declared in each package-info: no cycles, no
 * reach-ins past a module's API, only the listed dependencies. Also refreshes
 * the C4/PlantUML docs under {@code target/spring-modulith-docs}.
 */
class ModularityTests {

    private final ApplicationModules modules = ApplicationModules.of(BourdainApplication.class);

    @Test
    void moduleStructureIsValid() {
        modules.verify();
    }

    @Test
    void writeDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
