package org.geysermc.floodgate.core.scope;

import io.micronaut.context.annotation.Requires;
import jakarta.inject.Named;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Requires(property = "platform.integrated", value = "true")
@Named
public @interface IntegratedOnly {
}
