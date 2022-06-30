package com.symphony.bdk.bdd.mock;

public interface BddMockService<T> {
  T mockedInstance();
  Class<T> mockedClazz();
}
