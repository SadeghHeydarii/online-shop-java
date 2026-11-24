package org.example.infrastructure.persistence;

import org.example.service.port.UserRepository;

class InMemoryUserRepositoryTest extends UserRepositoryContractTest {

    @Override
    protected UserRepository createRepository() {
        return new InMemoryUserRepository();
    }
}
