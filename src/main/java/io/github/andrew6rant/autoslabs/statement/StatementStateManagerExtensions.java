package io.github.andrew6rant.autoslabs.statement;

import net.minecraft.state.State;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;

import java.util.Collection;
import java.util.Map;

public interface StatementStateManagerExtensions<O, S extends State<O, S>> {
    boolean statement_addProperty(Property<?> property, Object defaultValue);
    Collection<S> statement_reconstructStateList(Map<Property<?>, Collection<?>> propertyValues);
} 