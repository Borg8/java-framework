package borg.framework.compability;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.CLASS)
public @interface Contract
{
	boolean pure() default false;

	String value() default "";
}
