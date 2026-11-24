package org.example.infrastructure.persistence;

import org.example.service.port.OrderRepository;

class InMemoryOrderRepositoryTest extends OrderRepositoryContractTest {

    @Override
    protected OrderRepository createRepository() {
        return new InMemoryOrderRepository();
    }
}
