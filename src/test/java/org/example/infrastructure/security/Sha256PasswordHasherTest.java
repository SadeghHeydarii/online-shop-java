package org.example.infrastructure.security;

import org.example.service.port.PasswordHasher;

class Sha256PasswordHasherTest extends PasswordHasherContractTest {

    @Override
    protected PasswordHasher createHasher() {
        return new Sha256PasswordHasher();
    }
}
