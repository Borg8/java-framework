package borg.framework.serializers;

import org.jetbrains.annotations.Contract;

import borg.framework.auxiliaries.BinaryParser;

public interface RTyped<T extends BinaryParser.BinarySerializable>
{
	@Contract(pure = true)
	Class<? extends T> entityClass();
}
