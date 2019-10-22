package org.meveo.apiv2.models;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@Value.Immutable
@JsonSerialize
public interface Cause{
    @Nullable
    String getCauseMessage();
}