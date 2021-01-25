package borg.framework.serializers;

import org.jetbrains.annotations.Contract;

public interface BTyped<T extends BEntity>
{
	@Contract(pure = true)
	Class<? extends T> entityClass();
}
