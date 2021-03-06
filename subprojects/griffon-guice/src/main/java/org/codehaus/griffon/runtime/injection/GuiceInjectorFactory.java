/*
 * Copyright 2008-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.griffon.runtime.injection;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import griffon.core.ApplicationEvent;
import griffon.core.GriffonApplication;
import griffon.core.artifact.GriffonArtifact;
import griffon.core.injection.Binding;
import griffon.core.injection.Injector;
import griffon.core.injection.InjectorFactory;
import org.codehaus.griffon.runtime.core.injection.InjectorProvider;
import org.kordamp.jipsy.ServiceProviderFor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ServiceLoader;

import static com.google.inject.util.Providers.guicify;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static org.codehaus.griffon.runtime.injection.GuiceInjector.moduleFromBindings;
import static org.codehaus.griffon.runtime.injection.MethodUtils.invokeAnnotatedMethod;

/**
 * @author Andres Almiray
 * @since 2.0.0
 */
@ServiceProviderFor(InjectorFactory.class)
public class GuiceInjectorFactory implements InjectorFactory {
    private static final Logger LOG = LoggerFactory.getLogger(GuiceInjectorFactory.class);

    @Nonnull
    @Override
    public GuiceInjector createInjector(@Nonnull GriffonApplication application, @Nonnull Iterable<Binding<?>> bindings) {
        requireNonNull(application, "Argument 'application' must not be null");
        requireNonNull(bindings, "Argument 'bindings' must not be null");
        InjectorProvider injectorProvider = new InjectorProvider();
        GuiceInjector injector = createModules(application, injectorProvider, bindings);
        injectorProvider.setInjector(injector);
        return injector;
    }

    private GuiceInjector createModules(final @Nonnull GriffonApplication application, @Nonnull final InjectorProvider injectorProvider, @Nonnull Iterable<Binding<?>> bindings) {
        final InjectionListener<GriffonArtifact> injectionListener = new InjectionListener<GriffonArtifact>() {
            @Override
            public void afterInjection(GriffonArtifact injectee) {
                application.getEventRouter().publishEvent(
                    ApplicationEvent.NEW_INSTANCE.getName(),
                    asList(injectee.getClass(), injectee)
                );
            }
        };

        final InjectionListener<Object> postConstructorInjectorListener = new InjectionListener<Object>() {
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
            @Override
            public void afterInjection(Object injectee) {
                invokeAnnotatedMethod(injectee, PostConstruct.class);
            }
        };

        Module injectorModule = new AbstractModule() {
            @Override
            protected void configure() {
                bind(Injector.class)
                    .toProvider(guicify(injectorProvider))
                    .in(Singleton.class);

                bindListener(new AbstractMatcher<TypeLiteral<?>>() {
                                 public boolean matches(TypeLiteral<?> typeLiteral) {
                                     return GriffonArtifact.class.isAssignableFrom(typeLiteral.getRawType());
                                 }
                             }, new TypeListener() {
                                 @SuppressWarnings("unchecked")
                                 @Override
                                 public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
                                     if (GriffonArtifact.class.isAssignableFrom(type.getRawType())) {
                                         TypeEncounter<GriffonArtifact> artifactEncounter = (TypeEncounter<GriffonArtifact>) encounter;
                                         artifactEncounter.register(injectionListener);
                                     }
                                 }
                             }
                );

                bindListener(Matchers.any(), new TypeListener() {
                    @Override
                    public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
                        encounter.register(postConstructorInjectorListener);
                    }
                });
            }
        };

        Collection<Module> modules = new ArrayList<>();
        modules.add(injectorModule);
        modules.add(moduleFromBindings(bindings));

        ServiceLoader<Module> moduleLoader = ServiceLoader.load(Module.class, getClass().getClassLoader());
        for (Module module : moduleLoader) {
            LOG.trace("Adding module {}", module);
            modules.add(module);
        }

        com.google.inject.Injector injector = Guice.createInjector(modules);
        return new GuiceInjector(injector);
    }
}
