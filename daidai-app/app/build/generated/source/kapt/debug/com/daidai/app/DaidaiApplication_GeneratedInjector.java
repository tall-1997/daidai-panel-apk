package com.daidai.app;

import dagger.hilt.InstallIn;
import dagger.hilt.codegen.OriginatingElement;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.internal.GeneratedEntryPoint;

@OriginatingElement(
    topLevelClass = DaidaiApplication.class
)
@GeneratedEntryPoint
@InstallIn(SingletonComponent.class)
public interface DaidaiApplication_GeneratedInjector {
  void injectDaidaiApplication(DaidaiApplication daidaiApplication);
}
