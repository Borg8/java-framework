package borg.framework.serializers;

import org.jetbrains.annotations.CheckReturnValue;

import borg.framework.auxiliaries.BinaryParser;

public interface RTyped<T extends BinaryParser.BinarySerializable>
{
	@CheckReturnValue
	Class<? extends T> entityClass();
}
