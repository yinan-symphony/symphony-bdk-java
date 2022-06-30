package com.symphony.bdk.bdd.mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MockServicesFactory {

  Map<Class<?>, BddMockService<?>> mockServicesMap;

  public MockServicesFactory(@Autowired List<BddMockService<?>> mockServices) {
    mockServicesMap = mockServices.stream().collect(Collectors.toMap(BddMockService::mockedClazz, Function.identity()));
  }

  public <T> T mockService(Class<T> clz) {
    return (T) this.mockServicesMap.get(clz).mockedInstance();
  }
}
