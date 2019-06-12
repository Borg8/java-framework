package borg.framework.serializers;

import borg.framework.compability.Contract;

public interface Typed<T extends REntity>
{
	@Contract(pure = true)
	Class<? extends T> entityClass();
}
