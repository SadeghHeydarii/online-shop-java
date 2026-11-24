package org.example.infrastructure.persistence;

import org.example.service.port.CartRepository;

class InMemoryCartRepositoryTest extends CartRepositoryContractTest {

    @Override
    protected CartRepository createRepository() {
        return new InMemoryCartRepository();
    }
}
