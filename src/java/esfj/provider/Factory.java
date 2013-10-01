package esfj.provider;

import java.lang.annotation.*;
import javax.inject.Qualifier;
import clojure.lang.IFn;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@Qualifier
public @interface Factory {
}
