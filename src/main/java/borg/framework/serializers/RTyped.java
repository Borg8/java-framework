package borg.framework.serializers;

import org.jetbrains.annotations.Contract;

public interface RTyped<T extends REntity>
{
	@Contract(pure = true)
	Class<? extends T> entityClass();
}
