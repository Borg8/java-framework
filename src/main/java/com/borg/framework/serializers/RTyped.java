package com.borg.framework.serializers;

import org.jetbrains.annotations.Contract;

import com.borg.framework.auxiliaries.BinaryParser;

public interface RTyped<T extends BinaryParser.BinarySerializable>
{
	@Contract(pure = true)
	Class<? extends T> entityClass();
}
