package borg.framework.serializers;

import org.jetbrains.annotations.Contract;

public interface Typed<T extends REntity>
{
	@Contract(pure = true)
	Class<? extends T> entityClass();
}
